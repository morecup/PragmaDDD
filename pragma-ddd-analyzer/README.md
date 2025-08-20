# Pragma DDD Analyzer

这个模块提供了一个字节码分析工具，用于在 AspectJ 修改字节码之前分析被 `@AggregateRoot` 注解的类的属性访问情况。

## 功能特性

- 在 AspectJ 处理之前分析字节码
- 检测 `@AggregateRoot` 注解的类
- 分析方法内的属性访问（读取和写入）
- 支持直接字段访问和 getter/setter 方法调用
- 支持分析单个类文件、JAR 文件或目录
- 输出详细的分析报告

## 使用方法

### 1. 构建分析器

```bash
./gradlew :pragma-ddd-analyzer:build
```

### 2. 运行分析器

#### 方式一：使用 Gradle 应用插件

```bash
./gradlew :pragma-ddd-analyzer:run --args="build/classes/kotlin/main"
```

#### 方式二：使用可执行 JAR

```bash
java -jar pragma-ddd-analyzer/build/libs/pragma-ddd-analyzer.jar build/classes/kotlin/main
```

#### 方式三：直接运行主类

```bash
java -cp pragma-ddd-analyzer/build/libs/pragma-ddd-analyzer.jar \
  org.morecup.pragmaddd.analyzer.standalone.StandaloneAnalyzer \
  build/classes/kotlin/main
```

### 3. 分析不同类型的输入

```bash
# 分析目录
java -jar analyzer.jar build/classes/kotlin/main

# 分析单个类文件
java -jar analyzer.jar MyClass.class

# 分析 JAR 文件
java -jar analyzer.jar myapp.jar

# 分析多个路径
java -jar analyzer.jar build/classes/kotlin/main build/classes/java/main
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
        "modifiedProperties": ["name", "lastModified"]
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

分析器可以检测以下类型的属性访问：

1. **直接字段访问**
   - `GETFIELD` 指令（读取字段）
   - `PUTFIELD` 指令（写入字段）

2. **方法调用**
   - `getXxx()` 方法（getter）
   - `isXxx()` 方法（boolean getter）
   - `setXxx()` 方法（setter）

## 注意事项

- 只分析被 `@AggregateRoot` 注解的类
- 必须在 AspectJ 处理之前运行
- 需要先编译类文件才能进行分析