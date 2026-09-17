package com.zenlauncher.zenmode

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/**
 * TEMP: Kite basket-redirect spike (gold-streak instrument, see zenmode_docs/docs/features/gold-streak.md).
 * Fires the order-review POST (https://kite.trade/docs/connect/v3/basket/) directly from an
 * in-app WebView via WebView.postUrl, instead of routing through an externally hosted page —
 * removes the LAN/http.server dependency the earlier test page needed. Remove this whole class
 * once the Kite flow either graduates into the real settlement screen or gets dropped.
 */
class KiteBasketActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_QUANTITY = "quantity"
        private const val BASKET_URL = "https://kite.zerodha.com/connect/basket"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val quantity = intent.getIntExtra(EXTRA_QUANTITY, 1)
        val order = JSONArray().put(
            JSONObject()
                .put("exchange", "NSE")
                .put("tradingsymbol", "GOLDBEES")
                .put("transaction_type", "BUY")
                .put("order_type", "MARKET")
                .put("quantity", quantity)
                .put("product", "CNC")
                .put("validity", "IOC")
        )
        val postData = "api_key=${URLEncoder.encode(BuildConfig.KITE_API_KEY, "UTF-8")}" +
            "&data=${URLEncoder.encode(order.toString(), "UTF-8")}"

        setContent {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        webViewClient = WebViewClient()
                        postUrl(BASKET_URL, postData.toByteArray())
                    }
                }
            )
        }
    }
}
