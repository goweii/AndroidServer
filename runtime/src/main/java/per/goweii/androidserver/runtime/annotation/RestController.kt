package per.goweii.androidserver.runtime.annotation

import androidx.annotation.Keep

@Keep
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class RestController(
    val path: String = "",
    val protocol: String = "",
)
