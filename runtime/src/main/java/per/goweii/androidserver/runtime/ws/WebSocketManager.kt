package per.goweii.androidserver.runtime.ws

import com.koushikdutta.async.http.WebSocket

class WebSocketManager {
    private val webSockets = hashMapOf<Any, WebSocket>()

    fun add(key: Any, webSocket: WebSocket) {
        val value = webSockets.remove(key)
        if (value === webSocket) {
            webSockets[key] = value
            return
        }
        webSockets[key] = webSocket
    }

    fun remove(key: Any) {
        webSockets.remove(key)
    }

    operator fun get(key: Any): WebSocket? {
        return webSockets[key]
    }
}