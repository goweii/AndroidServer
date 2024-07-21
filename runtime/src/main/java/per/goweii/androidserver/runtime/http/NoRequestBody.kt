package per.goweii.androidserver.runtime.http

import com.koushikdutta.async.http.server.UnknownRequestBody

internal object NoRequestBody: UnknownRequestBody("*/*")