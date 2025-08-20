package org.morecup.jimmerddd.pragmaddd.aspect

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.*

@Aspect
class MethodAccessAspect {

//    @Pointcut("execution(* *(..)) && @within(org.morecup.pragmaddd.core.annotation.AggregateRoot) && !within(org.morecup.pragmaddd.aspect.MethodAccessAspect)")
//    fun anyMethodExecution() {}
//
//    @Before("anyMethodExecution()")
//    fun beforeMethodExecution(pjp: ProceedingJoinPoint) {
//        val methodSignature = pjp.signature as MethodSignature
//        MethodBridgeConfig.methodBridge.beforeMethodInvocation(
//            pjp,
//            methodSignature.method,
//            pjp.target,
//            pjp.args
//        )
//    }

//    @AfterReturning(pointcut = "anyMethodExecution()", returning = "result")
//    fun afterMethodExecution(pjp: ProceedingJoinPoint, result: Any?) {
//        val methodSignature = pjp.signature as MethodSignature
//        MethodBridgeConfig.methodBridge.afterMethodInvocation(
//            pjp,
//            methodSignature.method,
//            pjp.target,
//            pjp.args,
//            result
//        )
//    }
//
//    @Around("anyMethodExecution()")
//    fun aroundMethodExecution(pjp: ProceedingJoinPoint): Any? {
//        val methodSignature = pjp.signature as MethodSignature
//        return MethodBridgeConfig.methodBridge.aroundMethodInvocation(
//            pjp,
//            methodSignature.method,
//            pjp.target,
//            pjp.args
//        )
//    }
}