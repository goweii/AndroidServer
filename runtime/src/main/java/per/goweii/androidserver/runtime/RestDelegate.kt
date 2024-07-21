package per.goweii.androidserver.runtime

import per.goweii.androidserver.runtime.http.HttpPath

internal abstract class RestDelegate(
    val path: HttpPath,
    val protocol: String,
)