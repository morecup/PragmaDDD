package org.morecup.pragmaddd.analyzer.bytecode

import org.morecup.pragmaddd.analyzer.generator.JsonGeneratorImpl
import org.morecup.pragmaddd.analyzer.model.*
// 使用字符串匹配来检测注解，避免直接依赖
import java.io.File
import java.net.URLClassLoader
import java.util.jar.JarFile
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.javaType

/**
 * 字节码分析器 - 通过反射分析编译后的类
 */
class BytecodeAnalyzer {
    
    private val jsonGenerator = JsonGeneratorImpl()
    
    /**
     * 分析指定目录或 JAR 文件中的 DDD 类
     */
    fun analyzeClasspath(classpathEntries: List<String>): List<ClassMetadata> {
        val classLoader = createClassLoader(classpathEntries)
        val dddClasses = findDddClasses(classpathEntries, classLoader)
        
        return dddClasses.mapNotNull { kClass ->
            try {
                analyzeClass(kClass)
            } catch (e: Exception) {
                println("Error analyzing class ${kClass.qualifiedName}: ${e.message}")
                null
            }
        }
    }
    
    /**
     * 创建类加载器
     */
    private fun createClassLoader(classpathEntries: List<String>): ClassLoader {
        val urls = classpathEntries.map { entry ->
            File(entry).toURI().toURL()
        }.toTypedArray()
        
        return URLClassLoader(urls, this::class.java.classLoader)
    }
    
    /**
     * 查找所有 DDD 注解的类
     */
    private fun findDddClasses(classpathEntries: List<String>, classLoader: ClassLoader): List<KClass<*>> {
        val classes = mutableListOf<KClass<*>>()
        
        classpathEntries.forEach { entry ->
            val file = File(entry)
            when {
                file.isDirectory -> {
                    classes.addAll(findClassesInDirectory(file, classLoader))
                }
                file.name.endsWith(".jar") -> {
                    classes.addAll(findClassesInJar(file, classLoader))
                }
                file.name.endsWith(".class") -> {
                    val className = getClassNameFromFile(file, entry)
                    try {
                        val kClass = classLoader.loadClass(className).kotlin
                        if (hasDddAnnotation(kClass)) {
                            classes.add(kClass)
                        }
                    } catch (e: Exception) {
                        // 忽略加载失败的类
                    }
                }
            }
        }
        
        return classes
    }
    
    /**
     * 在目录中查找类
     */
    private fun findClassesInDirectory(directory: File, classLoader: ClassLoader): List<KClass<*>> {
        val classes = mutableListOf<KClass<*>>()
        
        directory.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".class") }
            .forEach { classFile ->
                val relativePath = classFile.relativeTo(directory).path
                val className = relativePath.replace(File.separator, ".").removeSuffix(".class")
                
                try {
                    val kClass = classLoader.loadClass(className).kotlin
                    if (hasDddAnnotation(kClass)) {
                        classes.add(kClass)
                    }
                } catch (e: Exception) {
                    // 忽略加载失败的类
                }
            }
        
        return classes
    }
    
    /**
     * 在 JAR 文件中查找类
     */
    private fun findClassesInJar(jarFile: File, classLoader: ClassLoader): List<KClass<*>> {
        val classes = mutableListOf<KClass<*>>()
        
        JarFile(jarFile).use { jar ->
            jar.entries().asSequence()
                .filter { !it.isDirectory && it.name.endsWith(".class") }
                .forEach { entry ->
                    val className = entry.name.replace("/", ".").removeSuffix(".class")
                    
                    try {
                        val kClass = classLoader.loadClass(className).kotlin
                        if (hasDddAnnotation(kClass)) {
                            classes.add(kClass)
                        }
                    } catch (e: Exception) {
                        // 忽略加载失败的类
                    }
                }
        }
        
        return classes
    }
    
    /**
     * 从文件路径获取类名
     */
    private fun getClassNameFromFile(classFile: File, basePath: String): String {
        val relativePath = classFile.relativeTo(File(basePath).parentFile).path
        return relativePath.replace(File.separator, ".").removeSuffix(".class")
    }
    
    /**
     * 检查类是否有 DDD 注解
     */
    private fun hasDddAnnotation(kClass: KClass<*>): Boolean {
        return kClass.annotations.any { annotation ->
            val annotationName = annotation.annotationClass.simpleName
            annotationName == "AggregateRoot" || 
            annotationName == "DomainEntity" || 
            annotationName == "ValueObj"
        }
    }
    
    /**
     * 分析单个类
     */
    private fun analyzeClass(kClass: KClass<*>): ClassMetadata {
        val className = kClass.simpleName ?: "Unknown"
        val packageName = kClass.qualifiedName?.substringBeforeLast(".") ?: ""
        val annotationType = getDddAnnotationType(kClass)
        
        val properties = kClass.declaredMemberProperties.map { property ->
            PropertyMetadata(
                name = property.name,
                type = property.returnType.javaType.typeName,
                isPrivate = !property.visibility.toString().contains("PUBLIC"),
                isMutable = property.isConst.not(),
                documentation = null, // 反射无法获取文档
                annotations = property.annotations.map { annotation ->
                    AnnotationMetadata(
                        name = annotation.annotationClass.simpleName ?: "Unknown",
                        parameters = emptyMap() // 简化实现
                    )
                }
            )
        }
        
        val methods = kClass.declaredMemberFunctions.map { function ->
            MethodMetadata(
                name = function.name,
                parameters = function.parameters.drop(1).map { param -> // 跳过 this 参数
                    ParameterMetadata(
                        name = param.name ?: "unknown",
                        type = param.type.javaType.typeName,
                        annotations = emptyList()
                    )
                },
                returnType = function.returnType.javaType.typeName,
                isPrivate = !function.visibility.toString().contains("PUBLIC"),
                methodCalls = emptyList(), // 字节码分析无法轻易获取方法调用
                propertyAccesses = emptyList(), // 字节码分析无法轻易获取属性访问
                documentation = null,
                annotations = function.annotations.map { annotation ->
                    AnnotationMetadata(
                        name = annotation.annotationClass.simpleName ?: "Unknown",
                        parameters = emptyMap()
                    )
                }
            )
        }
        
        return ClassMetadata(
            className = className,
            packageName = packageName,
            annotationType = annotationType,
            properties = properties,
            methods = methods,
            documentation = null,
            annotations = kClass.annotations.map { annotation ->
                AnnotationMetadata(
                    name = annotation.annotationClass.simpleName ?: "Unknown",
                    parameters = emptyMap()
                )
            }
        )
    }
    
    /**
     * 获取 DDD 注解类型
     */
    private fun getDddAnnotationType(kClass: KClass<*>): DddAnnotationType {
        kClass.annotations.forEach { annotation ->
            when (annotation.annotationClass.simpleName) {
                "AggregateRoot" -> return DddAnnotationType.AGGREGATE_ROOT
                "DomainEntity" -> return DddAnnotationType.DOMAIN_ENTITY
                "ValueObj" -> return DddAnnotationType.VALUE_OBJ
            }
        }
        return DddAnnotationType.AGGREGATE_ROOT // 默认值
    }
    
    /**
     * 分析并生成 JSON 文件
     */
    fun analyzeAndGenerateJson(
        classpathEntries: List<String>,
        outputPath: String,
        sourceType: String = "main"
    ) {
        val metadata = analyzeClasspath(classpathEntries)
        
        if (metadata.isNotEmpty()) {
            val json = jsonGenerator.generateMainSourcesJson(metadata)
            
            jsonGenerator.writeToFile(json, outputPath)
            println("Generated DDD analysis JSON at: $outputPath")
            println("Found ${metadata.size} DDD-annotated classes")
        } else {
            println("No DDD-annotated classes found in classpath")
        }
    }
}