package com.aim.earny.navigation

object Routes {
    const val SPLASH = "splash"
    const val ONBOARD = "onboard"
    const val AUTH_CHOICE = "auth_choice"
    const val EMAIL = "email"
    const val MAGIC_SENT = "magic_sent/{email}"
    const val PASSWORD = "password/{email}"
    const val PROFILE_SETUP = "profile_setup"
    const val MAIN = "main"

    fun magicSent(email: String) = "magic_sent/$email"
    fun password(email: String) = "password/$email"
}
