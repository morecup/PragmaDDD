package org.morecup.pragmaddd.analyzer.repository

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Repository调用分析器测试
 */
class RepositoryCallAnalyzerTest {
    
    private lateinit var analyzer: RepositoryCallAnalyzer
    
    @TempDir
    private lateinit var tempDir: File
    
    @BeforeEach
    fun setUp() {
        analyzer = RepositoryCallAnalyzer()
    }
    
    @Test
    fun `应该能够识别聚合根类`() {
        // 复制测试class文件到临时目录
        copyTestClassesToTempDir()
        
        // 执行分析
        val result = analyzer.analyzeDirectory(tempDir)
        
        // 验证结果
        assertNotNull(result)
        assertTrue(result.aggregateRoots.isNotEmpty(), "应该能够识别到聚合根")
        
        val goodsAggregateRoot = result.aggregateRoots.find { 
            it.className.endsWith("Goods") 
        }
        assertNotNull(goodsAggregateRoot, "应该能够识别到Goods聚合根")
        assertTrue(goodsAggregateRoot!!.fieldNames.contains("name"), "应该识别到name字段")
        assertTrue(goodsAggregateRoot.fieldNames.contains("id"), "应该识别到id字段")
    }
    
    @Test
    fun `应该能够识别Repository类`() {
        copyTestClassesToTempDir()
        
        val result = analyzer.analyzeDirectory(tempDir)
        
        assertTrue(result.repositories.isNotEmpty(), "应该能够识别到Repository")
        
        val goodsRepository = result.repositories.find { 
            it.className.contains("GoodsRepository") 
        }
        assertNotNull(goodsRepository, "应该能够识别到GoodsRepository")
    }
    
    @Test
    fun `应该能够匹配Repository与聚合根的关系`() {
        copyTestClassesToTempDir()
        
        val result = analyzer.analyzeDirectory(tempDir)
        
        val goodsRepository = result.repositories.find { 
            it.className.contains("GoodsRepository") 
        }
        val goodsAggregateRoot = result.aggregateRoots.find { 
            it.className.endsWith("Goods") 
        }
        
        assertNotNull(goodsRepository, "应该找到GoodsRepository")
        assertNotNull(goodsAggregateRoot, "应该找到Goods聚合根")
        
        if (goodsRepository != null && goodsAggregateRoot != null) {
            assertEquals(
                goodsAggregateRoot.className, 
                goodsRepository.targetAggregateRoot,
                "Repository应该正确匹配到对应的聚合根"
            )
        }
    }
    
    @Test
    fun `应该能够分析Repository调用链`() {
        copyTestClassesToTempDir()
        
        val result = analyzer.analyzeDirectory(tempDir)
        
        assertTrue(result.callAnalysis.isNotEmpty(), "应该能够分析到Repository调用")
        
        val changeAddressHandlerCalls = result.callAnalysis.find { 
            it.methodClass.contains("ChangeAddressCmdHandler")
        }
        
        assertNotNull(changeAddressHandlerCalls, "应该能够找到ChangeAddressCmdHandler的调用分析")
        
        if (changeAddressHandlerCalls != null) {
            assertTrue(changeAddressHandlerCalls.repositoryCalls.isNotEmpty(), "应该有Repository调用")
            
            val findByIdCall = changeAddressHandlerCalls.repositoryCalls.find { 
                it.repositoryMethod == "findByIdOrErr" 
            }
            assertNotNull(findByIdCall, "应该找到findByIdOrErr调用")
            
            val saveCall = changeAddressHandlerCalls.repositoryCalls.find { 
                it.repositoryMethod == "saveGoods" 
            }
            assertNotNull(saveCall, "应该找到saveGoods调用")
        }
    }
    
    @Test
    fun `应该能够分析聚合根方法调用和字段访问`() {
        copyTestClassesToTempDir()
        
        val result = analyzer.analyzeDirectory(tempDir)
        
        val changeAddressHandlerCalls = result.callAnalysis.find { 
            it.methodClass.contains("ChangeAddressCmdHandler")
        }
        
        if (changeAddressHandlerCalls != null) {
            val repositoryCall = changeAddressHandlerCalls.repositoryCalls.firstOrNull()
            
            if (repositoryCall != null) {
                assertNotNull(repositoryCall.aggregateRoot, "应该识别到目标聚合根")
                assertTrue(repositoryCall.calledAggregateRootMethod.isNotEmpty(), 
                    "应该识别到聚合根方法调用")
                
                val changeAddressMethodCall = repositoryCall.calledAggregateRootMethod.find {
                    it.aggregateRootMethod == "changeAddress"
                }
                assertNotNull(changeAddressMethodCall, "应该找到changeAddress方法调用")
                
                if (changeAddressMethodCall != null) {
                    assertTrue(changeAddressMethodCall.requiredFields.isNotEmpty(), 
                        "changeAddress方法应该有字段访问")
                }
            }
        }
    }
    
    @Test
    fun `应该正确计算所需字段的并集`() {
        copyTestClassesToTempDir()
        
        val result = analyzer.analyzeDirectory(tempDir)
        
        result.callAnalysis.forEach { callAnalysis ->
            callAnalysis.repositoryCalls.forEach { repoCall ->
                assertNotNull(repoCall.requiredFields, "应该计算出必需字段")
                assertTrue(repoCall.requiredFields.isNotEmpty() || 
                    repoCall.calledAggregateRootMethod.isEmpty(),
                    "如果有聚合根方法调用，应该有必需字段")
            }
        }
    }
    
    /**
     * 复制测试用的class文件到临时目录
     * 这里需要实际的编译后的class文件来进行测试
     */
    private fun copyTestClassesToTempDir() {
        // 复制jimmer模块中的测试类
        val testClasses = listOf(
            "org/morecup/pragmaddd/jimmer/domain/goods/Goods.class",
            "org/morecup/pragmaddd/jimmer/domain/goods/GoodsRepository.class",
            "org/morecup/pragmaddd/jimmer/admin/goods/GoodsRepositoryImpl.class",
            "org/morecup/pragmaddd/jimmer/domain/goods/ChangeAddressCmdHandler.class"
        )
        
        testClasses.forEach { className ->
            val classResource = this.javaClass.classLoader.getResourceAsStream(className)
            if (classResource != null) {
                val targetFile = File(tempDir, className)
                targetFile.parentFile?.mkdirs()
                FileOutputStream(targetFile).use { output ->
                    classResource.use { input ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }
}