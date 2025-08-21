package org.morecup.pragmaddd.analyzer

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

/**
 * 用于读取 JAR 包中的 DDD 分析结果的工具类
 */
object AnalysisResultReader {
    
    private val mapper = jacksonObjectMapper()
    
    /**
     * 从当前类路径中读取 DDD 分析结果
     * 
     * @param resourcePath 资源路径，默认为 "META-INF/pragma-ddd-analysis.json"
     * @return 分析结果列表，如果文件不存在则返回空列表
     */
    fun readAnalysisResults(resourcePath: String = "META-INF/pragma-ddd-analysis.json"): List<ClassAnalysisResult> {
        return try {
            val inputStream = this::class.java.classLoader.getResourceAsStream(resourcePath)
                ?: return emptyList()
            
            inputStream.use { stream ->
                mapper.readValue<List<ClassAnalysisResult>>(stream)
            }
        } catch (e: Exception) {
            // 如果读取失败，返回空列表而不是抛出异常
            emptyList()
        }
    }
    
    /**
     * 从指定的 JAR 文件中读取 DDD 分析结果
     * 
     * @param jarFilePath JAR 文件路径
     * @param resourcePath 资源路径，默认为 "META-INF/pragma-ddd-analysis.json"
     * @return 分析结果列表，如果文件不存在则返回空列表
     */
    fun readAnalysisResultsFromJar(jarFilePath: String, resourcePath: String = "META-INF/pragma-ddd-analysis.json"): List<ClassAnalysisResult> {
        return try {
            val jarFile = java.util.jar.JarFile(jarFilePath)
            val entry = jarFile.getJarEntry(resourcePath) ?: return emptyList()
            
            jarFile.getInputStream(entry).use { stream ->
                mapper.readValue<List<ClassAnalysisResult>>(stream)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 检查当前类路径中是否存在 DDD 分析结果文件
     */
    fun hasAnalysisResults(resourcePath: String = "META-INF/pragma-ddd-analysis.json"): Boolean {
        return this::class.java.classLoader.getResource(resourcePath) != null
    }
}