package per.goweii.androidserver.runtime.utils

object HashUtils {
    fun interProcessHashCode(any: Any): Int {
        var hash = android.os.Process.myPid()
        hash = hash shl 24
        hash = hash or System.identityHashCode(any)
        return hash
    }

    fun innerProcessHashCode(any: Any): Int {
        return System.identityHashCode(any)
    }
}

val Any.interProcessHashCode: Int
    get() = HashUtils.interProcessHashCode(this)

val Any.innerProcessHashCode: Int
    get() = HashUtils.innerProcessHashCode(this)