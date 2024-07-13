package per.goweii.androidserver.runtime

import com.koushikdutta.async.http.server.AsyncHttpServerRequest
import com.koushikdutta.async.http.server.AsyncHttpServerResponse
import org.json.JSONArray
import org.json.JSONObject
import per.goweii.androidserver.runtime.utils.JsonUtils
import java.lang.reflect.Method

internal data class ReturnParam(
    val method: Method,
    val type: Class<*>,
) {
    val isVoid: Boolean = type == Void.TYPE

    fun prepare() {
    }

    fun sendValue(request: AsyncHttpServerRequest, response: AsyncHttpServerResponse, returnValue: Any?) {
        if (isVoid) {
            return
        }

        if (returnValue == null) {
            return
        }

        if (type.isPrimitive) {
            response.send(returnValue.toString())
        } else if (type == java.lang.Short::class.java) {
            response.send(returnValue.toString())
        } else if (type == java.lang.Integer::class.java) {
            response.send(returnValue.toString())
        } else if (type == java.lang.Long::class.java) {
            response.send(returnValue.toString())
        } else if (type == java.lang.Byte::class.java) {
            response.send(returnValue.toString())
        } else if (type == java.lang.Float::class.java) {
            response.send(returnValue.toString())
        } else if (type == java.lang.Double::class.java) {
            response.send(returnValue.toString())
        } else if (type == java.lang.Boolean::class.java) {
            response.send(returnValue.toString())
        } else if (type == String::class.java) {
            response.send(returnValue.toString())
        } else if (type == JSONObject::class.java) {
            response.send(returnValue as JSONObject)
        } else if (type == JSONArray::class.java) {
            response.send(returnValue as JSONArray)
        } else {
            response.send(JsonUtils.toJson(returnValue))
        }
    }
}