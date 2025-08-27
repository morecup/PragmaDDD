# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## ai talk rule
- 如果正在开发gradle插件，记得考虑到gradle缓存的情况，使用--no-build-cache参数忽略掉缓存的影响
- 编写的代码需要准确 简洁明了 可读性强
- 不允许使用类似todo的方式不去实现代码

## Project Overview

This is PragmaDDD-cc, a Domain-Driven Design (DDD) toolkit for Kotlin and Java that provides:
- Static code analysis for DDD aggregate roots
- Runtime aspect weaving for enhanced DDD capabilities  
- Integration with ORM frameworks like Jimmer
- Code generation and symbol processing

## Multi-Module Architecture

The project consists of several Gradle modules:

### Core Modules
- **pragma-ddd-core**: Core annotations (`@AggregateRoot`, `@DomainEntity`, `@ValueObject`) and runtime infrastructure
- **pragma-ddd-analyzer**: Bytecode analysis plugin that analyzes property access patterns in aggregate roots
- **pragma-ddd-aspect**: AspectJ aspects for method/field access tracking and constructor enhancement
- **pragma-ddd-jimmer**: Integration with Jimmer ORM framework
- **pragma-ddd-ksp**: Kotlin Symbol Processing integration (development stage)

### Support Modules
- **demo**: Example project demonstrating usage patterns
- **buildSrc**: Build configuration and shared Gradle scripts

## Common Development Commands

### Build and Test
```bash
# Build entire project
./gradlew build

# Build specific module
./gradlew :pragma-ddd-core:build

# Run all tests
./gradlew test

# Run tests for specific module  
./gradlew :pragma-ddd-analyzer:test
```

### Analysis and Development
```bash
# Run DDD analyzer on demo project
./gradlew :demo:build

# Run analyzer standalone on compiled classes
./gradlew :pragma-ddd-analyzer:run --args="build/classes/kotlin/main"

# Clean and rebuild
./gradlew clean build
```

### Publishing (if configured)
```bash
# Publish to local repository
./gradlew publishToMavenLocal
```

## Key Architecture Concepts

### Analyzer Plugin System
The `pragma-ddd-analyzer` module provides a Gradle plugin that:
- Automatically analyzes bytecode after compilation
- Tracks property access patterns in `@AggregateRoot` classes
- Records method calls across all classes (not just current class)
- Generates JSON analysis reports in `build/generated/pragmaddd/{sourceSet}/resources/META-INF/pragma-ddd-analyzer/domain-analyzer.json`
- Supports Lambda expression analysis and functional interface detection

### Analysis Output Structure
The analyzer produces JSON with this structure:
```json
{
  "className": "com.example.User",
  "isAggregateRoot": true,
  "methods": [
    {
      "methodName": "updateProfile", 
      "methodDescriptor": "(Ljava/lang/String;)V",
      "accessedProperties": ["name", "email"],
      "modifiedProperties": ["name"],
      "calledMethods": [
        {
          "className": "java.lang.System",
          "methodName": "currentTimeMillis",
          "callCount": 1,
          "associatedLambdas": []
        }
      ],
      "lambdaExpressions": []
    }
  ]
}
```

### Annotation System
- `@AggregateRoot`: Marks domain aggregate roots for analysis
- `@DomainEntity`: Marks domain entities
- `@ValueObject`/`@ValueObj`: Marks value objects
- `@OrmObject`, `@OrmField`, `@OrmFields`: ORM integration annotations

### Aspect Integration
Uses AspectJ for runtime enhancement:
- `AggregateRootConstructorAspect`: Enhances aggregate construction
- `FieldAccessAspect`: Tracks field access patterns
- `MethodAccessAspect`: Tracks method invocations

## File Organization Patterns

### Source Structure
- Kotlin code: `src/main/kotlin/org/morecup/pragmaddd/{module}/`
- Java code: `src/main/java/` (limited use)
- Tests: `src/test/kotlin/org/morecup/pragmaddd/{module}/`
- Resources: `src/main/resources/`

### Build Configuration
- Root: `settings.gradle.kts`, `gradle.properties`
- Module builds: `{module}/build.gradle.kts`
- Shared config: `buildSrc/src/main/kotlin/` (includes `java-jvm.gradle.kts`, `kotlin-jvm.gradle.kts`)

## Development Guidelines

### Code Analysis Focus
When working with this codebase:
1. Always consider DDD principles and aggregate boundaries
2. The analyzer is the core value - ensure changes maintain bytecode analysis accuracy
3. Test both compile-time (analysis) and runtime (aspect) behaviors
4. Preserve compatibility between Kotlin and Java codebases

### Testing Strategy
- Unit tests for individual components
- Integration tests for plugin behavior
- Demo project serves as end-to-end validation
- Use `test-mixed-strategy.sh` in demo for mixed-language testing

### Common Issues
- The analyzer requires compilation to complete before analysis
- AspectJ integration needs proper classpath configuration
- Mixed Kotlin/Java projects need special attention for metadata extraction
- Lambda expressions require specialized bytecode analysis techniques

## Key Dependencies
- Gradle 8.x with Kotlin DSL
- Kotlin 1.9+
- ASM bytecode manipulation library (for analysis)
- AspectJ (for runtime weaving)
- Jimmer ORM (for integration module)