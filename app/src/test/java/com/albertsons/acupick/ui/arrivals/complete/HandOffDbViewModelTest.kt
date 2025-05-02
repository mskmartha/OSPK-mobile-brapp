package com.albertsons.acupick.ui.arrivals.complete

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.albertsons.acupick.test.BaseTest
import com.albertsons.acupick.test.SetDispatcherOnMain
import com.albertsons.acupick.test.TestDispatcherProvider
import com.albertsons.acupick.test.runPrivateMethod
import com.albertsons.acupick.ui.BaseBindableViewModel
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule

class HandOffDbViewModelTest : BaseTest() {

    @get:Rule
    val rule: TestRule = InstantTaskExecutorRule()

    @get:Rule
    val dispatcherRule = SetDispatcherOnMain(TestDispatcherProvider().Unconfined)

    private fun getTestVM() = HandOffDbViewModel(item = HandOffRegulatedItem(description = "test", totalQty = 10.0, itemId = "12", originalItemId = "12", upc = "upc", imageUrl = "url"))

    @Test
    fun handOffDbViewModel_getItemFactory() {
        val vm = getTestVM()
        val vmItem = vm.runPrivateMethod("getItemFactory")
        assertThat(vmItem is BaseBindableViewModel)
    }

    @Test
    fun handOffDbViewModel_miscThings() {
        val vm = getTestVM()
        assertThat(vm.description).isEqualTo(vm.item.description)
        assertThat(vm.count).isEqualTo(vm.item.totalQty.toInt().toString())
        vm.count = "blerg"
        vm.description = "bloop test bleep"
        assertThat(vm.count).isNotEqualTo(vm.item.totalQty.toInt().toString())
        assertThat(vm.description).isNotEqualTo(vm.item.description)
    }
}
