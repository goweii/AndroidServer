package per.goweii.androidserver.runtime

import com.koushikdutta.async.http.server.UnknownRequestBody

internal object NoRequestBody: UnknownRequestBody("*/*")