package com.github.Higman.jruntime_compiler

import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import javax.lang.model.SourceVersion
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.ToolProvider

/**
 * 指定のクラスファイルの動的コンパイルオブジェクト<br>
 * 動的コンパイルを行うには、compileメソッドを用いる。<br>
 * 現状、JDK8でのみ動作する。<br>
 */
object DynamicCompiler {
    const val DEFAULT_PACKAGE_NAME = "hoge.piyo.default._package"

    val MIN_JDK_VERSION = SourceVersion.RELEASE_8

    private val resourceBundle: ResourceBundle = ResourceBundle.getBundle(javaClass.name, Locale.getDefault(), ResourceBundleWithUtf8)

    private var listener: DiagnosticCollector<in JavaFileObject>? = null
    private val compiler by lazy { ToolProvider.getSystemJavaCompiler() }

    /**
     * 指定のURLのjavaファイルのクラスインスタンスを取得する
     * @param url javaファイルのURL
     * @return javaファイルを動的コンパイルして取得したインスタンス
     * @exception NonSystemJavaCompilerException 動的コンパイル用のコンパイラを取得できなかった際に発生.環境変数JAVA_HOMEに指定されている実行環境がJDKのものでは場合がある.
     * @exception IllegalJavaRuntimeEnvironmentVersion 環境変数で指定されているJDKのバージョンが不適切である場合発生.
     */
    fun <T> compile(url: URL): Class<T> {
        if (compiler == null) throw NonSystemJavaCompilerException()
        // バージョン確認
        if (compiler.sourceVersions.last().ordinal < MIN_JDK_VERSION.ordinal) throw IllegalJavaRuntimeEnvironmentVersion()
        val fileLines = Files.readAllLines(Paths.get(url.toURI()))
        val (code, packageName) = convertCodeIncludingPackagePhrase(fileLines)
        val className = packageName + "." + extractClassName(url)
        return compile(className, code)
    }

    private val SEPARATOR_LINE = "------"

    /**
     * 指定のクラス情報を持つクラスインスタンスを取得する
     * @param className クラス名
     * @param sourceCode コード
     * @return コンパイルして取得したインスタンス
     */
    private fun <T> compile(className: String, sourceCode: String): Class<T> {
        val jsfo = JavaSourceFileObject(className, sourceCode)

        val compilationUnits = Arrays.asList<JavaFileObject>(jsfo)
        val options = Arrays.asList("-classpath", System.getProperty("java.class.path"))
        val manager = ClassFileManager(compiler, listener)
        listener = DiagnosticCollector()
        val task = compiler.getTask(null,
                manager,  //出力ファイルを扱うマネージャー
                listener, //エラー時の処理を行うリスナー（nullでもよい）
                options, null,
                compilationUnits    //コンパイル対象ファイル群
        )

        //コンパイル実行
        val isSuccessed = task.call()!!
        if (!isSuccessed) {
            var message = "${resourceBundle.getString("exception.message.fail_compile")}\n"
            message += listener?.diagnostics?.joinToString(separator = "\n$SEPARATOR_LINE\n", prefix = "$SEPARATOR_LINE\n", postfix = "\n$SEPARATOR_LINE\n") { it.toString() }  // 各エラー詳細を連結
            throw RuntimeException(message)
        }

        try {
            return manager.getClassLoader(null).loadClass(className) as Class<T>
        } catch (e: ClassNotFoundException) {
            throw RuntimeException(e)
        }
    }

    /**
     * 指定のjavaファイルのクラス名の抽出
     * @param url javaファイルのURL
     * @return クラス名文字列
     */
    private fun extractClassName(url: URL): String {
        val regex = """.*/(\w+)\.java$""".toRegex()
        return regex.matchEntire(url.toString())?.groupValues?.get(1)
                ?: throw IllegalArgumentException(resourceBundle.getString("exception.message.illegale_url"))
    }

    /**
     * ソースコードの変換
     * ソースコードを文字列のリストで受け取り、パッケージ名を抽出する
     * コード中にパッケージ名が指定されていない場合、パッケージ名を指定する行をコードに追加する
     * @param codeLines ソースコードのリスト
     * @return ソースコード文字列とパッケージ名文字列
     */
    private fun convertCodeIncludingPackagePhrase(codeLines: List<String>): Pair<String, String> {
        val regex = """^\s*package ([\w.]+);""".toRegex()
        val indexAtPackagePhrase = codeLines.indexOfFirst { line -> regex.matches(line) }
        val code = codeLines.joinToString("\n")

        return if (indexAtPackagePhrase >= 0) {
            val existedPackageName = regex.matchEntire(codeLines[indexAtPackagePhrase])?.groupValues?.get(1)
                    ?: throw IllegalArgumentException(resourceBundle.getString("exception.message.illegale_package"))
            Pair(code, existedPackageName)
        } else {
            val modifiedCode = "package ${DEFAULT_PACKAGE_NAME};\n" + code
            Pair(modifiedCode, DEFAULT_PACKAGE_NAME)
        }
    }
}

