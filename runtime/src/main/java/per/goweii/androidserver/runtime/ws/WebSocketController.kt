package per.goweii.androidserver.runtime.ws

import com.koushikdutta.async.http.WebSocket
import com.koushikdutta.async.http.server.AsyncHttpServerRequest
import com.koushikdutta.async.http.server.AsyncHttpServerResponse

interface WebSocketController {
    fun onRequest(request: AsyncHttpServerRequest, response: AsyncHttpServerResponse)

    fun onConnect(webSocket: WebSocket)

    fun onClose(e: Throwable)

    fun onPing(message: String)

    fun onPong(message: String)

    fun onMessage(message: String)
}