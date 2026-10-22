package com.arham.ssorganizer.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.arham.ssorganizer.ui.chats.ChatListScreen
import com.arham.ssorganizer.ui.home.HomeScreen
import com.arham.ssorganizer.ui.home.ScreenshotViewModel
import com.arham.ssorganizer.ui.permission.PermissionScreen
import com.arham.ssorganizer.ui.permission.hasStoragePermission
import com.arham.ssorganizer.ui.screenshots.ScreenshotGridScreen
import com.arham.ssorganizer.ui.search.SearchScreen

sealed class Screen(val route: String) {
    object Permission : Screen("permission")
    object Home : Screen("home")
    object ChatList : Screen("chats/{appName}") {
        fun createRoute(appName: String): String = "chats/${Uri.encode(appName)}"
    }
    object Screenshots : Screen("screenshots?appName={appName}&chatName={chatName}") {
        fun createRoute(appName: String? = null, chatName: String? = null): String {
            val params = mutableListOf<String>()
            if (!appName.isNullOrBlank()) params.add("appName=${Uri.encode(appName)}")
            if (!chatName.isNullOrBlank()) params.add("chatName=${Uri.encode(chatName)}")
            return if (params.isNotEmpty()) "screenshots?${params.joinToString("&")}" else "screenshots"
        }
    }
    object Search : Screen("search?query={query}&appName={appName}&chatName={chatName}") {
        fun createRoute(
            query: String? = null,
            appName: String? = null,
            chatName: String? = null
        ): String {
            val params = mutableListOf<String>()
            if (!query.isNullOrBlank()) params.add("query=${Uri.encode(query)}")
            if (!appName.isNullOrBlank()) params.add("appName=${Uri.encode(appName)}")
            if (!chatName.isNullOrBlank()) params.add("chatName=${Uri.encode(chatName)}")
            return if (params.isNotEmpty()) "search?${params.joinToString("&")}" else "search"
        }
    }
}

@Composable
fun AppNavigation(
    viewModel: ScreenshotViewModel = viewModel()
) {
    val context = LocalContext.current
    val navController = rememberNavController()

    val startDestination = if (hasStoragePermission(context)) {
        Screen.Home.route
    } else {
        Screen.Permission.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Permission.route) {
            PermissionScreen(
                onPermissionGranted = {
                    viewModel.syncWithMediaStore()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Permission.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToChats = { appName ->
                    navController.navigate(Screen.ChatList.createRoute(appName))
                },
                onNavigateToScreenshots = { appName ->
                    navController.navigate(Screen.Screenshots.createRoute(appName = appName))
                },
                onNavigateToSearch = {
                    navController.navigate(Screen.Search.createRoute())
                }
            )
        }

        composable(
            route = Screen.ChatList.route,
            arguments = listOf(
                navArgument("appName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val appName = backStackEntry.arguments?.getString("appName") ?: ""
            ChatListScreen(
                appName = appName,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onSelectChat = { chatName ->
                    val resolvedChat = if (chatName.isNullOrBlank()) "__unspecified__" else chatName
                    navController.navigate(
                        Screen.Screenshots.createRoute(appName = appName, chatName = resolvedChat)
                    )
                },
                onViewAll = {
                    navController.navigate(Screen.Screenshots.createRoute(appName = appName))
                }
            )
        }

        composable(
            route = Screen.Screenshots.route,
            arguments = listOf(
                navArgument("appName") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("chatName") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val appName = backStackEntry.arguments?.getString("appName")
            val chatName = backStackEntry.arguments?.getString("chatName")
            ScreenshotGridScreen(
                viewModel = viewModel,
                appName = appName,
                chatName = chatName,
                onBack = { navController.popBackStack() },
                onNavigateToSearch = {
                    navController.navigate(Screen.Search.createRoute(appName = appName, chatName = chatName))
                }
            )
        }

        composable(
            route = Screen.Search.route,
            arguments = listOf(
                navArgument("query") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("appName") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("chatName") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val query = backStackEntry.arguments?.getString("query") ?: ""
            val appName = backStackEntry.arguments?.getString("appName")
            val chatName = backStackEntry.arguments?.getString("chatName")
            SearchScreen(
                viewModel = viewModel,
                initialQuery = query,
                initialAppName = appName,
                initialChatName = chatName,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
