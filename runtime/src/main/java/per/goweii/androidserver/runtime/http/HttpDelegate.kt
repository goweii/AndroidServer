package per.goweii.androidserver.runtime.http

import per.goweii.androidserver.runtime.RestDelegate

internal open class HttpDelegate(
    path: HttpPath,
    val requests: List<HttpRequest>,
) : RestDelegate(
    path = path,
)