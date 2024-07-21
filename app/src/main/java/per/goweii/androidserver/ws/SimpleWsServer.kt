package per.goweii.androidserver.ws

import per.goweii.androidserver.runtime.HttpServer
import per.goweii.androidserver.runtime.PathMatcher

class SimpleWsServer: HttpServer() {
    override val port: Int = 24721
    override val pathMatcher: PathMatcher = PathMatcher()
        .includePathStartWith("/ws")
}