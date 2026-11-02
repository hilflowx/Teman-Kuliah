package com.temankuliah.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.temankuliah.app.ui.theme.BackgroundLight
import com.temankuliah.app.ui.theme.TkWidthClass
import com.temankuliah.app.ui.theme.rememberTkWidthClass

@Composable
fun TkAdaptiveNavigationSuiteScaffold(
    items: List<TkNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val reducedMotion = rememberReducedMotion()
    // Rail samping hanya untuk Expanded; Compact dan Medium memakai bilah bawah.
    if (rememberTkWidthClass() != TkWidthClass.Expanded) {
        Scaffold(
            modifier = modifier,
            containerColor = BackgroundLight,
            bottomBar = {
                TkBottomNav(
                    items = items,
                    selectedIndex = selectedIndex,
                    onSelect = onSelect,
                    reducedMotion = reducedMotion
                )
            }
        ) { innerPadding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                content()
            }
        }
    } else {
        Row(modifier = modifier.fillMaxSize().background(BackgroundLight)) {
            TkNavRail(
                items = items,
                selectedIndex = selectedIndex,
                onSelect = onSelect,
                reducedMotion = reducedMotion
            )
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                content()
            }
        }
    }
}
