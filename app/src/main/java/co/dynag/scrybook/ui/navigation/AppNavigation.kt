package co.dynag.scrybook.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import co.dynag.scrybook.ui.screens.*
import java.net.URLDecoder

@Composable
fun AppNavigation(openFilePath: String? = null) {
    val navController = rememberNavController()

    // If launched with a .sb file, navigate directly to Project
    LaunchedEffect(openFilePath) {
        openFilePath?.let { path ->
            navController.navigate(Screen.Project.createRoute(path)) {
                popUpTo(Screen.Home.route)
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onProjectOpen = { path ->
                    navController.navigate(Screen.Project.createRoute(path))
                }
            )
        }

        composable(
            route = Screen.Project.route,
            arguments = listOf(navArgument("projectPath") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("projectPath") ?: ""
            val projectPath = URLDecoder.decode(encodedPath, "UTF-8")
            ProjectScreen(
                projectPath = projectPath,
                onChapterOpen = { chapterId ->
                    navController.navigate(Screen.Editor.createRoute(projectPath, chapterId))
                },
                onInfoOpen = {
                    navController.navigate(Screen.ProjectInfo.createRoute(projectPath))
                },
                onCharactersOpen = {
                    navController.navigate(Screen.Characters.createRoute(projectPath))
                },
                onPlacesOpen = {
                    navController.navigate(Screen.Places.createRoute(projectPath))
                },
                onSettingsOpen = {
                    navController.navigate(Screen.Settings.createRoute(projectPath))
                },
                onFullSummaryOpen = {
                    navController.navigate(Screen.FullSummary.createRoute(projectPath))
                },
                onBack = { navController.popBackStack(Screen.Home.route, inclusive = false) }
            )
        }

        composable(
            route = Screen.Editor.route,
            arguments = listOf(
                navArgument("projectPath") { type = NavType.StringType },
                navArgument("chapterId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("projectPath") ?: ""
            val projectPath = URLDecoder.decode(encodedPath, "UTF-8")
            val chapterId = backStackEntry.arguments?.getLong("chapterId") ?: 0L
            EditorScreen(
                projectPath = projectPath,
                chapterId = chapterId,
                onBack = { navController.popBackStack(Screen.Project.route, inclusive = false) },
                onChapterOpen = { newId ->
                    navController.navigate(Screen.Editor.createRoute(projectPath, newId)) {
                        // Replace current if same destination but different ID (optional, but keep default stack for now)
                    }
                },
                onCharactersOpen = { navController.navigate(Screen.Characters.createRoute(projectPath)) },
                onPlacesOpen = { navController.navigate(Screen.Places.createRoute(projectPath)) }
            )
        }

        composable(
            route = Screen.ProjectInfo.route,
            arguments = listOf(navArgument("projectPath") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("projectPath") ?: ""
            val projectPath = URLDecoder.decode(encodedPath, "UTF-8")
            ProjectInfoScreen(projectPath = projectPath, onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.Characters.route,
            arguments = listOf(navArgument("projectPath") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("projectPath") ?: ""
            val projectPath = URLDecoder.decode(encodedPath, "UTF-8")
            CharactersScreen(projectPath = projectPath, onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.Places.route,
            arguments = listOf(navArgument("projectPath") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("projectPath") ?: ""
            val projectPath = URLDecoder.decode(encodedPath, "UTF-8")
            PlacesScreen(projectPath = projectPath, onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.Settings.route,
            arguments = listOf(navArgument("projectPath") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("projectPath") ?: ""
            val projectPath = URLDecoder.decode(encodedPath, "UTF-8")
            SettingsScreen(projectPath = projectPath, onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.FullSummary.route,
            arguments = listOf(navArgument("projectPath") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("projectPath") ?: ""
            val projectPath = URLDecoder.decode(encodedPath, "UTF-8")
            FullSummaryScreen(projectPath = projectPath, onBack = { navController.popBackStack() })
        }
    }
}
