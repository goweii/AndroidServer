package per.goweii.androidserver.runtime

import android.os.Build
import com.koushikdutta.async.http.body.AsyncHttpRequestBody
import com.koushikdutta.async.http.body.JSONArrayBody
import com.koushikdutta.async.http.body.JSONObjectBody
import com.koushikdutta.async.http.body.StringBody
import com.koushikdutta.async.http.server.AsyncHttpServerRequest
import com.koushikdutta.async.http.server.AsyncHttpServerResponse
import org.json.JSONArray
import org.json.JSONObject
import per.goweii.androidserver.runtime.annotation.PathVariable
import per.goweii.androidserver.runtime.annotation.QueryParam
import per.goweii.androidserver.runtime.annotation.RequestBody
import per.goweii.androidserver.runtime.utils.JsonUtils
import java.lang.reflect.Method

internal data class RequestParam(
    val method: Method,
    val type: Class<*>,
    val annotations: List<Annotation>,
) {
    val isRequestType: Boolean = type == AsyncHttpServerRequest::class.java

    val isResponseType: Boolean = type == AsyncHttpServerResponse::class.java

    private lateinit var mExtractor: Extractor<*>

    fun prepare() {
        if (::mExtractor.isInitialized) return

        val annotations = arrayListOf<Annotation>()

        val pathVariable = this.annotations.filterIsInstance<PathVariable>().firstOrNull()?.also { annotations.add(it) }
        val queryParam = this.annotations.filterIsInstance<QueryParam>().firstOrNull()?.also { annotations.add(it) }
        val requestBody = this.annotations.filterIsInstance<RequestBody>().firstOrNull()?.also { annotations.add(it) }

        if (annotations.size > 1) {
            throw RequestMethodParseException(
                "RequestMapping method '${method.toGenericString()}' parameter has more than one annotation: ${annotations}."
            )
        }

        if (isRequestType) {
            if (annotations.isNotEmpty()) {
                throw RequestMethodParseException(
                    "RequestMapping method '${method.toGenericString()}' parameter 'AsyncHttpServerRequest' cannot have " +
                            "'${annotations.first()}' annotations."
                )
            }

            mExtractor = AsyncHttpServerRequestExtractor
            return
        }

        if (isResponseType) {
            if (annotations.isNotEmpty()) {
                throw RequestMethodParseException(
                    "RequestMapping method '${method.toGenericString()}' parameter 'AsyncHttpServerResponse' cannot have " +
                            "'${annotations.first()}' annotations."
                )
            }

            mExtractor = AsyncHttpServerResponseExtractor
            return
        }

        val converter = StringToTypeConverters.getStringToTypeConverter(type)
            ?: throw RequestMethodParseException(
                "RequestMapping method '${method.toGenericString()}' parameter '$type' not supported."
            )

        if (pathVariable != null) {
            mExtractor = PathVariableExtractor(pathVariable.value, converter)
            return
        }

        if (queryParam != null) {
            mExtractor = QueryParamExtractor(queryParam.value, converter)
            return
        }

        if (requestBody != null) {
            mExtractor = RequestBodyExtractor(type, converter)
            return
        }

        throw RequestMethodParseException(
            "RequestMapping method '${method.toGenericString()}' parameter '${type}' without annotation."
        )
    }

    fun extractValue(request: AsyncHttpServerRequest, response: AsyncHttpServerResponse): Any? {
        prepare()
        return mExtractor.extract(request, response)
    }
}


/// region Extractor

private fun interface Extractor<T> {
    fun extract(request: AsyncHttpServerRequest, response: AsyncHttpServerResponse): T
}

private object AsyncHttpServerRequestExtractor : Extractor<AsyncHttpServerRequest> {
    override fun extract(request: AsyncHttpServerRequest, response: AsyncHttpServerResponse): AsyncHttpServerRequest {
        return request
    }
}

private object AsyncHttpServerResponseExtractor : Extractor<AsyncHttpServerResponse> {
    override fun extract(request: AsyncHttpServerRequest, response: AsyncHttpServerResponse): AsyncHttpServerResponse {
        return response
    }
}

private class PathVariableExtractor<T>(
    private val key: String,
    private val converter: Converter<String, T>,
) : Extractor<T?> {
    override fun extract(request: AsyncHttpServerRequest, response: AsyncHttpServerResponse): T? {
        val value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            request.matcher.group(key)
        } else {
            var index = 0
            val pattern = request.matcher.pattern().pattern()
            val matcher = """\(\?<(\w+)>.*?\)""".toPattern().matcher(pattern)
            while (matcher.find()) {
                index--
                val group = matcher.group(1)
                if (group == key) {
                    index = -index
                    break
                }
            }

            if (index > 0) {
                request.matcher.group(index)
            } else {
                null
            }
        }

        if (value != null) {
            return converter.convert(value)
        }

        return null
    }
}

private class QueryParamExtractor<T>(
    private val key: String,
    private val converter: Converter<String, T>,
) : Extractor<T?> {
    override fun extract(request: AsyncHttpServerRequest, response: AsyncHttpServerResponse): T? {
        val value = request.query.getString(key)

        if (value != null) {
            return converter.convert(value)
        }

        return null
    }
}

private class RequestBodyExtractor(
    private val type: Class<*>,
    private val converter: Converter<String, *>,
) : Extractor<Any?> {
    override fun extract(request: AsyncHttpServerRequest, response: AsyncHttpServerResponse): Any? {
        if (type.isPrimitive) {
            return when (val body = request.getBody<AsyncHttpRequestBody<*>>()) {
                is JSONObjectBody -> {
                    throw RequestException("request body is not a '$type'.")
                }

                is JSONArrayBody -> {
                    throw RequestException("request body is not a '$type'.")
                }

                is StringBody -> {
                    val string = body.get()

                    if (string != null) {
                        converter.convert(string)
                    } else {
                        null
                    }
                }

                else -> throw RequestException("Unsupported body type '${body.contentType}'.")
            }
        }

        return when (type) {
            String::class.java -> {
                when (val body = request.getBody<AsyncHttpRequestBody<*>>()) {
                    is JSONObjectBody -> body.get().toString()
                    is JSONArrayBody -> body.get().toString()
                    is StringBody -> body.get()
                    else -> throw RequestException("Unsupported body type '${body.contentType}'.")
                }
            }

            JSONObject::class.java -> {
                when (val body = request.getBody<AsyncHttpRequestBody<*>>()) {
                    is JSONObjectBody -> body.get()
                    is JSONArrayBody -> throw RequestException("request body is not a JSONObject.")
                    is StringBody -> JSONObject(body.get())
                    else -> throw RequestException("Unsupported body type '${body.contentType}'.")
                }
            }

            JSONArray::class.java -> {
                when (val body = request.getBody<AsyncHttpRequestBody<*>>()) {
                    is JSONObjectBody -> throw RequestException("request body is not a JSONArray.")
                    is JSONArrayBody -> body.get()
                    is StringBody -> JSONObject(body.get())
                    else -> throw RequestException("Unsupported body type '${body.contentType}'.")
                }
            }

            else -> {
                when (val body = request.getBody<AsyncHttpRequestBody<*>>()) {
                    is JSONObjectBody -> JsonUtils.fromJson(body.get().toString(), type)
                    is JSONArrayBody -> JsonUtils.fromJson(body.get().toString(), type)
                    is StringBody -> JsonUtils.fromJson(body.get(), type)
                    else -> throw RequestException("Unsupported body type '${body.contentType}'.")
                }
            }
        }
    }
}

/// endregion


/// region Converter

private fun interface Converter<I, O> {
    fun convert(value: I): O
}

private object StringToTypeConverters {
    private val stringToStringConverter = Converter<String, String> { it }
    private val stringToShortConverter = Converter<String, Short> { it.toShort() }
    private val stringToIntConverter = Converter<String, Int> { it.toInt() }
    private val stringToLongConverter = Converter<String, Long> { it.toLong() }
    private val stringToByteConverter = Converter<String, Byte> { it.toByte() }
    private val stringToFloatConverter = Converter<String, Float> { it.toFloat() }
    private val stringToDoubleConverter = Converter<String, Double> { it.toDouble() }
    private val stringToBooleanConverter = Converter<String, Boolean> { it.toBoolean() }
    private val stringToJSONObjectConverter = Converter<String, JSONObject> { JSONObject(it) }
    private val stringToJSONArrayConverter = Converter<String, JSONArray> { JSONArray(it) }
    private fun stringToAnyConverter(type: Class<*>) = Converter<String, Any> { JsonUtils.fromJson(it, type) }

    fun getStringToTypeConverter(type: Class<*>) =
        if (type.isPrimitive) {
            @Suppress("RemoveRedundantQualifierName")
            when (type) {
                java.lang.Short.TYPE -> stringToShortConverter
                java.lang.Integer.TYPE -> stringToIntConverter
                java.lang.Long.TYPE -> stringToLongConverter
                java.lang.Byte.TYPE -> stringToByteConverter
                java.lang.Float.TYPE -> stringToFloatConverter
                java.lang.Double.TYPE -> stringToDoubleConverter
                java.lang.Boolean.TYPE -> stringToBooleanConverter
                else -> null
            }
        } else if (type == java.lang.Short::class.java) {
            stringToShortConverter
        } else if (type == java.lang.Integer::class.java) {
            stringToIntConverter
        } else if (type == java.lang.Long::class.java) {
            stringToLongConverter
        } else if (type == java.lang.Byte::class.java) {
            stringToByteConverter
        } else if (type == java.lang.Float::class.java) {
            stringToFloatConverter
        } else if (type == java.lang.Double::class.java) {
            stringToDoubleConverter
        } else if (type == java.lang.Boolean::class.java) {
            stringToBooleanConverter
        } else if (type == String::class.java) {
            stringToStringConverter
        } else if (type == JSONObject::class.java) {
            stringToJSONObjectConverter
        } else if (type == JSONArray::class.java) {
            stringToJSONArrayConverter
        } else {
            stringToAnyConverter(type)
        }
}

/// endregion