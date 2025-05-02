package com.albertsons.acupick.ui.picklistitems

/** Defines the available targets to be scanned. */
enum class ScanTarget {
    Item,
    Tote,
    Bag,
    Box,
    Zone,
    PharmacyArrivalLabel,
    PharmacyReturnLabel,
    BagOrZone, // StagingPart2, the picker has scanned a bag into a zone and has option to scan additional bags into the same zone or move to a new zone
    ToteOrZone,
    None, // the app is in a state where it is not able to accept a scan from DataWedge
}
