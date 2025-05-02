package com.albertsons.acupick.data.environment

/** Represents available client APS (Albertson's api) environments */
enum class ApsEnvironmentType(val shortName: String) {
    /** Dev is updated more frequently than QA and is possibly less stable. Use as a secondary environment to QA */
    DEV("DEV"),

    /** Previous primary dev/QA environment. Used before migrating to QA3. */
    QA("QA"),

    /** New acceptance environment */
    QA2("QA2_ACEPTANCE"),

    /**
     * Primary dev/QA APS environment used before APIM_QA3 was introduced.
     *
     * Left in the build in case dev/qa needs to comparison test against the APIM environment or if the APIM environment is down.
     */
    @Deprecated("APIM_QA3 is the replacement for this environment")
    QA3("QA3"),

    /** Additional dev/QA APS enviroment. Used for MFC sites */
    QA7("QA7"),

    /** Primary dev/QA APS environment used today. */
    APIM_QA1("APIM_QA1 (West)"),

    /** Primary dev/QA APS environment that can be used for west region stores. */
    APIM_QA2("APIM_QA2 (West)"),

    APIM_QA3("APIM_QA3 (West)"),
    APIM_QA4("APIM_QA4 (West)"),
    APIM_QA5("APIM_QA5 (West)"),
    APIM_PERF("APIM_PERF (West)"),

    /** Canary environment used to pilot new features */
    CANARY("CANARY"),

    /** Production Canary environment used to pilot new features */
    PRODUCTION_CANARY("PRODUCTION (Canary)"),

    /**
     * Primary production APS environment used before APIM_PRODUCTION was introduced.
     *
     * Left in the build in case dev/qa needs to comparison test against the APIM environment or if the APIM environment is down.
     */
    @Deprecated("APIM_PRODUCTION is the replacement for this environment")
    PRODUCTION("PRODUCTION"),

    /** Primary production environment, west */
    APIM_PRODUCTION("APIM_PRODUCTION (West)"),

    /** Primary production environment, east */
    APIM_PRODUCTION_EAST("APIM_PRODUCTION (East)"),
}
