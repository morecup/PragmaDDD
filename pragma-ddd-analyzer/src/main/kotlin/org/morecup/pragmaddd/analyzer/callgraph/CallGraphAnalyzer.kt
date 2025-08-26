package org.morecup.pragmaddd.analyzer.callgraph

import org.objectweb.asm.*
import java.io.File
import java.io.FileInputStream

/**
 * 编译期调用关系分析器
 * 
 * 使用ASM分析字节码，识别所有调用Repository方法的业务代码位置，
 * 构建完整的方法调用图谱，排除框架代理类
 */
class CallGraphAnalyzer(
    private val includePackages: List<String> = emptyList(),
    private val excludePackages: List<String> = emptyList(),
    private val debugMode: Boolean = false
) {
    
    /**
     * 方法调用信息
     */
    data class MethodCallInfo(
        val callerClass: String,
        val callerMethod: String,
        val callerMethodDescriptor: String,
        val callerLineStart: Int,
        val callerLineEnd: Int,
        val repositoryClass: String,
        val repositoryMethod: String,
        val repositoryMethodDescriptor: String,
        val aggregateRootClass: String
    )
    
    /**
     * 调用图分析结果
     */
    data class CallGraphResult(
        val repositoryCalls: List<MethodCallInfo>,
        val aggregateRootMethodCalls: Map<String, List<AggregateRootMethodCall>>
    )
    
    /**
     * 聚合根方法调用信息
     */
    data class AggregateRootMethodCall(
        val methodName: String,
        val methodDescriptor: String,
        val callerClass: String,
        val callerMethod: String,
        val callerMethodDescriptor: String
    )
    
    /**
     * 分析调用图
     */
    fun analyzeCallGraph(
        classDirectory: File,
        repositories: List<RepositoryIdentifier.RepositoryInfo>
    ): CallGraphResult {
        val repositoryCalls = mutableListOf<MethodCallInfo>()
        val aggregateRootMethodCalls = mutableMapOf<String, MutableList<AggregateRootMethodCall>>()
        
        // 构建Repository方法映射
        val repositoryMethodMap = buildRepositoryMethodMap(repositories)
        
        // 构建聚合根类集合
        val aggregateRootClasses = repositories.map { it.aggregateRootClass }.toSet()
        
        if (debugMode) {
            println("[CallGraphAnalyzer] 开始分析调用图")
            println("[CallGraphAnalyzer] Repository数量: ${repositories.size}")
            println("[CallGraphAnalyzer] 聚合根数量: ${aggregateRootClasses.size}")
        }
        
        // 扫描所有类文件
        classDirectory.walkTopDown()
            .filter { it.isFile && it.extension == "class" }
            .forEach { classFile ->
                try {
                    val className = getClassNameFromFile(classFile, classDirectory)
                    
                    // 应用包过滤规则
                    if (shouldAnalyzeClass(className)) {
                        analyzeClassFile(
                            classFile, 
                            className,
                            repositoryMethodMap,
                            aggregateRootClasses,
                            repositoryCalls,
                            aggregateRootMethodCalls
                        )
                    }
                } catch (e: Exception) {
                    if (debugMode) {
                        println("[CallGraphAnalyzer] 分析类文件失败: ${classFile.name}, 错误: ${e.message}")
                    }
                }
            }
        
        if (debugMode) {
            println("[CallGraphAnalyzer] 分析完成")
            println("[CallGraphAnalyzer] Repository调用数量: ${repositoryCalls.size}")
            println("[CallGraphAnalyzer] 聚合根方法调用数量: ${aggregateRootMethodCalls.values.sumOf { it.size }}")
        }
        
        return CallGraphResult(
            repositoryCalls = repositoryCalls,
            aggregateRootMethodCalls = aggregateRootMethodCalls
        )
    }
    
    /**
     * 构建Repository方法映射
     */
    private fun buildRepositoryMethodMap(repositories: List<RepositoryIdentifier.RepositoryInfo>): Map<String, RepositoryIdentifier.RepositoryInfo> {
        val methodMap = mutableMapOf<String, RepositoryIdentifier.RepositoryInfo>()
        
        repositories.forEach { repo ->
            repo.methods.forEach { method ->
                val key = "${repo.className}.${method.methodName}${method.methodDescriptor}"
                methodMap[key] = repo
            }
        }
        
        return methodMap
    }
    
    /**
     * 检查是否应该分析该类
     */
    private fun shouldAnalyzeClass(className: String): Boolean {
        // 排除代理类和框架类
        if (className.contains("\$\$EnhancerByCGLIB\$\$") ||
            className.contains("\$\$EnhancerBySpringCGLIB\$\$") ||
            className.contains("\$\$FastClassByCGLIB\$\$") ||
            className.contains("\$\$FastClassBySpringCGLIB\$\$")) {
            return false
        }
        
        // 应用排除规则
        for (excludePattern in excludePackages) {
            if (matchesPattern(className, excludePattern)) {
                return false
            }
        }
        
        // 应用包含规则（如果指定了包含规则）
        if (includePackages.isNotEmpty()) {
            var included = false
            for (includePattern in includePackages) {
                if (matchesPattern(className, includePattern)) {
                    included = true
                    break
                }
            }
            if (!included) {
                return false
            }
        }
        
        return true
    }
    
    /**
     * 模式匹配（支持通配符）
     */
    private fun matchesPattern(className: String, pattern: String): Boolean {
        val regex = pattern.replace("**", ".*").replace("*", "[^.]*")
        return className.matches(Regex(regex))
    }
    
    /**
     * 分析单个类文件
     */
    private fun analyzeClassFile(
        classFile: File,
        className: String,
        repositoryMethodMap: Map<String, RepositoryIdentifier.RepositoryInfo>,
        aggregateRootClasses: Set<String>,
        repositoryCalls: MutableList<MethodCallInfo>,
        aggregateRootMethodCalls: MutableMap<String, MutableList<AggregateRootMethodCall>>
    ) {
        FileInputStream(classFile).use { input ->
            val classReader = ClassReader(input)
            val visitor = CallGraphClassVisitor(
                className,
                repositoryMethodMap,
                aggregateRootClasses,
                repositoryCalls,
                aggregateRootMethodCalls
            )
            classReader.accept(visitor, ClassReader.EXPAND_FRAMES)
        }
    }
    
    /**
     * 从文件路径获取类名
     */
    private fun getClassNameFromFile(classFile: File, baseDirectory: File): String {
        val relativePath = baseDirectory.toURI().relativize(classFile.toURI()).path
        return relativePath.removeSuffix(".class").replace('/', '.')
    }
    
    /**
     * 调用图类访问器
     */
    private inner class CallGraphClassVisitor(
        private val className: String,
        private val repositoryMethodMap: Map<String, RepositoryIdentifier.RepositoryInfo>,
        private val aggregateRootClasses: Set<String>,
        private val repositoryCalls: MutableList<MethodCallInfo>,
        private val aggregateRootMethodCalls: MutableMap<String, MutableList<AggregateRootMethodCall>>
    ) : ClassVisitor(Opcodes.ASM9) {
        
        override fun visitMethod(
            access: Int,
            name: String,
            descriptor: String,
            signature: String?,
            exceptions: Array<String>?
        ): MethodVisitor {
            return CallGraphMethodVisitor(
                className,
                name,
                descriptor,
                repositoryMethodMap,
                aggregateRootClasses,
                repositoryCalls,
                aggregateRootMethodCalls
            )
        }
    }
    
    /**
     * 调用图方法访问器
     */
    private inner class CallGraphMethodVisitor(
        private val callerClass: String,
        private val callerMethod: String,
        private val callerMethodDescriptor: String,
        private val repositoryMethodMap: Map<String, RepositoryIdentifier.RepositoryInfo>,
        private val aggregateRootClasses: Set<String>,
        private val repositoryCalls: MutableList<MethodCallInfo>,
        private val aggregateRootMethodCalls: MutableMap<String, MutableList<AggregateRootMethodCall>>
    ) : MethodVisitor(Opcodes.ASM9) {
        
        private var currentLineNumber: Int = -1
        private var methodStartLine: Int = -1
        private var methodEndLine: Int = -1
        
        override fun visitLineNumber(line: Int, start: Label?) {
            currentLineNumber = line
            if (methodStartLine == -1) {
                methodStartLine = line
            }
            methodEndLine = line
        }
        
        override fun visitMethodInsn(
            opcode: Int,
            owner: String,
            name: String,
            descriptor: String,
            isInterface: Boolean
        ) {
            val targetClass = owner.replace('/', '.')
            val methodKey = "$targetClass.$name$descriptor"
            
            // 检查是否为Repository方法调用
            val repositoryInfo = repositoryMethodMap[methodKey]
            if (repositoryInfo != null) {
                val callInfo = MethodCallInfo(
                    callerClass = callerClass,
                    callerMethod = callerMethod,
                    callerMethodDescriptor = callerMethodDescriptor,
                    callerLineStart = methodStartLine,
                    callerLineEnd = methodEndLine,
                    repositoryClass = repositoryInfo.className,
                    repositoryMethod = name,
                    repositoryMethodDescriptor = descriptor,
                    aggregateRootClass = repositoryInfo.aggregateRootClass
                )
                repositoryCalls.add(callInfo)
                
                if (debugMode) {
                    println("[CallGraphAnalyzer] 发现Repository调用: ${callerClass}.${callerMethod} -> ${repositoryInfo.className}.${name}")
                }
            }
            
            // 检查是否为聚合根方法调用
            if (aggregateRootClasses.contains(targetClass)) {
                val aggregateRootCall = AggregateRootMethodCall(
                    methodName = name,
                    methodDescriptor = descriptor,
                    callerClass = callerClass,
                    callerMethod = callerMethod,
                    callerMethodDescriptor = callerMethodDescriptor
                )
                
                aggregateRootMethodCalls.getOrPut(targetClass) { mutableListOf() }
                    .add(aggregateRootCall)
                
                if (debugMode) {
                    println("[CallGraphAnalyzer] 发现聚合根方法调用: ${callerClass}.${callerMethod} -> ${targetClass}.${name}")
                }
            }
        }
    }
}
