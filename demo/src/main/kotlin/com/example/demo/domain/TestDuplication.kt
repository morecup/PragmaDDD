package com.example.demo.domain

import org.morecup.pragmaddd.core.annotation.AggregateRoot

@AggregateRoot
class TestDuplication {
    private var value: Int = 0
    
    fun testMethod() {
        helper()
    }
    
    private fun helper() {
        value = 42
    }
}