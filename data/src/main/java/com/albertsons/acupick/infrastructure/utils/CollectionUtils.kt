package com.albertsons.acupick.infrastructure.utils

/**
 * Extension function wrapper that provides the not version of [isNullOrEmpty].
 *
 * Created to add the opposite (borrowed the idea from the existence of [isEmpty] and [isNotEmpty] stblib extensions)
 */
fun <T> Collection<T>?.isNotNullOrEmpty(): Boolean = !this.isNullOrEmpty()

/**
 * Returns first index of [element], or null if the collection does not contain element.
 *
 * Related Kotlin YouTrack issues to add native kotlin stdlib support:
 * * https://youtrack.jetbrains.com/issue/KT-37585
 * * https://youtrack.jetbrains.com/issue/KT-8113
 */
fun <T> Collection<T>.indexOfOrNull(element: T): Int? = indexOf(element).takeIf { it >= 0 }
