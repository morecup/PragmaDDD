#!/bin/bash

# 混合处理策略测试脚本
echo "=== 混合处理策略测试 ==="

# 清理构建目录
echo "1. 清理构建目录..."
./gradlew :demo:clean > /dev/null 2>&1

# 测试1：只编译Kotlin类
echo "2. 测试Kotlin类处理..."
./gradlew :demo:compileKotlin --rerun-tasks > kotlin-build.log 2>&1
if [ -f "demo/build/generated/pragmaddd/main/resources/META-INF/pragma-ddd-analyzer/domain-analyzer.json" ]; then
    kotlin_classes=$(grep -o '"totalClasses" : [0-9]*' demo/build/generated/pragmaddd/main/resources/META-INF/pragma-ddd-analyzer/domain-analyzer.json | grep -o '[0-9]*')
    echo "   Kotlin类分析结果: $kotlin_classes 个类"
    cp demo/build/generated/pragmaddd/main/resources/META-INF/pragma-ddd-analyzer/domain-analyzer.json kotlin-result.json
else
    echo "   错误: Kotlin分析结果文件不存在"
fi

# 清理并测试2：只编译Java类
echo "3. 测试Java类处理..."
./gradlew :demo:clean > /dev/null 2>&1
./gradlew :demo:compileJava --rerun-tasks > java-build.log 2>&1
if [ -f "demo/build/generated/pragmaddd/main/resources/META-INF/pragma-ddd-analyzer/domain-analyzer.json" ]; then
    java_classes=$(grep -o '"totalClasses" : [0-9]*' demo/build/generated/pragmaddd/main/resources/META-INF/pragma-ddd-analyzer/domain-analyzer.json | grep -o '[0-9]*')
    echo "   Java类分析结果: $java_classes 个类"
    cp demo/build/generated/pragmaddd/main/resources/META-INF/pragma-ddd-analyzer/domain-analyzer.json java-result.json
else
    echo "   错误: Java分析结果文件不存在"
fi

# 测试3：完整构建
echo "4. 测试完整构建..."
./gradlew :demo:build > full-build.log 2>&1
if [ -f "demo/build/generated/pragmaddd/main/resources/META-INF/pragma-ddd-analyzer/domain-analyzer.json" ]; then
    total_classes=$(grep -o '"totalClasses" : [0-9]*' demo/build/generated/pragmaddd/main/resources/META-INF/pragma-ddd-analyzer/domain-analyzer.json | grep -o '[0-9]*')
    echo "   完整构建结果: $total_classes 个类"
else
    echo "   错误: 完整构建结果文件不存在"
fi

echo "=== 测试完成 ==="
echo "结果文件:"
echo "  - kotlin-result.json: Kotlin类分析结果"
echo "  - java-result.json: Java类分析结果"
echo "  - demo/build/generated/pragmaddd/main/resources/META-INF/pragma-ddd-analyzer/domain-analyzer.json: 最终结果"
