package per.goweii.androidserver.runtime.exception_handler

import com.koushikdutta.async.http.server.AsyncHttpServerRequest
import com.koushikdutta.async.http.server.AsyncHttpServerResponse

interface ExceptionHandler<E: Throwable> {
    fun handle(
        request: AsyncHttpServerRequest,
        response: AsyncHttpServerResponse,
        e: E
    )
}