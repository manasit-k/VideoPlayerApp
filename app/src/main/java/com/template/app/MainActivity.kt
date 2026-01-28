package com.template.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.template.app.navigation.AppNavGraph
import com.template.app.navigation.AppNavigatorImpl
import com.template.core.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main Activity
 * Entry point of the application
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var navigator: AppNavigatorImpl
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val openVideoUri = if (intent?.action == android.content.Intent.ACTION_VIEW) {
            intent.data?.toString()
        } else null
        
        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val navController = rememberNavController()
                    
                    AppNavGraph(
                        navController = navController,
                        navigator = navigator,
                        startVideoUri = openVideoUri
                    )
                }
            }
        }
    }
}
