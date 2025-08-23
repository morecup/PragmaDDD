# Pragma DDD Analyzer

这个模块提供了一个字节码分析工具，用于分析被 `@AggregateRoot` 注解的类的属性访问情况。

## 功能特性

- 分析编译后的字节码文件
- 检测 `@AggregateRoot` 注解的类
- **分析所有方法，包括没有属性访问或方法调用的方法**
- **记录所有方法调用，不限于当前类的方法**
- **Lambda 表达式分析和关联**
  - 检测方法中的 Lambda 表达式
  - 分析 Lambda 与方法调用的关联关系
  - 识别函数式接口类型
- 分析方法内的属性访问（读取和写入）
- 支持直接字段访问和 getter/setter 方法调用
- 支持分析单个类文件、JAR 文件或目录
- **支持main和test源集的独立分析**
- 输出详细的分析报告

## 使用方法

### 1. 作为Gradle插件使用（推荐）

在项目的 `build.gradle.kts` 中应用插件：

```kotlin
plugins {
    kotlin("jvm")
    id("org.morecup.pragmaddd.pragma-ddd-analyzer")
}

// 可选配置
pragmaDddAnalyzer {
    verbose.set(true)  // 启用详细输出
}
```

插件会自动在编译时分析代码，并生成分析报告：
- **main源集**: `build/generated/pragmaddd/main/resources/META-INF/pragma-ddd-analyzer/domain-analyzer.json`
- **test源集**: `build/generated/pragmaddd/test/resources/META-INF/pragma-ddd-analyzer/domain-analyzer.json`

### 2. 独立分析器使用

#### 构建分析器

```bash
./gradlew :pragma-ddd-analyzer:build
```

#### 运行分析器

##### 方式一：使用 Gradle 应用插件

```bash
./gradlew :pragma-ddd-analyzer:run --args="build/classes/kotlin/main"
```

##### 方式二：使用可执行 JAR

```bash
java -jar pragma-ddd-analyzer/build/libs/pragma-ddd-analyzer.jar build/classes/kotlin/main
```

##### 方式三：直接运行主类

```bash
java -cp pragma-ddd-analyzer/build/libs/pragma-ddd-analyzer.jar \
  org.morecup.pragmaddd.analyzer.standalone.StandaloneAnalyzer \
  build/classes/kotlin/main
```

### 3. 分析不同类型的输入

```bash
# 分析main源集目录
java -jar analyzer.jar build/classes/kotlin/main

# 分析test源集目录
java -jar analyzer.jar build/classes/kotlin/test

# 分析单个类文件
java -jar analyzer.jar MyClass.class

# 分析 JAR 文件
java -jar analyzer.jar myapp.jar

# 分析多个路径
java -jar analyzer.jar build/classes/kotlin/main build/classes/kotlin/test
```

## 输出格式

### JSON 格式示例

```json
[
  {
    "className": "com.example.User",
    "isAggregateRoot": true,
    "methods": [
      {
        "className": "com.example.User",
        "methodName": "updateProfile",
        "methodDescriptor": "(Ljava/lang/String;)V",
        "accessedProperties": ["name", "email"],
        "modifiedProperties": ["name", "lastModified"],
        "calledMethods": [
          {
            "className": "java.lang.System",
            "methodName": "currentTimeMillis",
            "methodDescriptor": "()J",
            "callCount": 1,
            "associatedLambdas": []
          },
          {
            "className": "java.util.List",
            "methodName": "forEach",
            "methodDescriptor": "(Ljava/util/function/Consumer;)V",
            "callCount": 1,
            "associatedLambdas": [
              {
                "className": "com.example.User",
                "methodName": "lambda$updateProfile$0",
                "methodDescriptor": "(Ljava/lang/String;)V",
                "lambdaType": "java.util.function.Consumer"
              }
            ]
          },
          {
            "className": "com.example.User",
            "methodName": "validate",
            "methodDescriptor": "()Z",
            "callCount": 2,
            "associatedLambdas": []
          }
        ],
        "lambdaExpressions": [
          {
            "className": "com.example.User",
            "methodName": "lambda$updateProfile$0",
            "methodDescriptor": "(Ljava/lang/String;)V",
            "lambdaType": "java.util.function.Consumer"
          }
        ]
      },
      {
        "className": "com.example.User",
        "methodName": "getId",
        "methodDescriptor": "()Ljava/lang/Long;",
        "accessedProperties": ["id"],
        "modifiedProperties": [],
        "calledMethods": []
      }
    ]
  }
]
```

### 文本格式示例

```
Pragma DDD 分析结果
==================================================

类: com.example.User
------------------------------
  方法: updateProfile(Ljava/lang/String;)V
    访问属性: name, email
    修改属性: name, lastModified
```

## 检测能力

分析器可以检测以下内容：

1. **直接字段访问**
   - `GETFIELD` 指令（读取字段）
   - `PUTFIELD` 指令（写入字段）

2. **方法调用**
   - **所有方法调用，包括其他类的方法**
   - 记录完整的类名和方法签名
   - 统计方法调用次数
   - 支持接口方法调用

3. **方法分析**
   - **记录所有方法，包括空方法**
   - 提供完整的方法描述符
   - 区分属性访问和方法调用

4. **Lambda 表达式分析**
   - **检测 invokedynamic 指令生成的 Lambda 表达式**
   - 识别 Lambda 实现方法和函数式接口类型
   - 关联 Lambda 与调用它们的方法
   - 支持各种函数式接口（Consumer, Function, Predicate 等）

## 插件配置

### 扩展配置选项

```kotlin
pragmaDddAnalyzer {
    verbose.set(true)                                   // 启用详细日志输出
    classPaths.set(setOf("build/classes/kotlin/main"))  // 自定义类路径（通常不需要）
}
```

### 输出文件位置

- **main源集分析结果**: `build/generated/pragmaddd/main/resources/META-INF/pragma-ddd-analyzer/domain-analyzer.json`
- **test源集分析结果**: `build/generated/pragmaddd/test/resources/META-INF/pragma-ddd-analyzer/domain-analyzer.json`

生成的资源文件会自动添加到对应源集的资源路径中，可以在运行时通过类路径访问。

## 注意事项

- 只分析被 `@AggregateRoot` 注解的类
- 需要先编译类文件才能进行分析
- 插件会自动处理main和test源集的编译依赖关系
- 分析会在编译完成后自动执行，无需手动运行
- **现在会记录所有方法，即使没有属性访问或方法调用**
- **方法调用记录包含完整的类名，不仅限于当前类**

## 更新日志

### v1.2.0
- ✅ **新功能**: Lambda 表达式分析和关联
- ✅ **增强**: 检测方法中的 Lambda 表达式并关联到方法调用
- ✅ **改进**: 识别函数式接口类型和 Lambda 实现方法

### v1.1.0
- ✅ **修复**: 现在会显示所有方法，包括没有属性访问和方法调用的方法
- ✅ **增强**: 记录所有方法调用，不再过滤只记录当前类的方法
- ✅ **改进**: 方法调用信息包含完整的类名和调用次数统计