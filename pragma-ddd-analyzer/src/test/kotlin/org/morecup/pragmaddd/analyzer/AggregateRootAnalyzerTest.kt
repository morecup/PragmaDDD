package org.morecup.pragmaddd.analyzer

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import org.assertj.core.api.Assertions.assertThat
import java.io.File

class AggregateRootAnalyzerTest {
    
    @TempDir
    lateinit var tempDir: File
    
    private lateinit var analyzer: AggregateRootAnalyzer
    
    @BeforeEach
    fun setup() {
        analyzer = AggregateRootAnalyzer()
    }
    
    @Test
    fun `should record all methods including those without property access or method calls`() {
        // 这个测试验证修改后的行为：所有方法都应该被记录，不管是否有属性访问或方法调用
        
        // 创建一个简单的测试类字节码（这里我们模拟测试场景）
        // 实际的字节码分析需要真实的class文件，这里我们验证逻辑
        
        val visitor = AggregateRootClassVisitor()
        
        // 模拟访问一个带有@AggregateRoot注解的类
        visitor.visit(52, 1, "com/example/TestClass", null, "java/lang/Object", null)
        visitor.visitAnnotation("Lorg/morecup/pragmaddd/core/annotation/AggregateRoot;", true)
        
        // 模拟访问一个没有任何属性访问或方法调用的方法
        val methodVisitor = visitor.visitMethod(1, "emptyMethod", "()V", null, null)
        assertThat(methodVisitor).isInstanceOf(PropertyAccessMethodVisitor::class.java)
        
        // 结束方法访问
        methodVisitor?.visitEnd()
        
        val result = visitor.getResult()
        assertThat(result).isNotNull()
        assertThat(result!!.isAggregateRoot).isTrue()
        
        // 验证即使是空方法也被记录了
        assertThat(result.methods).hasSize(1)
        val method = result.methods[0]
        assertThat(method.methodName).isEqualTo("emptyMethod")
        assertThat(method.methodDescriptor).isEqualTo("()V")
        assertThat(method.accessedProperties).isEmpty()
        assertThat(method.modifiedProperties).isEmpty()
        assertThat(method.calledMethods).isEmpty()
    }
    
    @Test
    fun `should record method calls from all classes not just current class`() {
        // 这个测试验证修改后的行为：应该记录所有类的方法调用，不仅仅是当前类
        
        val methods = mutableListOf<PropertyAccessInfo>()
        val methodVisitor = PropertyAccessMethodVisitor("com.example.TestClass", "testMethod", "()V", methods)
        
        // 模拟调用当前类的方法
        methodVisitor.visitMethodInsn(182, "com/example/TestClass", "currentClassMethod", "()V", false)
        
        // 模拟调用其他类的方法
        methodVisitor.visitMethodInsn(182, "com/example/OtherClass", "otherClassMethod", "()V", false)
        methodVisitor.visitMethodInsn(182, "java/lang/String", "toString", "()Ljava/lang/String;", false)
        
        methodVisitor.visitEnd()
        
        assertThat(methods).hasSize(1)
        val method = methods[0]
        
        // 验证记录了所有方法调用，包括其他类的
        assertThat(method.calledMethods).hasSize(3)
        
        val methodCalls = method.calledMethods.map { "${it.className}.${it.methodName}" }
        assertThat(methodCalls).contains(
            "com.example.TestClass.currentClassMethod",
            "com.example.OtherClass.otherClassMethod", 
            "java.lang.String.toString"
        )
    }
    
    @Test
    fun `should record method calls with full class names`() {
        val methods = mutableListOf<PropertyAccessInfo>()
        val methodVisitor = PropertyAccessMethodVisitor("com.example.TestClass", "testMethod", "()V", methods)
        
        // 模拟方法调用
        methodVisitor.visitMethodInsn(182, "java/util/List", "add", "(Ljava/lang/Object;)Z", true)
        methodVisitor.visitEnd()
        
        assertThat(methods).hasSize(1)
        val method = methods[0]
        assertThat(method.calledMethods).hasSize(1)
        
        val calledMethod = method.calledMethods.first()
        assertThat(calledMethod.className).isEqualTo("java.util.List")
        assertThat(calledMethod.methodName).isEqualTo("add")
        assertThat(calledMethod.methodDescriptor).isEqualTo("(Ljava/lang/Object;)Z")
        assertThat(calledMethod.callCount).isEqualTo(1)
    }
    
    @Test
    fun `should count multiple calls to same method`() {
        val methods = mutableListOf<PropertyAccessInfo>()
        val methodVisitor = PropertyAccessMethodVisitor("com.example.TestClass", "testMethod", "()V", methods)
        
        // 模拟多次调用同一个方法
        methodVisitor.visitMethodInsn(182, "java/lang/String", "toString", "()Ljava/lang/String;", false)
        methodVisitor.visitMethodInsn(182, "java/lang/String", "toString", "()Ljava/lang/String;", false)
        methodVisitor.visitMethodInsn(182, "java/lang/String", "toString", "()Ljava/lang/String;", false)
        methodVisitor.visitEnd()
        
        assertThat(methods).hasSize(1)
        val method = methods[0]
        assertThat(method.calledMethods).hasSize(1)
        
        val calledMethod = method.calledMethods.first()
        assertThat(calledMethod.className).isEqualTo("java.lang.String")
        assertThat(calledMethod.methodName).isEqualTo("toString")
        assertThat(calledMethod.callCount).isEqualTo(3)
    }
}