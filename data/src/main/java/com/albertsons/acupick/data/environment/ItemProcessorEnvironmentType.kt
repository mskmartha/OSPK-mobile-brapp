package com.albertsons.acupick.data.environment

/** Represents available client APS (Albertson's api) environments */
enum class ItemProcessorEnvironmentType(val shortName: String) {
    QA("QA"),
    QA3("QA3"),
    QA7("QA7"),
    APIM_QA1("ITEM_PROCESSOR_APIM_QA1 (West)"),
    APIM_QA2("ITEM_PROCESSOR_APIM_QA2 (West)"),
    APIM_QA3("ITEM_PROCESSOR_APIM_QA3 (West)"),
    APIM_QA4("ITEM_PROCESSOR_APIM_QA4 (West)"),
    APIM_QA5("ITEM_PROCESSOR_APIM_QA5 (West)"),
    APIM_PERF("ITEM_PROCESSOR_APIM_PERF (West)"),
    PRODUCTION_CANARY("ITEM_PROCESSOR_PRODUCTION (Canary)"),
    PRODUCTION("ITEM_PROCESSOR_PRODUCTION (West)"),
    PRODUCTION_EAST("ITEM_PROCESSOR_PRODUCTION (East)")
}
