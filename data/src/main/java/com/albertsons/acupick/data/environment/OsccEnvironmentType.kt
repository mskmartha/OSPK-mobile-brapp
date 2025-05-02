package com.albertsons.acupick.data.environment

/** Represents available client APS (Albertson's api) environments */
enum class OsccEnvironmentType(val shortName: String) {
    QA("QA"),
    QA2("QA2"),
    PRODUCTION("PROD")
}
