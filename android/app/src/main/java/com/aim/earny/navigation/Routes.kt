package com.aim.earny.navigation

object Routes {
    const val SPLASH = "splash"
    const val ONBOARD = "onboard"
    const val SIGNUP = "signup"
    const val LOGIN = "login"
    const val VERIFY = "verify/{email}"
    const val MAIN = "main"
    const val EDIT_PROFILE = "edit_profile"
    const val ADD_FRIENDS = "add_friends"

    fun verify(email: String): String {
        val encoded = try {
            java.net.URLEncoder.encode(email, "UTF-8")
        } catch (t: Throwable) { email }
        return "verify/$encoded"
    }
}
