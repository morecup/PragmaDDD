# Pragma DDD Analyzer

这是一个 Gradle 插件，使用 Kotlin 编译器插件方式，在 Kotlin 编译期分析被 DDD 注解的类（@AggregateRoot、@DomainEntity、@ValueObj），并生成包含类结构、方法调用和属性访问模式的 JSON 元数据文件。

## 功能特性

- **编译期分析**：在 Kotlin 编译过程中自动分析 DDD 注解类
- **多注解支持**：支持 @AggregateRoot、@DomainEntity、@ValueObj 注解
- **全面元数据收集**：
  - 类名、包名、注解类型
  - 所有属性名称和类型
  - 所有方法签名和参数
  - 方法内的属性访问模式（读取/写入）
  - 方法内的方法调用关系
  - 文档注释和注解信息
- **JSON 输出**：生成结构化的 JSON 文件，便于后续处理
- **JAR 分发**：JSON 文件随 JAR 包一起分发
- **测试支持**：支持为测试源码生成独立的 JSON 文件

## 快速开始

### 1. 应用插件

在你的 `build.gradle.kts` 中添加：

```kotlin
plugins {
    kotlin("jvm")
    id("org.morecup.pragmaddd.pragma-ddd-analyzer")
}
```

### 2. 配置插件（可选）

```kotlin
pragmaDddAnalyzer {
    outputDirectory.set("build/generated/ddd-analysis")
    jsonFileNaming.set("my-ddd-analysis")
    includeTestSources.set(true)
    enableMethodAnalysis.set(true)
    enablePropertyAnalysis.set(true)
    enableDocumentationExtraction.set(true)
}
```

### 3. 运行分析

```bash
# 分析 DDD 类并生成 JSON
./gradlew analyzeDddClasses

# 或者在构建过程中自动生成
./gradlew build
```

## 示例输出

生成的 JSON 文件包含完整的类元数据：

```json
{
  "generatedAt": "2025-08-21T22:32:37.741",
  "sourceType": "main",
  "classes": [
    {
      "className": "User",
      "packageName": "com.example.demo.domain",
      "annotationType": "AGGREGATE_ROOT",
      "properties": [
        {
          "name": "id",
          "type": "String",
          "isPrivate": true,
          "isMutable": false,
          "documentation": {
            "summary": "User unique identifier"
          }
        }
      ],
      "methods": [
        {
          "name": "updateProfile",
          "parameters": [
            {
              "name": "newName",
              "type": "String"
            }
          ],
          "returnType": "Unit",
          "methodCalls": [
            {
              "targetMethod": "isNotBlank",
              "receiverType": "String"
            }
          ],
          "propertyAccesses": [
            {
              "propertyName": "name",
              "accessType": "READ",
              "ownerClass": "com.example.demo.domain.User"
            },
            {
              "propertyName": "name",
              "accessType": "WRITE",
              "ownerClass": "com.example.demo.domain.User"
            }
          ],
          "documentation": {
            "summary": "Update user profile information",
            "description": "Accesses and modifies multiple properties"
          }
        }
      ]
    }
  ]
}
```

## 配置选项

| 选项 | 类型 | 默认值 | 描述 |
|------|------|--------|------|
| `outputDirectory` | String | `"build/generated/ddd-analysis"` | JSON 文件输出目录 |
| `jsonFileNaming` | String | `"ddd-analysis"` | JSON 文件名前缀 |
| `includeTestSources` | Boolean | `true` | 是否分析测试源码 |
| `enableMethodAnalysis` | Boolean | `true` | 是否启用方法分析 |
| `enablePropertyAnalysis` | Boolean | `true` | 是否启用属性分析 |
| `enableDocumentationExtraction` | Boolean | `true` | 是否提取文档注释 |

## 支持的注解

- `@AggregateRoot`：聚合根
- `@DomainEntity`：领域实体
- `@ValueObj`：值对象

## 输出文件

- **主源码**：`{jsonFileNaming}-main.json`
- **测试源码**：`{jsonFileNaming}-test.json`
- **位置**：`META-INF/pragma-ddd/` 目录下，随 JAR 包分发

## 开发和测试

### 构建插件

```bash
./gradlew :pragma-ddd-analyzer:build
```

### 运行测试

```bash
./gradlew :pragma-ddd-analyzer:test
```

### 发布到本地仓库

```bash
./gradlew :pragma-ddd-analyzer:publishToMavenLocal
```

### 运行示例分析

```bash
./gradlew :pragma-ddd-analyzer:run
```

## 架构设计

插件采用模块化设计：

- **注解检测器**：识别 DDD 注解
- **类分析器**：分析类结构和属性
- **方法分析器**：分析方法调用和属性访问
- **文档提取器**：提取 KDoc 注释
- **JSON 生成器**：生成结构化输出
- **Gradle 集成**：提供 Gradle 任务和配置

## 注意事项

- 需要 Kotlin 2.1.0 或更高版本
- 仅分析带有指定 DDD 注解的类
- JSON 文件会自动包含在构建产物中
- 支持增量编译