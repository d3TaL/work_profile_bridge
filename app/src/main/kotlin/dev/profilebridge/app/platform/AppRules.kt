package dev.profilebridge.app.platform

import android.content.Context
import androidx.core.content.edit
import dev.profilebridge.core.links.BankLinks

enum class AppMode { AUTOMATIC, CONFIRM, DISABLED }
data class SupportedApp(val label: String, val packageName: String)

object AppRules {
    val apps = listOf(SupportedApp("Ozon", "ru.ozon.app.android"), SupportedApp("Wildberries", "com.wildberries.ru"),
        ) + BankLinks.banks.map { SupportedApp(it.label, it.packageName) }
    fun mode(context: Context, pkg: String): AppMode {
        val value = context.getSharedPreferences("app-rules", Context.MODE_PRIVATE).getString(pkg, null)
        val mode = AppMode.entries.firstOrNull { it.name == value }
        if (BankLinks.isPackage(pkg)) return mode ?: AppMode.CONFIRM
        return mode ?: AppMode.AUTOMATIC
    }
    fun set(context: Context, packages: Set<String>, mode: AppMode) {
        context.getSharedPreferences("app-rules", Context.MODE_PRIVATE).edit {
            apps.filter { it.packageName in packages }.forEach {
                putString(it.packageName, mode.name)
            }
        }
    }
}
