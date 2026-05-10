package com.tuapp.inventory.navigation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.*
import androidx.navigation.compose.*
import com.tuapp.inventory.audit.ui.AuditReportScreen
import com.tuapp.inventory.ui.detail.ItemDetailScreen
import com.tuapp.inventory.ui.home.HomeScreen
import com.tuapp.inventory.ui.scanner.ScannerScreen

object Routes {
    const val HOME         = "home"
    const val SCANNER      = "scanner"
    const val ITEM_DETAIL  = "item_detail/{itemId}"
    const val AUDIT_REPORT = "audit_report?uri={encodedUri}"

    fun itemDetail(itemId: Long): String = "item_detail/$itemId"
    fun auditReport(uri: Uri): String = "audit_report?uri=${Uri.encode(uri.toString())}"
}

private val enterSlideFromRight  = slideInHorizontally(tween(300)) { it }
private val exitSlideToLeft      = slideOutHorizontally(tween(300)) { -it / 3 }
private val enterSlideFromLeft   = slideInHorizontally(tween(300)) { -it / 3 }
private val exitSlideToRight     = slideOutHorizontally(tween(300)) { it }
private val enterFade            = fadeIn(tween(350))
private val exitFade             = fadeOut(tween(200))

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    val auditFilePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            navController.navigate(Routes.auditReport(it)) { launchSingleTop = true }
        }
    }

    NavHost(
        navController    = navController,
        startDestination = Routes.HOME
    ) {

        composable(
            route              = Routes.HOME,
            enterTransition    = { enterFade },
            exitTransition     = { exitSlideToLeft },
            popEnterTransition = { enterSlideFromLeft },
            popExitTransition  = { exitFade }
        ) {
            HomeScreen(
                onNavigateToScanner = { navController.navigate(Routes.SCANNER) },
                onNavigateToAudit   = {
                    auditFilePicker.launch(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    )
                },
                onNavigateToDetail  = { itemId -> navController.navigate(Routes.itemDetail(itemId)) }
            )
        }

        composable(
            route              = Routes.SCANNER,
            enterTransition    = { enterFade + enterSlideFromRight },
            exitTransition     = { exitFade },
            popEnterTransition = { enterFade },
            popExitTransition  = { exitSlideToRight }
        ) {
            ScannerScreen(
                onNavigateBack     = { navController.popBackStack() },
                onNavigateToDetail = { itemId ->
                    navController.navigate(Routes.itemDetail(itemId)) {
                        popUpTo(Routes.SCANNER) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route              = Routes.ITEM_DETAIL,
            arguments          = listOf(navArgument("itemId") { type = NavType.LongType; defaultValue = -1L }),
            enterTransition    = { enterSlideFromRight + enterFade },
            exitTransition     = { exitFade },
            popEnterTransition = { enterFade },
            popExitTransition  = { exitSlideToRight + exitFade }
        ) {
            ItemDetailScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(
            route              = Routes.AUDIT_REPORT,
            arguments          = listOf(navArgument("encodedUri") { type = NavType.StringType; defaultValue = ""; nullable = false }),
            enterTransition    = { enterSlideFromRight + enterFade },
            exitTransition     = { exitFade },
            popEnterTransition = { enterFade },
            popExitTransition  = { exitSlideToRight + exitFade }
        ) { backStackEntry ->
            val encodedUri = backStackEntry.arguments?.getString("encodedUri") ?: ""
            val auditUri   = remember(encodedUri) {
                if (encodedUri.isNotBlank()) Uri.parse(Uri.decode(encodedUri)) else null
            }
            AuditReportScreen(
                initialUri      = auditUri,
                onNavigateBack  = { navController.popBackStack() }
            )
        }
    }
}
