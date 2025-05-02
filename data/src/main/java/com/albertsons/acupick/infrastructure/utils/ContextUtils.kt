package com.albertsons.acupick.infrastructure.utils

import android.app.Application
import android.content.Context

/** Syntax sugar to retrieve Application from any context */
fun Context.asApplication(): Application = applicationContext as Application
