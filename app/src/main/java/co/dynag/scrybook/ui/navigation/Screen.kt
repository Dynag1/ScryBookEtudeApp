package co.dynag.scrybook.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Project : Screen("project/{projectPath}") {
        fun createRoute(projectPath: String) = "project/${java.net.URLEncoder.encode(projectPath, "UTF-8")}"
    }
    object Editor : Screen("editor/{projectPath}/{chapterId}") {
        fun createRoute(projectPath: String, chapterId: Long) =
            "editor/${java.net.URLEncoder.encode(projectPath, "UTF-8")}/$chapterId"
    }
    object ProjectInfo : Screen("project_info/{projectPath}") {
        fun createRoute(projectPath: String) = "project_info/${java.net.URLEncoder.encode(projectPath, "UTF-8")}"
    }
    object Sites : Screen("sites/{projectPath}") {
        fun createRoute(projectPath: String) = "sites/${java.net.URLEncoder.encode(projectPath, "UTF-8")}"
    }
    object Settings : Screen("settings/{projectPath}") {
        fun createRoute(projectPath: String) = "settings/${java.net.URLEncoder.encode(projectPath, "UTF-8")}"
    }
    object FullSummary : Screen("full_summary/{projectPath}") {
        fun createRoute(projectPath: String) = "full_summary/${java.net.URLEncoder.encode(projectPath, "UTF-8")}"
    }
}
