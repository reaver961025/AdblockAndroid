package io.github.edsuns.adblockclient.sample.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.github.edsuns.adblockclient.sample.main.blocking.BlockingInfo
import io.github.edsuns.adblockclient.sample.stripParamsAndAnchor
import io.github.edsuns.adfilter.FilterResult
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by Edsuns@qq.com on 2021/2/27.
 */
class MainViewModel : ViewModel() {

    // logRequest() runs on the WebView background interceptor thread (one call per
    // concurrent resource load) while clearDirty() and the LiveData observers run
    // on the Main thread. The original plain HashMap raced here —
    // ConcurrentModificationException on the map plus lost counter updates — so the
    // outer map is a ConcurrentHashMap and all mutations are serialized on a lock.
    private val lock = Any()
    private val _blockingInfoMap = ConcurrentHashMap<String, BlockingInfo>()

    val blockingInfoMap = MutableLiveData<Map<String, BlockingInfo>>(_blockingInfoMap)

    var currentPageUrl: MutableLiveData<String> = MutableLiveData()

    var dirtyBlockingInfo = false

    fun clearDirty() {
        synchronized(lock) {
            if (dirtyBlockingInfo) {
                _blockingInfoMap.clear()
                dirtyBlockingInfo = false
            }
        }
    }

    fun logRequest(filterResult: FilterResult) {
        val pageUrl = currentPageUrl.value ?: return
        val snapshot: Map<String, BlockingInfo>
        synchronized(lock) {
            val blockingInfo = _blockingInfoMap[pageUrl] ?: BlockingInfo()
            _blockingInfoMap[pageUrl] = blockingInfo
            if (filterResult.shouldBlock) {
                val requestUrl = filterResult.resourceUrl.stripParamsAndAnchor()
                blockingInfo.blockedUrlMap[requestUrl] = filterResult.rule ?: ""
                blockingInfo.blockedRequests++
                Timber.v("Web request $requestUrl blocked by rule \"${filterResult.rule}\"")
            }
            blockingInfo.allRequests++
            // Hand observers a stable shallow copy so a later background mutation
            // can't race a Main-thread read of the outer map.
            snapshot = HashMap(_blockingInfoMap)
        }
        blockingInfoMap.postValue(snapshot)
    }
}
