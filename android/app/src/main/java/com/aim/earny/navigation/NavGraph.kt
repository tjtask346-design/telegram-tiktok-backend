package com.aim.earny.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aim.earny.BuildConfig
import com.aim.earny.ui.screens.auth.*
import com.aim.earny.ui.screens.main.MainScaffold
import com.aim.earny.ui.screens.onboard.OnboardingScreen
import com.aim.earny.ui.screens.onboard.SplashScreen
import com.aim.earny.vm.AuthViewModel

@Composable
fun EarnyNavGraph(
    startSignedIn: Boolean,
    onSignOutRequest: () -> Unit
) {
    val nav = rememberNavController()
    val authVm: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    val start = remember {
        when {
            startSignedIn -> Routes.MAIN
            else -> Routes.SPLASH
        }
    }

    NavHost(navController = nav, startDestination = start) {

        composable(Routes.SPLASH) {
            SplashScreen(onDone = {
                nav.navigate(Routes.ONBOARD) { popUpTo(Routes.SPLASH) { inclusive = true } }
            })
        }

        composable(Routes.ONBOARD) {
            OnboardingScreen(onDone = {
                nav.navigate(Routes.AUTH_CHOICE) { popUpTo(Routes.ONBOARD) { inclusive = true } }
            })
        }

        composable(Routes.AUTH_CHOICE) {
            AuthChoiceScreen(onContinue = { nav.navigate(Routes.EMAIL) })
        }

        composable(Routes.EMAIL) {
            EmailInputScreen(
                onBack = { nav.popBackStack() },
                onMagicLink = { email ->
                    authVm.sendMagicLink(email, BuildConfig.MAGIC_LINK) {
                        nav.navigate(Routes.magicSent(email))
                    }
                },
                onPassword = { email -> nav.navigate(Routes.password(email)) }
            )
        }

        composable(
            Routes.MAGIC_SENT,
            arguments = listOf(navArgument("email") { type = NavType.StringType })
        ) { entry ->
            val email = entry.arguments?.getString("email") ?: ""
            MagicLinkSentScreen(
                email = email,
                onResend = { authVm.sendMagicLink(email, BuildConfig.MAGIC_LINK) {} },
                onUsePassword = { nav.navigate(Routes.password(email)) }
            )
        }

        composable(
            Routes.PASSWORD,
            arguments = listOf(navArgument("email") { type = NavType.StringType })
        ) { entry ->
            val email = entry.arguments?.getString("email") ?: ""
            PasswordScreen(
                email = email,
                onBack = { nav.popBackStack() },
                onSuccess = {
                    nav.navigate(Routes.PROFILE_SETUP) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                vm = authVm
            )
        }

        composable(Routes.PROFILE_SETUP) {
            ProfileSetupScreen(
                onDone = {
                    nav.navigate(Routes.MAIN) { popUpTo(0) { inclusive = true } }
                },
                vm = authVm
            )
        }

        composable(Routes.MAIN) {
            MainScaffold(onSignOut = {
                authVm.signOut()
                onSignOutRequest()
                nav.navigate(Routes.AUTH_CHOICE) { popUpTo(0) { inclusive = true } }
            })
        }
    }
}
