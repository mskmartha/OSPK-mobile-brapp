package com.albertsons.acupick.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Corresponds to the ActivityAndErDto.Status swagger api. Descriptions from https://confluence.safeway.com/display/EOM/Introduction
 */
@JsonClass(generateAdapter = false)
enum class ErOrderStatus {

    /**
     * When an order is created but is not released into handheld.
     */
    @Json(name = "NEW")
    NEW,

    /**
     * When an order is flagged as ready for execution based on a time, external triggers or even manual drops.
     */
    @Json(name = "RELEASED")
    RELEASED,

    /**
     * When an order is ready for picking
     */
    @Json(name = "READY")
    READY,

    /**
     * When execution of an order is started by a specific user.
     */
    @Json(name = "ASSIGNED")
    ASSIGNED,

    /**
     * When picking is complete and ready for staging.
     */
    @Json(name = "PACKED")
    PACKED,

    /**
     * When the order is staged into the staging area
     */
    @Json(name = "DROPPED_OFF")
    DROPPED_OFF,

    /**
     * When the order is handed over to the customer
     */
    @Json(name = "PICKED_UP")
    PICKED_UP,

    /**
     * When the order is handed over to the Van
     */
    @Json(name = "DISPATCHED")
    DISPATCHED,

    /**
     * When an order is canceled due to lack of resources or other operational reasons.
     */
    @Json(name = "CANCELLED")
    CANCELLED,
}
