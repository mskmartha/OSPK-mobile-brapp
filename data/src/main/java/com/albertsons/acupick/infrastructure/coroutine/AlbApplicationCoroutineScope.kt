package com.albertsons.acupick.infrastructure.coroutine

import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

/**
 * Represents application lifecycle Coroutine Scope, originated from https://medium.com/androiddevelopers/coroutines-patterns-for-work-that-shouldnt-be-cancelled-e26c40f142ad
 *
 * Use as a singleton scope that can execute as a child of other coroutine scopes (and inside suspend functions) and continue execution when the parent coroutine is cancelled.
 * A good use case for this type of scope could be for important side effecting (mutating) operations that need to continue processing until complete (instead of having their lifecycle
 * arbitrarily cut short when the parent coroutine scope is cancelled).
 *
 * One example would be using this scope in a repository that is executing recordPick but also needs to execute getActivityDetails to retrieve the latest pick list state to show correct UI and support
 * proper logic with the parent coroutine being a viewModelScope. Even if a user backs out of the screen while the repository logic is executing (viewModelScope children coroutines are cancelled),
 * the repository api calls should continue to execute and process/store the results in memory/long term storage/etc for use by other observers of the pick list state.
 *
 * Note: Using AlbApplicationCoroutineScope instead of ApplicationCoroutineScope to prevent confusion of who owns this scope (I think androidx might have an applicationScope)
 */
class AlbApplicationCoroutineScope(override val coroutineContext: CoroutineContext) : CoroutineScope
