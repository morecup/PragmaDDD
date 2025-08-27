package org.morecup.pragmaddd.analyzer.repository

import org.objectweb.asm.*
import org.objectweb.asm.tree.*
import java.io.FileInputStream
import java.io.File

/**
 * 高级调用链分析器
 * 处理复杂的方法调用链，包括Lambda表达式、方法引用和深度调用链分析
 */
class AdvancedCallChainAnalyzer {
    
    // 调用图缓存 (methodKey -> Set<methodKey>)
    private val callGraph = mutableMapOf<String, MutableSet<String>>()
    
    // Lambda关联映射 (lambdaMethodKey -> originalMethodKey)
    private val lambdaMethodMapping = mutableMapOf<String, String>()
    
    // 字段访问缓存 (methodKey -> Set<fieldName>)
    private val methodFieldAccess = mutableMapOf<String, MutableSet<String>>()
    
    /**
     * 分析目录构建完整的调用图
     */
    fun buildCallGraph(directory: File) {
        directory.walkTopDown()
            .filter { it.isFile && it.extension == "class" }
            .forEach { classFile ->
                analyzeClassCallGraph(classFile)
            }
    }
    
    /**
     * 分析单个类文件构建调用图
     */
    private fun analyzeClassCallGraph(classFile: File) {
        try {
            FileInputStream(classFile).use { input ->
                val classReader = ClassReader(input)
                val classNode = ClassNode()
                classReader.accept(classNode, ClassReader.EXPAND_FRAMES)
                
                analyzeClassNode(classNode)
            }
        } catch (e: Exception) {
            println("分析调用图失败: ${classFile.absolutePath}, 错误: ${e.message}")
        }
    }
    
    /**
     * 分析ClassNode构建调用关系
     */
    private fun analyzeClassNode(classNode: ClassNode) {
        val className = classNode.name.replace('/', '.')
        
        classNode.methods?.forEach { methodNode ->
            val methodKey = createMethodKey(className, methodNode.name, methodNode.desc)
            
            // 分析方法调用
            val callAnalyzer = CallGraphMethodVisitor(methodKey)
            methodNode.accept(callAnalyzer)
            
            // 添加到调用图
            callGraph[methodKey] = callAnalyzer.calledMethods
            
            // 添加Lambda映射
            lambdaMethodMapping.putAll(callAnalyzer.lambdaMappings)
            
            // 分析字段访问
            val fieldAnalyzer = MethodFieldAnalyzer(classNode.name)
            methodNode.accept(fieldAnalyzer)
            methodFieldAccess[methodKey] = fieldAnalyzer.accessedFields
        }
    }
    
    /**
     * 获取方法的传递闭包调用链（包含所有间接调用）
     */
    fun getTransitiveCallChain(methodKey: String): Set<String> {
        val visited = mutableSetOf<String>()
        val result = mutableSetOf<String>()
        
        fun dfs(currentMethod: String) {
            if (currentMethod in visited) return
            visited.add(currentMethod)
            
            // 添加直接调用
            callGraph[currentMethod]?.forEach { calledMethod ->
                result.add(calledMethod)
                dfs(calledMethod)
            }
            
            // 处理Lambda关联
            lambdaMethodMapping[currentMethod]?.let { originalMethod ->
                result.add(originalMethod)
                dfs(originalMethod)
            }
        }
        
        dfs(methodKey)
        return result
    }
    
    /**
     * 获取方法访问的所有字段（包含传递访问）
     */
    fun getTransitiveFieldAccess(methodKey: String, targetClassName: String): Set<String> {
        val allFields = mutableSetOf<String>()
        val callChain = getTransitiveCallChain(methodKey)
        
        // 直接字段访问
        methodFieldAccess[methodKey]?.let { fields ->
            fields.filter { isTargetClassField(it, targetClassName) }
                .let { allFields.addAll(it) }
        }
        
        // 间接字段访问
        callChain.forEach { calledMethod ->
            methodFieldAccess[calledMethod]?.let { fields ->
                fields.filter { isTargetClassField(it, targetClassName) }
                    .let { allFields.addAll(it) }
            }
        }
        
        return allFields
    }
    
    /**
     * 检查字段是否属于目标类
     */
    private fun isTargetClassField(fieldKey: String, targetClassName: String): Boolean {
        return fieldKey.startsWith("${targetClassName.replace('.', '/')}.")
    }
    
    /**
     * 创建方法唯一键
     */
    private fun createMethodKey(className: String, methodName: String, methodDescriptor: String): String {
        return "$className.$methodName$methodDescriptor"
    }
}

/**
 * 调用图方法访问器
 */
private class CallGraphMethodVisitor(
    private val currentMethodKey: String
) : MethodVisitor(Opcodes.ASM9) {
    
    val calledMethods = mutableSetOf<String>()
    val lambdaMappings = mutableMapOf<String, String>()
    
    override fun visitMethodInsn(
        opcode: Int,
        owner: String,
        name: String,
        descriptor: String,
        isInterface: Boolean
    ) {
        val calledMethodKey = "${owner.replace('/', '.')}.$name$descriptor"
        calledMethods.add(calledMethodKey)
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
    }
    
    override fun visitInvokeDynamicInsn(
        name: String?,
        descriptor: String?,
        bootstrapMethodHandle: Handle?,
        vararg bootstrapMethodArguments: Any?
    ) {
        // 处理Lambda表达式
        if (isLambdaBootstrap(bootstrapMethodHandle)) {
            bootstrapMethodArguments.filterIsInstance<Handle>().forEach { handle ->
                when (handle.tag) {
                    Opcodes.H_INVOKESTATIC,
                    Opcodes.H_INVOKEVIRTUAL,
                    Opcodes.H_INVOKESPECIAL,
                    Opcodes.H_INVOKEINTERFACE -> {
                        val lambdaMethodKey = "${handle.owner.replace('/', '.')}." +
                                            "${handle.name}${handle.desc}"
                        
                        // 建立Lambda到原始方法的映射
                        lambdaMappings[currentMethodKey] = lambdaMethodKey
                        calledMethods.add(lambdaMethodKey)
                    }
                }
            }
        }
        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, *bootstrapMethodArguments)
    }
    
    private fun isLambdaBootstrap(handle: Handle?): Boolean {
        return handle?.run {
            owner == "java/lang/invoke/LambdaMetafactory" &&
                    (name == "metafactory" || name == "altMetafactory")
        } == true
    }
}

/**
 * 方法字段分析器
 */
private class MethodFieldAnalyzer(
    private val currentClassName: String
) : MethodVisitor(Opcodes.ASM9) {
    
    val accessedFields = mutableSetOf<String>()
    
    override fun visitFieldInsn(opcode: Int, owner: String, name: String, descriptor: String) {
        if (opcode == Opcodes.GETFIELD || opcode == Opcodes.PUTFIELD) {
            accessedFields.add("$owner.$name")
        }
        super.visitFieldInsn(opcode, owner, name, descriptor)
    }
    
    override fun visitMethodInsn(
        opcode: Int,
        owner: String,
        name: String,
        descriptor: String,
        isInterface: Boolean
    ) {
        // 检测Kotlin属性访问器模式（getter/setter）
        if (owner == currentClassName && isPropertyAccessor(name)) {
            val propertyName = extractPropertyNameFromAccessor(name)
            propertyName?.let { accessedFields.add("$owner.$it") }
        }
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
    }
    
    private fun isPropertyAccessor(methodName: String): Boolean {
        return methodName.startsWith("get") || 
               methodName.startsWith("set") || 
               methodName.startsWith("is")
    }
    
    private fun extractPropertyNameFromAccessor(methodName: String): String? {
        return when {
            methodName.startsWith("get") && methodName.length > 3 -> {
                methodName.substring(3).replaceFirstChar { it.lowercase() }
            }
            methodName.startsWith("set") && methodName.length > 3 -> {
                methodName.substring(3).replaceFirstChar { it.lowercase() }
            }
            methodName.startsWith("is") && methodName.length > 2 -> {
                methodName.substring(2).replaceFirstChar { it.lowercase() }
            }
            else -> null
        }
    }
}