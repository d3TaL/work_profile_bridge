package dev.profilebridge.core.links

/** Screenshot declarations, not proof of a payment protocol or destination. */
object BankLinks {
    const val T_BANK = "com.idamob.tinkoff.android"
    val hosts = setOf("www.ob-tb.ru", "t-calls.ru", "id.srv-hub.org", "ipa.srv-hub.org",
        "b.tbao.su", "www.tbank.ru", "tbank.ru", "tt.ru")
    fun target(link: SafeLink): String? = T_BANK.takeIf { link.host in hosts }
}
