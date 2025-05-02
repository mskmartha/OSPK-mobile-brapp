package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ServerErrorCodeDto(
    /** Raw value directly from the server. Needed for logging/display purposes when an unknown ServerErrorCodeType is encountered */
    val rawValue: Int,
    /** Strongly typed "known" error code to use for matching purposes in the app. */
    val resolvedType: ServerErrorCode,
) : Parcelable

/**
 * Strongly typed "known" error code to use for matching purposes in the app.
 *
 * Note that some error codes do not seem to match what they should be.
 * TODO will probably need to redo at some point
 */
enum class ServerErrorCode(val value: Int) {
    // returned if /scanContainers or /preCompleteActivity is called on an activity already in the PRE_COMPLETED state,
    // or if /pickupComplete is called a second time on the same activity
    CANNOT_ASSIGN_COMPLETED_ACTIVITY(3),
    CONTAINER_NOT_FOUND(13),
    CONTAINER_ALREADY_ADDED(14),
    CONTAINER_ATTACHED_TO_ENTITY_ID(15),
    ACTIVITY_CONTAINER_NOT_LINKED(16),
    CONTAINER_NOT_EMPTY(17),
    REQUEST_CANCELLED(18),
    CA_ALREADY_SCANNED(19),
    NO_USER_TO_ASSIGN_ACTIVITY(20),
    USER_ALREADY_ASSIGNED_TO_AN_ACTIVITY(21),
    ITEM_ACTIVITY_NOT_FOUND(22),
    QTY_ALREADY_PICKED(23),
    CONTAINER_DOES_NOT_EXIT(24),
    SHORTAGE_REASON_CODE_INVALID(25),
    CONTAINER_BELONG_TO_DIFF_ER(26),
    SYNC_REQ_NULL(27),
    ALL_OPERATIONS_NOT_SAME_ACTIVITY(28),
    CONTAINER_ADDED_DIFF_PICKLIST_SAME_ORDER(30),
    STORAGE_TYPE_IS_DIFFERENT(31),
    INVALID_OTP(44),
    ACTIVITY_NOT_FOUND(54),
    ITEM_ACTIVITY_CANCELLED(55),
    NO_ITEM_FOUND(56),
    USER_NOT_VALID(57),
    NO_SITE_DEVICE(60),
    CANT_CONNECT_DEVICE(61),
    NO_OVER_RIDE_FLAG(70),

    // returned if /cancelHandoff is called a second time on the same order
    CANNOT_CANCEL_RELEASED_HANDOFF(500),

    // returned if /setStageLocationForCustomerOrder is called for an order that already has a set stagingLocation
    STAGING_LOCATION_ALREADY_EXISTS(209),
    STAGING_LOCATION_MAX_REACHED(8990),

    // Staging plallet closed for wine and dark store order
    STAGING_PALLET_CLOSED(364),
    FFC_RESTRICTION(192),
    BPN_RESTRICTION(193),
    DUPLICATE_CALL(413),
    REJECTED_REMOVAL_COMPLETED(421),
    SCAN_CONTAINER_ISSUE(428),
    PICKING_IN_PROGRESS(1306),
    CONCURRENT_SWAP_SUBSTITUTION(97),
    OPTIMISTIC_LOCKING_ERROR(95),

    /** Represents an unknown server error code. Note that -99 is not the value received from the backend. Use [ServerErrorCodeDto.rawValue] in this instance. */
    UNKNOWN_SERVER_ERROR_CODE(-99),
    COMBINED_ALREADY_ASSIGNED(100);

    fun cannotAssignToOrder() =
        this == NO_USER_TO_ASSIGN_ACTIVITY ||
            this == NO_OVER_RIDE_FLAG ||
            this == USER_NOT_VALID ||
            this == CANNOT_ASSIGN_COMPLETED_ACTIVITY
}

enum class CannotAssignToOrderDialogTypes {
    REGULAR,
    PICKLIST,
    HANDOFF,
    HANDOFF_REASSIGN,
    STAGING
}
