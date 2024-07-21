package per.goweii.androidserver.runtime.utils

object HashUtils {
    fun interProcessHashCode(any: Any): Int {
        return android.os.Process.myPid() + System.identityHashCode(any)
    }

    fun innerProcessHashCode(any: Any): Int {
        return System.identityHashCode(any)
    }
}

val Any.interProcessHashCode: Int
    get() = HashUtils.interProcessHashCode(this)

val Any.innerProcessHashCode: Int
    get() = HashUtils.innerProcessHashCode(this)