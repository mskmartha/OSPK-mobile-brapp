package com.albertsons.acupick.data.test.pick

internal const val TEST_3_ITEM_UPC_LIST_JSON =
    """[{
	"itemId": "188150167",
	"upc": ["0020305700000", "0020303900000"]
}, {
	"itemId": "960134449",
	"upc": ["0001300000218"]
}, {
	"itemId": "184700085",
	"upc": ["0072906216749", "0003307430181", "0076245302126", "0048470001602", "0072906294958", "0081468301018", "0048470074902", "0075197234020", "0048470048401", "0085535400619", "0068395394958", "0048470026102", "0086733100037", "0076777690443", "0048470026101", "0076777690442", "0003651594053", "0011238341540", "0048470006601", "0048470001601", "0060504994958", "0085001485109", "0000000094958", "0075197234040", "0048470001101", "0068395394053", "0075197234041", "0060504994053", "0084720401158", "0085883400707", "0048470048402", "0065357194053", "0085000998423", "0048470074901", "0072906215450", "0060504951103", "0003307430182", "0081675400997", "0048470013901", "0048470001102", "0082890400023", "0085535400618", "0075197234031", "0000000094053", "0076245302127", "0048470006602", "0073382194958", "0085883400708", "0081796801060", "0004525520132", "0085000998424", "0084720401159", "0003651594958", "0081675400998", "0073382194053", "0081457001008", "0072906294053"]
}, {
	"itemId": "188300244",
	"upc": ["0026081300000"]
}, {
	"itemId": "114150166",
	"upc": ["0005210000256"]
}, {
	"itemId": "184110040",
	"upc": ["0004525524704", "0082329800013", "0085021200211", "0086000397383", "0085000252608", "0009582980911", "0082757520001", "0068761570010", "0081781801195", "0009582980031", "0073382103421", "0073000800002", "0009582980886", "0082757520002", "0000000003421", "0085080200204", "0009582980597", "0089624700146", "0085841600216", "0081478001024", "0066785903421", "0073655800026", "0082178303421", "0085001461909", "0068761500063", "0070737503421", "0081642601067", "0086002300235", "0007895100329", "0009582980939", "0081781801162", "0048411025201", "0085080600269", "0081781801161", "0085445200705", "0009884303421", "0082757520007", "0081781801080", "0082757520004", "0009582980007", "0009582904103", "0085429800212", "0071179020120", "0085390300518", "0085119500206", "0006602203421", "0081457001006", "0081781801197", "0082757520003", "0082757520106", "0085697700516", "0088100620206", "0085000258370", "0082757520000", "0086018500180", "0085080600298", "0009582980063", "0081781801196", "0048411013001", "0081781801189"]
}, {
	"itemId": "960468979",
	"upc": ["0085509900702"]
}, {
	"itemId": "960228461",
	"upc": ["0008200077659"]
}, {
	"itemId": "184060016",
	"upc": ["0082178304236", "0040423800000", "0064312604236", "0048406004001", "0040423600000", "0003784200984", "0048406005001", "0048406001601", "0048406001001", "0004525517987", "0048406006001", "0048406001701"]
}]"""

internal const val TEST_3_ACTIVITY_DETAIL_ZERO_PICKS_JSON =
    """{
	"actId": 3138,
	"erId": 2113,
	"siteId": "2941",
	"createdDate": "2020-10-12T12:58:59.428Z",
	"status": "RELEASED",
	"expectedEndTime": "2020-10-12T22:00:00Z",
	"completionTime": null,
	"actType": "PICK_PACK",
	"batch": "DUG-2020-10-14T22:59:00Z-2020-10-14T23:59:00Z",
	"routeVanNumber": "DUG",
	"assignedTo": null,
	"activityNo": "1123",
	"storageTypes": ["AM", "CH", "FZ"],
	"handshakeType": null,
	"slotStartDate": "2020-10-14T22:59:00Z",
	"slotEndDate": "2020-10-14T23:59:00Z",
	"fulfillmentType": "DUG",
	"entityReference": {
		"entityId": "9000001405",
		"entityType": "ORDER"
	},
	"containerActivities": null,
	"itemActivities": [{
		"id": 7068,
		"itemId": "184060016",
		"itemAddress": null,
		"imageURL": "https://images.albertsons-media.com/is/image/ABS/184060016",
		"itemDescription": "Bananas Red",
		"attemptToRemove": false,
		"qty": 2.0,
		"pickedUpcCodes": [],
		"shortedItemUpc": [],
		"exceptionQty": 0.0,
		"processedQty": 0.0,
		"entityReference": {
			"entityId": "9000001405",
			"entityType": "ORDER"
		},
		"instruction": null,
		"amount": {
			"amount": 1.99,
			"currency": "USD"
		},
		"completionTime": null,
		"subAllowed": true,
		"seqNumber": null,
		"subCode": "03",
		"subValue": "Same Brand Diff Size",
		"primaryUpc": "0048406001701",
		"uom": "LB",
		"pluCode": "4238",
		"sellByWeightInd": "W",
		"depName": "Fruits & Vegetables",
		"itemWeight": "0.25",
		"itemWeightUom": "lb",
		"storageType": "AM",
		"customerOrderNumber": "9001405"
	}, {
		"id": 7070,
		"itemId": "184110040",
		"itemAddress": null,
		"imageURL": "https://images.albertsons-media.com/is/image/ABS/184110040",
		"itemDescription": "Mini Seedless Watermelon",
		"attemptToRemove": false,
		"qty": 1.0,
		"pickedUpcCodes": [],
		"shortedItemUpc": [],
		"exceptionQty": 0.0,
		"processedQty": 0.0,
		"entityReference": {
			"entityId": "9000001405",
			"entityType": "ORDER"
		},
		"instruction": null,
		"amount": {
			"amount": 2.99,
			"currency": "USD"
		},
		"completionTime": null,
		"subAllowed": true,
		"seqNumber": null,
		"subCode": "01",
		"subValue": "No Substitution",
		"primaryUpc": "0061002013000",
		"uom": "EACH",
		"pluCode": "3421",
		"sellByWeightInd": "E",
		"depName": "Fruits & Vegetables",
		"itemWeight": "0.0",
		"itemWeightUom": "lb",
		"storageType": "AM",
		"customerOrderNumber": "9001405"
	}, {
		"id": 7073,
		"itemId": "188150167",
		"itemAddress": null,
		"imageURL": "https://images.albertsons-media.com/is/image/ABS/188150167",
		"itemDescription": "Open Nature Lamb Shanks - 1.50 LB",
		"attemptToRemove": false,
		"qty": 1.0,
		"pickedUpcCodes": [],
		"shortedItemUpc": [],
		"exceptionQty": 0.0,
		"processedQty": 0.0,
		"entityReference": {
			"entityId": "9000001405",
			"entityType": "ORDER"
		},
		"instruction": null,
		"amount": {
			"amount": 9.99,
			"currency": "USD"
		},
		"completionTime": null,
		"subAllowed": true,
		"seqNumber": null,
		"subCode": "01",
		"subValue": "No Substitution",
		"primaryUpc": "0020303900000",
		"uom": "LB",
		"pluCode": "0",
		"sellByWeightInd": "P",
		"depName": "Meat & Seafood",
		"itemWeight": "1.5",
		"itemWeightUom": "lb",
		"storageType": "CH",
		"customerOrderNumber": "9001405"
	}, {
		"id": 7071,
		"itemId": "184700085",
		"itemAddress": {
			"bay": "06",
			"aisleSeq": "977",
			"level": "019",
			"side": "L",
			"deptShortName": "977"
		},
		"imageURL": "https://images.albertsons-media.com/is/image/ABS/184700085",
		"itemDescription": "Organic Lemon",
		"attemptToRemove": false,
		"qty": 3.0,
		"pickedUpcCodes": [],
		"shortedItemUpc": [],
		"exceptionQty": 0.0,
		"processedQty": 0.0,
		"entityReference": {
			"entityId": "9000001405",
			"entityType": "ORDER"
		},
		"instruction": null,
		"amount": {
			"amount": 0.99,
			"currency": "USD"
		},
		"completionTime": null,
		"subAllowed": true,
		"seqNumber": 4,
		"subCode": "01",
		"subValue": "No Substitution",
		"primaryUpc": "0072906294053",
		"uom": "EACH",
		"pluCode": "94053",
		"sellByWeightInd": "E",
		"depName": "Fruits & Vegetables",
		"itemWeight": "0.0",
		"itemWeightUom": "lb",
		"storageType": "AM",
		"customerOrderNumber": "9001405"
	}, {
		"id": 7067,
		"itemId": "960228461",
		"itemAddress": {
			"bay": "01",
			"aisleSeq": "803",
			"level": "011",
			"side": "L",
			"deptShortName": "803"
		},
		"imageURL": "https://images.albertsons-media.com/is/image/ABS/960228461",
		"itemDescription": "Crown Royal Whisky Flavored Vanilla 70 Proof - 750 Ml",
		"attemptToRemove": false,
		"qty": 1.0,
		"pickedUpcCodes": [],
		"shortedItemUpc": [],
		"exceptionQty": 0.0,
		"processedQty": 0.0,
		"entityReference": {
			"entityId": "9000001405",
			"entityType": "ORDER"
		},
		"instruction": null,
		"amount": {
			"amount": 49.99,
			"currency": "USD"
		},
		"completionTime": null,
		"subAllowed": true,
		"seqNumber": 5,
		"subCode": "03",
		"subValue": "Same Brand Diff Size",
		"primaryUpc": null,
		"uom": "COUNT",
		"pluCode": "",
		"sellByWeightInd": "I",
		"depName": "Spirits",
		"itemWeight": " 000000.1800",
		"itemWeightUom": null,
		"storageType": "AM",
		"customerOrderNumber": "9001405"
	}, {
		"id": 7074,
		"itemId": "960468979",
		"itemAddress": {
			"bay": "10",
			"aisleSeq": "09",
			"level": "018",
			"side": "L",
			"deptShortName": null
		},
		"imageURL": "https://images.albertsons-media.com/is/image/ABS/960468979",
		"itemDescription": "Alpha Foods Burrito Plant Based Philly Sandwich - 5 Oz",
		"attemptToRemove": false,
		"qty": 4.0,
		"pickedUpcCodes": [],
		"shortedItemUpc": [],
		"exceptionQty": 0.0,
		"processedQty": 0.0,
		"entityReference": {
			"entityId": "9000001405",
			"entityType": "ORDER"
		},
		"instruction": null,
		"amount": {
			"amount": 2.99,
			"currency": "USD"
		},
		"completionTime": null,
		"subAllowed": true,
		"seqNumber": 6,
		"subCode": "03",
		"subValue": "Same Brand Diff Size",
		"primaryUpc": "0085509900702",
		"uom": "COUNT",
		"pluCode": "0",
		"sellByWeightInd": "I",
		"depName": "Frozen Foods",
		"itemWeight": "0.0",
		"itemWeightUom": "lb",
		"storageType": "FZ",
		"customerOrderNumber": "9001405"
	}, {
		"id": 7066,
		"itemId": "960134449",
		"itemAddress": {
			"bay": "26",
			"aisleSeq": "01",
			"level": "044",
			"side": "R",
			"deptShortName": null
		},
		"imageURL": "https://images.albertsons-media.com/is/image/ABS/960134449",
		"itemDescription": "Heinz Mustard Yellow - 20 Oz",
		"attemptToRemove": false,
		"qty": 2.0,
		"pickedUpcCodes": [],
		"shortedItemUpc": [],
		"exceptionQty": 0.0,
		"processedQty": 0.0,
		"entityReference": {
			"entityId": "9000001405",
			"entityType": "ORDER"
		},
		"instruction": {
			"type": null,
			"text": "hello"
		},
		"amount": {
			"amount": 5.99,
			"currency": "USD"
		},
		"completionTime": null,
		"subAllowed": true,
		"seqNumber": 19,
		"subCode": "02",
		"subValue": "Same Brand Diff Size",
		"primaryUpc": "0001300000218",
		"uom": "COUNT",
		"pluCode": "0",
		"sellByWeightInd": "I",
		"depName": "Condiments, Spice & Bake",
		"itemWeight": "0.0",
		"itemWeightUom": "lb",
		"storageType": "AM",
		"customerOrderNumber": "9001405"
	}, {
		"id": 7069,
		"itemId": "114150166",
		"itemAddress": {
			"bay": "29",
			"aisleSeq": "03",
			"level": "114",
			"side": "L",
			"deptShortName": null
		},
		"imageURL": "https://images.albertsons-media.com/is/image/ABS/114150166",
		"itemDescription": "McCormick Ground Nutmeg - 1.1 Oz",
		"attemptToRemove": false,
		"qty": 2.0,
		"pickedUpcCodes": [],
		"shortedItemUpc": [],
		"exceptionQty": 0.0,
		"processedQty": 0.0,
		"entityReference": {
			"entityId": "9000001405",
			"entityType": "ORDER"
		},
		"instruction": null,
		"amount": {
			"amount": 5.99,
			"currency": "USD"
		},
		"completionTime": null,
		"subAllowed": true,
		"seqNumber": 21,
		"subCode": "01",
		"subValue": "No Substitution",
		"primaryUpc": "0005210000256",
		"uom": "COUNT",
		"pluCode": "0",
		"sellByWeightInd": "I",
		"depName": "Condiments, Spice & Bake",
		"itemWeight": "0.0",
		"itemWeightUom": "lb",
		"storageType": "AM",
		"customerOrderNumber": "9001405"
	}, {
		"id": 7072,
		"itemId": "188300244",
		"itemAddress": {
			"bay": "06",
			"aisleSeq": "ME",
			"level": "017",
			"side": "B",
			"deptShortName": "Meat"
		},
		"imageURL": "https://images.albertsons-media.com/is/image/ABS/188300244",
		"itemDescription": "Foster Farms Chicken Livers Fresh - 1.00 LB",
		"attemptToRemove": false,
		"qty": 2.0,
		"pickedUpcCodes": [],
		"shortedItemUpc": [],
		"exceptionQty": 0.0,
		"processedQty": 0.0,
		"entityReference": {
			"entityId": "9000001405",
			"entityType": "ORDER"
		},
		"instruction": null,
		"amount": {
			"amount": 2.99,
			"currency": "USD"
		},
		"completionTime": null,
		"subAllowed": true,
		"seqNumber": 26,
		"subCode": "03",
		"subValue": "Same Brand Diff Size",
		"primaryUpc": null,
		"uom": "LB",
		"pluCode": "",
		"sellByWeightInd": "P",
		"depName": "Chicken",
		"itemWeight": " 000000.0000",
		"itemWeightUom": null,
		"storageType": "CH",
		"customerOrderNumber": "9001405"
	}],
	"expectedCount": 18,
	"itemQty": 18.0,
	"processedQty": null,
	"exceptionQty": null,
	"pickUpBay": null,
	"contactFirstName": "Amber",
	"contactLastName": "Amber2",
	"seqNo": "1",
	"totalSeqNo": "1",
	"stopNumber": "539",
	"bagCountRequired": true,
	"reProcess": false,
	"bagCount": null,
	"customerOrderNumber": "9001405",
	"nextActivityId": null
}"""

internal const val TEST_3_ACTIVITY_DETAIL_ONE_PICK_FZ_JSON =
    """{
	"actId": 3138,
	"erId": 2113,
	"siteId": "2941",
	"createdDate": "2020-10-12T12:58:59.428Z",
	"status": "IN_PROGRESS",
	"expectedEndTime": "2020-10-12T22:00:00Z",
	"completionTime": null,
	"actType": "PICK_PACK",
	"batch": "DUG-2020-10-14T22:59:00Z-2020-10-14T23:59:00Z",
	"routeVanNumber": "DUG",
	"assignedTo": null,
	"activityNo": "1123",
	"storageTypes": ["AM", "CH", "FZ"],
	"handshakeType": null,
	"slotStartDate": "2020-10-14T22:59:00Z",
	"slotEndDate": "2020-10-14T23:59:00Z",
	"fulfillmentType": "DUG",
	"entityReference": {
		"entityId": "9000001405",
		"entityType": "ORDER"
	},
	"containerActivities": [{
		"id": 3302,
		"containerId": "TTC01",
		"location": null,
		"nextDestination": null,
		"containerType": "FZ",
		"reference": {
			"entityId": "9000001405",
			"entityType": "ORDER"
		},
		"status": "EXPECTED",
		"attemptToRemove": false,
		"lastScanTime": "2020-10-12T21:42:56.177Z",
		"containerItems": [{
			"imageUrl": "https://images.albertsons-media.com/is/image/ABS/960468979",
			"itemDesc": "Alpha Foods Burrito Plant Based Philly Sandwich - 5 Oz",
			"itemId": "960468979",
			"qty": 1.0,
			"regulated": false
		}],
		"bagCount": null,
		"regulated": false
	}],
	"itemActivities": [{
		"id": 7068,
		"itemId": "184060016",
		"itemAddress": null,
		"imageURL": "https://images.albertsons-media.com/is/image/ABS/184060016",
		"itemDescription": "Bananas Red",
		"attemptToRemove": false,
		"qty": 2.0,
		"pickedUpcCodes": [],
		"shortedItemUpc": [],
		"exceptionQty": 0.0,
		"processedQty": 0.0,
		"entityReference": {
			"entityId": "9000001405",
			"entityType": "ORDER"
		},
		"instruction": null,
		"amount": {
			"amount": 1.99,
			"currency": "USD"
		},
		"completionTime": null,
		"subAllowed": true,
		"seqNumber": null,
		"subCode": "03",
		"subValue": "Same Brand Diff Size",
		"primaryUpc": "0048406001701",
		"uom": "LB",
		"pluCode": "4238",
		"sellByWeightInd": "W",
		"depName": "Fruits & Vegetables",
		"itemWeight": "0.25",
		"itemWeightUom": "lb",
		"storageType": "AM",
		"customerOrderNumber": "9001405"
	}, {
		"id": 7070,
		"itemId": "184110040",
		"itemAddress": null,
		"imageURL": "https://images.albertsons-media.com/is/image/ABS/184110040",
		"itemDescription": "Mini Seedless Watermelon",
		"attemptToRemove": false,
		"qty": 1.0,
		"pickedUpcCodes": [],
		"shortedItemUpc": [],
		"exceptionQty": 0.0,
		"processedQty": 0.0,
		"entityReference": {
			"entityId": "9000001405",
			"entityType": "ORDER"
		},
		"instruction": null,
		"amount": {
			"amount": 2.99,
			"currency": "USD"
		},
		"completionTime": null,
		"subAllowed": true,
		"seqNumber": null,
		"subCode": "01",
		"subValue": "No Substitution",
		"primaryUpc": "0061002013000",
		"uom": "EACH",
		"pluCode": "3421",
		"sellByWeightInd": "E",
		"depName": "Fruits & Vegetables",
		"itemWeight": "0.0",
		"itemWeightUom": "lb",
		"storageType": "AM",
		"customerOrderNumber": "9001405"
	}, {
		"id": 7073,
		"itemId": "188150167",
		"itemAddress": null,
		"imageURL": "https://images.albertsons-media.com/is/image/ABS/188150167",
		"itemDescription": "Open Nature Lamb Shanks - 1.50 LB",
		"attemptToRemove": false,
		"qty": 1.0,
		"pickedUpcCodes": [],
		"shortedItemUpc": [],
		"exceptionQty": 0.0,
		"processedQty": 0.0,
		"entityReference": {
			"entityId": "9000001405",
			"entityType": "ORDER"
		},
		"instruction": null,
		"amount": {
			"amount": 9.99,
			"currency": "USD"
		},
		"completionTime": null,
		"subAllowed": true,
		"seqNumber": null,
		"subCode": "01",
		"subValue": "No Substitution",
		"primaryUpc": "0020303900000",
		"uom": "LB",
		"pluCode": "0",
		"sellByWeightInd": "P",
		"depName": "Meat & Seafood",
		"itemWeight": "1.5",
		"itemWeightUom": "lb",
		"storageType": "CH",
		"customerOrderNumber": "9001405"
	}, {
		"id": 7071,
		"itemId": "184700085",
		"itemAddress": {
			"bay": "06",
			"aisleSeq": "977",
			"level": "019",
			"side": "L",
			"deptShortName": "977"
		},
		"imageURL": "https://images.albertsons-media.com/is/image/ABS/184700085",
		"itemDescription": "Organic Lemon",
		"attemptToRemove": false,
		"qty": 3.0,
		"pickedUpcCodes": [],
		"shortedItemUpc": [],
		"exceptionQty": 0.0,
		"processedQty": 0.0,
		"entityReference": {
			"entityId": "9000001405",
			"entityType": "ORDER"
		},
		"instruction": null,
		"amount": {
			"amount": 0.99,
			"currency": "USD"
		},
		"completionTime": null,
		"subAllowed": true,
		"seqNumber": 4,
		"subCode": "01",
		"subValue": "No Substitution",
		"primaryUpc": "0072906294053",
		"uom": "EACH",
		"pluCode": "94053",
		"sellByWeightInd": "E",
		"depName": "Fruits & Vegetables",
		"itemWeight": "0.0",
		"itemWeightUom": "lb",
		"storageType": "AM",
		"customerOrderNumber": "9001405"
	}, {
		"id": 7067,
		"itemId": "960228461",
		"itemAddress": {
			"bay": "01",
			"aisleSeq": "803",
			"level": "011",
			"side": "L",
			"deptShortName": "803"
		},
		"imageURL": "https://images.albertsons-media.com/is/image/ABS/960228461",
		"itemDescription": "Crown Royal Whisky Flavored Vanilla 70 Proof - 750 Ml",
		"attemptToRemove": false,
		"qty": 1.0,
		"pickedUpcCodes": [],
		"shortedItemUpc": [],
		"exceptionQty": 0.0,
		"processedQty": 0.0,
		"entityReference": {
			"entityId": "9000001405",
			"entityType": "ORDER"
		},
		"instruction": null,
		"amount": {
			"amount": 49.99,
			"currency": "USD"
		},
		"completionTime": null,
		"subAllowed": true,
		"seqNumber": 5,
		"subCode": "03",
		"subValue": "Same Brand Diff Size",
		"primaryUpc": null,
		"uom": "COUNT",
		"pluCode": "",
		"sellByWeightInd": "I",
		"depName": "Spirits",
		"itemWeight": " 000000.1800",
		"itemWeightUom": null,
		"storageType": "AM",
		"customerOrderNumber": "9001405"
	}, {
		"id": 7074,
		"itemId": "960468979",
		"itemAddress": {
			"bay": "10",
			"aisleSeq": "09",
			"level": "018",
			"side": "L",
			"deptShortName": null
		},
		"imageURL": "https://images.albertsons-media.com/is/image/ABS/960468979",
		"itemDescription": "Alpha Foods Burrito Plant Based Philly Sandwich - 5 Oz",
		"attemptToRemove": false,
		"qty": 4.0,
		"pickedUpcCodes": [{
			"upcId": 2021,
			"upc": "855099007023",
			"qty": 1.0,
			"upcQty": 1.0,
			"userId": "jstoc62",
			"containerId": "TTC01",
			"substituteItemId": null,
			"substituteItemDesc": null,
			"pickedTime": "2020-10-12T21:42:54.252Z",
			"isSubstitution": false
		}],
		"shortedItemUpc": [],
		"exceptionQty": 0.0,
		"processedQty": 1.0,
		"entityReference": {
			"entityId": "9000001405",
			"entityType": "ORDER"
		},
		"instruction": null,
		"amount": {
			"amount": 2.99,
			"currency": "USD"
		},
		"completionTime": null,
		"subAllowed": true,
		"seqNumber": 6,
		"subCode": "03",
		"subValue": "Same Brand Diff Size",
		"primaryUpc": "0085509900702",
		"uom": "COUNT",
		"pluCode": "0",
		"sellByWeightInd": "I",
		"depName": "Frozen Foods",
		"itemWeight": "0.0",
		"itemWeightUom": "lb",
		"storageType": "FZ",
		"customerOrderNumber": "9001405"
	}, {
		"id": 7066,
		"itemId": "960134449",
		"itemAddress": {
			"bay": "26",
			"aisleSeq": "01",
			"level": "044",
			"side": "R",
			"deptShortName": null
		},
		"imageURL": "https://images.albertsons-media.com/is/image/ABS/960134449",
		"itemDescription": "Heinz Mustard Yellow - 20 Oz",
		"attemptToRemove": false,
		"qty": 2.0,
		"pickedUpcCodes": [],
		"shortedItemUpc": [],
		"exceptionQty": 0.0,
		"processedQty": 0.0,
		"entityReference": {
			"entityId": "9000001405",
			"entityType": "ORDER"
		},
		"instruction": {
			"type": null,
			"text": "hello"
		},
		"amount": {
			"amount": 5.99,
			"currency": "USD"
		},
		"completionTime": null,
		"subAllowed": true,
		"seqNumber": 19,
		"subCode": "02",
		"subValue": "Same Brand Diff Size",
		"primaryUpc": "0001300000218",
		"uom": "COUNT",
		"pluCode": "0",
		"sellByWeightInd": "I",
		"depName": "Condiments, Spice & Bake",
		"itemWeight": "0.0",
		"itemWeightUom": "lb",
		"storageType": "AM",
		"customerOrderNumber": "9001405"
	}, {
		"id": 7069,
		"itemId": "114150166",
		"itemAddress": {
			"bay": "29",
			"aisleSeq": "03",
			"level": "114",
			"side": "L",
			"deptShortName": null
		},
		"imageURL": "https://images.albertsons-media.com/is/image/ABS/114150166",
		"itemDescription": "McCormick Ground Nutmeg - 1.1 Oz",
		"attemptToRemove": false,
		"qty": 2.0,
		"pickedUpcCodes": [],
		"shortedItemUpc": [],
		"exceptionQty": 0.0,
		"processedQty": 0.0,
		"entityReference": {
			"entityId": "9000001405",
			"entityType": "ORDER"
		},
		"instruction": null,
		"amount": {
			"amount": 5.99,
			"currency": "USD"
		},
		"completionTime": null,
		"subAllowed": true,
		"seqNumber": 21,
		"subCode": "01",
		"subValue": "No Substitution",
		"primaryUpc": "0005210000256",
		"uom": "COUNT",
		"pluCode": "0",
		"sellByWeightInd": "I",
		"depName": "Condiments, Spice & Bake",
		"itemWeight": "0.0",
		"itemWeightUom": "lb",
		"storageType": "AM",
		"customerOrderNumber": "9001405"
	}, {
		"id": 7072,
		"itemId": "188300244",
		"itemAddress": {
			"bay": "06",
			"aisleSeq": "ME",
			"level": "017",
			"side": "B",
			"deptShortName": "Meat"
		},
		"imageURL": "https://images.albertsons-media.com/is/image/ABS/188300244",
		"itemDescription": "Foster Farms Chicken Livers Fresh - 1.00 LB",
		"attemptToRemove": false,
		"qty": 2.0,
		"pickedUpcCodes": [],
		"shortedItemUpc": [],
		"exceptionQty": 0.0,
		"processedQty": 0.0,
		"entityReference": {
			"entityId": "9000001405",
			"entityType": "ORDER"
		},
		"instruction": null,
		"amount": {
			"amount": 2.99,
			"currency": "USD"
		},
		"completionTime": null,
		"subAllowed": true,
		"seqNumber": 26,
		"subCode": "03",
		"subValue": "Same Brand Diff Size",
		"primaryUpc": null,
		"uom": "LB",
		"pluCode": "",
		"sellByWeightInd": "P",
		"depName": "Chicken",
		"itemWeight": " 000000.0000",
		"itemWeightUom": null,
		"storageType": "CH",
		"customerOrderNumber": "9001405"
	}],
	"expectedCount": 18,
	"itemQty": 18.0,
	"processedQty": null,
	"exceptionQty": null,
	"pickUpBay": null,
	"contactFirstName": "Amber",
	"contactLastName": "Amber2",
	"seqNo": "1",
	"totalSeqNo": "1",
	"stopNumber": "539",
	"bagCountRequired": true,
	"reProcess": false,
	"bagCount": null,
	"customerOrderNumber": "9001405",
	"nextActivityId": null
}"""

internal const val TEST_3_ACTIVITY_DETAIL_ONE_PICK_AM_JSON =
    """{
	"actId": 3138,
	"erId": 2113,
	"siteId": "2941",
	"createdDate": "2020-10-12T12:58:59.428Z",
	"status": "IN_PROGRESS",
	"expectedEndTime": "2020-10-12T22:00:00Z",
	"completionTime": null,
	"actType": "PICK_PACK",
	"batch": "DUG-2020-10-14T22:59:00Z-2020-10-14T23:59:00Z",
	"routeVanNumber": "DUG",
	"assignedTo": null,
	"activityNo": "1123",
	"storageTypes": ["AM", "CH", "FZ"],
	"handshakeType": null,
	"slotStartDate": "2020-10-14T22:59:00Z",
	"slotEndDate": "2020-10-14T23:59:00Z",
	"fulfillmentType": "DUG",
	"entityReference": {
		"entityId": "9000001405",
		"entityType": "ORDER"
	},
	"containerActivities": [{
		"id": 3304,
		"containerId": "TTC01",
		"location": null,
		"nextDestination": null,
		"containerType": "AM",
		"reference": {
			"entityId": "9000001405",
			"entityType": "ORDER"
		},
		"status": "EXPECTED",
		"attemptToRemove": false,
		"lastScanTime": "2020-10-12T21:46:09.762Z",
		"containerItems": [{
			"imageUrl": "https://images.albertsons-media.com/is/image/ABS/960228461",
			"itemDesc": "Crown Royal Whisky Flavored Vanilla 70 Proof - 750 Ml",
			"itemId": "960228461",
			"qty": 1.0,
			"regulated": true
		}],
		"bagCount": null,
		"regulated": true
	}],
	"itemActivities": [{
		"id": 7068,
		"itemId": "184060016",
		"itemAddress": null,
		"imageURL": "https://images.albertsons-media.com/is/image/ABS/184060016",
		"itemDescription": "Bananas Red",
		"attemptToRemove": false,
		"qty": 2.0,
		"pickedUpcCodes": [],
		"shortedItemUpc": [],
		"exceptionQty": 0.0,
		"processedQty": 0.0,
		"entityReference": {
			"entityId": "9000001405",
			"entityType": "ORDER"
		},
		"instruction": null,
		"amount": {
			"amount": 1.99,
			"currency": "USD"
		},
		"completionTime": null,
		"subAllowed": true,
		"seqNumber": null,
		"subCode": "03",
		"subValue": "Same Brand Diff Size",
		"primaryUpc": "0048406001701",
		"uom": "LB",
		"pluCode": "4238",
		"sellByWeightInd": "W",
		"depName": "Fruits & Vegetables",
		"itemWeight": "0.25",
		"itemWeightUom": "lb",
		"storageType": "AM",
		"customerOrderNumber": "9001405"
	}, {
		"id": 7070,
		"itemId": "184110040",
		"itemAddress": null,
		"imageURL": "https://images.albertsons-media.com/is/image/ABS/184110040",
		"itemDescription": "Mini Seedless Watermelon",
		"attemptToRemove": false,
		"qty": 1.0,
		"pickedUpcCodes": [],
		"shortedItemUpc": [],
		"exceptionQty": 0.0,
		"processedQty": 0.0,
		"entityReference": {
			"entityId": "9000001405",
			"entityType": "ORDER"
		},
		"instruction": null,
		"amount": {
			"amount": 2.99,
			"currency": "USD"
		},
		"completionTime": null,
		"subAllowed": true,
		"seqNumber": null,
		"subCode": "01",
		"subValue": "No Substitution",
		"primaryUpc": "0061002013000",
		"uom": "EACH",
		"pluCode": "3421",
		"sellByWeightInd": "E",
		"depName": "Fruits & Vegetables",
		"itemWeight": "0.0",
		"itemWeightUom": "lb",
		"storageType": "AM",
		"customerOrderNumber": "9001405"
	}, {
		"id": 7073,
		"itemId": "188150167",
		"itemAddress": null,
		"imageURL": "https://images.albertsons-media.com/is/image/ABS/188150167",
		"itemDescription": "Open Nature Lamb Shanks - 1.50 LB",
		"attemptToRemove": false,
		"qty": 1.0,
		"pickedUpcCodes": [],
		"shortedItemUpc": [],
		"exceptionQty": 0.0,
		"processedQty": 0.0,
		"entityReference": {
			"entityId": "9000001405",
			"entityType": "ORDER"
		},
		"instruction": null,
		"amount": {
			"amount": 9.99,
			"currency": "USD"
		},
		"completionTime": null,
		"subAllowed": true,
		"seqNumber": null,
		"subCode": "01",
		"subValue": "No Substitution",
		"primaryUpc": "0020303900000",
		"uom": "LB",
		"pluCode": "0",
		"sellByWeightInd": "P",
		"depName": "Meat & Seafood",
		"itemWeight": "1.5",
		"itemWeightUom": "lb",
		"storageType": "CH",
		"customerOrderNumber": "9001405"
	}, {
		"id": 7071,
		"itemId": "184700085",
		"itemAddress": {
			"bay": "06",
			"aisleSeq": "977",
			"level": "019",
			"side": "L",
			"deptShortName": "977"
		},
		"imageURL": "https://images.albertsons-media.com/is/image/ABS/184700085",
		"itemDescription": "Organic Lemon",
		"attemptToRemove": false,
		"qty": 3.0,
		"pickedUpcCodes": [],
		"shortedItemUpc": [],
		"exceptionQty": 0.0,
		"processedQty": 0.0,
		"entityReference": {
			"entityId": "9000001405",
			"entityType": "ORDER"
		},
		"instruction": null,
		"amount": {
			"amount": 0.99,
			"currency": "USD"
		},
		"completionTime": null,
		"subAllowed": true,
		"seqNumber": 4,
		"subCode": "01",
		"subValue": "No Substitution",
		"primaryUpc": "0072906294053",
		"uom": "EACH",
		"pluCode": "94053",
		"sellByWeightInd": "E",
		"depName": "Fruits & Vegetables",
		"itemWeight": "0.0",
		"itemWeightUom": "lb",
		"storageType": "AM",
		"customerOrderNumber": "9001405"
	}, {
		"id": 7067,
		"itemId": "960228461",
		"itemAddress": {
			"bay": "01",
			"aisleSeq": "803",
			"level": "011",
			"side": "L",
			"deptShortName": "803"
		},
		"imageURL": "https://images.albertsons-media.com/is/image/ABS/960228461",
		"itemDescription": "Crown Royal Whisky Flavored Vanilla 70 Proof - 750 Ml",
		"attemptToRemove": false,
		"qty": 1.0,
		"pickedUpcCodes": [{
			"upcId": 2023,
			"upc": "082000776591",
			"qty": 1.0,
			"upcQty": 1.0,
			"userId": "jstoc62",
			"containerId": "TTC01",
			"substituteItemId": null,
			"substituteItemDesc": null,
			"pickedTime": "2020-10-12T21:46:07.886Z",
			"isSubstitution": false
		}],
		"shortedItemUpc": [],
		"exceptionQty": 0.0,
		"processedQty": 1.0,
		"entityReference": {
			"entityId": "9000001405",
			"entityType": "ORDER"
		},
		"instruction": null,
		"amount": {
			"amount": 49.99,
			"currency": "USD"
		},
		"completionTime": null,
		"subAllowed": true,
		"seqNumber": 5,
		"subCode": "03",
		"subValue": "Same Brand Diff Size",
		"primaryUpc": null,
		"uom": "COUNT",
		"pluCode": "",
		"sellByWeightInd": "I",
		"depName": "Spirits",
		"itemWeight": " 000000.1800",
		"itemWeightUom": null,
		"storageType": "AM",
		"customerOrderNumber": "9001405"
	}, {
		"id": 7074,
		"itemId": "960468979",
		"itemAddress": {
			"bay": "10",
			"aisleSeq": "09",
			"level": "018",
			"side": "L",
			"deptShortName": null
		},
		"imageURL": "https://images.albertsons-media.com/is/image/ABS/960468979",
		"itemDescription": "Alpha Foods Burrito Plant Based Philly Sandwich - 5 Oz",
		"attemptToRemove": false,
		"qty": 4.0,
		"pickedUpcCodes": [],
		"shortedItemUpc": [],
		"exceptionQty": 0.0,
		"processedQty": 0.0,
		"entityReference": {
			"entityId": "9000001405",
			"entityType": "ORDER"
		},
		"instruction": null,
		"amount": {
			"amount": 2.99,
			"currency": "USD"
		},
		"completionTime": null,
		"subAllowed": true,
		"seqNumber": 6,
		"subCode": "03",
		"subValue": "Same Brand Diff Size",
		"primaryUpc": "0085509900702",
		"uom": "COUNT",
		"pluCode": "0",
		"sellByWeightInd": "I",
		"depName": "Frozen Foods",
		"itemWeight": "0.0",
		"itemWeightUom": "lb",
		"storageType": "FZ",
		"customerOrderNumber": "9001405"
	}, {
		"id": 7066,
		"itemId": "960134449",
		"itemAddress": {
			"bay": "26",
			"aisleSeq": "01",
			"level": "044",
			"side": "R",
			"deptShortName": null
		},
		"imageURL": "https://images.albertsons-media.com/is/image/ABS/960134449",
		"itemDescription": "Heinz Mustard Yellow - 20 Oz",
		"attemptToRemove": false,
		"qty": 2.0,
		"pickedUpcCodes": [],
		"shortedItemUpc": [],
		"exceptionQty": 0.0,
		"processedQty": 0.0,
		"entityReference": {
			"entityId": "9000001405",
			"entityType": "ORDER"
		},
		"instruction": {
			"type": null,
			"text": "hello"
		},
		"amount": {
			"amount": 5.99,
			"currency": "USD"
		},
		"completionTime": null,
		"subAllowed": true,
		"seqNumber": 19,
		"subCode": "02",
		"subValue": "Same Brand Diff Size",
		"primaryUpc": "0001300000218",
		"uom": "COUNT",
		"pluCode": "0",
		"sellByWeightInd": "I",
		"depName": "Condiments, Spice & Bake",
		"itemWeight": "0.0",
		"itemWeightUom": "lb",
		"storageType": "AM",
		"customerOrderNumber": "9001405"
	}, {
		"id": 7069,
		"itemId": "114150166",
		"itemAddress": {
			"bay": "29",
			"aisleSeq": "03",
			"level": "114",
			"side": "L",
			"deptShortName": null
		},
		"imageURL": "https://images.albertsons-media.com/is/image/ABS/114150166",
		"itemDescription": "McCormick Ground Nutmeg - 1.1 Oz",
		"attemptToRemove": false,
		"qty": 2.0,
		"pickedUpcCodes": [],
		"shortedItemUpc": [],
		"exceptionQty": 0.0,
		"processedQty": 0.0,
		"entityReference": {
			"entityId": "9000001405",
			"entityType": "ORDER"
		},
		"instruction": null,
		"amount": {
			"amount": 5.99,
			"currency": "USD"
		},
		"completionTime": null,
		"subAllowed": true,
		"seqNumber": 21,
		"subCode": "01",
		"subValue": "No Substitution",
		"primaryUpc": "0005210000256",
		"uom": "COUNT",
		"pluCode": "0",
		"sellByWeightInd": "I",
		"depName": "Condiments, Spice & Bake",
		"itemWeight": "0.0",
		"itemWeightUom": "lb",
		"storageType": "AM",
		"customerOrderNumber": "9001405"
	}, {
		"id": 7072,
		"itemId": "188300244",
		"itemAddress": {
			"bay": "06",
			"aisleSeq": "ME",
			"level": "017",
			"side": "B",
			"deptShortName": "Meat"
		},
		"imageURL": "https://images.albertsons-media.com/is/image/ABS/188300244",
		"itemDescription": "Foster Farms Chicken Livers Fresh - 1.00 LB",
		"attemptToRemove": false,
		"qty": 2.0,
		"pickedUpcCodes": [],
		"shortedItemUpc": [],
		"exceptionQty": 0.0,
		"processedQty": 0.0,
		"entityReference": {
			"entityId": "9000001405",
			"entityType": "ORDER"
		},
		"instruction": null,
		"amount": {
			"amount": 2.99,
			"currency": "USD"
		},
		"completionTime": null,
		"subAllowed": true,
		"seqNumber": 26,
		"subCode": "03",
		"subValue": "Same Brand Diff Size",
		"primaryUpc": null,
		"uom": "LB",
		"pluCode": "",
		"sellByWeightInd": "P",
		"depName": "Chicken",
		"itemWeight": " 000000.0000",
		"itemWeightUom": null,
		"storageType": "CH",
		"customerOrderNumber": "9001405"
	}],
	"expectedCount": 18,
	"itemQty": 18.0,
	"processedQty": null,
	"exceptionQty": null,
	"pickUpBay": null,
	"contactFirstName": "Amber",
	"contactLastName": "Amber2",
	"seqNo": "1",
	"totalSeqNo": "1",
	"stopNumber": "539",
	"bagCountRequired": true,
	"reProcess": false,
	"bagCount": null,
	"customerOrderNumber": "9001405",
	"nextActivityId": null
}"""

internal const val TEST_3_RECORD_PICK_ONE_FZ_ITEM_TTC01_REQUEST_JSON =
    """{
	"actId": 3138,
	"lineReq": [{
		"containerId": "TTC01",
		"iaId": 7074,
		"upcId": "855099007023",
		"ignoreUpc": true,
		"pickedTime": "2020-10-12T21:42:54.252Z",
		"disableContainerValidation": false,
		"fulfilledQty": 1.0,
		"substitution": false,
		"upcQty": 1.0,
		"userId": "jstoc62"
	}]
}"""

internal const val TEST_3_RECORD_PICK_ONE_FZ_ITEM_TTC01_RESPONSE_JSON =
    """[{
	"iaId": 7074,
	"processedQty": 1.0,
	"pickedUpcCodes": [{
		"upcId": 2021,
		"upc": "855099007023",
		"qty": 1.0,
		"upcQty": 1.0,
		"userId": "jstoc62",
		"containerId": "TTC01",
		"substituteItemId": null,
		"substituteItemDesc": null,
		"pickedTime": "2020-10-12T21:42:54.252Z",
		"isSubstitution": false
	}]
}]"""

internal const val TEST_3_RECORD_PICK_ONE_AM_ITEM_TTC01_REQUEST_JSON =
    """{
	"actId": 3138,
	"lineReq": [{
		"containerId": "TTC01",
		"iaId": 7067,
		"upcId": "082000776591",
		"ignoreUpc": true,
		"pickedTime": "2020-10-12T21:46:07.886Z",
		"disableContainerValidation": false,
		"fulfilledQty": 1.0,
		"substitution": false,
		"upcQty": 1.0,
		"userId": "jstoc62"
	}]
}"""

internal const val TEST_3_RECORD_PICK_ONE_AM_ITEM_TTC01_RESPONSE_JSON =
    """[{
	"iaId": 7067,
	"processedQty": 1.0,
	"pickedUpcCodes": [{
		"upcId": 2023,
		"upc": "082000776591",
		"qty": 1.0,
		"upcQty": 1.0,
		"userId": "jstoc62",
		"containerId": "TTC01",
		"substituteItemId": null,
		"substituteItemDesc": null,
		"pickedTime": "2020-10-12T21:46:07.886Z",
		"isSubstitution": false
	}]
}]"""

internal const val TEST_3_UNDO_PICK_ONE_FZ_ITEM_REQUEST_JSON =
    """{
    "containerId": "TTC01",
    "undoPickRequestDto": {
	"actId": 3138,
	"iaId": 7074,
	"pickedUpcId": 2021,
	"qty": 1.0
    }
}"""

internal const val TEST_3_UNDO_PICK_ONE_FZ_ITEM_RESPONSE_JSON =
    """[{
	"iaId": 7074,
	"processedQty": 0.0,
	"pickedUpcCodes": []
}]"""
