package org.morecup.pragmaddd.analyzer.callanalysis

import org.morecup.pragmaddd.analyzer.callanalysis.model.MethodCallContext
import org.morecup.pragmaddd.analyzer.callanalysis.model.AggregateRootMethodCall
import org.objectweb.asm.*
import java.io.File
import java.io.FileInputStream

/**
 * 字段访问分析器
 * 
 * 基于已有的domain-analyzer.json结果，分析方法对聚合根字段的访问模式
 */
class FieldAccessAnalyzer(
    private val domainAnalysisResult: Map<String, Any>, // 从domain-analyzer.json加载的结果
    private val aggregateRootClasses: Set<String>
) {
    
    /**
     * 增强方法调用上下文，添加字段访问信息
     */
    fun enhanceWithFieldAccess(contexts: List<MethodCallContext>): List<MethodCallContext> {
        return contexts.map { context ->
            enhanceMethodCallContext(context)
        }
    }
    
    /**
     * 增强单个方法调用上下文
     */
    private fun enhanceMethodCallContext(context: MethodCallContext): MethodCallContext {
        val enhancedAggregateRootCalls = context.aggregateRootMethodCalls.map { call ->
            enhanceAggregateRootMethodCall(call)
        }
        
        val allRequiredFields = enhancedAggregateRootCalls.flatMap { it.requiredFields }.toSet()
        
        return context.copy(
            aggregateRootMethodCalls = enhancedAggregateRootCalls,
            requiredFields = allRequiredFields
        )
    }
    
    /**
     * 增强聚合根方法调用，添加字段访问信息
     */
    private fun enhanceAggregateRootMethodCall(call: AggregateRootMethodCall): AggregateRootMethodCall {
        val requiredFields = analyzeMethodFieldAccess(
            call.aggregateRootMethod,
            call.aggregateRootMethodDescriptor
        )
        
        return call.copy(requiredFields = requiredFields)
    }
    
    /**
     * 分析方法的字段访问
     */
    private fun analyzeMethodFieldAccess(methodName: String, methodDescriptor: String): Set<String> {
        val fields = mutableSetOf<String>()
        

        
        // 从domain-analyzer.json中查找对应方法的字段访问信息
        // 首先检查classes数组
        @Suppress("UNCHECKED_CAST")
        val classes = domainAnalysisResult["classes"] as? List<Map<String, Any>> ?: return fields
        
        for (classInfo in classes) {
            val className = classInfo["className"] as? String ?: continue
            if (!aggregateRootClasses.contains(className)) continue
            
            // 检查propertyAccessAnalysis数组
            @Suppress("UNCHECKED_CAST")
            val propertyAccessAnalysis = classInfo["propertyAccessAnalysis"] as? List<Map<String, Any>> ?: continue
            
            for (methodInfo in propertyAccessAnalysis) {
                val methodNameInAnalysis = methodInfo["methodName"] as? String ?: continue
                val methodDescriptorInAnalysis = methodInfo["methodDescriptor"] as? String ?: continue
                
                if (methodNameInAnalysis == methodName && methodDescriptorInAnalysis == methodDescriptor) {
                    // 收集访问的属性
                    @Suppress("UNCHECKED_CAST")
                    val accessedProperties = methodInfo["accessedProperties"] as? List<String> ?: emptyList()
                    fields.addAll(accessedProperties)
                    
                    // 收集修改的属性（修改也意味着需要加载）
                    @Suppress("UNCHECKED_CAST")
                    val modifiedProperties = methodInfo["modifiedProperties"] as? List<String> ?: emptyList()
                    fields.addAll(modifiedProperties)
                    
                    // 递归分析调用的方法
                    @Suppress("UNCHECKED_CAST")
                    val calledMethods = methodInfo["calledMethods"] as? List<Map<String, Any>> ?: emptyList()
                    for (calledMethod in calledMethods) {
                        val calledClassName = calledMethod["className"] as? String ?: continue
                        val calledMethodName = calledMethod["methodName"] as? String ?: continue
                        val calledMethodDescriptor = calledMethod["methodDescriptor"] as? String ?: continue
                        
                        if (aggregateRootClasses.contains(calledClassName)) {
                            // 递归分析被调用的聚合根方法
                            val recursiveFields = analyzeMethodFieldAccess(calledMethodName, calledMethodDescriptor)
                            fields.addAll(recursiveFields)
                        }
                    }
                }
            }
        }
        
        return fields
    }
    
    /**
     * 直接分析类文件的字段访问（作为备用方案）
     */
    fun analyzeClassFieldAccess(classFile: File, methodName: String, methodDescriptor: String): Set<String> {
        return try {
            FileInputStream(classFile).use { input ->
                val classReader = ClassReader(input)
                val visitor = FieldAccessClassVisitor(methodName, methodDescriptor)
                classReader.accept(visitor, ClassReader.EXPAND_FRAMES)
                visitor.getAccessedFields()
            }
        } catch (e: Exception) {
            println("分析字段访问失败: ${classFile.absolutePath}, 错误: ${e.message}")
            emptySet()
        }
    }
    
    /**
     * 字段访问类访问器
     */
    private class FieldAccessClassVisitor(
        private val targetMethodName: String,
        private val targetMethodDescriptor: String
    ) : ClassVisitor(Opcodes.ASM9) {
        
        private val accessedFields = mutableSetOf<String>()
        
        override fun visitMethod(
            access: Int,
            name: String,
            descriptor: String,
            signature: String?,
            exceptions: Array<out String>?
        ): MethodVisitor? {
            return if (name == targetMethodName && descriptor == targetMethodDescriptor) {
                FieldAccessMethodVisitor()
            } else {
                super.visitMethod(access, name, descriptor, signature, exceptions)
            }
        }
        
        fun getAccessedFields(): Set<String> {
            return accessedFields.toSet()
        }
        
        /**
         * 字段访问方法访问器
         */
        private inner class FieldAccessMethodVisitor : MethodVisitor(Opcodes.ASM9) {
            
            override fun visitFieldInsn(opcode: Int, owner: String, name: String, descriptor: String) {
                if (opcode == Opcodes.GETFIELD || opcode == Opcodes.PUTFIELD) {
                    accessedFields.add(name)
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
                // 分析getter/setter方法调用
                if (name.startsWith("get") || name.startsWith("set") || name.startsWith("is")) {
                    val fieldName = extractFieldNameFromAccessor(name)
                    if (fieldName.isNotEmpty()) {
                        accessedFields.add(fieldName)
                    }
                }
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
            }
            
            /**
             * 从访问器方法名提取字段名
             */
            private fun extractFieldNameFromAccessor(methodName: String): String {
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
                    else -> ""
                }
            }
        }
    }
}