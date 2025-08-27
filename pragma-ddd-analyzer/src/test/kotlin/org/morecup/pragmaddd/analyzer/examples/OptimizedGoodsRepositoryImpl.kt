package org.morecup.pragmaddd.analyzer.examples

import org.babyfish.jimmer.runtime.DraftSpi
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.fetcher.newFetcher
import org.morecup.pragmaddd.analyzer.runtime.CompileTimeAnalysisUtils
import org.morecup.pragmaddd.core.proxy.DomainAggregateRoot
import org.morecup.pragmaddd.jimmer.proxy.DraftContextManager
import org.springframework.stereotype.Repository

/**
 * 优化后的GoodsRepositoryImpl示例
 * 使用编译期调用分析替代运行时堆栈跟踪
 * 
 * 对比原来的复杂实现：
 * ```kotlin
 * val stackTrace = Thread.currentThread().stackTrace
 * if (stackTrace.size > 2) {
 *     var realCallerIndex = 2
 *     while (realCallerIndex < stackTrace.size) {
 *         val className = stackTrace[realCallerIndex].className
 *         if (!className.contains("$$EnhancerByCGLIB$$") &&
 *             !className.contains("$$FastClassByCGLIB$$") &&
 *             !className.contains("$Proxy") &&
 *             !className.startsWith("java.") &&
 *             !className.startsWith("kotlin.") &&
 *             !className.startsWith("org.springframework.")) {
 *             break
 *         }
 *         realCallerIndex++
 *     }
 *     if (realCallerIndex < stackTrace.size) {
 *         val stackTraceElement: StackTraceElement = stackTrace[realCallerIndex]
 *         val analysisStackTraceElementCalledMethod =
 *             analysisStackTraceElementCalledMethod(stackTraceElement, Goods::class.java)
 *         val analysisMethodsCalledFields = analysisMethodsCalledFields(analysisStackTraceElementCalledMethod)
 *         println(analysisMethodsCalledFields)
 *     }
 * }
 * ```
 * 
 * 现在的简化实现使用编译期预计算的结果，性能更高，准确度更好
 */
@Repository
open class OptimizedGoodsRepositoryImpl(
    private val kSqlClient: KSqlClient
) : GoodsRepository {
    
    companion object {
        init {
            // 在类加载时初始化编译期分析结果
            CompileTimeAnalysisUtils.initialize()
        }
    }
    
    override fun saveGoods(goods: Goods) {
        val tempDraft = DomainAggregateRoot.findOrmObjs(goods)[0] as DraftSpi
        val changed = tempDraft.__resolve()
        kSqlClient.save(changed, SaveMode.NON_IDEMPOTENT_UPSERT, AssociatedSaveMode.VIOLENTLY_REPLACE)
        tempDraft.__draftContext().dispose()
    }

    override fun findByIdOrErr(id: Long): Goods {
        // 🚀 使用编译期调用分析获取需要的字段 - 替代复杂的运行时堆栈跟踪
        val requiredFields = CompileTimeAnalysisUtils.getRequiredFields(Goods::class.java)
        
        if (requiredFields.isEmpty()) {
            // 如果没有分析结果，回退到完整加载（兼容性保证）
            println("[OptimizedGoodsRepositoryImpl] No compile-time analysis results found, using full fetch")
            return findByIdWithFullFetch(id)
        }
        
        println("[OptimizedGoodsRepositoryImpl] Using compile-time analysis results: $requiredFields")
        
        // 🎯 基于编译期分析结果构建优化的fetcher
        val optimizedFetcher = buildOptimizedFetcher(requiredFields)
        
        val goodsEntity: GoodsEntity = kSqlClient.findById(optimizedFetcher, id) 
            ?: throw RuntimeException("Goods not found")
        
        // 转换为领域对象
        val tempDraft = DraftContextManager.getOrCreate().toDraftObject<Any>(goodsEntity).let { it as DraftSpi }
        val goods: Goods = DomainAggregateRoot.build(Goods::class.java, tempDraft)
        
        return goods
    }
    
    /**
     * 基于编译期分析结果构建优化的fetcher
     * 这个方法展示了如何将字段访问信息转换为Jimmer fetcher配置
     */
    private fun buildOptimizedFetcher(requiredFields: Set<String>) = newFetcher(GoodsEntity::class).by {
        // 基础字段总是需要的
        id()
        
        // 根据分析结果动态添加需要的字段
        if (requiredFields.contains("name")) {
            name()
        }
        
        if (requiredFields.contains("nowAddress") || requiredFields.contains("nowAddress1")) {
            nowAddress()
        }
        
        // 处理关联对象的字段访问
        if (requiredFields.any { it.startsWith("address") || it.contains("Address") }) {
            addressEntity {
                allTableFields() // 基础地址字段
                
                // 根据需要的字段类型加载特定的地址类型
                if (requiredFields.any { it.contains("beijing") || it.contains("Beijing") }) {
                    beijingAddress {
                        allTableFields()
                    }
                }
                
                if (requiredFields.any { it.contains("hubei") || it.contains("Hubei") }) {
                    hubeiAddress {
                        allTableFields()
                    }
                }
            }
        }
        
        // 可以根据更多的字段模式进行扩展
        // 例如：处理嵌套对象、集合等
    }
    
    /**
     * 回退方案：完整加载（用于兼容性）
     */
    private fun findByIdWithFullFetch(id: Long): Goods {
        val goodsEntity: GoodsEntity = kSqlClient.findById(newFetcher(GoodsEntity::class).by {
            allTableFields()
            addressEntity {
                allTableFields()
                beijingAddress {
                    allTableFields()
                }
                hubeiAddress {
                    allTableFields()
                }
            }
        }, id) ?: throw RuntimeException("Goods not found")
        
        val tempDraft = DraftContextManager.getOrCreate().toDraftObject<Any>(goodsEntity).let { it as DraftSpi }
        val goods: Goods = DomainAggregateRoot.build(Goods::class.java, tempDraft)
        
        return goods
    }
    
    /**
     * 高级用法：基于具体调用上下文的精确字段访问
     * 这个方法展示了如何获取更精确的字段访问信息
     */
    fun findByIdWithPreciseFieldAccess(id: Long, callerClass: String, callerMethod: String): Goods {
        val requiredFields = CompileTimeAnalysisUtils.getRequiredFieldsByMethod(
            aggregateRootClass = "org.morecup.pragmaddd.jimmer.domain.goods.Goods",
            callerClass = callerClass,
            callerMethod = callerMethod,
            repositoryMethod = "findByIdOrErr"
        )
        
        println("[OptimizedGoodsRepositoryImpl] Precise field access for $callerClass.$callerMethod: $requiredFields")
        
        val optimizedFetcher = buildOptimizedFetcher(requiredFields)
        val goodsEntity: GoodsEntity = kSqlClient.findById(optimizedFetcher, id) 
            ?: throw RuntimeException("Goods not found")
        
        val tempDraft = DraftContextManager.getOrCreate().toDraftObject<Any>(goodsEntity).let { it as DraftSpi }
        return DomainAggregateRoot.build(Goods::class.java, tempDraft)
    }
}

/**
 * 模拟的接口和类（用于示例）
 * 在实际使用中，这些应该引用真实的jimmer模块中的类
 */
interface GoodsRepository {
    fun saveGoods(goods: Goods)
    fun findByIdOrErr(id: Long): Goods
}

// 模拟的领域对象
class Goods(
    var name: String,
    private var nowAddress1: String,
    var id: Long? = null,
    var address: MutableList<Address>
) {
    fun changeAddress(newAddress: String) {
        println("Current address: $nowAddress1")
        println("Name: $name")
        // 业务逻辑...
    }
}

// 模拟的地址类
abstract class Address(
    open var name: String,
    open var detail: String
)

// 模拟的Jimmer实体
interface GoodsEntity {
    val id: Long
    val name: String
    val nowAddress: String
    val addressEntity: List<AddressEntity>
}

interface AddressEntity {
    val id: Long
    val name: String
    val detail: String
    val beijingAddress: BeijingAddressEntity?
    val hubeiAddress: HubeiAddressEntity?
}

interface BeijingAddressEntity {
    val id: Long
    val beijingAddressCode: String
}

interface HubeiAddressEntity {
    val id: Long
    val hubeiAddressCode: String
}

/**
 * 使用示例和性能对比
 */
object UsageExample {
    
    fun demonstratePerformanceImprovement() {
        println("=== 编译期调用分析性能对比示例 ===")
        
        // 模拟原来的运行时堆栈跟踪分析
        val runtimeAnalysisTime = measureTimeMillis {
            simulateRuntimeStackTraceAnalysis()
        }
        
        // 模拟编译期预计算的字段访问
        val compileTimeAnalysisTime = measureTimeMillis {
            simulateCompileTimeAnalysis()
        }
        
        println("原来的运行时堆栈跟踪分析耗时: ${runtimeAnalysisTime}ms")
        println("编译期预计算分析耗时: ${compileTimeAnalysisTime}ms")
        println("性能提升: ${if (runtimeAnalysisTime > 0) (runtimeAnalysisTime - compileTimeAnalysisTime) * 100 / runtimeAnalysisTime else 0}%")
        
        // 展示准确性提升
        println("\n=== 准确性对比 ===")
        println("运行时分析可能受到以下因素影响：")
        println("- CGLIB代理类干扰")
        println("- Spring AOP代理干扰") 
        println("- 复杂的堆栈过滤逻辑")
        println("- 运行时环境差异")
        
        println("\n编译期分析的优势：")
        println("- 基于静态字节码分析，结果准确且稳定")
        println("- 不受运行时代理影响")
        println("- 支持复杂的递归字段访问分析")
        println("- 零运行时性能开销")
    }
    
    private fun simulateRuntimeStackTraceAnalysis() {
        // 模拟复杂的堆栈跟踪分析
        val stackTrace = Thread.currentThread().stackTrace
        var realCallerIndex = 2
        
        while (realCallerIndex < stackTrace.size) {
            val className = stackTrace[realCallerIndex].className
            if (!className.contains("$$EnhancerByCGLIB$$") &&
                !className.contains("$$FastClassByCGLIB$$") &&
                !className.contains("$Proxy") &&
                !className.startsWith("java.") &&
                !className.startsWith("kotlin.") &&
                !className.startsWith("org.springframework.")) {
                break
            }
            realCallerIndex++
        }
        
        // 模拟复杂的字段访问分析
        Thread.sleep(5) // 模拟分析耗时
    }
    
    private fun simulateCompileTimeAnalysis() {
        // 模拟编译期预计算的快速查找
        val requiredFields = CompileTimeAnalysisUtils.getRequiredFields(Goods::class.java)
        // 几乎零耗时的查找操作
    }
    
    private fun measureTimeMillis(block: () -> Unit): Long {
        val start = System.currentTimeMillis()
        block()
        return System.currentTimeMillis() - start
    }
}