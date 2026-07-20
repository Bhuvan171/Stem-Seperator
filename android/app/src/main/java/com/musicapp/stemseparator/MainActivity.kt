package com.musicapp.stemseparator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.musicapp.stemseparator.ui.navigation.StemSeparatorNavGraph
import com.musicapp.stemseparator.ui.theme.StemSeparatorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as StemSeparatorApp).container

        setContent {
            StemSeparatorTheme {
                StemSeparatorNavGraph(container = container)
            }
        }
    }
}
