package dev.profilebridge.app.platform

import android.content.Context
import androidx.core.content.edit
import dev.profilebridge.core.links.BankLinks

enum class AppMode { AUTOMATIC, CONFIRM, DISABLED }
data class SupportedApp(val label: String, val packageName: String)

object AppRules {
    val apps = listOf(SupportedApp("Ozon", "ru.ozon.app.android"), SupportedApp("Wildberries", "com.wildberries.ru"),
        SupportedApp("Т-Банк", BankLinks.T_BANK))
    fun mode(context: Context, pkg: String): AppMode {
        val value = context.getSharedPreferences("app-rules", Context.MODE_PRIVATE).getString(pkg, null)
        val mode = AppMode.entries.firstOrNull { it.name == value }
        if (pkg == BankLinks.T_BANK) return if (mode == AppMode.DISABLED) mode else AppMode.CONFIRM
        return mode ?: AppMode.AUTOMATIC
    }
    fun set(context: Context, packages: Set<String>, mode: AppMode) {
        context.getSharedPreferences("app-rules", Context.MODE_PRIVATE).edit {
            apps.filter { it.packageName in packages }.forEach {
                putString(it.packageName, if (it.packageName == BankLinks.T_BANK && mode == AppMode.AUTOMATIC)
                    AppMode.CONFIRM.name else mode.name)
            }
        }
    }
}
