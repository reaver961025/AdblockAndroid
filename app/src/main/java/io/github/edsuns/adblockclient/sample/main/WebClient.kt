package io.github.edsuns.adblockclient.sample.main

import android.graphics.Bitmap
import android.webkit.*
import io.github.edsuns.adfilter.AdFilter

/**
 * Created by Edsuns@qq.com on 2021/1/1.
 */
class WebClient(private val webViewClientListener: WebViewClientListener) : WebViewClient() {

    private val filter = AdFilter.get()

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return false
        return !URLUtil.isNetworkUrl(url)
    }

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        // WebView can invoke these callbacks with a null view/request during
        // teardown; bail out instead of force-unwrapping and NPE'ing.
        if (view == null || request == null) {
            return null
        }
        val result = filter.shouldIntercept(view, request)
        webViewClientListener.onShouldInterceptRequest(result)
        return result.resourceResponse
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        filter.performScript(view, url)
        webViewClientListener.onPageStarted(url, favicon)
    }
}
