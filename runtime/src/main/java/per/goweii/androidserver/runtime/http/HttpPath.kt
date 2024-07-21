package per.goweii.androidserver.runtime.http

data class HttpPath(
    private val value: String,
) {
    val path: String = kotlin.run {
        var path = value
        if (path.isBlank()) {
            path = "/"
        }
        if (path == "/") {
            return@run path
        }
        if (!path.startsWith("/")) {
            path = "/$path"
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length - 1)
        }
        path
    }

    val pathPattern: String = path.replace("""\{([^/]+?)\}""".toRegex(), "(?<$1>[^/]+?)")

    operator fun plus(path: String): HttpPath {
        return this + HttpPath(path)
    }

    operator fun plus(other: HttpPath): HttpPath {
        if (other.path == "/") {
            return this
        }
        if (this.path == "/") {
            return other
        }
        return HttpPath(this.path + other.path)
    }
}