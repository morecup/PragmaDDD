package org.morecup.pragmaddd.analyzer.compiletime

import org.morecup.pragmaddd.analyzer.compiletime.model.*
import org.objectweb.asm.*
import org.objectweb.asm.commons.Method
import java.io.File

/**
 * 调用图构建器
 * 负责分析编译后的class文件，构建方法调用关系图
 */
class CallGraphBuilder(
    private val repositoryMappings: List<AggregateRootRepositoryMapping>,
    private val config: CompileTimeAnalysisConfig = CompileTimeAnalysisConfig()
) {
    
    /**
     * 构建调用图
     * @param classFiles 编译输出的class文件列表
     * @return 完整的调用图
     */
    fun buildCallGraph(classFiles: List<File>): CallGraph {
        val callGraph = CallGraph()
        val repositoryClasses = repositoryMappings.map { it.repositoryClass }.toSet()
        
        for (classFile in classFiles) {
            if (isBusinessClass(classFile)) {
                try {
                    analyzeClassFile(classFile, callGraph, repositoryClasses)
                } catch (e: Exception) {
                    if (config.debugMode) {
                        println("Failed to analyze class file: ${classFile.name}, error: ${e.message}")
                    }
                }
            }
        }
        
        return callGraph
    }
    
    private fun analyzeClassFile(
        classFile: File,
        callGraph: CallGraph,
        repositoryClasses: Set<String>
    ) {
        val visitor = CallAnalysisClassVisitor(callGraph, repositoryClasses, repositoryMappings)
        ClassReader(classFile.inputStream()).accept(visitor, 0)
    }
    
    private fun isBusinessClass(classFile: File): Boolean {
        val fileName = classFile.name
        if (!fileName.endsWith(".class")) return false
        
        val className = extractClassNameFromFile(classFile)
        
        return !className.contains("$$") && // 排除CGLIB代理
               !className.startsWith("java.") && // 排除JDK类
               !className.startsWith("kotlin.") && // 排除Kotlin标准库
               !className.startsWith("org.springframework.") && // 排除Spring框架类
               !className.startsWith("org.junit.") && // 排除测试框架
               !className.contains("$\$Lambda") && // 排除Lambda生成类
               isIncludedPackage(className) && 
               !isExcludedPackage(className)
    }
    
    private fun extractClassNameFromFile(classFile: File): String {
        // 简化实现：从文件路径推导类名
        val path = classFile.absolutePath
        val classesIndex = path.lastIndexOf("classes")
        if (classesIndex != -1) {
            val afterClasses = path.substring(classesIndex + "classes".length)
                .replace(File.separator, ".")
                .removePrefix(".")
                .removeSuffix(".class")
            return afterClasses
        }
        
        return classFile.nameWithoutExtension
    }
    
    private fun isIncludedPackage(className: String): Boolean {
        val includePackages = config.repositoryConfig.includePackages
        if (includePackages.isEmpty() || includePackages.contains("**")) {
            return true
        }
        
        return includePackages.any { pattern ->
            when {
                pattern == "**" -> true
                pattern.endsWith("**") -> className.startsWith(pattern.removeSuffix("**"))
                pattern.endsWith("*") -> className.startsWith(pattern.removeSuffix("*"))
                else -> className.startsWith(pattern)
            }
        }
    }
    
    private fun isExcludedPackage(className: String): Boolean {
        val excludePackages = config.repositoryConfig.excludePackages
        return excludePackages.any { pattern ->
            when {
                pattern == "**" -> true
                pattern.endsWith("**") -> className.startsWith(pattern.removeSuffix("**"))
                pattern.endsWith("*") -> className.startsWith(pattern.removeSuffix("*"))
                else -> className.startsWith(pattern)
            }
        }
    }
}

/**
 * 调用分析类访问器
 */
private class CallAnalysisClassVisitor(
    private val callGraph: CallGraph,
    private val repositoryClasses: Set<String>,
    private val repositoryMappings: List<AggregateRootRepositoryMapping>
) : ClassVisitor(Opcodes.ASM9) {
    
    private lateinit var currentClassName: String
    private var currentSourceFile: String? = null
    
    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        currentClassName = name.replace('/', '.')
    }
    
    override fun visitSource(source: String?, debug: String?) {
        currentSourceFile = source
    }
    
    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor? {
        return CallAnalysisMethodVisitor(
            currentClassName,
            name,
            descriptor,
            callGraph,
            repositoryClasses,
            repositoryMappings
        )
    }
}

/**
 * 调用分析方法访问器
 */
private class CallAnalysisMethodVisitor(
    private val className: String,
    private val methodName: String,
    private val methodDescriptor: String,
    private val callGraph: CallGraph,
    private val repositoryClasses: Set<String>,
    private val repositoryMappings: List<AggregateRootRepositoryMapping>
) : MethodVisitor(Opcodes.ASM9) {
    
    private var currentLineNumber = -1
    private var startLineNumber = -1
    private var endLineNumber = -1
    
    override fun visitLineNumber(line: Int, start: Label?) {
        currentLineNumber = line
        if (startLineNumber == -1) {
            startLineNumber = line
        }
        endLineNumber = line
    }
    
    override fun visitMethodInsn(
        opcode: Int,
        owner: String,
        name: String,
        descriptor: String,
        isInterface: Boolean
    ) {
        val ownerClassName = owner.replace('/', '.')
        val calledMethod = MethodInfo(ownerClassName, name, descriptor)
        val callerMethod = MethodInfo(
            className, 
            methodName, 
            methodDescriptor,
            if (startLineNumber != -1 && endLineNumber != -1) Pair(startLineNumber, endLineNumber) else null
        )
        
        // 记录方法调用关系
        callGraph.addMethodCall(callerMethod, calledMethod)
        
        // 检查是否是Repository方法调用
        if (repositoryClasses.contains(ownerClassName)) {
            val repositoryMapping = repositoryMappings.find { it.repositoryClass == ownerClassName }
            if (repositoryMapping != null) {
                val repositoryCallInfo = RepositoryCallInfo(
                    callerMethod = callerMethod,
                    repositoryClass = ownerClassName,
                    repositoryMethod = name,
                    repositoryMethodDescriptor = descriptor,
                    aggregateRootClass = repositoryMapping.aggregateRootClass
                )
                callGraph.addRepositoryCall(repositoryCallInfo)
            }
        }
        
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
    }
    
    override fun visitInvokeDynamicInsn(
        name: String,
        descriptor: String,
        bootstrapMethodHandle: Handle,
        vararg bootstrapMethodArguments: Any
    ) {
        // 处理Lambda表达式和方法引用
        // 这里简化处理，如果需要更精确的Lambda分析可以扩展
        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, *bootstrapMethodArguments)
    }
    
    override fun visitFieldInsn(opcode: Int, owner: String, name: String, descriptor: String) {
        // 记录字段访问（用于后续字段访问分析）
        super.visitFieldInsn(opcode, owner, name, descriptor)
    }
}

/**
 * 调用图分析工具
 */
object CallGraphUtils {
    
    /**
     * 查找到达指定方法的所有路径
     */
    fun findPathsToMethod(
        callGraph: CallGraph,
        targetMethod: MethodInfo,
        maxDepth: Int = 5
    ): List<List<MethodInfo>> {
        val paths = mutableListOf<List<MethodInfo>>()
        val visited = mutableSetOf<MethodInfo>()
        
        fun dfs(current: MethodInfo, path: List<MethodInfo>, depth: Int) {
            if (depth > maxDepth || current in visited) return
            
            val newPath = path + current
            
            if (current == targetMethod) {
                paths.add(newPath)
                return
            }
            
            visited.add(current)
            
            val calledMethods = callGraph.getMethodCalls(current)
            for (called in calledMethods) {
                dfs(called, newPath, depth + 1)
            }
            
            visited.remove(current)
        }
        
        // 从所有可能的起点开始搜索
        for (caller in callGraph.getAllCallers()) {
            dfs(caller, emptyList(), 0)
        }
        
        return paths
    }
    
    /**
     * 查找方法的直接和间接调用者
     */
    fun findAllCallers(
        callGraph: CallGraph,
        targetMethod: MethodInfo
    ): Set<MethodInfo> {
        val callers = mutableSetOf<MethodInfo>()
        
        for (caller in callGraph.getAllCallers()) {
            val calledMethods = callGraph.getMethodCalls(caller)
            if (targetMethod in calledMethods) {
                callers.add(caller)
            }
        }
        
        return callers
    }
    
    /**
     * 检测循环依赖
     */
    fun detectCircularDependencies(callGraph: CallGraph): List<List<MethodInfo>> {
        val cycles = mutableListOf<List<MethodInfo>>()
        val visited = mutableSetOf<MethodInfo>()
        val recursionStack = mutableSetOf<MethodInfo>()
        
        fun dfs(method: MethodInfo, path: List<MethodInfo>): Boolean {
            if (method in recursionStack) {
                // 找到循环
                val cycleStart = path.indexOf(method)
                if (cycleStart != -1) {
                    cycles.add(path.subList(cycleStart, path.size) + method)
                }
                return true
            }
            
            if (method in visited) return false
            
            visited.add(method)
            recursionStack.add(method)
            
            val calledMethods = callGraph.getMethodCalls(method)
            for (called in calledMethods) {
                if (dfs(called, path + method)) {
                    recursionStack.remove(method)
                    return true
                }
            }
            
            recursionStack.remove(method)
            return false
        }
        
        for (method in callGraph.getAllCallers()) {
            if (method !in visited) {
                dfs(method, emptyList())
            }
        }
        
        return cycles
    }
}