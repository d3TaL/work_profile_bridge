package dev.profilebridge.app.platform

import android.content.Context

enum class AppMode { AUTOMATIC, CONFIRM, DISABLED }
data class SupportedApp(val label: String, val packageName: String)

object AppRules {
    val apps = listOf(SupportedApp("Ozon", "ru.ozon.app.android"), SupportedApp("Wildberries", "com.wildberries.ru"))
    fun mode(context: Context, pkg: String): AppMode {
        val value = context.getSharedPreferences("app-rules", Context.MODE_PRIVATE).getString(pkg, null)
        return AppMode.entries.firstOrNull { it.name == value } ?: AppMode.AUTOMATIC
    }
    fun set(context: Context, packages: Set<String>, mode: AppMode) {
        val edit = context.getSharedPreferences("app-rules", Context.MODE_PRIVATE).edit()
        apps.filter { it.packageName in packages }.forEach { edit.putString(it.packageName, mode.name) }
        edit.apply()
    }
}
