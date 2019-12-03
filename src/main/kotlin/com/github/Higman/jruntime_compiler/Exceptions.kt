package com.github.Higman.jruntime_compiler

/**
 * 実行環境のバージョンが不適切であった場合発生する例外
 */
internal class IllegalJavaRuntimeEnvironmentVersion : Exception {
    constructor() : super() {}

    constructor(message: String) : super(message) {}
}

/**
 * コンパイルモジュールが取得できなかった場合発生する例外
 * JDKが正しく指定されてない場合、コンパイルモジュールの取得に失敗する
 */
internal class NonSystemJavaCompilerException : Exception {

    constructor() : super() {}

    constructor(message: String) : super(message) {}
}