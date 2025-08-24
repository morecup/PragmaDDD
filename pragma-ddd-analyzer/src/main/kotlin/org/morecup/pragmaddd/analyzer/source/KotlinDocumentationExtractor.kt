package org.morecup.pragmaddd.analyzer.source

import java.io.File
import java.util.regex.Pattern

/**
 * Kotlin 源码文档注释提取器
 * 使用简单的正则表达式解析 Kotlin 源码中的 KDoc 注释
 *
 * 注意：这是一个简化的实现，使用正则表达式而不是完整的 PSI 解析
 * 对于复杂的 Kotlin 代码可能不够准确，但对于基本的文档注释提取是足够的
 */
class KotlinDocumentationExtractor : SourceDocumentationExtractor {

    override fun supports(sourceFile: File): Boolean {
        return sourceFile.extension.equals("kt", ignoreCase = true)
    }

    override fun extractDocumentation(sourceFile: File): SourceDocumentationInfo? {
        return try {
            val sourceCode = sourceFile.readText()
            extractFromSourceCode(sourceCode, sourceFile.nameWithoutExtension)
        } catch (e: Exception) {
            println("解析 Kotlin 文件失败: ${sourceFile.absolutePath}, 错误: ${e.message}")
            null
        }
    }

    private fun extractFromSourceCode(sourceCode: String, fileName: String): SourceDocumentationInfo? {
        // 提取包名
        val packagePattern = Pattern.compile("package\\s+([\\w.]+)")
        val packageMatcher = packagePattern.matcher(sourceCode)
        val packageName = if (packageMatcher.find()) packageMatcher.group(1) else ""

        // 提取类名
        val classPattern = Pattern.compile("(?:/\\*\\*[\\s\\S]*?\\*/\\s*)?(?:@\\w+\\s*)*(?:data\\s+)?class\\s+(\\w+)")
        val classMatcher = classPattern.matcher(sourceCode)
        if (!classMatcher.find()) {
            return null
        }

        val simpleClassName = classMatcher.group(1)
        val fullClassName = if (packageName.isNotEmpty()) {
            "$packageName.$simpleClassName"
        } else {
            simpleClassName
        }

        // 提取类文档注释
        val classDocumentation = extractClassDocumentation(sourceCode, simpleClassName)

        // 提取字段文档注释
        val fieldDocumentations = extractFieldDocumentations(sourceCode)

        // 提取方法文档注释
        val methodDocumentations = extractMethodDocumentations(sourceCode)

        return SourceDocumentationInfo(
            className = fullClassName,
            classDocumentation = classDocumentation,
            fieldDocumentations = fieldDocumentations,
            methodDocumentations = methodDocumentations
        )
    }

    private fun extractClassDocumentation(sourceCode: String, className: String): String? {
        val pattern = Pattern.compile("/\\*\\*([\\s\\S]*?)\\*/\\s*(?:@\\w+\\s*)*(?:data\\s+)?class\\s+$className")
        val matcher = pattern.matcher(sourceCode)
        return if (matcher.find()) {
            cleanDocComment(matcher.group(1))
        } else null
    }

    private fun extractFieldDocumentations(sourceCode: String): Map<String, String> {
        val fieldDocs = mutableMapOf<String, String>()
        val pattern = Pattern.compile("/\\*\\*([\\s\\S]*?)\\*/\\s*(?:val|var)\\s+(\\w+)")
        val matcher = pattern.matcher(sourceCode)

        while (matcher.find()) {
            val doc = cleanDocComment(matcher.group(1))
            val fieldName = matcher.group(2)
            if (doc.isNotBlank()) {
                fieldDocs[fieldName] = doc
            }
        }

        return fieldDocs
    }

    private fun extractMethodDocumentations(sourceCode: String): Map<String, String> {
        val methodDocs = mutableMapOf<String, String>()
        val pattern = Pattern.compile("/\\*\\*([\\s\\S]*?)\\*/\\s*fun\\s+(\\w+)\\s*\\(([^)]*)\\)")
        val matcher = pattern.matcher(sourceCode)

        while (matcher.find()) {
            val doc = cleanDocComment(matcher.group(1))
            val methodName = matcher.group(2)
            val params = matcher.group(3)

            if (doc.isNotBlank()) {
                // 简化的方法签名
                val signature = "$methodName($params)"
                methodDocs[signature] = doc
                // 也添加简单的方法名映射
                methodDocs[methodName] = doc
            }
        }

        return methodDocs
    }

    private fun cleanDocComment(rawComment: String): String {
        return rawComment
            .lines()
            .joinToString("\n") { line ->
                line.trim().removePrefix("*").trim()
            }
            .trim()
    }
}
