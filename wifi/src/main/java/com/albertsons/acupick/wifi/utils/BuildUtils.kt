package com.albertsons.acupick.wifi.utils

import android.os.Build

fun buildMinVersionR(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

fun buildMinVersionQ(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

fun buildMinVersionP(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

fun buildVersionP(): Boolean = Build.VERSION.SDK_INT == Build.VERSION_CODES.P

fun buildMinVersionM(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
