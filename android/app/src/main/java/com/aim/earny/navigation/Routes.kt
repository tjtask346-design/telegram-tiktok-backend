package com.aim.earny.navigation

object Routes {
    const val SPLASH = "splash"
    const val ONBOARD = "onboard"
    const val SIGNUP = "signup"
    const val LOGIN = "login"
    const val VERIFY = "verify/{email}"
    const val MAIN = "main"

    fun verify(email: String): String {
        val encoded = try {
            java.net.URLEncoder.encode(email, "UTF-8")
        } catch (t: Throwable) { email }
        return "verify/$encoded"
    }
}
