package per.goweii.androidserver.runtime.interceptor

import com.koushikdutta.async.http.server.AsyncHttpServerRequest
import com.koushikdutta.async.http.server.AsyncHttpServerResponse

interface RequestInterceptor {
    fun intercept(
        request: AsyncHttpServerRequest,
        response: AsyncHttpServerResponse,
    ): Boolean
}