package org.morecup.pragmaddd.analyzer.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import java.io.File

/**
 * 最简单的测试编译器插件，用于验证注册是否工作
 */
@OptIn(ExperimentalCompilerApi::class)
class TestCompilerPluginRegistrar : CompilerPluginRegistrar() {
    
    override val supportsK2: Boolean = true
    
    init {
        // 写入文件确保我们能检测到这个被调用了
        try {
            val debugFile = File("test-compiler-plugin-init.log")
            debugFile.appendText("TestCompilerPluginRegistrar initialized at ${System.currentTimeMillis()}\n")
        } catch (e: Exception) {
            // Ignore
        }
    }
    
    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        try {
            val debugFile = File("test-compiler-plugin-register.log")
            debugFile.appendText("TestCompilerPluginRegistrar.registerExtensions called at ${System.currentTimeMillis()}\n")
        } catch (e: Exception) {
            // Ignore
        }
        
        // 注册一个简单的 IR 扩展
        IrGenerationExtension.registerExtension(TestIrGenerationExtension())
    }
}

/**
 * 最简单的 IR 生成扩展
 */
class TestIrGenerationExtension : IrGenerationExtension {
    
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        try {
            val debugFile = File("test-ir-extension-generate.log")
            debugFile.appendText("TestIrGenerationExtension.generate called at ${System.currentTimeMillis()}\n")
        } catch (e: Exception) {
            // Ignore
        }
    }
}