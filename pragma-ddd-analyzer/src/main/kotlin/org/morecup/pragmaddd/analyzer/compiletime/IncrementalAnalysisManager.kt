package org.morecup.pragmaddd.analyzer.compiletime

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.morecup.pragmaddd.analyzer.compiletime.model.CallAnalysisResult
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * 增量分析管理器
 * 负责管理缓存、检测文件变更、执行增量分析
 */
class IncrementalAnalysisManager(
    private val cacheDir: File,
    private val enabled: Boolean = true
) {
    
    private val objectMapper = ObjectMapper().registerModule(kotlinModule())
    private val fileChecksumCache = ConcurrentHashMap<String, String>()
    private val analysisCache = ConcurrentHashMap<String, CachedAnalysisResult>()
    
    init {
        if (enabled) {
            cacheDir.mkdirs()
            loadExistingCache()
        }
    }
    
    /**
     * 检查是否需要重新分析
     * @param compilationOutputDir 编译输出目录
     * @param domainAnalysisFile domain-analyzer.json文件
     * @return 是否需要重新分析
     */
    fun shouldAnalyze(
        compilationOutputDir: File,
        domainAnalysisFile: File
    ): Boolean {
        if (!enabled) return true
        
        // 检查domain-analyzer.json是否变化
        val domainAnalysisChecksum = calculateFileChecksum(domainAnalysisFile)
        val cachedDomainChecksum = fileChecksumCache["domain-analyzer.json"]
        
        if (cachedDomainChecksum != domainAnalysisChecksum) {
            println("[IncrementalAnalysis] Domain analysis file changed, full analysis required")
            return true
        }
        
        // 检查编译输出目录中的class文件是否有变化
        val classFiles = collectClassFiles(compilationOutputDir)
        val changedFiles = findChangedFiles(classFiles)
        
        if (changedFiles.isNotEmpty()) {
            println("[IncrementalAnalysis] ${changedFiles.size} class files changed, analysis required")
            return true
        }
        
        // 检查是否存在分析结果缓存
        val cacheKey = generateCacheKey(compilationOutputDir, domainAnalysisFile)
        val cachedResult = analysisCache[cacheKey]
        
        if (cachedResult == null) {
            println("[IncrementalAnalysis] No cached analysis result found, analysis required")
            return true
        }
        
        println("[IncrementalAnalysis] Using cached analysis result")
        return false
    }
    
    /**
     * 获取缓存的分析结果
     */
    fun getCachedResult(
        compilationOutputDir: File,
        domainAnalysisFile: File
    ): CallAnalysisResult? {
        if (!enabled) return null
        
        val cacheKey = generateCacheKey(compilationOutputDir, domainAnalysisFile)
        val cachedResult = analysisCache[cacheKey]
        
        return cachedResult?.result
    }
    
    /**
     * 缓存分析结果
     */
    fun cacheResult(
        compilationOutputDir: File,
        domainAnalysisFile: File,
        result: CallAnalysisResult
    ) {
        if (!enabled) return
        
        val cacheKey = generateCacheKey(compilationOutputDir, domainAnalysisFile)
        val cachedResult = CachedAnalysisResult(
            result = result,
            timestamp = System.currentTimeMillis(),
            cacheKey = cacheKey
        )
        
        analysisCache[cacheKey] = cachedResult
        
        // 更新文件校验和缓存
        updateFileChecksums(compilationOutputDir, domainAnalysisFile)
        
        // 持久化缓存
        persistCache()
        
        println("[IncrementalAnalysis] Analysis result cached with key: $cacheKey")
    }
    
    /**
     * 执行增量分析
     * 只分析变更的文件，合并已有的分析结果
     */
    fun performIncrementalAnalysis(
        compilationOutputDir: File,
        domainAnalysisFile: File,
        analyzer: CompileTimeCallAnalyzer
    ): CallAnalysisResult {
        if (!enabled) {
            return analyzer.analyze(compilationOutputDir, domainAnalysisFile)
        }
        
        val changedFiles = findChangedFiles(collectClassFiles(compilationOutputDir))
        
        if (changedFiles.isEmpty()) {
            // 没有文件变化，返回缓存结果
            return getCachedResult(compilationOutputDir, domainAnalysisFile)
                ?: analyzer.analyze(compilationOutputDir, domainAnalysisFile)
        }
        
        println("[IncrementalAnalysis] Performing incremental analysis for ${changedFiles.size} changed files")
        
        // 创建临时目录，只包含变更的文件
        val tempDir = createTempDirectoryWithChangedFiles(changedFiles)
        
        try {
            // 分析变更的文件
            val incrementalResult = analyzer.analyze(tempDir, domainAnalysisFile)
            
            // 获取已有的分析结果
            val cachedResult = getCachedResult(compilationOutputDir, domainAnalysisFile)
            
            // 合并结果
            val mergedResult = if (cachedResult != null) {
                val serializer = CallAnalysisResultSerializer()
                serializer.mergeResults(listOf(cachedResult, incrementalResult))
            } else {
                incrementalResult
            }
            
            // 缓存合并后的结果
            cacheResult(compilationOutputDir, domainAnalysisFile, mergedResult)
            
            return mergedResult
        } finally {
            // 清理临时目录
            tempDir.deleteRecursively()
        }
    }
    
    /**
     * 清理过期缓存
     */
    fun cleanupExpiredCache(maxAge: Long = 7 * 24 * 60 * 60 * 1000L) { // 7天
        if (!enabled) return
        
        val currentTime = System.currentTimeMillis()
        val expiredKeys = analysisCache.entries
            .filter { (_, cachedResult) -> 
                currentTime - cachedResult.timestamp > maxAge 
            }
            .map { it.key }
        
        expiredKeys.forEach { key ->
            analysisCache.remove(key)
            
            // 删除对应的缓存文件
            val cacheFile = File(cacheDir, "$key.cache")
            if (cacheFile.exists()) {
                cacheFile.delete()
            }
        }
        
        if (expiredKeys.isNotEmpty()) {
            println("[IncrementalAnalysis] Cleaned up ${expiredKeys.size} expired cache entries")
            persistCache()
        }
    }
    
    /**
     * 清除所有缓存
     */
    fun clearAllCache() {
        analysisCache.clear()
        fileChecksumCache.clear()
        
        if (cacheDir.exists()) {
            cacheDir.deleteRecursively()
            cacheDir.mkdirs()
        }
        
        println("[IncrementalAnalysis] All cache cleared")
    }
    
    private fun loadExistingCache() {
        try {
            val checksumCacheFile = File(cacheDir, "file-checksums.json")
            if (checksumCacheFile.exists()) {
                val checksums: Map<String, String> = objectMapper.readValue(checksumCacheFile)
                fileChecksumCache.putAll(checksums)
            }
            
            val analysisCacheFile = File(cacheDir, "analysis-cache.json")
            if (analysisCacheFile.exists()) {
                val cache: Map<String, CachedAnalysisResult> = objectMapper.readValue(analysisCacheFile)
                analysisCache.putAll(cache)
            }
            
            println("[IncrementalAnalysis] Loaded existing cache: ${fileChecksumCache.size} file checksums, ${analysisCache.size} analysis results")
        } catch (e: Exception) {
            println("[IncrementalAnalysis] Failed to load existing cache: ${e.message}")
        }
    }
    
    private fun persistCache() {
        try {
            val checksumCacheFile = File(cacheDir, "file-checksums.json")
            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(checksumCacheFile, fileChecksumCache)
            
            val analysisCacheFile = File(cacheDir, "analysis-cache.json")
            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(analysisCacheFile, analysisCache)
                
        } catch (e: Exception) {
            println("[IncrementalAnalysis] Failed to persist cache: ${e.message}")
        }
    }
    
    private fun collectClassFiles(compilationOutputDir: File): List<File> {
        val classFiles = mutableListOf<File>()
        
        if (compilationOutputDir.exists() && compilationOutputDir.isDirectory) {
            compilationOutputDir.walkTopDown()
                .filter { it.isFile && it.extension == "class" }
                .forEach { classFiles.add(it) }
        }
        
        return classFiles
    }
    
    private fun findChangedFiles(classFiles: List<File>): List<File> {
        val changedFiles = mutableListOf<File>()
        
        for (classFile in classFiles) {
            val relativePath = getRelativePath(classFile)
            val currentChecksum = calculateFileChecksum(classFile)
            val cachedChecksum = fileChecksumCache[relativePath]
            
            if (cachedChecksum != currentChecksum) {
                changedFiles.add(classFile)
            }
        }
        
        return changedFiles
    }
    
    private fun updateFileChecksums(
        compilationOutputDir: File,
        domainAnalysisFile: File
    ) {
        // 更新domain-analyzer.json的校验和
        val domainAnalysisChecksum = calculateFileChecksum(domainAnalysisFile)
        fileChecksumCache["domain-analyzer.json"] = domainAnalysisChecksum
        
        // 更新所有class文件的校验和
        val classFiles = collectClassFiles(compilationOutputDir)
        for (classFile in classFiles) {
            val relativePath = getRelativePath(classFile)
            val checksum = calculateFileChecksum(classFile)
            fileChecksumCache[relativePath] = checksum
        }
    }
    
    private fun calculateFileChecksum(file: File): String {
        if (!file.exists()) return ""
        
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = Files.readAllBytes(file.toPath())
            val hashBytes = digest.digest(bytes)
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }
    
    private fun getRelativePath(file: File): String {
        return file.absolutePath.replace("\\", "/")
    }
    
    private fun generateCacheKey(
        compilationOutputDir: File,
        domainAnalysisFile: File
    ): String {
        val key = "${compilationOutputDir.absolutePath}:${domainAnalysisFile.absolutePath}"
        return key.hashCode().toString(16)
    }
    
    private fun createTempDirectoryWithChangedFiles(changedFiles: List<File>): File {
        val tempDir = Files.createTempDirectory("pragma-ddd-incremental").toFile()
        
        for (changedFile in changedFiles) {
            val relativePath = getRelativePath(changedFile)
            val targetFile = File(tempDir, relativePath)
            targetFile.parentFile?.mkdirs()
            changedFile.copyTo(targetFile)
        }
        
        return tempDir
    }
}

/**
 * 缓存的分析结果
 */
data class CachedAnalysisResult(
    val result: CallAnalysisResult,
    val timestamp: Long,
    val cacheKey: String
)

/**
 * 缓存统计信息
 */
data class CacheStatistics(
    val totalCacheEntries: Int,
    val totalFileChecksums: Int,
    val cacheHitRate: Double,
    val averageAnalysisTime: Long,
    val cacheSize: Long // 字节
) {
    fun toFormattedString(): String {
        return """
            === 增量分析缓存统计 ===
            缓存条目总数: $totalCacheEntries
            文件校验和总数: $totalFileChecksums
            缓存命中率: ${"%.2f".format(cacheHitRate * 100)}%
            平均分析时间: ${averageAnalysisTime}ms
            缓存大小: ${"%.2f".format(cacheSize / 1024.0 / 1024.0)}MB
        """.trimIndent()
    }
}

/**
 * 缓存性能监控器
 */
class CachePerformanceMonitor {
    private var totalAnalysisRequests = 0L
    private var cacheHits = 0L
    private var totalAnalysisTime = 0L
    private var analysisCount = 0L
    
    fun recordAnalysisRequest() {
        totalAnalysisRequests++
    }
    
    fun recordCacheHit() {
        cacheHits++
    }
    
    fun recordAnalysisTime(timeMs: Long) {
        totalAnalysisTime += timeMs
        analysisCount++
    }
    
    fun getStatistics(cacheManager: IncrementalAnalysisManager): CacheStatistics {
        val hitRate = if (totalAnalysisRequests > 0) {
            cacheHits.toDouble() / totalAnalysisRequests
        } else 0.0
        
        val averageTime = if (analysisCount > 0) {
            totalAnalysisTime / analysisCount
        } else 0L
        
        return CacheStatistics(
            totalCacheEntries = 0, // 需要从cacheManager获取
            totalFileChecksums = 0, // 需要从cacheManager获取
            cacheHitRate = hitRate,
            averageAnalysisTime = averageTime,
            cacheSize = 0L // 需要计算缓存目录大小
        )
    }
    
    fun reset() {
        totalAnalysisRequests = 0
        cacheHits = 0
        totalAnalysisTime = 0
        analysisCount = 0
    }
}