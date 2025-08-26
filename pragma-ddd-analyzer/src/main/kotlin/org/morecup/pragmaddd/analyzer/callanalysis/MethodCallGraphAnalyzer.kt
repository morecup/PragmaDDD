package org.morecup.pragmaddd.analyzer.callanalysis

import org.morecup.pragmaddd.analyzer.callanalysis.model.*
import org.objectweb.asm.*
import java.io.File
import java.io.FileInputStream

/**
 * 方法调用图分析器
 * 
 * 分析字节码文件，构建方法调用关系图，识别Repository方法调用
 */
class MethodCallGraphAnalyzer(
    private val repositories: List<RepositoryInfo>,
    private val aggregateRootClasses: Set<String>
) {
    
    private val repositoryMethodMap = mutableMapOf<String, RepositoryInfo>()
    
    init {
        // 构建Repository方法映射
        repositories.forEach { repo ->
            repositoryMethodMap[repo.className] = repo
        }
    }
    
    /**
     * 分析目录中的所有类文件
     */
    fun analyzeDirectory(directory: File): List<MethodCallContext> {
        val contexts = mutableListOf<MethodCallContext>()
        
        directory.walkTopDown()
            .filter { it.isFile && it.extension == "class" }
            .forEach { classFile ->
                contexts.addAll(analyzeClassFile(classFile))
            }
        
        return contexts
    }
    
    /**
     * 分析单个类文件
     */
    fun analyzeClassFile(classFile: File): List<MethodCallContext> {
        return try {
            FileInputStream(classFile).use { input ->
                val classReader = ClassReader(input)
                val visitor = CallGraphClassVisitor()
                classReader.accept(visitor, ClassReader.EXPAND_FRAMES)
                visitor.getMethodCallContexts()
            }
        } catch (e: Exception) {
            println("分析调用图失败: ${classFile.absolutePath}, 错误: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * 调用图类访问器
     */
    private inner class CallGraphClassVisitor : ClassVisitor(Opcodes.ASM9) {
        private var className: String = ""
        private val methodContexts = mutableListOf<MethodCallContext>()
        
        override fun visit(
            version: Int,
            access: Int,
            name: String,
            signature: String?,
            superName: String?,
            interfaces: Array<out String>?
        ) {
            this.className = name.replace('/', '.')
            super.visit(version, access, name, signature, superName, interfaces)
        }
        
        override fun visitMethod(
            access: Int,
            name: String,
            descriptor: String,
            signature: String?,
            exceptions: Array<out String>?
        ): MethodVisitor? {
            // 跳过构造函数、静态初始化块等
            if (name == "<init>" || name == "<clinit>") {
                return super.visitMethod(access, name, descriptor, signature, exceptions)
            }
            
            return CallGraphMethodVisitor(className, name, descriptor)
        }
        
        fun getMethodCallContexts(): List<MethodCallContext> {
            return methodContexts.toList()
        }
        
        /**
         * 方法调用图访问器
         */
        private inner class CallGraphMethodVisitor(
            private val ownerClass: String,
            private val methodName: String,
            private val methodDescriptor: String
        ) : MethodVisitor(Opcodes.ASM9) {
            
            private val repositoryCalls = mutableListOf<RepositoryCallInfo>()
            private val aggregateRootMethodCalls = mutableListOf<AggregateRootMethodCall>()
            private var startLine: Int = -1
            private var endLine: Int = -1
            
            override fun visitLineNumber(line: Int, start: Label?) {
                if (startLine == -1) {
                    startLine = line
                }
                endLine = line
                super.visitLineNumber(line, start)
            }
            
            override fun visitMethodInsn(
                opcode: Int,
                owner: String,
                name: String,
                descriptor: String,
                isInterface: Boolean
            ) {
                val ownerClassName = owner.replace('/', '.')
                
                // 检查是否为Repository方法调用
                repositoryMethodMap[ownerClassName]?.let { repoInfo ->
                    val repositoryCall = RepositoryCallInfo(
                        repositoryClass = ownerClassName,
                        repositoryMethod = name,
                        repositoryMethodDescriptor = descriptor,
                        aggregateRootClass = repoInfo.aggregateRootClass
                    )
                    repositoryCalls.add(repositoryCall)
                }
                
                // 检查是否为聚合根方法调用
                if (aggregateRootClasses.contains(ownerClassName)) {
                    val aggregateRootCall = AggregateRootMethodCall(
                        aggregateRootMethod = name,
                        aggregateRootMethodDescriptor = descriptor,
                        requiredFields = emptySet() // 稍后通过字段访问分析填充
                    )
                    aggregateRootMethodCalls.add(aggregateRootCall)
                }
                
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
            }
            
            override fun visitEnd() {
                // 只有包含Repository调用的方法才需要记录
                if (repositoryCalls.isNotEmpty()) {
                    val context = MethodCallContext(
                        className = ownerClass,
                        methodName = methodName,
                        methodDescriptor = methodDescriptor,
                        startLine = startLine,
                        endLine = endLine,
                        repositoryCall = repositoryCalls.firstOrNull(), // 简化处理，取第一个
                        aggregateRootMethodCalls = aggregateRootMethodCalls,
                        requiredFields = emptySet() // 稍后通过字段访问分析填充
                    )
                    methodContexts.add(context)
                }
                super.visitEnd()
            }
        }
    }
}