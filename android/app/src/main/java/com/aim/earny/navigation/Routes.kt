package com.aim.earny.navigation

object Routes {
    const val SPLASH = "splash"
    const val ONBOARD = "onboard"
    const val SIGNUP = "signup"
    const val VERIFY = "verify/{email}"
    const val MAIN = "main"

    fun verify(email: String): String {
        val encoded = java.net.URLEncoder.encode(email, "UTF-8")
        return "verify/$encoded"
    }
}
