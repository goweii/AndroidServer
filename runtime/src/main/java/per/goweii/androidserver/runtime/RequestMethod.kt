package per.goweii.androidserver.runtime

import com.koushikdutta.async.http.server.AsyncHttpServerRequest
import com.koushikdutta.async.http.server.AsyncHttpServerResponse
import java.lang.reflect.Method

internal class RequestMethod(
    val instance: Any,
    val method: Method,
) {
    private val requestParams = arrayListOf<RequestParam>()
    private val returnParam: ReturnParam

    init {
        method.isAccessible = true

        val parameterTypes = method.parameterTypes
        val parameterAnnotations = method.parameterAnnotations
        for (i in 0 until method.parameterCount) {
            requestParams.add(
                RequestParam(
                    method = method,
                    type = parameterTypes[i],
                    annotations = parameterAnnotations[i].toList(),
                )
            )
        }

        returnParam = ReturnParam(
            method = method,
            type = method.returnType,
        )

        ensureValidMethod()
    }

    fun invoke(request: AsyncHttpServerRequest, response: AsyncHttpServerResponse) {
        val args = requestParams.map { it.extractValue(request, response) }.toTypedArray()

        val returnValue = method.invoke(instance, *args)

        returnParam.sendValue(request, response, returnValue)

        response.end()
    }

    private fun ensureValidMethod() {
        when (requestParams.count { it.isRequestType }) {
            0 -> {}
            1 -> {}
            else -> throw RequestMethodParseException(
                "RequestMapping method '${method.toGenericString()}' has more than one " +
                        "'AsyncHttpServerRequest' parameter."
            )
        }

        when (requestParams.count { it.isResponseType }) {
            0 -> {}
            1 -> {
                if (!returnParam.isVoid) {
                    throw RequestMethodParseException(
                        "RequestMapping method '${method.toGenericString()}' cannot simultaneously set the " +
                                "'AsyncHttpServerResponse' parameter and return value."
                    )
                }
            }

            else -> throw RequestMethodParseException(
                "RequestMapping method '${method.toGenericString()}' has more than one " +
                        "'AsyncHttpServerResponse' parameter."
            )
        }

        requestParams.forEach { it.prepare() }

        returnParam.prepare()
    }
}