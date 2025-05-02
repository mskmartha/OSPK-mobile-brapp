package com.albertsons.acupick.data.environment

/** Represents available client auth environments */
enum class AuthEnvironmentType(val shortName: String) {
    /**
     * Primary dev/QA Auth environment used before APIM_QA3 was introduced. Requires use of your own ldap credentials.
     *
     * Left in the build in case dev/qa needs to comparison test against the APIM environment or if the APIM environment is down.
     */
    QA3("QA3"),

    QA("QA"),

    QA2("QA2"),
    QA4("QA4"),
    QA5("QA5"),
    PERF("PERF"),

    /**
     * Primary production Auth environment used before APIM_PRODUCTION was introduced.
     *
     * Left in the build in case dev/qa needs to comparison test against the APIM environment or if the APIM environment is down.
     */
    // @Deprecated("APIM_PRODUCTION is the replacement for this environment")
    // PRODUCTION("PRODUCTION"),

    /** Primary production Auth environment */
    APIM_PRODUCTION("APIM_PRODUCTION"),

    PRODUCTION_CANARY("PRODUCTION_CANARY")
}
