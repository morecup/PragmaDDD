package org.morecup.pragmaddd.analyzer.compiletime

import org.morecup.pragmaddd.analyzer.compiletime.model.*
import org.morecup.pragmaddd.analyzer.model.DetailedClassInfo
import org.morecup.pragmaddd.analyzer.model.DetailedMethodInfo
import org.morecup.pragmaddd.analyzer.model.DomainObjectType
import org.morecup.pragmaddd.analyzer.model.PropertyAccessInfo

/**
 * 字段访问递归分析器
 * 基于domain-analyzer.json结果，递归分析聚合根方法的字段访问模式
 */
class FieldAccessAnalyzer(
    private val config: FieldAccessAnalysisConfig = FieldAccessAnalysisConfig()
) {
    
    /**
     * 分析Repository方法调用需要的字段
     * @param repositoryCallInfo Repository调用信息
     * @param callGraph 方法调用图
     * @param domainAnalysisResult domain-analyzer.json分析结果
     * @return 需要的字段集合
     */
    fun analyzeRequiredFields(
        repositoryCallInfo: RepositoryCallInfo,
        callGraph: CallGraph,
        domainAnalysisResult: Map<String, DetailedClassInfo>
    ): FieldAccessResult {
        val aggregateRootClass = repositoryCallInfo.aggregateRootClass
        val callerMethod = repositoryCallInfo.callerMethod
        
        // 查找调用方方法调用了哪些聚合根方法
        val calledAggregateRootMethods = findCalledAggregateRootMethods(
            callerMethod, 
            aggregateRootClass,
            callGraph, 
            domainAnalysisResult
        )
        
        // 递归分析每个被调用的聚合根方法需要的字段
        val allRequiredFields = mutableSetOf<String>()
        val calledMethodInfos = mutableListOf<CalledMethodInfo>()
        
        for (calledMethod in calledAggregateRootMethods) {
            val methodRequiredFields = analyzeMethodFieldAccess(
                calledMethod,
                domainAnalysisResult,
                mutableSetOf(), // 访问跟踪，避免循环依赖
                0 // 递归深度
            )
            
            allRequiredFields.addAll(methodRequiredFields)
            calledMethodInfos.add(
                CalledMethodInfo(
                    aggregateRootMethod = calledMethod.methodName,
                    aggregateRootMethodDescriptor = calledMethod.descriptor,
                    requiredFields = methodRequiredFields
                )
            )
        }
        
        return FieldAccessResult(
            calledAggregateRootMethods = calledMethodInfos,
            requiredFields = allRequiredFields
        )
    }
    
    /**
     * 查找调用方方法调用了哪些聚合根方法
     */
    private fun findCalledAggregateRootMethods(
        callerMethod: MethodInfo,
        aggregateRootClass: String,
        callGraph: CallGraph,
        domainAnalysisResult: Map<String, DetailedClassInfo>
    ): Set<MethodInfo> {
        val calledMethods = mutableSetOf<MethodInfo>()
        val calledMethodInfos = callGraph.getMethodCalls(callerMethod)
        
        for (calledMethod in calledMethodInfos) {
            if (calledMethod.className == aggregateRootClass) {
                // 验证该方法确实存在于聚合根类中
                val aggregateRootClassInfo = domainAnalysisResult[aggregateRootClass]
                if (aggregateRootClassInfo != null) {
                    val methodExists = aggregateRootClassInfo.methods.any { method ->
                        method.name == calledMethod.methodName && 
                        method.descriptor == calledMethod.descriptor
                    }
                    if (methodExists) {
                        calledMethods.add(calledMethod)
                    }
                }
            }
        }
        
        return calledMethods
    }
    
    /**
     * 递归分析方法的字段访问
     */
    private fun analyzeMethodFieldAccess(
        method: MethodInfo,
        domainAnalysisResult: Map<String, DetailedClassInfo>,
        visited: MutableSet<MethodInfo>,
        depth: Int
    ): Set<String> {
        // 防止循环依赖和过深递归
        if (method in visited || depth > config.maxRecursionDepth) {
            return emptySet()
        }
        
        if (config.enableCircularDependencyDetection) {
            visited.add(method)
        }
        
        val requiredFields = mutableSetOf<String>()
        val classInfo = domainAnalysisResult[method.className] ?: return requiredFields
        
        // 查找对应的方法信息
        val methodInfo = findMethodInfo(classInfo, method)
        if (methodInfo != null) {
            // 收集直接字段访问
            requiredFields.addAll(methodInfo.accessedProperties)
            
            // 从原有的属性访问分析中获取字段访问信息
            val propertyAccessInfo = findPropertyAccessInfo(classInfo, method)
            if (propertyAccessInfo != null) {
                requiredFields.addAll(propertyAccessInfo.accessedProperties)
                
                // 递归分析被调用的方法
                for (calledMethodInfo in propertyAccessInfo.calledMethods) {
                    if (isAggregateRootMethod(calledMethodInfo.className, domainAnalysisResult)) {
                        val calledMethod = MethodInfo(
                            calledMethodInfo.className,
                            calledMethodInfo.methodName,
                            calledMethodInfo.methodDescriptor
                        )
                        val nestedFields = analyzeMethodFieldAccess(
                            calledMethod,
                            domainAnalysisResult,
                            visited,
                            depth + 1
                        )
                        requiredFields.addAll(nestedFields)
                    }
                }
                
                // 处理外部属性访问（对其他聚合根的访问）
                for (externalAccess in propertyAccessInfo.externalPropertyAccesses) {
                    if (externalAccess.targetDomainObjectType == DomainObjectType.AGGREGATE_ROOT ||
                        externalAccess.targetDomainObjectType == DomainObjectType.DOMAIN_ENTITY ||
                        externalAccess.targetDomainObjectType == DomainObjectType.VALUE_OBJECT) {
                        
                        // 使用类名前缀表示嵌套属性访问
                        val qualifiedPropertyName = "${externalAccess.targetClassName.substringAfterLast('.')}.${externalAccess.propertyName}"
                        requiredFields.add(qualifiedPropertyName)
                    }
                }
            }
        }
        
        if (config.enableCircularDependencyDetection) {
            visited.remove(method)
        }
        
        return requiredFields
    }
    
    private fun findMethodInfo(
        classInfo: DetailedClassInfo,
        method: MethodInfo
    ): DetailedMethodInfo? {
        return classInfo.methods.find { methodInfo ->
            methodInfo.name == method.methodName && 
            methodInfo.descriptor == method.descriptor
        }
    }
    
    private fun findPropertyAccessInfo(
        classInfo: DetailedClassInfo,
        method: MethodInfo
    ): PropertyAccessInfo? {
        return classInfo.propertyAccessAnalysis.find { accessInfo ->
            accessInfo.methodName == method.methodName && 
            accessInfo.methodDescriptor == method.descriptor
        }
    }
    
    private fun isAggregateRootMethod(
        className: String,
        domainAnalysisResult: Map<String, DetailedClassInfo>
    ): Boolean {
        val classInfo = domainAnalysisResult[className] ?: return false
        return classInfo.domainObjectType == DomainObjectType.AGGREGATE_ROOT ||
               classInfo.domainObjectType == DomainObjectType.DOMAIN_ENTITY ||
               classInfo.domainObjectType == DomainObjectType.VALUE_OBJECT
    }
    
    /**
     * 字段访问分析结果
     */
    data class FieldAccessResult(
        val calledAggregateRootMethods: List<CalledMethodInfo>,
        val requiredFields: Set<String>
    )
}

/**
 * 字段访问分析工具类
 */
object FieldAccessAnalysisUtils {
    
    /**
     * 扁平化嵌套字段访问
     * 例如：["address.name", "address.detail"] -> ["address", "address.name", "address.detail"]
     */
    fun flattenNestedFields(fields: Set<String>): Set<String> {
        val flattenedFields = mutableSetOf<String>()
        
        for (field in fields) {
            val parts = field.split('.')
            for (i in 1..parts.size) {
                flattenedFields.add(parts.take(i).joinToString("."))
            }
        }
        
        return flattenedFields
    }
    
    /**
     * 过滤只读字段访问
     * 排除setter方法和修改操作的字段
     */
    fun filterReadOnlyFields(
        fields: Set<String>,
        propertyAccessInfos: List<PropertyAccessInfo>
    ): Set<String> {
        val modifiedProperties = propertyAccessInfos
            .flatMap { it.modifiedProperties }
            .toSet()
        
        return fields.filter { field ->
            !modifiedProperties.contains(field) && 
            !field.contains("set") && // 简单过滤setter相关
            !field.contains("update") && // 过滤更新相关
            !field.contains("change") // 过滤修改相关
        }.toSet()
    }
    
    /**
     * 合并多个字段访问结果
     */
    fun mergeFieldAccessResults(results: List<FieldAccessAnalyzer.FieldAccessResult>): FieldAccessAnalyzer.FieldAccessResult {
        val allCalledMethods = mutableListOf<CalledMethodInfo>()
        val allRequiredFields = mutableSetOf<String>()
        
        for (result in results) {
            allCalledMethods.addAll(result.calledAggregateRootMethods)
            allRequiredFields.addAll(result.requiredFields)
        }
        
        return FieldAccessAnalyzer.FieldAccessResult(
            calledAggregateRootMethods = allCalledMethods,
            requiredFields = allRequiredFields
        )
    }
    
    /**
     * 生成字段访问路径
     * 用于调试和可视化
     */
    fun generateFieldAccessPaths(
        method: MethodInfo,
        domainAnalysisResult: Map<String, DetailedClassInfo>
    ): Map<String, List<String>> {
        val paths = mutableMapOf<String, List<String>>()
        val classInfo = domainAnalysisResult[method.className] ?: return paths
        
        val propertyAccessInfo = classInfo.propertyAccessAnalysis.find { accessInfo ->
            accessInfo.methodName == method.methodName && 
            accessInfo.methodDescriptor == method.descriptor
        } ?: return paths
        
        // 构建字段访问路径
        for (property in propertyAccessInfo.accessedProperties) {
            paths[property] = listOf(method.className, method.methodName, property)
        }
        
        // 添加外部属性访问路径
        for (externalAccess in propertyAccessInfo.externalPropertyAccesses) {
            val qualifiedProperty = "${externalAccess.targetClassName}.${externalAccess.propertyName}"
            paths[qualifiedProperty] = listOf(
                method.className, 
                method.methodName, 
                externalAccess.targetClassName,
                externalAccess.propertyName
            )
        }
        
        return paths
    }
}