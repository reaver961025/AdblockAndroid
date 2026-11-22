package io.github.edsuns.adfilter.impl

import io.github.edsuns.adfilter.CustomFilter
import io.github.edsuns.adfilter.util.RuleIterator

/**
 * Created by Edsuns@qq.com on 2021/7/29.
 */
internal class CustomFilterImpl constructor(
    private val filterDataLoader: FilterDataLoader,
    data: String? = null
) : CustomFilter, RuleIterator(data) {

    override fun flush() {
        val blacklistStr = dataBuilder.toString()
        if (blacklistStr.isNotBlank()) {
            // The JNI side (AdBlockClient) expects UTF-8 bytes; be explicit instead
            // of relying on the platform default charset.
            val rawData = blacklistStr.toByteArray(Charsets.UTF_8)
            filterDataLoader.loadCustomFilter(rawData)
        }
    }
}
