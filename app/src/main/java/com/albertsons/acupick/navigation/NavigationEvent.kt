package com.albertsons.acupick.navigation

import androidx.annotation.IdRes
import androidx.navigation.NavController
import androidx.navigation.NavDirections

/**
 * Encapsulates navigation components navigation action. Note that it doesn't handle navigation to/from [CustomDialogFragment]
 *
 * @see NavigationObserver
 */
sealed class NavigationEvent {

    /**
     * Use when passing args (via safe args) to another fragment.
     *
     * Example usage:
     * `NavigationEvent.Directions(PickListItemsFragmentDirections.actionPickListItemsFragmentToItemDetailsFragment(ItemDetailsParams(item)))`
     */
    data class Directions(val directions: NavDirections) : NavigationEvent()

    /**
     * Use to navigate to a destination (fragment).
     *
     * Example usage:
     * `NavigationEvent.Action(R.id.action_loginFragment_to_devOptionsFragment)`
     */
    data class Action(@IdRes val destinationId: Int) : NavigationEvent()

    /**
     * Represents "up" navigation.
     *
     * @see [NavController.navigateUp]
     */
    object Up : NavigationEvent()

    /**
     * Represents "back" navigation to [destinationId] (just pops backstack if not specified). If [inclusive], pops [destinationId] as well.
     * If [returnResults] is provided, it will be returned to the destination
     */
    data class Back(@IdRes val destinationId: Int = NO_DESTINATION_ID, val inclusive: Boolean = false, val returnResults: List<NavResult> = listOf()) : NavigationEvent()

    /** Contains logic to navigate using [navController] (intended to be called from [NavigationObserver]) */
    fun navigate(navController: NavController) {
        when (this) {
            is Directions -> navController.navigate(directions)
            is Action -> navController.navigate(destinationId)
            is Up -> navController.navigateUp()
            is Back -> {
                returnResults.forEach { navController.previousBackStackEntry?.savedStateHandle?.set(it.key, it.data) }
                if (destinationId == NO_DESTINATION_ID) {
                    navController.popBackStack()
                } else {
                    navController.popBackStack(destinationId, inclusive)
                }
            }
        }
    }

    companion object {
        const val NO_DESTINATION_ID = -1
    }
}
