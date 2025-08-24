package org.morecup.pragmaddd.analyzer

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.comments.JavadocComment

import java.io.File

/**
 * 源代码文档解析器
 * 使用 JavaParser 解析 Java 文件的 Javadoc
 * 使用 Kotlin PSI 解析 Kotlin 文件的 KDoc
 */
class SourceDocumentationParser {
    
    private val javaParser = JavaParser()
    private val kotlinEnvironment by lazy { createKotlinEnvironment() }
    
    /**
     * 解析源文件并提取文档信息
     */
    fun parseSourceFile(sourceFile: File, className: String): SourceDocumentationInfo? {
        return when (sourceFile.extension.lowercase()) {
            "java" -> parseJavaFile(sourceFile, className)
            "kt" -> parseKotlinFile(sourceFile, className)
            else -> null
        }
    }
    
    /**
     * 解析 Java 文件
     */
    private fun parseJavaFile(sourceFile: File, className: String): SourceDocumentationInfo? {
        return try {
            val parseResult = javaParser.parse(sourceFile)
            if (!parseResult.isSuccessful) {
                println("Failed to parse Java file: ${sourceFile.absolutePath}")
                return null
            }
            
            val compilationUnit = parseResult.result.get()
            val simpleClassName = className.substringAfterLast('.')
            
            // 查找目标类
            val classDeclaration = compilationUnit.findAll(ClassOrInterfaceDeclaration::class.java)
                .firstOrNull { it.nameAsString == simpleClassName }
                ?: return null
            
            // 提取类文档
            val classDoc = classDeclaration.javadocComment.orElse(null)?.content?.trim()
            
            // 提取字段文档
            val fieldDocs = mutableMapOf<String, String>()
            classDeclaration.fields.forEach { field ->
                field.javadocComment.ifPresent { javadoc ->
                    field.variables.forEach { variable ->
                        fieldDocs[variable.nameAsString] = javadoc.content.trim()
                    }
                }
            }
            
            // 提取方法文档
            val methodDocs = mutableMapOf<String, String>()
            classDeclaration.methods.forEach { method ->
                method.javadocComment.ifPresent { javadoc ->
                    val methodKey = "${method.nameAsString}${method.signature.asString()}"
                    methodDocs[methodKey] = javadoc.content.trim()
                }
            }
            
            SourceDocumentationInfo(
                className = className,
                classDocumentation = classDoc,
                fieldDocumentations = fieldDocs,
                methodDocumentations = methodDocs
            )
        } catch (e: Exception) {
            println("Error parsing Java file ${sourceFile.absolutePath}: ${e.message}")
            null
        }
    }
    
    /**
     * 解析 Kotlin 文件
     * 使用改进的正则表达式解析 KDoc，但更加精确
     */
    private fun parseKotlinFile(sourceFile: File, className: String): SourceDocumentationInfo? {
        return try {
            val content = sourceFile.readText()
            val simpleClassName = className.substringAfterLast('.')
            
            // 提取类文档
            val classDoc = extractKotlinClassDoc(content, simpleClassName)
            
            // 提取属性文档
            val fieldDocs = extractKotlinFieldDocs(content)
            
            // 提取方法文档
            val methodDocs = extractKotlinMethodDocs(content)
            
            SourceDocumentationInfo(
                className = className,
                classDocumentation = classDoc,
                fieldDocumentations = fieldDocs,
                methodDocumentations = methodDocs
            )
        } catch (e: Exception) {
            println("Error parsing Kotlin file ${sourceFile.absolutePath}: ${e.message}")
            null
        }
    }
    
    /**
     * 提取 Kotlin 类文档
     */
    private fun extractKotlinClassDoc(content: String, className: String): String? {
        val lines = content.lines()
        // 改进的正则表达式，支持各种修饰符（open, abstract, sealed, data, etc.）
        val classPattern = Regex("\\b(?:open\\s+|abstract\\s+|sealed\\s+|data\\s+|inner\\s+|enum\\s+|annotation\\s+)*(class|interface|object)\\s+$className\\b")
        
        val classLineIndex = lines.indexOfFirst { line ->
            classPattern.containsMatchIn(line.trim())
        }
        
        if (classLineIndex == -1) return null
        
        return extractKDocFromLines(lines, classLineIndex)
    }
    
    /**
     * 提取 Kotlin 属性文档
     */
    private fun extractKotlinFieldDocs(content: String): Map<String, String> {
        val fieldDocs = mutableMapOf<String, String>()
        val lines = content.lines()
        
        // 匹配属性定义：val/var name: Type 或构造函数参数
        val fieldPattern = Regex("\\b(val|var)\\s+(\\w+)\\s*:")
        val paramPattern = Regex("\\s*(\\w+)\\s*:")
        
        lines.forEachIndexed { index, line ->
            // 匹配类属性
            fieldPattern.find(line.trim())?.let { match ->
                val fieldName = match.groupValues[2]
                extractKDocFromLines(lines, index)?.let { doc ->
                    fieldDocs[fieldName] = doc
                }
            }
            
            // 匹配构造函数参数（在构造函数上下文中）
            if (line.trim().contains("(") && !line.trim().startsWith("fun")) {
                paramPattern.find(line.trim())?.let { match ->
                    val paramName = match.groupValues[1]
                    extractKDocFromLines(lines, index)?.let { doc ->
                        fieldDocs[paramName] = doc
                    }
                }
            }
        }
        
        return fieldDocs
    }
    
    /**
     * 提取 Kotlin 方法文档
     */
    private fun extractKotlinMethodDocs(content: String): Map<String, String> {
        val methodDocs = mutableMapOf<String, String>()
        val lines = content.lines()
        
        val methodPattern = Regex("\\bfun\\s+(\\w+)\\s*\\(")
        
        lines.forEachIndexed { index, line ->
            methodPattern.find(line.trim())?.let { match ->
                val methodName = match.groupValues[1]
                extractKDocFromLines(lines, index)?.let { doc ->
                    methodDocs[methodName] = doc
                }
            }
        }
        
        return methodDocs
    }
    
    /**
     * 从指定行向上提取 KDoc 内容
     */
    private fun extractKDocFromLines(lines: List<String>, targetLineIndex: Int): String? {
        val docLines = mutableListOf<String>()
        var currentIndex = targetLineIndex - 1
        var inDocComment = false
        
        while (currentIndex >= 0) {
            val line = lines[currentIndex].trim()
            
            if (line.endsWith("*/")) {
                inDocComment = true
                val content = line.removeSuffix("*/").trim()
                if (content.isNotEmpty()) {
                    docLines.add(0, content)
                }
            } else if (line.startsWith("/**")) {
                if (inDocComment) {
                    val content = line.removePrefix("/**").trim()
                    if (content.isNotEmpty()) {
                        docLines.add(0, content)
                    }
                    break
                }
            } else if (line.startsWith("*") && inDocComment) {
                val content = line.removePrefix("*").trim()
                if (content.isNotEmpty()) {
                    docLines.add(0, content)
                }
            } else if (line.isEmpty() && inDocComment) {
                // 空行，继续
            } else if (inDocComment) {
                // 遇到非文档注释行，停止
                break
            } else if (line.startsWith("//")) {
                // 单行注释，跳过
            } else if (line.startsWith("@")) {
                // 注解，跳过继续查找
            } else if (line.isNotEmpty()) {
                // 遇到其他代码，停止查找
                break
            }
            
            currentIndex--
        }
        
        return if (docLines.isNotEmpty()) {
            docLines.joinToString("\n").trim()
        } else {
            null
        }
    }
    
    /**
     * 创建 Kotlin 编译环境（暂时不使用，保留接口兼容性）
     */
    private fun createKotlinEnvironment(): Any {
        // 简化实现，避免复杂的 PSI 环境配置
        return Any()
    }
}

/**
 * 源代码文档信息
 */
data class SourceDocumentationInfo(
    val className: String,
    val classDocumentation: String?,
    val fieldDocumentations: Map<String, String>,
    val methodDocumentations: Map<String, String>
)