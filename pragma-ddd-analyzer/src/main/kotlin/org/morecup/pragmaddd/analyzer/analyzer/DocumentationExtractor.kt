package org.morecup.pragmaddd.analyzer.analyzer

import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.fileEntry
import org.morecup.pragmaddd.analyzer.model.DocumentationMetadata

/**
 * Interface for extracting documentation from IR declarations
 */
interface DocumentationExtractor {
    fun extractClassDocumentation(irClass: IrClass): DocumentationMetadata?
    fun extractMethodDocumentation(irFunction: IrSimpleFunction): DocumentationMetadata?
    fun extractPropertyDocumentation(irProperty: IrProperty): DocumentationMetadata?
    fun extractFieldDocumentation(irField: IrField): DocumentationMetadata?
    fun parseKDoc(kdocText: String): DocumentationMetadata
}

/**
 * Implementation of DocumentationExtractor that extracts KDoc information from IR declarations
 * 
 * Note: KDoc information is not directly available in Kotlin IR. This implementation provides
 * a framework for documentation extraction that can be enhanced with source file access or
 * PSI integration in future versions.
 */
class DocumentationExtractorImpl : DocumentationExtractor {
    
    /**
     * Extracts class-level documentation from KDoc comments
     */
    override fun extractClassDocumentation(irClass: IrClass): DocumentationMetadata? {
        val docComment = getDocumentationFromDeclaration(irClass)
        return if (docComment != null) {
            parseKDoc(docComment)
        } else {
            null
        }
    }
    
    /**
     * Extracts method documentation from KDoc comments
     */
    override fun extractMethodDocumentation(irFunction: IrSimpleFunction): DocumentationMetadata? {
        val docComment = getDocumentationFromDeclaration(irFunction)
        return if (docComment != null) {
            parseKDoc(docComment)
        } else {
            null
        }
    }
    
    /**
     * Extracts property documentation from KDoc comments
     */
    override fun extractPropertyDocumentation(irProperty: IrProperty): DocumentationMetadata? {
        val docComment = getDocumentationFromDeclaration(irProperty)
        return if (docComment != null) {
            parseKDoc(docComment)
        } else {
            null
        }
    }
    
    /**
     * Extracts field documentation from KDoc comments
     */
    override fun extractFieldDocumentation(irField: IrField): DocumentationMetadata? {
        val docComment = getDocumentationFromDeclaration(irField)
        return if (docComment != null) {
            parseKDoc(docComment)
        } else {
            null
        }
    }
    
    /**
     * Parses KDoc text and extracts structured documentation information
     * 
     * Supports standard KDoc tags:
     * - Summary (first line)
     * - Description (subsequent lines before tags)
     * - @param paramName description
     * - @return description
     */
    override fun parseKDoc(kdocText: String): DocumentationMetadata {
        if (kdocText.isBlank()) {
            return DocumentationMetadata(null, null, emptyMap(), null)
        }
        
        val cleanedText = cleanKDocText(kdocText)
        val lines = cleanedText.lines().filter { it.isNotBlank() }
        
        if (lines.isEmpty()) {
            return DocumentationMetadata(null, null, emptyMap(), null)
        }
        
        var summary: String? = null
        val descriptionLines = mutableListOf<String>()
        val parameters = mutableMapOf<String, String>()
        var returnDescription: String? = null
        
        var currentSection = "content"
        var isFirstContentLine = true
        
        for (line in lines) {
            when {
                line.startsWith("@param") -> {
                    currentSection = "param"
                    parseParamTag(line, parameters)
                }
                line.startsWith("@return") -> {
                    currentSection = "return"
                    returnDescription = line.removePrefix("@return").trim()
                }
                line.startsWith("@") -> {
                    // Other tags - ignore for now but stop processing content
                    currentSection = "other"
                }
                currentSection == "content" -> {
                    if (isFirstContentLine) {
                        summary = line
                        isFirstContentLine = false
                    } else {
                        descriptionLines.add(line)
                    }
                }
                currentSection == "return" -> {
                    // Multi-line return description
                    returnDescription = (returnDescription ?: "") + " " + line
                }
            }
        }
        
        val description = if (descriptionLines.isNotEmpty()) {
            descriptionLines.joinToString(" ")
        } else {
            null
        }
        
        return DocumentationMetadata(
            summary = summary,
            description = description,
            parameters = parameters,
            returnDescription = returnDescription?.trim()
        )
    }
    
    /**
     * Attempts to extract documentation from IR declaration
     * 
     * Note: In the current Kotlin IR implementation, KDoc is not directly accessible.
     * This method provides a framework for future enhancement with source file access.
     */
    private fun getDocumentationFromDeclaration(declaration: IrDeclaration): String? {
        // Strategy 1: Try to access source file information
        try {
            val sourceFile = declaration.file
            val fileEntry = declaration.fileEntry
            
            // In a full implementation, we would:
            // 1. Read the source file content
            // 2. Parse the file to find KDoc comments
            // 3. Match comments to declarations by position
            
            // For now, return null as KDoc is not directly available in IR
            return null
        } catch (e: Exception) {
            // Source file access may not be available in all contexts
            return null
        }
    }
    
    /**
     * Cleans KDoc text by removing comment markers and normalizing whitespace
     */
    private fun cleanKDocText(kdocText: String): String {
        return kdocText
            .lines()
            .map { line ->
                line.trim()
                    .removePrefix("/**")
                    .removePrefix("/*")
                    .removeSuffix("*/")
                    .removePrefix("*")
                    .trim()
            }
            .joinToString("\n")
            .trim()
    }
    
    /**
     * Parses @param tag and extracts parameter name and description
     */
    private fun parseParamTag(line: String, parameters: MutableMap<String, String>) {
        val paramContent = line.removePrefix("@param").trim()
        val spaceIndex = paramContent.indexOf(' ')
        
        if (spaceIndex > 0) {
            val paramName = paramContent.substring(0, spaceIndex).trim()
            val paramDescription = paramContent.substring(spaceIndex + 1).trim()
            
            if (paramName.isNotEmpty() && paramDescription.isNotEmpty()) {
                parameters[paramName] = paramDescription
            }
        }
    }
}

