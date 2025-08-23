# Pragma DDD Analyzer 改进总结

## 改进内容

本次改进为 `pragma-ddd-analyzer` 模块增加了对 **test源集** 的支持，现在插件可以同时分析 main 和 test 源集中的 `@AggregateRoot` 类。

## 主要变更

### 1. 插件核心逻辑改进

**文件**: `pragma-ddd-analyzer/src/main/kotlin/org/morecup/pragmaddd/analyzer/PragmaDddAnalyzerPlugin.kt`

- **之前**: 只处理 `main` 源集的编译任务
- **现在**: 同时处理 `main` 和 `test` 源集的编译任务

```kotlin
// 之前的代码
val mainSourceSet = sourceSets.findByName("main")
if (mainSourceSet != null) {
    // 只配置main源集
}

// 改进后的代码
listOf("main", "test").forEach { sourceSetName ->
    val sourceSet = sourceSets.findByName(sourceSetName)
    if (sourceSet != null) {
        // 配置main和test源集
    }
}
```

### 2. 资源处理任务配置

- **增强了 `processTestResources` 任务的依赖配置**
- **确保test源集的编译任务正确依赖于对应的编译任务**

### 3. 输出路径分离

- **main源集输出**: `build/generated/pragmaddd/main/resources/META-INF/pragma-ddd-analyzer/domain-analyzer.json`
- **test源集输出**: `build/generated/pragmaddd/test/resources/META-INF/pragma-ddd-analyzer/domain-analyzer.json`

### 4. 测试覆盖

**新增测试文件**:
- `PragmaDddAnalyzerPluginTest.kt` - 插件功能测试
- `IntegrationTest.kt` - 集成测试

测试覆盖了以下场景：
- 插件正确应用
- main和test源集任务配置
- 输出路径正确性
- 源集名称配置
- 空结果处理

## 使用示例

### 1. 插件配置

```kotlin
plugins {
    kotlin("jvm")
    id("org.morecup.pragmaddd.pragma-ddd-analyzer")
}

pragmaDddAnalyzer {
    verbose.set(true)  // 启用详细输出
}
```

### 2. 源码结构示例

```
src/
├── main/kotlin/com/example/
│   └── User.kt                    // @AggregateRoot 类
└── test/kotlin/com/example/
    └── TestUser.kt               // @AggregateRoot 测试类
```

### 3. 编译后的输出

```
build/
└── generated/pragmaddd/
    ├── main/resources/META-INF/pragma-ddd-analyzer/
    │   └── domain-analyzer.json   // main源集分析结果
    └── test/resources/META-INF/pragma-ddd-analyzer/
        └── domain-analyzer.json   // test源集分析结果
```

## 技术细节

### 1. 源集检测

插件现在会自动检测项目中的 `main` 和 `test` 源集，并为每个源集配置相应的分析任务。

### 2. 任务依赖

- `processResources` 依赖于 `compileJava` 和 `compileKotlin`
- `processTestResources` 依赖于 `compileTestJava` 和 `compileTestKotlin`

### 3. 输出管理

每个源集的分析结果独立存储，避免相互覆盖，便于区分main代码和测试代码的分析结果。

## 兼容性

- **向后兼容**: 现有的main源集功能完全保持不变
- **新功能**: 增加了test源集支持，无需额外配置
- **自动检测**: 如果项目没有test源集，插件会自动跳过相关配置

## 验证方法

1. **运行测试**: `./gradlew :pragma-ddd-analyzer:test`
2. **应用到项目**: 在包含test源集的项目中应用插件
3. **检查输出**: 验证两个不同的JSON文件是否生成

## 文档更新

- 更新了 `pragma-ddd-analyzer/README.md`
- 添加了test源集支持的说明
- 更新了配置示例和输出路径说明

## 总结

此次改进显著增强了 `pragma-ddd-analyzer` 的功能，使其能够全面分析项目中的所有 `@AggregateRoot` 类，包括测试代码中的聚合根。这对于确保测试代码的领域模型设计一致性具有重要意义。