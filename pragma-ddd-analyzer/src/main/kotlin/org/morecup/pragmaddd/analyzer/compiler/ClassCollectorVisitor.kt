package org.morecup.pragmaddd.analyzer.compiler

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid

/**
 * IR visitor that collects all class declarations from the IR tree
 */
class ClassCollectorVisitor(
    private val classes: MutableList<IrClass>
) : IrElementVisitorVoid {
    
    override fun visitElement(element: IrElement) {
        element.acceptChildren(this, null)
    }
    
    override fun visitClass(declaration: IrClass) {
        // Add this class to our collection
        classes.add(declaration)
        
        // Continue visiting nested classes
        super.visitClass(declaration)
    }
}