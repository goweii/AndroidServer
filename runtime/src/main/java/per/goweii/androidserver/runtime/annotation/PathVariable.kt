package per.goweii.androidserver.runtime.annotation

import androidx.annotation.Keep

@Keep
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class PathVariable(
    val value: String,
)
