package com.github.Higman.jruntime_compiler

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.URI
import java.security.SecureClassLoader
import java.util.*
import javax.tools.*

/**
 * class情報を管理するクラス
 */
internal class ClassFileManager(compiler: JavaCompiler, listener: DiagnosticListener<in JavaFileObject>?) : ForwardingJavaFileManager<JavaFileManager>(compiler.getStandardFileManager(listener, null, null)) {

    protected val map: MutableMap<String, JavaClassFileObject> = mutableMapOf()
    protected var loader: ClassLoader? = null

    override fun getJavaFileForOutput(location: JavaFileManager.Location, className: String, kind: JavaFileObject.Kind, sibling: FileObject): JavaFileObject {
        return JavaClassFileObject(className, kind).apply { map[className] = this }
    }

    override fun getClassLoader(location: JavaFileManager.Location?): ClassLoader = loader
            ?: Loader().apply { loader = this }

    private inner class Loader : SecureClassLoader() {
        override fun findClass(name: String): Class<*>? {
            val jcfo = map[name] ?: return super.findClass(name)

            if (jcfo.clazz == null) {
                val b = jcfo.getBytes()
                jcfo.clazz = super.defineClass(name, b, 0, b.size)
            }
            return jcfo.clazz
        }
    }
}

/**
 * javaファイルを保持するクラス
 */
internal class JavaSourceFileObject(name: String, val code: String) : SimpleJavaFileObject(
    URI.create("string:///" + name.replace('.', '/') + JavaFileObject.Kind.SOURCE.extension),
    JavaFileObject.Kind.SOURCE
) {
    override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence = code
}

/**
 * classファイルを保持するクラス
 */
internal class JavaClassFileObject(name: String, kind: JavaFileObject.Kind) : SimpleJavaFileObject(
    URI.create("string:///" + name.replace('.', '/') + JavaFileObject.Kind.SOURCE.extension), kind) {
    protected val bos = ByteArrayOutputStream()

    override fun openOutputStream(): OutputStream = bos
    fun getBytes(): ByteArray = bos.toByteArray()
    var clazz: Class<*>? = null
}

/**
 * コンパイル時のエラー出力クラス
 */
internal class ErrorListener : DiagnosticListener<JavaFileObject> {
    override fun report(diagnostic: Diagnostic<out JavaFileObject>) {
        println("errcode：" + diagnostic.code)
        println("line   ：" + diagnostic.lineNumber)
        println("column ：" + diagnostic.columnNumber)
        println("message：" + diagnostic.getMessage(Locale.getDefault()))
    }
}