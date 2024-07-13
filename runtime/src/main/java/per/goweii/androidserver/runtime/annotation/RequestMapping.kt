package per.goweii.androidserver.runtime.annotation

import androidx.annotation.Keep
import per.goweii.androidserver.runtime.HttpMethod

@Keep
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class RequestMapping(
    val method: HttpMethod = HttpMethod.GET,
    val path: String,
)