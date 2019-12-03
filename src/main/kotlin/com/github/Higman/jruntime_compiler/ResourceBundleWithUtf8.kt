package com.github.Higman.jruntime_compiler

import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

/**
 * UTF-8で記述されたプロパティファイルを取り扱う{@link ResourceBundle} オブジェクト
 */
object ResourceBundleWithUtf8 : ResourceBundle.Control() {
    override fun newBundle(baseName: String, locale: Locale, format: String, loader: ClassLoader, reload: Boolean): ResourceBundle? {
        if (format != "java.properties") return null
        val bundleName = toBundleName(baseName, locale)
        val resourceName = toResourceName(bundleName, "properties")
        loader.getResourceAsStream(resourceName).use { inputStream ->
            InputStreamReader(inputStream, "UTF-8").use { inputStreamReader ->
                BufferedReader(inputStreamReader).use { reader -> return PropertyResourceBundle(reader) }
            }
        }
    }
}