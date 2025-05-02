package com.albertsons.acupick.data.model

import com.albertsons.acupick.data.model.notification.NotificationType
import com.albertsons.acupick.data.test.BaseTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NotificationTypeTest : BaseTest() {
    @Test
    fun getByValueTest() {
        assertThat(NotificationType.getByValue("ARRIVED")).isEqualTo(NotificationType.ARRIVED)
        assertThat(NotificationType.getByValue("STORE-NOTIFIED")).isEqualTo(NotificationType.ARRIVED)
        assertThat(NotificationType.getByValue("GEO-FENCE-BROKEN")).isEqualTo(NotificationType.ARRIVING)
        assertThat(NotificationType.getByValue("DISMISS")).isEqualTo(NotificationType.DISMISS)
        assertThat(NotificationType.getByValue("PICKING")).isEqualTo(NotificationType.PICKING)
        assertThat(NotificationType.getByValue("picking")).isEqualTo(NotificationType.PICKING)
        assertThat(NotificationType.getByValue("ABC")).isEqualTo(NotificationType.UNKNOWN)
        assertThat(NotificationType.getByValue("")).isEqualTo(NotificationType.UNKNOWN)
        assertThat(NotificationType.getByValue(null)).isEqualTo(NotificationType.UNKNOWN)
    }
}
