package com.albertsons.acupick.test.mocks

import android.app.Application
import android.content.Context
import android.content.res.Resources
import com.albertsons.acupick.R
import org.mockito.kotlin.any
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString

val testContext: Context = mock {
    // Order is important here, most generic match first. Last match is final value.
    on { getString(any()) } doReturn ""
    on { getDrawable(any()) } doReturn null
    on { getString(any(), anyVararg()) } doReturn ""
    on { resources } doReturn testResources

    // Make sure string argument matches 123
    on { getString(eq(R.string.item_details_plu_format), any()) } doAnswer { "PLU: ${it.getArgument<String>(1)}" }
    on { getString(R.string.same_brand_diff_size) } doReturn "Same brand, different size"
}

val testResources: Resources = mock {
    // Order is important here, most generic match first. Last match is final value.
    on { getQuantityString(any(), any(), any()) } doReturn ""
    on { getDrawable(any()) } doReturn null
    on { getDrawable(any(), any()) } doReturn null
    on { getString(any(), anyVararg()) } doReturn ""
    on { getString(R.string.uom_default) } doReturn "lb"
    on { getString(R.string.stage_order) } doReturn "Stage Order"
    on { getString(R.string.continue_picking) } doReturn "Continue Picking"
    on { getString(R.string.start_picking) } doReturn "Start Picking"
    on { getString(R.string.hello_continue_pick) } doReturn "Hi, wanna pick up where you left off?"
    on { getString(R.string.hello_again_next_pick, "") } doReturn "Hello again! Here’s your next pick."
    on { getString(R.string.something_wrong_body_with_type_api_error) } doReturn "TYPE: API Response Error"
    on { getString(R.string.something_wrong_body_with_source_backend) } doReturn "SOURCE: Backend"
    on { getString(R.string.something_wrong_body_with_http_code_format, "500") } doReturn "HTTP CODE: 500"
    on { getString(R.string.something_wrong_body_with_server_code_format, "57") } doReturn "SERVER CODE: Server error here"
    on { getString(R.string.something_wrong_body_with_server_message_format, "Server message here") } doReturn "MESSAGE: Server message here"
    on { getString(R.string.something_wrong_body_with_type_network_error) } doReturn "TYPE: Network Error"
    on { getString(R.string.something_wrong_body_with_source_device) } doReturn "SOURCE: Device"
    on { getString(R.string.something_wrong_body_with_cause_format, "java.lang.Exception") } doReturn "CAUSE: java.lang.Exception"
    on { getString(R.string.something_wrong_body_with_additional_info_timeout_error) } doReturn "ADDITIONAL INFO: Possible timeout or connection issue"
    on { getString(R.string.something_wrong_body_with_additional_info_vpn_error) } doReturn "ADDITIONAL INFO: Possible VPN connection issue"
    on { getQuantityString(R.plurals.bags_plural, 0, 0) } doReturn "0 bags"
    on { getQuantityString(R.plurals.bags_plural, 1, 1) } doReturn "1 bags"
    on { getQuantityString(R.plurals.bags_plural, 2, 2) } doReturn "2 bags"
    on { getQuantityString(R.plurals.bags_plural, 3, 3) } doReturn "3 bags"
    on { getString(R.string.complete_order_issue_dialog_body, "0 bags", "ambient") } doReturn "0 bags ambient"
    on { getString(R.string.complete_order_issue_dialog_body, "1 bags", "ambient") } doReturn "1 bags ambient"
    on { getString(R.string.complete_order_issue_dialog_body, "1 bags", "frozen") } doReturn "1 bags frozen"
    on { getString(R.string.complete_order_issue_dialog_body, "1 bags", "chilled") } doReturn "1 bags chilled"
    on { getString(R.string.complete_order_issue_dialog_body, "1 bags", "hot") } doReturn "1 bags hot"
    on { getString(R.string.complete_order_issue_dialog_body, "2 bags", "frozen and chilled") } doReturn "2 bags frozen and chilled"
    on { getString(R.string.complete_order_issue_dialog_body, "2 bags", "frozen and ambient") } doReturn "2 bags frozen and ambient"
    on { getString(R.string.complete_order_issue_dialog_body, "2 bags", "chilled and ambient") } doReturn "2 bags chilled and ambient"
    on { getString(R.string.complete_order_issue_dialog_body, "3 bags", "frozen, chilled, and ambient") } doReturn "3 bags frozen, chilled, and ambient"
    on { getString(R.string.bags_total, 0) } doReturn "TOTAL: 0"
    on { getString(R.string.bags_total, 10) } doReturn "TOTAL: 10"
    on {
        getQuantityString(
            R.plurals.add_to_handoff_snackbar_prompt_plural_format,
            1,
            "B. Glerp",
            "B. Glerp"
        )
    } doReturn "B. Glerp has been added to your handoff"
    on {
        getString(R.string.success_bag_scanned_out_of_zone_format, "rp", "vlerp")
    } doReturn "Bag rp scanned out of Zone vlerp"
    on {
        getString(R.string.success_bag_scanned_out_of_zone_format, "d1", "vlerp")
    } doReturn "Bag d1 scanned out of Zone vlerp"
    on {
        getString(R.string.success_bag_scanned_out_of_zone_format, "d1", "LocationPlace")
    } doReturn "Bag d1 scanned out of Zone LocationPlace"
    on {
        getQuantityString(R.plurals.hot_item_reminder_piece, 0, "DUG-blerp", 0)
    } doReturn "0 bags for DUG-blerp"
    on {
        getQuantityString(R.plurals.hot_item_reminder_piece, 0, "3PL-glerp", 0)
    } doReturn "0 bags for 3PL-blerp"
    on {
        getQuantityString(R.plurals.hot_item_reminder_piece, 0, "glerp-dlerp", 0)
    } doReturn "0 bags for glerp-dlerp"
    on {
        getString(R.string.hot_item_reminder_body_piece_two, "0 bags for DUG-blerp", "0 bags for 3PL-blerp")
    } doReturn "0 bags for DUG-blerp and 0 bags for 3PL-blerp"
    on {
        getString(R.string.hot_item_reminder_body_piece_three, "0 bags for DUG-blerp", "0 bags for 3PL-blerp", "0 bags for glerp-dlerp")
    } doReturn "0 bags for DUG-blerp, 0 bags for 3PL-blerp, and 0 bags for glerp-dlerp"
}

fun testApplicationFactory(
    mockContext: Context = testContext
): Application = mock {
    on { applicationContext } doReturn mockContext
    on { resources } doReturn testResources
    // Order is important here, most generic match first. Last match is final value.
    on { getDrawable(any()) } doReturn null
    on { getString(any()) } doReturn ""
    on { getString(any(), anyVararg()) } doReturn ""
    on { getString(eq(R.string.success_item_scanned_format), any()) } doAnswer { "Bag${it.getArgument<String>(1)} scanned" }
    on { getString(R.string.wrong_item_scanned) } doReturn "Wrong Item Scanned"
    on { getString(R.string.substitute_scan_item) } doReturn "Scan substitution item"
    on { getString(R.string.scan_to_new_tote) } doReturn "Scan a new tote"
    on { getString(eq(R.string.number_short_format), any()) } doAnswer { "${it.getArgument<String>(1)} short" }
    on { getString(eq(R.string.number_short_format), anyString()) } doAnswer { "${it.getArgument<String>(1)} short" }
    on { getString(eq(R.string.total_format), anyInt()) } doAnswer { "Total: ${it.getArgument<Int>(1)}" }
    on { getString(R.string.same_brand_diff_size) } doReturn "Same brand, different size"
    on { getString(R.string.item_details_1_item) } doReturn "1 item"
    on { getString(R.string.toolbar_stage_by) } doReturn "Stage By"
    on { getString(R.string.handoff_reassign_body, "abcdef") } doReturn "Handoff is in progress by abcdef. Are you sure you want to take this handoff?"
    on { getString(R.string.handoff_reassign_body, "B. Glerp") } doReturn "Handoff is in progress by Blerp Glerp. Are you sure you want to take this handoff?"
    on { getString(R.string.search_order_ready_fail_toast) } doReturn "Failed to refresh data"
    on { getString(R.string.success_item_scanned_format, "34") } doReturn "Bag 34 scanned"
    on { getString(R.string.item_scanned_upc_format, "8675309") } doReturn "UPC 8675309 successfully scanned"
    on { getString(R.string.ambient) } doReturn "ambient"
    on { getString(R.string.frozen) } doReturn "frozen"
    on { getString(R.string.hot) } doReturn "hot"
    on { getString(R.string.chilled) } doReturn "chilled"

    // Search Results Strings
    on { resources.getString(eq(R.string.not_found_format), anyString()) } doAnswer { "${it.getArgument<String>(1)} not found" }
    on { resources.getQuantityString(eq(R.plurals.search_results_plural), anyInt(), anyInt()) } doAnswer {
        if (it.getArgument<Int>(1) == 1) {
            "${it.getArgument<Int>(1)} result"
        } else {
            "${it.getArgument<Int>(2)} results"
        }
    }
    on { resources.getString(eq(R.string.searched_params_format), "\"${ArgumentMatchers.anyString()}\"") } doAnswer { "for ${it.getArgument<String>(1)}" }

    // Short Items Strings
    on { getString(R.string.short_item) } doReturn "Short item"
    on { getString(R.string.toolbar_stage_by) } doReturn "Stage By"

    // SearchResultsDetailsDb Strings
    on { resources.getString(R.string.bag_count_multiple_bags_format, 0, 1) } doReturn "0 of 1 bags"

    on { getString(R.string.scan_error_bag) } doReturn "Wrong item scanned - Please scan a bag"
    on { getString(R.string.error_item_not_in_order) } doReturn "Bag scanned not part of order"
    on { getString(R.string.error_tote_not_in_order) } doReturn "Tote scanned not part of order"
    on { getString(R.string.error_bag_already_scanned_format, "d1") } doReturn "Bag d1 already scanned"
    on { getString(R.string.error_tote_already_scanned_format, "d1") } doReturn "Tote d1 already scanned"
}
