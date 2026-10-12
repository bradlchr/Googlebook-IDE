package com.bradlchr.googlebookide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.bradlchr.googlebookide.ui.GooglebookApp
import com.bradlchr.googlebookide.ui.IdeViewModel
import com.bradlchr.googlebookide.ui.theme.DarkBackground
import com.bradlchr.googlebookide.ui.theme.GooglebookIDETheme

class MainActivity : ComponentActivity() {

    private val viewModel: IdeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GooglebookIDETheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    GooglebookApp(viewModel = viewModel)
                }
            }
        }
    }
}
