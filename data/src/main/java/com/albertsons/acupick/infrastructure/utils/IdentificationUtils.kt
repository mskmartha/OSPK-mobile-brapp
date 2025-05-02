package com.albertsons.acupick.infrastructure.utils

import com.google.mlkit.vision.barcode.common.Barcode.DriverLicense

fun DriverLicense?.identificationName() = "${this?.firstName} ${this?.lastName}"
