package per.goweii.androidserver.runtime

import java.util.regex.Pattern

class PathMatcher {
    private val includePatterns: MutableList<Pattern> = arrayListOf()
    private val excludePatterns: MutableList<Pattern> = arrayListOf()

    fun includePath(pattern: Pattern) = apply {
        includePatterns.add(pattern)
    }

    fun includePath(path: String) = apply {
        includePatterns.add("""^${path}$""".toPattern())
    }

    fun includePathContains(path: String) = apply {
        includePatterns.add("""^.*?${path}.*?$""".toPattern())
    }

    fun includePathStartWith(prefix: String) = apply {
        includePatterns.add("^${prefix}.*".toPattern())
    }

    fun includePathEndWith(prefix: String) = apply {
        includePatterns.add(".*${prefix}$".toPattern())
    }

    fun excludePath(pattern: Pattern) = apply {
        excludePatterns.add(pattern)
    }

    fun excludePath(path: String) = apply {
        excludePatterns.add("""^${path}$""".toPattern())
    }

    fun excludePathContains(path: String) = apply {
        excludePatterns.add("""^.*?${path}.*?$""".toPattern())
    }

    fun excludePathStartWith(prefix: String) = apply {
        excludePatterns.add("^${prefix}.*".toPattern())
    }

    fun excludePathEndWith(prefix: String) = apply {
        excludePatterns.add(".*${prefix}$".toPattern())
    }

    fun match(path: String): Boolean {
        var matched = false

        kotlin.run {
            includePatterns.forEach { pattern ->
                val matcher = pattern.matcher(path)
                if (matcher.matches()) {
                    matched = true
                    return@run
                }
            }
        }

        if (!matched) {
            return false
        }

        kotlin.run {
            excludePatterns.forEach { pattern ->
                val matcher = pattern.matcher(path)
                if (matcher.matches()) {
                    matched = false
                    return@run
                }
            }
        }

        return matched
    }
}