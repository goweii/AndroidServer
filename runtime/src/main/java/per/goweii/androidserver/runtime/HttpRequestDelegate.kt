package per.goweii.androidserver.runtime

import android.util.Log
import com.koushikdutta.async.http.server.AsyncHttpServerRequest
import com.koushikdutta.async.http.server.AsyncHttpServerResponse
import com.koushikdutta.async.http.server.HttpServerRequestCallback

internal class HttpRequestDelegate(
    val method: HttpMethod,
    val path: String,
    val requestMethod: RequestMethod,
): HttpServerRequestCallback {
    companion object {
        private const val TAG = "HttpRequestDelegate"
    }

    val pathRegex: String = path.replace("""\{([^/]+?)\}""".toRegex(), "(?<$1>[^/]+?)")

    override fun onRequest(request: AsyncHttpServerRequest, response: AsyncHttpServerResponse) {
        Log.i(TAG, request.toString())
        try {
            requestMethod.invoke(request, response)
        } catch (e: Throwable) {
            response.send(e.toString())
        } finally {
            response.end()
            Log.i(TAG, response.toString())
        }
    }
}