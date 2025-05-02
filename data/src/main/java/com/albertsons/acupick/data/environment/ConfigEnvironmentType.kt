package com.albertsons.acupick.data.environment

/** Represents available client Config flags environments */
enum class ConfigEnvironmentType(val shortName: String) {
    /**
     * Dev/QA Config environment.
     */
    QA1("QA1"),

    /**
     * Production Config environment.
     */
    PROD("PROD")
}
