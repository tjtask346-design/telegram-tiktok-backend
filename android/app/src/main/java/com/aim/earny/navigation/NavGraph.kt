package com.aim.earny.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aim.earny.ui.screens.auth.LoginScreen
import com.aim.earny.ui.screens.auth.SignupScreen
import com.aim.earny.ui.screens.auth.VerifyEmailScreen
import com.aim.earny.ui.screens.main.MainScaffold
import com.aim.earny.ui.screens.onboard.OnboardingScreen
import com.aim.earny.ui.screens.onboard.SplashScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun EarnyNavGraph(startRoute: String, onSignedOut: () -> Unit) {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = startRoute) {

        composable(Routes.SPLASH) {
            SplashScreen(onDone = {
                val u = FirebaseAuth.getInstance().currentUser
                when {
                    u == null -> nav.navigate(Routes.ONBOARD) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                    !u.isEmailVerified && !u.email.isNullOrBlank() ->
                        nav.navigate(Routes.verify(u.email!!)) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    else -> nav.navigate(Routes.MAIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            })
        }

        composable(Routes.ONBOARD) {
            OnboardingScreen(onDone = {
                nav.navigate(Routes.SIGNUP) {
                    popUpTo(Routes.ONBOARD) { inclusive = true }
                }
            })
        }

        composable(Routes.SIGNUP) {
            SignupScreen(
                onSignupSuccess = {
                    val email = FirebaseAuth.getInstance().currentUser?.email ?: ""
                    nav.navigate(Routes.verify(email)) {
                        popUpTo(Routes.SIGNUP) { inclusive = true }
                    }
                },
                onGoLogin = { nav.navigate(Routes.LOGIN) }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    val u = FirebaseAuth.getInstance().currentUser
                    when {
                        u?.isEmailVerified == true ->
                            nav.navigate(Routes.MAIN) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        !u?.email.isNullOrBlank() ->
                            nav.navigate(Routes.verify(u!!.email!!)) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                    }
                },
                onGoSignup = { nav.navigate(Routes.SIGNUP) }
            )
        }

        composable(
            Routes.VERIFY,
            arguments = listOf(navArgument("email") { type = NavType.StringType })
        ) { entry ->
            val raw = entry.arguments?.getString("email") ?: ""
            val email = try { java.net.URLDecoder.decode(raw, "UTF-8") } catch (t: Throwable) { raw }
            VerifyEmailScreen(
                email = email,
                onVerified = {
                    nav.navigate(Routes.MAIN) {
                        popUpTo(Routes.VERIFY) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.MAIN) {
            MainScaffold(onSignOut = {
                // SAFE ORDER: navigate first, sign out after
                // This avoids "no destination" crash when the back stack clears
                val navOk = runCatching {
                    nav.navigate(Routes.LOGIN) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                        launchSingleTop = true
                    }
                }.isSuccess

                // If for some reason navigation failed, still sign out
                // (the app will just sit on Main until user taps again)
                if (navOk) {
                    FirebaseAuth.getInstance().signOut()
                    onSignedOut()
                } else {
                    FirebaseAuth.getInstance().signOut()
                    onSignedOut()
                }
            })
        }
    }
}
