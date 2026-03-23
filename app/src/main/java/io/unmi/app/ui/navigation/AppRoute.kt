package io.unmi.app.ui.navigation

sealed class AppRoute(val route: String) {
    data object Unlock : AppRoute("unlock")
    data object Init : AppRoute("init")
    data object Home : AppRoute("home")
    data object DomainList : AppRoute("domain_list")
    data object DomainDetail : AppRoute("domain_detail/{id}") {
        fun createRoute(id: Long) = "domain_detail/$id"
    }
    data object DomainEdit : AppRoute("domain_edit/{id}") {
        fun createRoute(id: Long) = "domain_edit/$id"
    }
    data object DomainCreate : AppRoute("domain_create")
    data object Statistics : AppRoute("statistics")
    data object Tools : AppRoute("tools")
    data object ImportExport : AppRoute("import_export")
    data object Whois : AppRoute("whois")
    data object WhoisQuery : AppRoute("whois_query/{domain}") {
        fun createRoute(domain: String) = "whois_query/$domain"
    }
    data object BackupSync : AppRoute("backup_sync")
    data object Settings : AppRoute("settings")
    data object About : AppRoute("about")
}
