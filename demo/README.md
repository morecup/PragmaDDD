# Pragma DDD Analyzer Demo

这个demo模块演示了如何使用Pragma DDD Analyzer来分析DDD聚合根类的属性访问模式。

## 项目结构

```
demo/
├── src/main/kotlin/com/example/demo/domain/
│   ├── User.kt      # 用户聚合根 - 演示复杂的属性访问模式
│   ├── Order.kt     # 订单聚合根 - 演示集合属性和状态管理
│   └── Product.kt   # 产品聚合根 - 演示简单的属性访问模式
└── build.gradle.kts # 配置了Pragma DDD Analyzer插件
```

## 如何使用

### 1. 应用插件

在你的 `build.gradle.kts` 中添加插件：

```kotlin
plugins {
    kotlin("jvm")
    id("org.morecup.pragma-ddd-analyzer")
}

dependencies {
    implementation("org.morecup:pragma-ddd-core:1.0.0")
}
```

### 2. 自动分析

插件会自动集成到构建流程中，当你运行以下任何命令时都会自动执行分析：

```bash
# 构建项目（会自动运行分析）
./gradlew build

# 编译类文件（会自动运行分析）
./gradlew classes

# 分析会在编译时自动运行
# 编译项目即可触发 DDD 分析
./gradlew build
```

### 3. 查看分析结果

分析完成后，结果会自动保存在 `build/reports/pragma-ddd-analysis.json` 文件中。

### 4. 可选配置

如果需要自定义配置，可以在 `build.gradle.kts` 中添加：

```kotlin
pragmaDddAnalyzer {
    verbose.set(true)                                    // 启用详细输出
    outputFormat.set("JSON")                             // 输出格式：JSON 或 TXT
    outputFile.set("build/reports/my-ddd-analysis.json") // 自定义输出文件
}
```

### 5. 零配置使用

对于大多数项目，你只需要应用插件即可，无需任何额外配置：

```kotlin
plugins {
    kotlin("jvm")
    id("org.morecup.pragma-ddd-analyzer")
}
```

插件会：
- 自动检测编译输出目录
- 在构建时自动运行分析
- 将结果保存到标准位置
- 提供合理的默认配置

## 分析内容

Pragma DDD Analyzer会分析以下内容：

1. **属性访问模式**：哪些方法访问了哪些属性
2. **属性修改模式**：哪些方法修改了哪些属性
3. **方法签名**：完整的方法描述符

## 示例聚合根说明

### User.kt
- 演示了复杂的用户管理场景
- 包含多种属性访问和修改模式
- 方法包括：更新资料、激活/停用、年龄增长、数据验证等

### Order.kt
- 演示了订单管理的复杂业务逻辑
- 包含集合属性的操作
- 状态机模式的实现
- 金额计算和订单项管理

### Product.kt
- 演示了简单的产品管理
- 基本的属性访问和修改
- 库存管理逻辑

## 预期分析结果

分析器会识别出：
- 每个 `@AggregateRoot` 类的所有方法
- 每个方法访问和修改的属性
- 属性访问的模式和频率

这些信息可以帮助：
- 理解聚合根的内部结构
- 识别潜在的设计问题
- 优化属性访问模式
- 进行代码重构决策