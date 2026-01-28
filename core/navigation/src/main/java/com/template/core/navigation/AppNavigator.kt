package com.template.core.navigation

/**
 * Decoupled navigation interface
 * This interface is defined in :core:navigation module
 * and implemented in :app module to avoid circular dependencies
 */
interface AppNavigator {
    
    /**
     * Navigate to a specific route
     */
    fun navigateTo(route: Route)
    
    /**
     * Navigate back to previous screen
     */
    fun navigateBack()
    
    /**
     * Navigate up in the navigation hierarchy
     */
    fun navigateUp()
    
    /**
     * Pop back stack to a specific route
     */
    fun popBackStackTo(route: Route, inclusive: Boolean = false)
    
    /**
     * Clear entire back stack and navigate to route
     */
    fun clearBackStackAndNavigateTo(route: Route)
}
