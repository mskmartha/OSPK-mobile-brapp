package com.albertsons.acupick.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Not currently tied to swagger json directly. Inferred from the /api/activitiesInTimeRange actType request parameter
 * and associated documentation from https://confluence.safeway.com/display/EOM/Activity+Search. Descriptions from https://confluence.safeway.com/display/EOM/Introduction
 */
@JsonClass(generateAdapter = false)
enum class ActivityType {
    /**
     * This activity models the picking operation (both single order as well as multi-order pick list).
     * It includes both picking from store shelves as well as packing. Packing is segregating items by order and temperature zone into separate totes and scanning totes as part of picking operation.
     */
    @Json(name = "PICK_PACK")
    PICK_PACK,

    /**
     * Aka Staging. This activity models the staging operation that involves scanning totes and the staging locations they are being dropped off at. Staging is done post completion of picking.
     */
    @Json(name = "DROP_OFF")
    DROP_OFF,

    /**
     * This activity models the customer pick-up operation. Starts with searching for a specific customer order and then scanning totes to make sure right items are being handed over. And then a confirmation of handing over to customer.
     */
    @Json(name = "PICKUP")
    PICKUP,

    /** This is a workaround for an unexpected value the backend is returning, which should be functionally equal to [PICKUP] */
    // TODO: Remove if backend team decides this value will not be returned
    @Json(name = "3PL_PICKUP")
    THREEPL_PICKUP,

    @Json(name = "1PL_PICKUP")
    ONEPL_PICKUP,

    /**
     * This activity models the dispatch operation involving loading up a delivery truck with dispatch scans. It is performed at a delivery route level.
     */
    @Json(name = "DISPATCH")
    DISPATCH
}
