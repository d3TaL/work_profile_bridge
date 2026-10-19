package dev.profilebridge.core.links

/** Screenshot declarations, not proof of a payment protocol or destination. */
object BankLinks {
    data class Bank(val label: String, val scheme: String, val packageName: String)
    const val T_BANK = "com.idamob.tinkoff.android"
    const val T_BANK_SCHEME = "bank100000000004"
    val banks = listOf(
        Bank("Т-Банк", T_BANK_SCHEME, T_BANK),
        Bank("Сбер", "bank100000000111", "ru.sberbankmobile"),
        Bank("ВТБ", "bank110000000005", "ru.vtb24.mobilebanking.android"),
        Bank("Альфа-Банк", "bank100000000008", "ru.alfabank.mobile.android"),
        Bank("Райффайзен", "bank100000000007", "ru.raiffeisennews"),
        Bank("Газпромбанк", "bank100000000001", "ru.gazprombank.android.mobilebank.app"),
        Bank("ПСБ", "bank100000000010", "logo.com.mbanking"),
        Bank("Совкомбанк", "bank100000000013", "ru.sovcomcard.halva.v1"),
        Bank("Россельхозбанк", "bank100000000020", "ru.rshb.dbo"),
        Bank("МКБ", "bank100000000025", "ru.mkb.mobile"),
        Bank("МТС Деньги", "bank100000000017", "ru.lewis.dbo"),
        Bank("ЮMoney", "bank100000000022", "ru.yoo.money"),
        Bank("ОТП", "bank100000000018", "ru.otpbank.mobile"),
        Bank("Ozon Банк", "bank100000000273", "ru.ozon.fintech.finance"),
    )
    fun isScheme(scheme: String?): Boolean = banks.any { it.scheme.equals(scheme, true) }
    fun isPackage(pkg: String): Boolean = banks.any { it.packageName == pkg }
    /** Payment policy remains keyed to the bank, even when the shop handles its URI. */
    fun handlerPackages(link: SafeLink): List<String> {
        val primary = target(link) ?: return emptyList()
        return if (link.scheme == "bank100000000273") listOf("ru.ozon.app.android", primary)
            else listOf(primary)
    }
    val hosts = setOf("www.ob-tb.ru", "t-calls.ru", "id.srv-hub.org", "ipa.srv-hub.org",
        "b.tbao.su", "www.tbank.ru", "tbank.ru", "tt.ru")
    fun target(link: SafeLink): String? = banks.firstOrNull { it.scheme == link.scheme }?.packageName
        ?: T_BANK.takeIf { link.scheme == "https" && link.host in hosts }
}
