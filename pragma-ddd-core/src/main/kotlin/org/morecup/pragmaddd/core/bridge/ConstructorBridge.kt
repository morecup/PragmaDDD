package org.morecup.pragmaddd.core.bridge

import org.aspectj.lang.ProceedingJoinPoint
import org.morecup.pragmaddd.core.proxy.DomainAggregateRootConstructor

object ConstructorBridgeConfig {
    var constructorBridge: IConstructorBridge = DomainAggregateRootConstructor()
}

class DefaultConstructorBridge: IConstructorBridge {
    override fun createInstance(pjp: ProceedingJoinPoint, args: Array<Any?>) {
        println("createInstance: ${pjp.signature}, args: ${args.contentToString()}")
    }
}

interface IConstructorBridge {
    fun createInstance(pjp: ProceedingJoinPoint, args: Array<Any?>)
}