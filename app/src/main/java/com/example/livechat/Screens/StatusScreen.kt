package com.example.livechat.Screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.example.livechat.LCViewModel

@Composable
fun StatusScreen(navController: NavController,vm:LCViewModel) {
    BottomNavigationMenu(selectedItem = BottomNavigationItem.STATUSLIST, navController =navController )
}