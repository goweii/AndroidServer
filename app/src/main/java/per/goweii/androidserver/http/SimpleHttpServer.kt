package per.goweii.androidserver.http

import per.goweii.androidserver.runtime.PathMatcher
import per.goweii.androidserver.runtime.HttpServer

class SimpleHttpServer : HttpServer() {
    override val port: Int = 24720
    override val pathMatcher: PathMatcher = PathMatcher()
        .includePathStartWith("/api")
}