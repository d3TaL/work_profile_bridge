package dev.profilebridge.app.ui

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.profilebridge.app.R

@Composable
fun BottomBar(selected: Int, select: (Int) -> Unit) {
    NavigationBar {
        listOf(R.string.nav_home, R.string.apps_title, R.string.nav_settings).forEachIndexed { index, label ->
            NavigationBarItem(selected = selected == index, onClick = { select(index) },
                icon = { Text(listOf("⌂", "▦", "⚙")[index]) }, label = { Text(stringResource(label)) })
        }
    }
}
