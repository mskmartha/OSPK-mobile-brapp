package com.albertsons.acupick.data.test.pick

import org.intellij.lang.annotations.Language

@Language("JSON")
internal const val TEST_2_RECORD_PICK_ONE_ITEM_REQUEST_JSON =
    """{
	"actId": 4445,
	"lineReq": [{
		"containerId": "TTC02",
		"iaId": 18982,
		"upcId": "070459009107",
		"ignoreUpc": true,
		"pickedTime": "2020-09-28T14:21:25.852Z",
		"fulfilledQty": 1.0,
		"substitution": false,
		"upcQty": 1.0,
		"userId": "Test16"
	}]
}"""

@Language("JSON")
internal const val TEST_2_RECORD_PICK_ONE_ITEM_RESPONSE_JSON =
    """[{
	"iaId": 18982,
	"processedQty": 1.0,
	"pickedUpcCodes": [{
		"upcId": 2120,
		"upc": "013000002189",
		"qty": 1.0,
		"upcQty": 1.0,
		"userId": "Test16",
		"containerId": "TTC02",
		"pickedTime": "2020-09-28T14:21:25.852Z",
		"isSubstitution": false
	}]
}]"""

@Language("JSON")
internal const val TEST_2_UNDO_PICK_ONE_ITEM_REQUEST_JSON =
    """{
    "containerId": "TTC02",
    "undoPickRequestDto": {
	"actId": 4445,
	"iaId": 18982,
	"pickedUpcId": 2120,
	"qty": 1.0
    }
}"""

@Language("JSON")
internal const val TEST_2_UNDO_PICK_ONE_ITEM_RESPONSE_JSON =
    """[{
	"iaId": 18982,
	"processedQty": 0.0,
	"pickedUpcCodes": []
}]"""
