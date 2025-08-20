package org.morecup.pragmaddd.analyzer

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.assertj.core.api.Assertions.assertThat
import org.objectweb.asm.*
import java.io.File
import java.io.FileOutputStream

class AggregateRootAnalyzerTest {
    
    @Test
    fun `should analyze aggregate root class with property access`() {
        // 创建测试用的字节码
        val classBytes = createTestAggregateRootClass()
        val tempFile = File.createTempFile("TestAggregateRoot", ".class")
        tempFile.deleteOnExit()
        
        FileOutputStream(tempFile).use { it.write(classBytes) }
        
        // 分析类
        val analyzer = AggregateRootAnalyzer()
        val result = analyzer.analyzeClass(tempFile)
        
        // 验证结果
        assertThat(result).isNotNull
        assertThat(result!!.className).isEqualTo("TestAggregateRoot")
        assertThat(result.isAggregateRoot).isTrue()
        assertThat(result.methods).isNotEmpty()
    }
    
    @Test
    fun `should return null for non-aggregate root class`() {
        // 创建没有 @AggregateRoot 注解的类
        val classBytes = createTestNormalClass()
        val tempFile = File.createTempFile("TestNormalClass", ".class")
        tempFile.deleteOnExit()
        
        FileOutputStream(tempFile).use { it.write(classBytes) }
        
        // 分析类
        val analyzer = AggregateRootAnalyzer()
        val result = analyzer.analyzeClass(tempFile)
        
        // 验证结果
        assertThat(result).isNull()
    }
    
    private fun createTestAggregateRootClass(): ByteArray {
        val cw = ClassWriter(ClassWriter.COMPUTE_FRAMES)
        
        cw.visit(
            Opcodes.V11,
            Opcodes.ACC_PUBLIC,
            "TestAggregateRoot",
            null,
            "java/lang/Object",
            null
        )
        
        // 添加 @AggregateRoot 注解
        val av = cw.visitAnnotation("Lorg/morecup/pragmaddd/core/annotation/AggregateRoot;", true)
        av.visitEnd()
        
        // 添加字段
        cw.visitField(Opcodes.ACC_PRIVATE, "name", "Ljava/lang/String;", null, null).visitEnd()
        cw.visitField(Opcodes.ACC_PRIVATE, "age", "I", null, null).visitEnd()
        
        // 添加构造函数
        val constructor = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null)
        constructor.visitCode()
        constructor.visitVarInsn(Opcodes.ALOAD, 0)
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
        constructor.visitInsn(Opcodes.RETURN)
        constructor.visitMaxs(1, 1)
        constructor.visitEnd()
        
        // 添加测试方法，包含属性访问
        val method = cw.visitMethod(Opcodes.ACC_PUBLIC, "updateName", "(Ljava/lang/String;)V", null, null)
        method.visitCode()
        // this.name = name;
        method.visitVarInsn(Opcodes.ALOAD, 0)
        method.visitVarInsn(Opcodes.ALOAD, 1)
        method.visitFieldInsn(Opcodes.PUTFIELD, "TestAggregateRoot", "name", "Ljava/lang/String;")
        // int currentAge = this.age;
        method.visitVarInsn(Opcodes.ALOAD, 0)
        method.visitFieldInsn(Opcodes.GETFIELD, "TestAggregateRoot", "age", "I")
        method.visitVarInsn(Opcodes.ISTORE, 2)
        method.visitInsn(Opcodes.RETURN)
        method.visitMaxs(2, 3)
        method.visitEnd()
        
        cw.visitEnd()
        return cw.toByteArray()
    }
    
    private fun createTestNormalClass(): ByteArray {
        val cw = ClassWriter(ClassWriter.COMPUTE_FRAMES)
        
        cw.visit(
            Opcodes.V11,
            Opcodes.ACC_PUBLIC,
            "TestNormalClass",
            null,
            "java/lang/Object",
            null
        )
        
        // 不添加 @AggregateRoot 注解
        
        // 添加构造函数
        val constructor = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null)
        constructor.visitCode()
        constructor.visitVarInsn(Opcodes.ALOAD, 0)
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
        constructor.visitInsn(Opcodes.RETURN)
        constructor.visitMaxs(1, 1)
        constructor.visitEnd()
        
        cw.visitEnd()
        return cw.toByteArray()
    }
}