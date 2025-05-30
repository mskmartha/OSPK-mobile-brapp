package com.albertsons.acupick.data.model

/**
 * [Marker interface](https://en.wikipedia.org/wiki/Marker_interface_pattern) for all [Dto]s used in the app to quickly/easily find them.
 *
 * More info at:
 * * https://confluence.bottlerocketapps.com/display/BKB/Common+Code+Quality+Issues#CommonCodeQualityIssues-Datamodels
 *
 * See [DomainModel] for related information.
 */
// TODO: Create corresponding DomainModels, create Moshi adapters to convert to/from each DTO<->DomainModel, and expose only DomainModels from the :data module (DTOs are implementation details of :data)
interface Dto
