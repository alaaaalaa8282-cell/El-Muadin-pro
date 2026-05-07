package com.AbuAlaa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import com.AbuAlaa.ui.NavGraph
import com.AbuAlaa.ui.theme.MuadhinTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MuadhinTheme {
                Surface(color = androidx.compose.material3.MaterialTheme.colorScheme.background) {
                    NavGraph()
                }
            }
        }
    }
}
