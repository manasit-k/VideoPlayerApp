package com.template.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.template.core.navigation.Route
import com.template.feature.dashboard.presentation.DashboardScreen
import com.template.feature.videoplayer.presentation.gallery.VideoGalleryScreen
import com.template.feature.videoplayer.presentation.VideoPlayerScreen

/**
 * Main navigation graph for the app
 * Uses type-safe navigation with Kotlin Serialization
 */
@Composable
fun AppNavGraph(
    navController: NavHostController,
    navigator: AppNavigatorImpl,
    startVideoUri: String? = null
) {
    // Set the NavController in the navigator
    LaunchedEffect(navController) {
        navigator.setNavController(navController)
    }
    
    // Handle external video launch
    LaunchedEffect(startVideoUri) {
        if (startVideoUri != null) {
            // We need a special route or parameter to handle URI directly
            // For now, let's assume we can map URI to ID or play directly.
            // Since we tied player to ID and Folder, handling raw URI requires modifying VideoPlayer to accept URI too.
            // Let's modify Route.VideoPlayer to allow optional URI or create a new Route.VideoPlayerUri
            // BUT, modifying Route now is complex. 
            // Better strategy: Pass a special ID like -1 and use folderName as URI? Or update Route.
            
            navController.navigate(Route.VideoPlayerFromUri(startVideoUri))
        }
    }
     
    NavHost(
        navController = navController,
        startDestination = if (startVideoUri != null) Route.VideoPlayerFromUri(startVideoUri) else Route.VideoGallery
    ) {
        composable<Route.VideoGallery> {
            VideoGalleryScreen(
                onVideoClick = { video ->
                    navController.navigate(Route.VideoPlayer(video.id, video.folderName))
                }
            )
        }
        
        // ... (Dashboard routes)
        
        composable<Route.VideoPlayer> { backStackEntry ->
            val route: Route.VideoPlayer = backStackEntry.toRoute()
            VideoPlayerScreen(
                startVideoId = route.startVideoId,
                folderName = route.folderName
            )
        }
        
        composable<Route.VideoPlayerFromUri> { backStackEntry ->
            val route: Route.VideoPlayerFromUri = backStackEntry.toRoute()
             VideoPlayerScreen(
                videoUri = route.uri
            )
        }
    }
}
