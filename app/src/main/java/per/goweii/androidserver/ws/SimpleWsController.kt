package per.goweii.androidserver.ws

import com.koushikdutta.async.http.WebSocket
import com.koushikdutta.async.http.server.AsyncHttpServerRequest
import com.koushikdutta.async.http.server.AsyncHttpServerResponse
import per.goweii.androidserver.runtime.annotation.RestController
import per.goweii.androidserver.runtime.ws.WebSocketController

@RestController(value = "/ws")
class SimpleWsController : WebSocketController {
    override fun onRequest(request: AsyncHttpServerRequest, response: AsyncHttpServerResponse) {
    }

    override fun onConnect(webSocket: WebSocket) {
    }

    override fun onClose(e: Throwable) {
    }

    override fun onPing(message: String) {
    }

    override fun onPong(message: String) {
    }

    override fun onMessage(message: String) {
    }
}