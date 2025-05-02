package com.albertsons.acupick.data.test.pick

import org.intellij.lang.annotations.Language

@Language("JSON")
internal const val TEST_1_ITEM_UPC_LIST_JSON =
    """[{
	"itemId": "960134449",
	"upc": ["0001300000218"]
}, {
	"itemId": "148100026",
	"upc": ["0007045900910"]
}, {
	"itemId": "960228461",
	"upc": ["0008200077659"]
}]"""

@Language("JSON")
internal const val TEST_1_ACTIVITY_DETAIL_ZERO_PICKS_JSON =
    """{
	"actId": 4445,
	"erId": 3966,
	"siteId": "2941",
	"createdDate": "2020-09-28T13:34:35.337Z",
	"status": "IN_PROGRESS",
	"expectedEndTime": "2020-09-28T20:59:00Z",
	"completionTime": null,
	"actType": "PICK_PACK",
	"batch": "DUG-2020-09-28T21:59:00Z-2020-09-28T22:59:00Z",
	"routeVanNumber": "DUG",
	"assignedTo": {
		"userId": "jstoc62",
		"lastName": null,
		"firstName": null
	},
	"activityNo": "1204",
	"storageTypes": ["AM"],
	"handshakeType": null,
	"slotStartDate": "2020-09-28T21:59:00Z",
	"slotEndDate": "2020-09-28T22:59:00Z",
	"fulfillmentType": "DUG",
	"entityReference": {
		"entityId": "1559844469",
		"entityType": "ORDER"
	},
	"containerActivities": null,
	"itemActivities": [{
		"id": 18980,
		"itemId": "960134449",
		"itemAddress": {
			"bay": "126",
			"aisleSeq": "001",
			"level": "044",
			"side": null,
			"deptShortName": null
		},
		"imageURL": "https://images.albertsons-media.com/is/image/ABS/960134449",
		"itemDescription": "Heinz Mustard Yellow - 20 Oz",
		"attemptToRemove": false,
		"qty": 20.0,
		"pickedUpcCodes": [],
		"shortedItemUpc": [],
		"exceptionQty": 0.0,
		"processedQty": 0.0,
		"processedWeight": 0.0,
		"entityReference": {
			"entityId": "1559844469",
			"entityType": "ORDER"
		},
		"instruction": {
			"type": null,
			"text": ""
		},
		"completionTime": null,
		"subAllowed": false,
		"seqNumber": 7,
		"subCode": "0",
		"subValue": "Do Not Sub",
		"primaryUpc": null,
		"uom": null,
		"pluCode": "",
		"sellByWeightInd": "I",
		"depName": "Condiments & Spreads",
		"itemWeight": " 000000.0000",
		"itemWeightUom": null,
		"storageType": "AM"
	}],
	"expectedCount": 60,
	"processedQty": null,
	"exceptionQty": null,
	"pickUpBay": null,
	"contactFirstName": "Test",
	"contactLastName": "Tester",
	"seqNo": "1",
	"totalSeqNo": "1",
	"stopNumber": "539",
	"bagCountRequired": true,
	"reProcess": false,
	"bagCount": null,
	"customerOrderNumber": "1559844",
	"nextActivityId": null
}"""

@Language("JSON")
internal const val TEST_1_ACTIVITY_DETAIL_ONE_PICK_JSON =
    """{
	"actId": 4445,
	"erId": 3966,
	"siteId": "2941",
	"createdDate": "2020-09-28T13:34:35.337Z",
	"status": "IN_PROGRESS",
	"expectedEndTime": "2020-09-28T20:59:00Z",
	"completionTime": null,
	"actType": "PICK_PACK",
	"batch": "DUG-2020-09-28T21:59:00Z-2020-09-28T22:59:00Z",
	"routeVanNumber": "DUG",
	"assignedTo": {
		"userId": "jstoc62",
		"lastName": null,
		"firstName": null
	},
	"activityNo": "1204",
	"storageTypes": ["AM"],
	"handshakeType": null,
	"slotStartDate": "2020-09-28T21:59:00Z",
	"slotEndDate": "2020-09-28T22:59:00Z",
	"fulfillmentType": "DUG",
	"entityReference": {
		"entityId": "1559844469",
		"entityType": "ORDER"
	},
	"containerActivities": [{
		"id": 3003,
		"containerId": "TTC01",
		"location": null,
		"nextDestination": null,
		"containerType": "AM",
		"reference": {
			"entityId": "1559844469",
			"entityType": "ORDER"
		},
		"status": "EXPECTED",
		"attemptToRemove": false,
		"lastScanTime": "2020-09-28T14:21:23.133Z",
		"containerItems": [{
			"imageUrl": "https://images.albertsons-media.com/is/image/ABS/960134449",
			"itemDesc": "Heinz Mustard Yellow - 20 Oz",
			"itemId": "960134449",
			"qty": 1.0,
			"regulated": false
		}],
		"bagCount": null,
		"regulated": false
	}],
	"itemActivities": [{
		"id": 18980,
		"itemId": "960134449",
		"itemAddress": {
			"bay": "126",
			"aisleSeq": "001",
			"level": "044",
			"side": null,
			"deptShortName": null
		},
		"imageURL": "https://images.albertsons-media.com/is/image/ABS/960134449",
		"itemDescription": "Heinz Mustard Yellow - 20 Oz",
		"attemptToRemove": false,
		"qty": 20.0,
		"pickedUpcCodes": [{
			"upcId": 2119,
			"upc": "013000002189",
			"qty": 1.0,
			"upcQty": 1.0,
			"userId": "jstoc62",
			"containerId": "TTC01",
			"pickedTime": "2020-09-28T14:21:19.852Z",
			"isSubstitution": false
		}],
		"shortedItemUpc": [],
		"exceptionQty": 0.0,
		"processedQty": 1.0,
		"processedWeight": 0.0,
		"entityReference": {
			"entityId": "1559844469",
			"entityType": "ORDER"
		},
		"instruction": {
			"type": null,
			"text": ""
		},
		"completionTime": null,
		"subAllowed": false,
		"seqNumber": 7,
		"subCode": "0",
		"subValue": "Do Not Sub",
		"primaryUpc": null,
		"uom": null,
		"pluCode": "",
		"sellByWeightInd": "I",
		"depName": "Condiments & Spreads",
		"itemWeight": " 000000.0000",
		"itemWeightUom": null,
		"storageType": "AM"
	}],
	"expectedCount": 60,
	"processedQty": null,
	"exceptionQty": null,
	"pickUpBay": null,
	"contactFirstName": "Test",
	"contactLastName": "Tester",
	"seqNo": "1",
	"totalSeqNo": "1",
	"stopNumber": "539",
	"bagCountRequired": true,
	"reProcess": false,
	"bagCount": null,
	"customerOrderNumber": "1559844",
	"nextActivityId": null
}"""

@Language("JSON")
internal const val TEST_1_ACTIVITY_DETAIL_TWO_PICKS_JSON =
    """{
	"actId": 4445,
	"erId": 3966,
	"siteId": "2941",
	"createdDate": "2020-09-28T13:34:35.337Z",
	"status": "IN_PROGRESS",
	"expectedEndTime": "2020-09-28T20:59:00Z",
	"completionTime": null,
	"actType": "PICK_PACK",
	"batch": "DUG-2020-09-28T21:59:00Z-2020-09-28T22:59:00Z",
	"routeVanNumber": "DUG",
	"assignedTo": {
		"userId": "jstoc62",
		"lastName": null,
		"firstName": null
	},
	"activityNo": "1204",
	"storageTypes": ["AM"],
	"handshakeType": null,
	"slotStartDate": "2020-09-28T21:59:00Z",
	"slotEndDate": "2020-09-28T22:59:00Z",
	"fulfillmentType": "DUG",
	"entityReference": {
		"entityId": "1559844469",
		"entityType": "ORDER"
	},
	"containerActivities": [{
		"id": 3003,
		"containerId": "TTC01",
		"location": null,
		"nextDestination": null,
		"containerType": "AM",
		"reference": {
			"entityId": "1559844469",
			"entityType": "ORDER"
		},
		"status": "EXPECTED",
		"attemptToRemove": false,
		"lastScanTime": "2020-09-28T14:21:23.133Z",
		"containerItems": [{
			"imageUrl": "https://images.albertsons-media.com/is/image/ABS/960134449",
			"itemDesc": "Heinz Mustard Yellow - 20 Oz",
			"itemId": "960134449",
			"qty": 1.0,
			"regulated": false
		}],
		"bagCount": null,
		"regulated": false
	},{
		"id": 3004,
		"containerId": "TTC02",
		"location": null,
		"nextDestination": null,
		"containerType": "AM",
		"reference": {
			"entityId": "1559844469",
			"entityType": "ORDER"
		},
		"status": "EXPECTED",
		"attemptToRemove": false,
		"lastScanTime": "2020-09-28T14:21:23.133Z",
		"containerItems": [{
			"imageUrl": "https://images.albertsons-media.com/is/image/ABS/960134449",
			"itemDesc": "Heinz Mustard Yellow - 20 Oz",
			"itemId": "960134449",
			"qty": 1.0,
			"regulated": false
		}],
		"bagCount": null,
		"regulated": false
	}],
	"itemActivities": [{
		"id": 18980,
		"itemId": "960134449",
		"itemAddress": {
			"bay": "126",
			"aisleSeq": "001",
			"level": "044",
			"side": null,
			"deptShortName": null
		},
		"imageURL": "https://images.albertsons-media.com/is/image/ABS/960134449",
		"itemDescription": "Heinz Mustard Yellow - 20 Oz",
		"attemptToRemove": false,
		"qty": 20.0,
		"pickedUpcCodes": [{
			"upcId": 2119,
			"upc": "013000002189",
			"qty": 1.0,
			"upcQty": 1.0,
			"userId": "jstoc62",
			"containerId": "TTC01",
			"pickedTime": "2020-09-28T14:21:19.852Z",
			"isSubstitution": false
		}, {
			"upcId": 2120,
			"upc": "013000002189",
			"qty": 1.0,
			"upcQty": 1.0,
			"userId": "jstoc62",
			"containerId": "TTC02",
			"pickedTime": "2020-09-28T14:21:25.852Z",
			"isSubstitution": false
		}],
		"shortedItemUpc": [],
		"exceptionQty": 0.0,
		"processedQty": 2.0,
		"processedWeight": 0.0,
		"entityReference": {
			"entityId": "1559844469",
			"entityType": "ORDER"
		},
		"instruction": {
			"type": null,
			"text": ""
		},
		"completionTime": null,
		"subAllowed": false,
		"seqNumber": 7,
		"subCode": "0",
		"subValue": "Do Not Sub",
		"primaryUpc": null,
		"uom": null,
		"pluCode": "",
		"sellByWeightInd": "I",
		"depName": "Condiments & Spreads",
		"itemWeight": " 000000.0000",
		"itemWeightUom": null,
		"storageType": "AM"
	}],
	"expectedCount": 60,
	"processedQty": null,
	"exceptionQty": null,
	"pickUpBay": null,
	"contactFirstName": "Test",
	"contactLastName": "Tester",
	"seqNo": "1",
	"totalSeqNo": "1",
	"stopNumber": "539",
	"bagCountRequired": true,
	"reProcess": false,
	"bagCount": null,
	"customerOrderNumber": "1559844",
	"nextActivityId": null
}"""

@Language("JSON")
internal const val TEST_1_ACTIVITY_DETAIL_ONE_SHORT_JSON =
    """{
	"actId": 4445,
	"erId": 3966,
	"siteId": "2941",
	"createdDate": "2020-09-28T13:34:35.337Z",
	"status": "IN_PROGRESS",
	"expectedEndTime": "2020-09-28T20:59:00Z",
	"completionTime": null,
	"actType": "PICK_PACK",
	"batch": "DUG-2020-09-28T21:59:00Z-2020-09-28T22:59:00Z",
	"routeVanNumber": "DUG",
	"assignedTo": {
		"userId": "jstoc62",
		"lastName": null,
		"firstName": null
	},
	"activityNo": "1204",
	"storageTypes": ["AM"],
	"handshakeType": null,
	"slotStartDate": "2020-09-28T21:59:00Z",
	"slotEndDate": "2020-09-28T22:59:00Z",
	"fulfillmentType": "DUG",
	"entityReference": {
		"entityId": "1559844469",
		"entityType": "ORDER"
	},
	"containerActivities": null,
	"itemActivities": [{
		"id": 18980,
		"itemId": "960134449",
		"itemAddress": {
			"bay": "126",
			"aisleSeq": "001",
			"level": "044",
			"side": null,
			"deptShortName": null
		},
		"imageURL": "https://images.albertsons-media.com/is/image/ABS/960134449",
		"itemDescription": "Heinz Mustard Yellow - 20 Oz",
		"attemptToRemove": false,
		"qty": 20.0,
		"pickedUpcCodes": [],
		"shortedItemUpc": [{
			"shortedId": 307,
			"exceptionQty": 20.0,
			"exceptionReasonCode": "Not In Stock",
			"exceptionReasonText": "Not In Stock",
			"userId": "jstoc62",
			"shortedTime": "2020-09-30T13:40:15.693Z"
		}],
		"exceptionQty": 20.0,
		"processedQty": 0.0,
		"processedWeight": 0.0,
		"entityReference": {
			"entityId": "1559844469",
			"entityType": "ORDER"
		},
		"instruction": {
			"type": null,
			"text": ""
		},
		"completionTime": null,
		"subAllowed": false,
		"seqNumber": 7,
		"subCode": "0",
		"subValue": "Do Not Sub",
		"primaryUpc": null,
		"uom": null,
		"pluCode": "",
		"sellByWeightInd": "I",
		"depName": "Condiments & Spreads",
		"itemWeight": " 000000.0000",
		"itemWeightUom": null,
		"storageType": "AM"
	}],
	"expectedCount": 60,
	"processedQty": null,
	"exceptionQty": null,
	"pickUpBay": null,
	"contactFirstName": "Test",
	"contactLastName": "Tester",
	"seqNo": "1",
	"totalSeqNo": "1",
	"stopNumber": "539",
	"bagCountRequired": true,
	"reProcess": false,
	"bagCount": null,
	"customerOrderNumber": "1559844",
	"nextActivityId": null
}"""

@Language("JSON")
internal const val TEST_1_ACTIVITY_DETAIL_ONE_PICK_SHORT_REMAINING_QTY_JSON =
    """{
	"actId": 4445,
	"erId": 3966,
	"siteId": "2941",
	"createdDate": "2020-09-28T13:34:35.337Z",
	"status": "IN_PROGRESS",
	"expectedEndTime": "2020-09-28T20:59:00Z",
	"completionTime": null,
	"actType": "PICK_PACK",
	"batch": "DUG-2020-09-28T21:59:00Z-2020-09-28T22:59:00Z",
	"routeVanNumber": "DUG",
	"assignedTo": {
		"userId": "jstoc62",
		"lastName": null,
		"firstName": null
	},
	"activityNo": "1204",
	"storageTypes": ["AM"],
	"handshakeType": null,
	"slotStartDate": "2020-09-28T21:59:00Z",
	"slotEndDate": "2020-09-28T22:59:00Z",
	"fulfillmentType": "DUG",
	"entityReference": {
		"entityId": "1559844469",
		"entityType": "ORDER"
	},
	"containerActivities": [{
		"id": 3003,
		"containerId": "TTC01",
		"location": null,
		"nextDestination": null,
		"containerType": "AM",
		"reference": {
			"entityId": "1559844469",
			"entityType": "ORDER"
		},
		"status": "EXPECTED",
		"attemptToRemove": false,
		"lastScanTime": "2020-09-28T14:21:23.133Z",
		"containerItems": [{
			"imageUrl": "https://images.albertsons-media.com/is/image/ABS/960134449",
			"itemDesc": "Heinz Mustard Yellow - 20 Oz",
			"itemId": "960134449",
			"qty": 1.0,
			"regulated": false
		}],
		"bagCount": null,
		"regulated": false
	}],
	"itemActivities": [{
		"id": 18980,
		"itemId": "960134449",
		"itemAddress": {
			"bay": "126",
			"aisleSeq": "001",
			"level": "044",
			"side": null,
			"deptShortName": null
		},
		"imageURL": "https://images.albertsons-media.com/is/image/ABS/960134449",
		"itemDescription": "Heinz Mustard Yellow - 20 Oz",
		"attemptToRemove": false,
		"qty": 20.0,
		"pickedUpcCodes": [{
			"upcId": 2119,
			"upc": "013000002189",
			"qty": 1.0,
			"upcQty": 1.0,
			"userId": "jstoc62",
			"containerId": "TTC01",
			"pickedTime": "2020-09-28T14:21:19.852Z",
			"isSubstitution": false
		}],
		"shortedItemUpc": [{
			"shortedId": 307,
			"exceptionQty": 19.0,
			"exceptionReasonCode": "Not In Stock",
			"exceptionReasonText": "Not In Stock",
			"userId": "jstoc62",
			"shortedTime": "2020-09-30T13:40:15.693Z"
		}],
		"exceptionQty": 19.0,
		"processedQty": 1.0,
		"processedWeight": 0.0,
		"entityReference": {
			"entityId": "1559844469",
			"entityType": "ORDER"
		},
		"instruction": {
			"type": null,
			"text": ""
		},
		"completionTime": null,
		"subAllowed": false,
		"seqNumber": 7,
		"subCode": "0",
		"subValue": "Do Not Sub",
		"primaryUpc": null,
		"uom": null,
		"pluCode": "",
		"sellByWeightInd": "I",
		"depName": "Condiments & Spreads",
		"itemWeight": " 000000.0000",
		"itemWeightUom": null,
		"storageType": "AM"
	}],
	"expectedCount": 60,
	"processedQty": null,
	"exceptionQty": null,
	"pickUpBay": null,
	"contactFirstName": "Test",
	"contactLastName": "Tester",
	"seqNo": "1",
	"totalSeqNo": "1",
	"stopNumber": "539",
	"bagCountRequired": true,
	"reProcess": false,
	"bagCount": null,
	"customerOrderNumber": "1559844",
	"nextActivityId": null
}"""

@Language("JSON")
internal const val TEST_1_ACTIVITY_DETAIL_ONE_SUB_JSON =
    """{
	"actId": 4445,
	"erId": 3966,
	"siteId": "2941",
	"createdDate": "2020-09-28T13:34:35.337Z",
	"status": "IN_PROGRESS",
	"expectedEndTime": "2020-09-28T20:59:00Z",
	"completionTime": null,
	"actType": "PICK_PACK",
	"batch": "DUG-2020-09-28T21:59:00Z-2020-09-28T22:59:00Z",
	"routeVanNumber": "DUG",
	"assignedTo": {
		"userId": "jstoc62",
		"lastName": null,
		"firstName": null
	},
	"activityNo": "1204",
	"storageTypes": ["AM"],
	"handshakeType": null,
	"slotStartDate": "2020-09-28T21:59:00Z",
	"slotEndDate": "2020-09-28T22:59:00Z",
	"fulfillmentType": "DUG",
	"entityReference": {
		"entityId": "1559844469",
		"entityType": "ORDER"
	},
	"containerActivities": [{
		"id": 3003,
		"containerId": "TTC01",
		"location": null,
		"nextDestination": null,
		"containerType": "AM",
		"reference": {
			"entityId": "1559844469",
			"entityType": "ORDER"
		},
		"status": "EXPECTED",
		"attemptToRemove": false,
		"lastScanTime": "2020-09-28T14:21:23.133Z",
		"containerItems": [{
			"imageUrl": "https://images.albertsons-media.com/is/image/ABS/960134449",
			"itemDesc": "Heinz Mustard Yellow - 20 Oz",
			"itemId": "960134449",
			"qty": 1.0,
			"regulated": false
		}],
		"bagCount": null,
		"regulated": false
	}],
	"itemActivities": [{
		"id": 18980,
		"itemId": "960134449",
		"itemAddress": {
			"bay": "126",
			"aisleSeq": "001",
			"level": "044",
			"side": null,
			"deptShortName": null
		},
		"imageURL": "https://images.albertsons-media.com/is/image/ABS/960134449",
		"itemDescription": "Heinz Mustard Yellow - 20 Oz",
		"attemptToRemove": false,
		"qty": 20.0,
		"pickedUpcCodes": [{
			"upcId": 1465,
			"upc": "0021130116310",
			"qty": 1.0,
			"upcQty": 1.0,
			"userId": "jstoc62",
			"containerId": "TTC01",
			"pickedTime": "2020-10-06T17:36:29.831Z",
            "substituteItemDesc": "Fresh Baked Signature SELECT Garlic Bread In Foil Bag - Each",
			"isSubstitution": true
		}],
		"shortedItemUpc": [],
		"exceptionQty": 0.0,
		"processedQty": 1.0,
		"processedWeight": 0.0,
		"entityReference": {
			"entityId": "1559844469",
			"entityType": "ORDER"
		},
		"instruction": {
			"type": null,
			"text": ""
		},
		"completionTime": null,
		"subAllowed": false,
		"seqNumber": 7,
		"subCode": "0",
		"subValue": "Do Not Sub",
		"primaryUpc": null,
		"uom": null,
		"pluCode": "",
		"sellByWeightInd": "I",
		"depName": "Condiments & Spreads",
		"itemWeight": " 000000.0000",
		"itemWeightUom": null,
		"storageType": "AM"
	}],
	"expectedCount": 60,
	"processedQty": null,
	"exceptionQty": null,
	"pickUpBay": null,
	"contactFirstName": "Test",
	"contactLastName": "Tester",
	"seqNo": "1",
	"totalSeqNo": "1",
	"stopNumber": "539",
	"bagCountRequired": true,
	"reProcess": false,
	"bagCount": null,
	"customerOrderNumber": "1559844",
	"nextActivityId": null
}"""

@Language("JSON")
internal const val TEST_1_RECORD_PICK_ONE_ITEM_REQUEST_JSON =
    """{
	"actId": 4445,
	"lineReq": [{
		"containerId": "TTC01",
		"iaId": 18980,
		"upcId": "013000002189",
		"ignoreUpc": true,
		"pickedTime": "2020-09-28T14:21:19.852Z",
		"fulfilledQty": 1.0,
		"substitution": false,
		"upcQty": 1.0,
		"userId": "jstoc62"
}]
}"""

@Language("JSON")
internal const val TEST_1_RECORD_PICK_ONE_ITEM_RESPONSE_JSON =
    """[{
	"iaId": 18980,
	"processedQty": 1.0,
	"pickedUpcCodes": [{
		"upcId": 2119,
		"upc": "013000002189",
		"qty": 1.0,
		"upcQty": 1.0,
		"userId": "jstoc62",
		"containerId": "TTC01",
		"pickedTime": "2020-09-28T14:21:19.852Z",
		"isSubstitution": false
	}]
}]"""

@Language("JSON")
internal const val TEST_1_RECORD_PICK_ONE_ITEM_DIFF_TOTE_REQUEST_JSON =
    """{
	"actId": 4445,
	"lineReq": [{
		"containerId": "TTC02",
		"iaId": 18980,
		"upcId": "013000002189",
		"ignoreUpc": true,
		"pickedTime": "2020-09-28T14:21:25.852Z",
		"fulfilledQty": 1.0,
		"substitution": false,
		"upcQty": 1.0,
		"userId": "jstoc62"
	}]
}"""

@Language("JSON")
internal const val TEST_1_RECORD_PICK_ONE_ITEM_DIFF_TOTE_RESPONSE_JSON =
    """[{
	"iaId": 18980,
	"processedQty": 1.0,
	"pickedUpcCodes": [{
		"upcId": 2120,
		"upc": "013000002189",
		"qty": 1.0,
		"upcQty": 1.0,
		"userId": "jstoc62",
		"containerId": "TTC02",
		"pickedTime": "2020-09-28T14:21:25.852Z",
		"isSubstitution": false
	}]
}]"""

@Language("JSON")
internal const val TEST_1_UNDO_PICK_ONE_TTC01_ITEM_REQUEST_JSON =
    """{
    "containerId": "TTC01",
    "undoPickRequestDto": {
	"actId": 4445,
	"iaId": 18980,
	"pickedUpcId": 2119,
	"qty": 1.0
    }
}"""

@Language("JSON")
internal const val TEST_1_UNDO_PICK_ONE_TTC02_ITEM_REQUEST_JSON =
    """{
    "containerId": "TTC02",
    "undoPickRequestDto": {
	"actId": 4445,
	"iaId": 18980,
	"pickedUpcId": 2119,
	"qty": 1.0
    }
}"""

@Language("JSON")
internal const val TEST_1_UNDO_PICK_ONE_ITEM_RESPONSE_JSON =
    """[{
	"iaId": 18980,
	"processedQty": 0.0,
	"pickedUpcCodes": []
}]"""

@Language("JSON")
internal const val TEST_1_RECORD_SHORT_ONE_ITEM_REQUEST_JSON =
    """{
	"actId": 4445,
	"containerNo": "TTC01",
	"shortReq": [{
		"iaId": 18980,
		"itemId": "960134449",
		"qty": 20.0,
		"shortageReasonCode": "Not In Stock",
		"shortageReasonText": "Not In Stock",
		"shortedTime": "2020-09-30T13:40:15.693Z",
		"userId": "jstoc62"
	}]
}"""

@Language("JSON")
internal const val TEST_1_RECORD_SHORT_ONE_ITEM_RESPONSE_JSON =
    """[{
	"iaId": 18980,
	"exceptionQty": 20.0,
	"shortageReasonCodes": [{
		"shortedId": 307,
		"exceptionQty": 20.0,
		"exceptionReasonCode": "Not In Stock",
		"exceptionReasonText": "Not In Stock",
		"userId": "jstoc62",
		"shortedTime": "2020-09-30T13:40:15.693Z"
	}]
}]"""

@Language("JSON")
internal const val TEST_1_UNDO_SHORT_ONE_ITEM_REQUEST_JSON =
    """{
	"actId": 4445,
	"iaId": 18980,
	"qty": 20.0,
	"shortedItemId": 307
}"""

@Language("JSON")
internal const val TEST_1_UNDO_SHORT_ONE_ITEM_RESPONSE_JSON =
    """[{
	"iaId": 18980,
	"exceptionQty": 0.0,
	"shortageReasonCodes": []
}]"""

@Language("JSON")
internal const val TEST_1_RECORD_SHORT_REMAINING_ITEM_QTY_REQUEST_JSON =
    """{
	"actId": 4445,
	"containerNo": "TTC01",
	"shortReq": [{
		"iaId": 18980,
		"itemId": "960134449",
		"qty": 19.0,
		"shortageReasonCode": "Not In Stock",
		"shortageReasonText": "Not In Stock",
		"shortedTime": "2020-09-30T13:40:15.693Z",
		"userId": "jstoc62"
	}]
}"""

@Language("JSON")
internal const val TEST_1_RECORD_SHORT_REMAINING_ITEM_QTY_RESPONSE_JSON =
    """[{
	"iaId": 18980,
	"exceptionQty": 19.0,
	"shortageReasonCodes": [{
		"shortedId": 307,
		"exceptionQty": 19.0,
		"exceptionReasonCode": "Not In Stock",
		"exceptionReasonText": "Not In Stock",
		"userId": "jstoc62",
		"shortedTime": "2020-09-30T13:40:15.693Z"
	}]
}]"""

@Language("JSON")
internal const val TEST_1_RECORD_SUB_ONE_ITEM_REQUEST_JSON =
    """{
	"actId": 4445,
	"lineReq": [{
		"containerId": "TTC01",
		"iaId": 18980,
		"upcId": "0021130116310",
		"ignoreUpc": true,
		"originalItemId": "960134449",
		"pickedTime": "2020-10-06T17:36:29.831Z",
		"fulfilledQty": 1.0,
		"substituteItemDesc": "Fresh Baked Signature SELECT Garlic Bread In Foil Bag - Each",
		"substituteItemId": "194010041",
		"substitution": true,
		"upcQty": 1.0,
		"userId": "jstoc62"
	}]
}"""

@Language("JSON")
internal const val TEST_1_RECORD_SUB_ONE_ITEM_RESPONSE_JSON =
    """[{
	"iaId": 18980,
	"processedQty": 1.0,
	"pickedUpcCodes": [{
		"upcId": 1465,
		"upc": "0021130116310",
		"qty": 1.0,
		"upcQty": 1.0,
		"userId": "jstoc62",
		"containerId": "TTC01",
		"pickedTime": "2020-10-06T17:36:29.831Z",
		"isSubstitution": true
	}]
	}]
}]"""
