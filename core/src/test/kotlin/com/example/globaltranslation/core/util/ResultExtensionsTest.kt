package com.example.globaltranslation.core.util

import org.junit.Assert.*
import org.junit.Test

class ResultExtensionsTest {
    
    @Test
    fun `mapBoth() transforms success value`() {
        val result = Result.success(5)
        val mapped = result.mapBoth(
            onSuccess = { it * 2 },
            onFailure = { it }
        )
        assertEquals(10, mapped.getOrNull())
    }
    
    @Test
    fun `mapBoth() transforms failure exception`() {
        val result = Result.failure<Int>(IllegalArgumentException("original"))
        val mapped = result.mapBoth(
            onSuccess = { it },
            onFailure = { IllegalStateException("transformed", it) }
        )
        assertTrue(mapped.isFailure)
        assertTrue(mapped.exceptionOrNull() is IllegalStateException)
        assertEquals("transformed", mapped.exceptionOrNull()?.message)
    }
    
    @Test
    fun `onSuccessAction() executes side effect on success`() {
        var sideEffectExecuted = false
        val result = Result.success(5)
        
        val returned = result.onSuccessAction { sideEffectExecuted = true }
        
        assertTrue(sideEffectExecuted)
        assertEquals(result, returned)
    }
    
    @Test
    fun `onSuccessAction() does not execute on failure`() {
        var sideEffectExecuted = false
        val result = Result.failure<Int>(Exception())
        
        result.onSuccessAction { sideEffectExecuted = true }
        
        assertFalse(sideEffectExecuted)
    }
    
    @Test
    fun `onFailureAction() executes side effect on failure`() {
        var capturedMessage: String? = null
        val result = Result.failure<Int>(Exception("test error"))
        
        val returned = result.onFailureAction { capturedMessage = it.message }
        
        assertEquals("test error", capturedMessage)
        assertEquals(result, returned)
    }
    
    @Test
    fun `onFailureAction() does not execute on success`() {
        var sideEffectExecuted = false
        val result = Result.success(5)
        
        result.onFailureAction { sideEffectExecuted = true }
        
        assertFalse(sideEffectExecuted)
    }
    
    @Test
    fun `orDefault() returns success value`() {
        val result = Result.success(5)
        assertEquals(5, result.orDefault(10))
    }
    
    @Test
    fun `orDefault() returns default on failure`() {
        val result = Result.failure<Int>(Exception())
        assertEquals(10, result.orDefault(10))
    }
    
    @Test
    fun `recoverWith() returns success value`() {
        val result = Result.success(5)
        assertEquals(5, result.recoverWith { 10 })
    }
    
    @Test
    fun `recoverWith() computes default on failure`() {
        val result = Result.failure<Int>(Exception("error"))
        val recovered = result.recoverWith { throwable ->
            if (throwable.message == "error") 42 else 0
        }
        assertEquals(42, recovered)
    }
    
    @Test
    fun `andThen() chains successful operations`() {
        val result = Result.success(5)
        val chained = result.andThen { value ->
            Result.success(value * 2)
        }
        assertEquals(10, chained.getOrNull())
    }
    
    @Test
    fun `andThen() short-circuits on failure`() {
        val result = Result.failure<Int>(Exception("first error"))
        val chained = result.andThen { value ->
            Result.success(value * 2)
        }
        assertTrue(chained.isFailure)
        assertEquals("first error", chained.exceptionOrNull()?.message)
    }
    
    @Test
    fun `andThen() propagates new failure`() {
        val result = Result.success(5)
        val chained = result.andThen { 
            Result.failure<Int>(Exception("second error"))
        }
        assertTrue(chained.isFailure)
        assertEquals("second error", chained.exceptionOrNull()?.message)
    }
    
    @Test
    fun `multiple operations can be chained`() {
        val result = Result.success(5)
            .andThen { Result.success(it * 2) }
            .andThen { Result.success(it + 3) }
            .andThen { Result.success(it.toString()) }
        
        assertEquals("13", result.getOrNull())
    }
}
