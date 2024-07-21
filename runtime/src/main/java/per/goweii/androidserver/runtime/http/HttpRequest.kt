package per.goweii.androidserver.runtime.http

import android.util.Log
import com.koushikdutta.async.http.server.AsyncHttpServerRequest
import com.koushikdutta.async.http.server.AsyncHttpServerResponse
import com.koushikdutta.async.http.server.HttpServerRequestCallback

internal class HttpRequest(
    val method: HttpMethod,
    val path: HttpPath,
    val requestMethod: HttpRequestMethod,
): HttpServerRequestCallback {
    companion object {
        private const val TAG = "HttpRequest"
    }

    override fun onRequest(request: AsyncHttpServerRequest, response: AsyncHttpServerResponse) {
        Log.i(TAG, request.toString())
        try {
            requestMethod.invoke(request, response)
        } finally {
            Log.i(TAG, response.toString())
        }
    }
}