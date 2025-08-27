我需要实现一个通用的编译期静态分析系统来替代当前的运行时堆栈跟踪分析，用于优化领域驱动设计（DDD）中的数据访问模式。

**核心目标**：
构建一个编译期分析系统，当任何 Repository 接口方法被调用时，能够准确分析调用链并确定需要加载的聚合根属性，从而优化数据库查询的 fetcher 配置。

**通用匹配规则**：
1. **聚合根识别**：
    - 使用 `@AggregateRoot` 注解标注的类
    - 每个聚合根只会有一个对应的 Repository

2. **Repository 识别**（按优先级顺序）：
    - 优先级1：继承 `DomainRepository<T>` 泛型接口，其中 T 为聚合根类型
    - 优先级2：使用 `@DomainRepository` 注解标注的接口
    - 优先级3：通过命名约定推导（如 `Goods` → `GoodsRepository`、`Goods` → `IGoodsRepository`，支持自定义命名规则配置）

3. **识别顺序**：
    - 先识别聚合根，再识别对应的 Repository

**当前问题分析**：
```kotlin
// 当前 GoodsRepositoryImpl.findByIdOrErr() 中的复杂代码
val stackTrace = Thread.currentThread().stackTrace
val analysisStackTraceElementCalledMethod = 
    analysisStackTraceElementCalledMethod(stackTraceElement, Goods::class.java)
val analysisMethodsCalledFields = analysisMethodsCalledFields(analysisStackTraceElementCalledMethod)
```

**存在的问题**：
- 运行时堆栈跟踪性能开销大，且信息不够准确
- 需要复杂的代理类过滤逻辑（CGLIB、Spring AOP 等）
- 无法在编译期进行优化
- 依赖运行时环境，调试困难

**期望的编译期解决方案**：

1. **编译期调用关系分析**：
    - 1.扫描字节码文件，识别所有调用 Repository 方法的业务代码位置
    - 2.分析并收集包含 Repository 方法调用的类和方法，排除无关代码
    - 3.分析这些方法内调用了哪些聚合根的哪些方法
    - 排除框架代理类和系统类，只关注业务逻辑调用
    - 构建完整的方法调用图谱

2. **递归字段访问分析**：
    - 收集Repository 方法最后访问了哪些聚合根的哪些方法
    - 基于之前插件分析的结果domain-analyzer.json，识别这些方法对聚合根对象的哪些字段访问（直接访问和间接访问）
    - 处理方法嵌套调用，实现循环依赖检测
    - 汇总所有需要的聚合根属性，包括嵌套对象属性，只需要read的，不需要write

3. **编译期缓存生成**：
    - 将分析结果序列化为 JSON 格式的缓存文件,需要和之前插件生成的domain-analyzer.json处于同一目录
    - 包含调用关系图：`调用方方法 → Repository方法 → 需要的属性集合`
    - 支持增量更新和缓存失效检测

4. **运行时 API 简化**：
   ```kotlin
   // 期望的简化 API
   override fun findByIdOrErr(id: Long): Goods {
       val requiredFields = CompileTimeAnalysisUtils.getRequiredFields(
           aggregateRootClass = Goods::class.java
       )
       // 基于 requiredFields 构建优化的 fetcher
       return sqlClient.findById(Goods::class, id, buildFetcher(requiredFields))
   }
   ```
5. **测试执行情况**:
    - 根据demo模块和jimmer模块内的test下的代码进行测试，确保能够正确的分析
    - 如果还不够，可以增加代码去测试各种情况

**技术实现要求**：

1. **ASM 字节码分析**：
    - 使用 ASM 分析编译后的 class 文件
    - 构建方法调用图和字段访问映射
    - 处理 Kotlin 和 Java 的语言差异
    - 支持 Lambda 表达式和方法引用的分析

2. **Gradle 插件集成**：
    - 扩展现有的 `pragma-ddd-analyzer` 插件
    - 在编译任务完成后自动执行分析
    - 生成缓存文件到 `build/generated/pragma-ddd/call-analysis.json`
    - 提供配置选项和调试模式

3. **通用性设计**：
    - 支持任意聚合根类型（不仅限于 Goods）
    - 支持多种 Repository 识别模式
    - 可配置的包名过滤和排除规则
    - 支持自定义命名约定规则

4. **性能优化**：
    - 增量分析，只处理变更的类文件
    - 缓存失效机制，确保数据一致性
    - 运行时零性能开销，直接读取预计算结果

**预期输出格式**：
```json
{
  "version": "1.0",
  "timestamp": "2024-01-01T00:00:00Z",
  "callGraph": {
    "com.example.domain.Goods": {//这是聚合根类
		"findByIdOrErr(J)Lcom/example/domain/Goods;":{//这是仓储接口的方法
		  "com.example.demo.service.changeAddressOrderCmdHandle.handle+15-20)":{//这个数字的意思是该方法开始结束所对应的源码行号范围
			  "methodClass":"com.example.demo.service.changeAddressOrderCmdHandle",//这是当前分析的方法类
			  "method":"handle",//这是当前分析的方法
			  "methodDescriptor":"()V",
			  "repository": "com.example.repository.GoodsRepository",//这是仓储接口
			  "repositoryMethod": "findByIdOrErr",//这是仓储接口的方法
			  "repositoryMethodDescriptor": "(J)Lcom/example/domain/Goods;",
			  "aggregateRoot": "com.example.domain.Goods",
			  "calledAggregateRootMethod":[{//这是当前分析的方法内调用了哪些聚合根的方法
					"aggregateRootMethod": "changeAddress",
					"aggregateRootMethodDescriptor": "(J)",
					"requiredFields":["id", "name"]//当前聚合根所访问的属性
				}]
			  "requiredFields": ["id", "name", "price", "category.name"]//当前聚合根方法所访问的属性总和
		  }
		}
	}
  }
}
```

**验证标准**：
1. 编译期能够准确识别所有 Repository 调用点（100% 覆盖率）
2. 字段访问分析结果与实际运行时需求一致（准确率 > 95%）
3. 运行时 API 调用简单，性能无明显开销（< 1ms）
4. 支持多种聚合根类型和 Repository 模式（至少支持 3 种识别模式）
5. 缓存机制稳定可靠，支持增量更新（缓存命中率 > 90%）
6. 提供详细的分析报告和调试信息
7. 支持 CI/CD 环境的自动化集成

**配置示例**：
```kotlin
// build.gradle.kts
compileTimeAnalysis {
    includePackages = listOf("com.example.**")
    excludePackages = listOf("com.example.test.**")
    repositoryNamingRules = listOf(
        "{AggregateRoot}Repository",
        "I{AggregateRoot}Repository",
        "{AggregateRoot}Repo"
    )
}
```

这个系统将彻底替代当前复杂的运行时堆栈跟踪分析，提供更准确、更高效、更可维护的字段访问模式分析能力。

再次审查，全局审查代码查找问题并修复。如果有些命令拿不准请参考官网文档。你只能在原来的框架上优化和完善绝对不能擅自精简功能或换用别的方式，导致性能损失,另外不准遗留旧代码没用代码重复代码减少冗余。