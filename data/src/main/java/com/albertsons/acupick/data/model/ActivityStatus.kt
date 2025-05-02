package com.albertsons.acupick.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Corresponds to the ActivityDto.Status swagger api. Descriptions from https://confluence.safeway.com/display/EOM/Introduction
 */
@JsonClass(generateAdapter = false)
enum class ActivityStatus {
    /**
     * When an activity is created but is not flagged for execution.
     */
    @Json(name = "NEW")
    NEW,

    /**
     * When an activity is flagged as ready for execution based on a time, external triggers or even manual drops.
     */
    @Json(name = "RELEASED")
    RELEASED,

    /**
     * When execution of an activity is started by a specific user.
     */
    @Json(name = "IN_PROGRESS")
    IN_PROGRESS,

    /**
     * When all basic operations on an activity have been completed and the user is ready to mark the activity as completed.
     * It is used to freeze further operations on the activity and provide a summary of operations/exceptions before prompting user to either keep working further or
     * completing the activity with exceptions.
     */
    @Json(name = "PRE_COMPLETED")
    PRE_COMPLETED,

    /**
     * Marks the operation as completed and triggers updates to upstream systems and other affected systems.
     */
    @Json(name = "COMPLETED")
    COMPLETED,

    /**
     * When an activity is canceled due to lack of resources or other operational reasons and also if an activity is rendered empty because of order cancellations.
     */
    @Json(name = "CANCELLED")
    CANCELLED,
}
