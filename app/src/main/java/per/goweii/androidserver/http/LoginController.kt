package per.goweii.androidserver.http

import per.goweii.androidserver.runtime.http.HttpMethod
import per.goweii.androidserver.runtime.annotation.PathVariable
import per.goweii.androidserver.runtime.annotation.QueryParam
import per.goweii.androidserver.runtime.annotation.RestController
import per.goweii.androidserver.runtime.annotation.RequestMapping

@RestController(value = "/api/user")
class LoginController {
    @RequestMapping(method = HttpMethod.GET, path = "/login/{account}")
    fun login(
        @PathVariable("account") account: String,
        @QueryParam("password") password: String,
    ): String {
        return "hello $account"
    }
}