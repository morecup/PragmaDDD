package org.morecup.pragmaddd.analyzer.source

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.comments.JavadocComment
import java.io.File
import java.io.FileInputStream

/**
 * Java 源码文档注释提取器
 * 使用 JavaParser 解析 Java 源码中的 Javadoc 注释
 */
class JavaDocumentationExtractor : SourceDocumentationExtractor {
    
    private val javaParser = JavaParser()
    
    override fun supports(sourceFile: File): Boolean {
        return sourceFile.extension.equals("java", ignoreCase = true)
    }
    
    override fun extractDocumentation(sourceFile: File): SourceDocumentationInfo? {
        return try {
            FileInputStream(sourceFile).use { inputStream ->
                val parseResult = javaParser.parse(inputStream)
                
                if (parseResult.isSuccessful && parseResult.result.isPresent) {
                    val compilationUnit = parseResult.result.get()
                    extractFromCompilationUnit(compilationUnit)
                } else {
                    println("解析 Java 文件失败: ${sourceFile.absolutePath}")
                    parseResult.problems.forEach { problem ->
                        println("  问题: ${problem.message}")
                    }
                    null
                }
            }
        } catch (e: Exception) {
            println("读取 Java 文件失败: ${sourceFile.absolutePath}, 错误: ${e.message}")
            null
        }
    }
    
    private fun extractFromCompilationUnit(compilationUnit: CompilationUnit): SourceDocumentationInfo? {
        // 查找主要的类声明
        val primaryClass = compilationUnit.primaryType.orElse(null)
        if (primaryClass !is ClassOrInterfaceDeclaration) {
            return null
        }
        
        val packageName = compilationUnit.packageDeclaration
            .map { it.nameAsString }
            .orElse("")
        
        val className = if (packageName.isNotEmpty()) {
            "$packageName.${primaryClass.nameAsString}"
        } else {
            primaryClass.nameAsString
        }
        
        // 提取类文档注释
        val classDocumentation = primaryClass.javadocComment
            .map { extractJavadocContent(it) }
            .orElse(null)
        
        // 提取字段文档注释
        val fieldDocumentations = mutableMapOf<String, String>()
        primaryClass.fields.forEach { field ->
            field.javadocComment.ifPresent { javadoc ->
                field.variables.forEach { variable ->
                    fieldDocumentations[variable.nameAsString] = extractJavadocContent(javadoc)
                }
            }
        }
        
        // 提取方法文档注释
        val methodDocumentations = mutableMapOf<String, String>()
        primaryClass.methods.forEach { method ->
            method.javadocComment.ifPresent { javadoc ->
                val methodSignature = buildMethodSignature(method)
                methodDocumentations[methodSignature] = extractJavadocContent(javadoc)
            }
        }
        
        return SourceDocumentationInfo(
            className = className,
            classDocumentation = classDocumentation,
            fieldDocumentations = fieldDocumentations,
            methodDocumentations = methodDocumentations
        )
    }
    
    /**
     * 提取 Javadoc 注释的内容
     */
    private fun extractJavadocContent(javadoc: JavadocComment): String {
        return javadoc.content.trim()
    }
    
    /**
     * 构建方法签名，用于匹配
     */
    private fun buildMethodSignature(method: MethodDeclaration): String {
        val parameters = method.parameters.joinToString(",") { param ->
            param.type.asString()
        }
        return "${method.nameAsString}($parameters)"
    }
}
