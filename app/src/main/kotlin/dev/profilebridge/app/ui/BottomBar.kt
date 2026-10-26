package dev.profilebridge.app.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import dev.profilebridge.app.R

@Composable
fun BottomBar(selected: Int, select: (Int) -> Unit) {
    val icons = listOf(R.drawable.ic_nav_home, R.drawable.ic_nav_apps, R.drawable.ic_nav_settings)
    NavigationBar {
        listOf(R.string.nav_home, R.string.apps_title, R.string.nav_settings).forEachIndexed { index, label ->
            NavigationBarItem(selected = selected == index, onClick = { select(index) },
                icon = { Icon(painterResource(icons[index]), contentDescription = null) },
                label = { Text(stringResource(label)) })
        }
    }
}
