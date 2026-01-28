package com.template.app.navigation

import androidx.navigation.NavHostController
import com.template.core.navigation.AppNavigator
import com.template.core.navigation.Route
import javax.inject.Inject

/**
 * Implementation of AppNavigator interface
 * This is in the :app module and uses NavHostController
 * Feature modules depend only on the interface, not this implementation
 */
class AppNavigatorImpl @Inject constructor() : AppNavigator {
    
    private var navController: NavHostController? = null
    
    fun setNavController(controller: NavHostController) {
        navController = controller
    }
    
    override fun navigateTo(route: Route) {
        navController?.navigate(route)
    }
    
    override fun navigateBack() {
        navController?.popBackStack()
    }
    
    override fun navigateUp() {
        navController?.navigateUp()
    }
    
    override fun popBackStackTo(route: Route, inclusive: Boolean) {
        navController?.popBackStack(route, inclusive)
    }
    
    override fun clearBackStackAndNavigateTo(route: Route) {
        navController?.navigate(route) {
            popUpTo(0) { inclusive = true }
        }
    }
}
