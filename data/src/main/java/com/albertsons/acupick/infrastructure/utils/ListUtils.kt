package com.albertsons.acupick.infrastructure.utils

fun <T> List<T>.filterIfElseEmpty(filterCondition: Boolean, predicate: (T) -> Boolean) = if (filterCondition) { filter(predicate) } else emptyList()
