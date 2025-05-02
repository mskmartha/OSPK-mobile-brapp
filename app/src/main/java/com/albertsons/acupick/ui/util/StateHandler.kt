package com.albertsons.acupick.ui.util

import androidx.lifecycle.SavedStateHandle
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * StateHandler is a generic property delegate that backs the the property data with SavedStateHandle
 *
 * Handle should be injected into VM and passed into delegate constructor
 */
class StateHandler<T>(private val handle: SavedStateHandle) : ReadWriteProperty<Any, T?> {
    private var backingField: T? = null
    private var initialized = false

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T?) {
        backingField = value
        handle[property.name] = value
        initialized = true
    }

    override fun getValue(thisRef: Any, property: KProperty<*>): T? {
        // Double sync lock
        if (!initialized) {
            synchronized(this) {
                if (!initialized) {
                    backingField = handle[property.name]
                    initialized = true
                }
            }
        }
        return backingField
    }
}
