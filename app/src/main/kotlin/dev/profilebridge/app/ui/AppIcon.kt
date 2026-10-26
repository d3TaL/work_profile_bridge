package dev.profilebridge.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.profilebridge.app.R

@Composable
fun AppIcon(pkg: String) {
    val icon = when (pkg) {
        "ru.ozon.app.android" -> R.drawable.app_ru_ozon_app_android
        "com.wildberries.ru" -> R.drawable.app_com_wildberries_ru
        "com.idamob.tinkoff.android" -> R.drawable.app_com_idamob_tinkoff_android
        "ru.sberbankmobile" -> R.drawable.app_ru_sberbankmobile
        "ru.vtb24.mobilebanking.android" -> R.drawable.app_ru_vtb24_mobilebanking_android
        "ru.alfabank.mobile.android" -> R.drawable.app_ru_alfabank_mobile_android
        "ru.raiffeisennews" -> R.drawable.app_ru_raiffeisennews
        "ru.gazprombank.android.mobilebank.app" -> R.drawable.app_ru_gazprombank_android_mobilebank_app
        "logo.com.mbanking" -> R.drawable.app_logo_com_mbanking
        "ru.sovcomcard.halva.v1" -> R.drawable.app_ru_sovcomcard_halva_v1
        "ru.rshb.dbo" -> R.drawable.app_ru_rshb_dbo
        "ru.mkb.mobile" -> R.drawable.app_ru_mkb_mobile
        "ru.lewis.dbo" -> R.drawable.app_ru_lewis_dbo
        "ru.yoo.money" -> R.drawable.app_ru_yoo_money
        "ru.otpbank.mobile" -> R.drawable.app_ru_otpbank_mobile
        "ru.ozon.fintech.finance" -> R.drawable.app_ru_ozon_fintech_finance
        else -> R.drawable.ic_bridge
    }
    Image(painterResource(icon), contentDescription = null,
        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)))
}
