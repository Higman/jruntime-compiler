package com.github.Higman.jruntime_compiler

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

internal class DynamicCompilerTest {

    @Test
    fun 指定のJavaファイルをコンパイルする() {
        val fileURL = javaClass.getResource("/DynamicCompilerTest/TestSample.java")?: fail("リソースがない")
        val clazz: Class<TestBaseClass> = DynamicCompiler.compile(fileURL)
        assertTrue(clazz.getConstructor().newInstance().test())
    }
}

interface TestBaseClass {
    fun test(): Boolean
}
