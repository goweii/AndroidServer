package per.goweii.androidserver.runtime.utils

import android.os.Build
import java.util.regex.Matcher

operator fun Matcher.get(name: String): String? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        group(name)
    } else {
        var index = 0
        val pattern = pattern().pattern()
        val matcher = """\(\?<(\w+)>.*?\)""".toPattern().matcher(pattern)
        while (matcher.find()) {
            index--
            val group = matcher.group(1)
            if (group == name) {
                index = -index
                break
            }
        }

        matcher.group(index)
    }
}