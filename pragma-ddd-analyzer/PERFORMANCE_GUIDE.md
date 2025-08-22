# Pragma DDD Analyzer - Performance Guide

This guide provides information on optimizing the performance of the Pragma DDD Analyzer for large projects.

## Performance Configuration Options

### Memory Management

The analyzer provides several configuration options to manage memory usage:

```kotlin
pragmaDddAnalyzer {
    // Limit the number of classes analyzed per compilation to prevent memory issues
    maxClassesPerCompilation.set(500) // Default: 1000
    
    // Disable specific analysis features to reduce memory usage
    enableMethodAnalysis.set(false)      // Disables method body analysis
    enablePropertyAnalysis.set(false)    // Disables property access detection
    enableDocumentationExtraction.set(false) // Disables KDoc extraction
}
```

### Analysis Feature Control

You can selectively disable analysis features to improve performance:

#### Method Analysis
- **What it does**: Analyzes method bodies for method calls and property access
- **Performance impact**: High - requires IR traversal of method bodies
- **When to disable**: For large codebases where method-level analysis isn't needed

```kotlin
pragmaDddAnalyzer {
    enableMethodAnalysis.set(false)
}
```

#### Property Analysis
- **What it does**: Detects property access patterns within methods
- **Performance impact**: Medium - requires method body traversal
- **When to disable**: When you only need class structure information
- **Note**: Requires `enableMethodAnalysis` to be true

```kotlin
pragmaDddAnalyzer {
    enablePropertyAnalysis.set(false)
}
```

#### Documentation Extraction
- **What it does**: Extracts KDoc comments from classes, methods, and properties
- **Performance impact**: Low - minimal overhead
- **When to disable**: When documentation metadata isn't needed

```kotlin
pragmaDddAnalyzer {
    enableDocumentationExtraction.set(false)
}
```

## Performance Monitoring

### Built-in Performance Metrics

The analyzer provides built-in performance monitoring:

```
DDD Analyzer: Found 150 total classes in module (45ms)
DDD Analyzer: Found 12 DDD-annotated classes for analysis in main sources (8ms)
DDD Analyzer: Successfully analyzed class com.example.User (23ms)
DDD Analyzer: Completed analysis of 12 classes (287ms)
```

### Memory Usage Tracking

The analyzer tracks approximate memory usage and provides warnings:

```
DDD Analyzer: High memory usage detected: 52MB
```

### Debug Mode

Enable debug mode for detailed performance information:

```bash
./gradlew build -Dddd.analyzer.debug=true
```

## Optimization Strategies

### For Large Projects (1000+ classes)

1. **Reduce class limit per compilation**:
   ```kotlin
   pragmaDddAnalyzer {
       maxClassesPerCompilation.set(500)
   }
   ```

2. **Disable unnecessary analysis features**:
   ```kotlin
   pragmaDddAnalyzer {
       enableMethodAnalysis.set(false)
       enablePropertyAnalysis.set(false)
   }
   ```

3. **Use selective compilation**:
   - Only apply the plugin to modules that contain DDD classes
   - Exclude test sources if not needed:
   ```kotlin
   pragmaDddAnalyzer {
       includeTestSources.set(false)
   }
   ```

### For CI/CD Environments

1. **Fail fast on errors**:
   ```kotlin
   pragmaDddAnalyzer {
       failOnAnalysisErrors.set(true)
   }
   ```

2. **Optimize output directory**:
   ```kotlin
   pragmaDddAnalyzer {
       outputDirectory.set("build/generated/ddd-analysis")
   }
   ```

### For Development Environments

1. **Enable all features for comprehensive analysis**:
   ```kotlin
   pragmaDddAnalyzer {
       enableMethodAnalysis.set(true)
       enablePropertyAnalysis.set(true)
       enableDocumentationExtraction.set(true)
   }
   ```

2. **Use descriptive JSON file naming**:
   ```kotlin
   pragmaDddAnalyzer {
       jsonFileNaming.set("${project.name}-ddd-analysis")
   }
   ```

## Performance Benchmarks

### Typical Performance Characteristics

| Project Size | Classes | Analysis Time | Memory Usage |
|-------------|---------|---------------|--------------|
| Small (< 50 classes) | 5-10 DDD classes | < 100ms | < 10MB |
| Medium (50-200 classes) | 10-30 DDD classes | 100-500ms | 10-30MB |
| Large (200-1000 classes) | 30-100 DDD classes | 500ms-2s | 30-100MB |
| Very Large (1000+ classes) | 100+ DDD classes | 2s+ | 100MB+ |

### Optimization Impact

| Configuration | Time Reduction | Memory Reduction |
|--------------|----------------|------------------|
| Disable method analysis | 40-60% | 30-50% |
| Disable property analysis | 20-30% | 15-25% |
| Disable documentation | 5-10% | 5-10% |
| Reduce class limit | Variable | 20-40% |

## Troubleshooting Performance Issues

### High Memory Usage

**Symptoms**: 
- OutOfMemoryError during compilation
- Very slow compilation times
- High memory usage warnings

**Solutions**:
1. Reduce `maxClassesPerCompilation`
2. Disable method and property analysis
3. Increase JVM heap size: `-Xmx4g`

### Slow Compilation

**Symptoms**:
- Compilation takes significantly longer with the plugin
- Analysis time > 5 seconds for medium projects

**Solutions**:
1. Profile with `--profile` flag
2. Disable unnecessary analysis features
3. Check for complex method bodies in DDD classes

### Analysis Errors

**Symptoms**:
- Classes not being analyzed
- Missing metadata in JSON output

**Solutions**:
1. Enable debug mode: `-Dddd.analyzer.debug=true`
2. Check for annotation detection issues
3. Verify class accessibility

## Best Practices

### Plugin Configuration

1. **Start with minimal configuration**:
   ```kotlin
   pragmaDddAnalyzer {
       // Use defaults initially
   }
   ```

2. **Gradually enable features as needed**:
   ```kotlin
   pragmaDddAnalyzer {
       enableMethodAnalysis.set(true) // Add when needed
   }
   ```

3. **Monitor performance impact**:
   - Use `--profile` flag
   - Check build times before/after
   - Monitor memory usage

### Code Organization

1. **Minimize DDD class complexity**:
   - Keep method bodies simple
   - Avoid deep inheritance hierarchies
   - Use composition over inheritance

2. **Organize packages efficiently**:
   - Group related DDD classes
   - Avoid circular dependencies
   - Use clear naming conventions

### Build Configuration

1. **Parallel builds**:
   ```kotlin
   org.gradle.parallel=true
   org.gradle.configureondemand=true
   ```

2. **Build cache**:
   ```kotlin
   org.gradle.caching=true
   ```

3. **Daemon optimization**:
   ```kotlin
   org.gradle.daemon=true
   org.gradle.jvmargs=-Xmx4g -XX:+UseG1GC
   ```

## Advanced Configuration

### Custom Error Handling

```kotlin
pragmaDddAnalyzer {
    failOnAnalysisErrors.set(false) // Continue on errors
}
```

### Output Customization

```kotlin
pragmaDddAnalyzer {
    outputDirectory.set("custom/output/path")
    jsonFileNaming.set("custom-analysis")
}
```

### Multi-Module Projects

```kotlin
// In root build.gradle.kts
subprojects {
    apply(plugin = "org.morecup.pragmaddd.analyzer")
    
    configure<PragmaDddAnalyzerExtension> {
        maxClassesPerCompilation.set(200) // Smaller limit for submodules
        jsonFileNaming.set("${project.name}-ddd")
    }
}
```