package com.albertsons.acupick.data.model.barcode

import com.albertsons.acupick.data.model.StorageType
import com.albertsons.acupick.data.test.BaseTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

// Prevents ktlint from failing on line length due to long test names - prefer to not auto format these test lines into multiple for readability
/* ktlint-disable max-line-length */
@RunWith(Enclosed::class)
class BarcodeMapperImplementationTest {

    @RunWith(Parameterized::class)
    class InferBarcodeTypeParameterizedTests(private val testName: String, private val barcode: String, private val output: BarcodeType) : BaseTest() {

        companion object {

            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun data() = listOf(

                // BR Sample Barcodes & Test Data Creation confluence page: https://confluence.bottlerocketapps.com/x/VpkFAw
                // ALB Barcode formats confluence page: https://confluence.safeway.com/display/EOM/Barcode+formats
                // Modification
                arrayOf("WHEN a Normal barcode has whitespace padding THEN trim it and return Normal", "  078000053463   ", BarcodeType.Item.Normal(catalogLookupUpc = "0007800005346", rawBarcode = "078000053463")),
                // Totes
                arrayOf("WHEN the barcode is a valid tote barcode THEN return Tote", "TTC01", BarcodeType.Tote("TTC01")),
                arrayOf(
                    "WHEN the barcode is a valid MFC picking tote license plate barcode that starts with 999 THEN return MfcPickingToteLicensePlate", "99901234567890",
                    BarcodeType.MfcPickingToteLicensePlate(
                        rawBarcode = "99901234567890",
                        mfcStorageTypes = listOf(StorageType.CH, StorageType.FZ)
                    )
                ),
                arrayOf(
                    "WHEN the barcode is a valid MFC picking tote license plate barcode that starts with 998 THEN return MfcPickingToteLicensePlate", "99801234567890",
                    BarcodeType.MfcPickingToteLicensePlate(
                        rawBarcode = "99801234567890",
                        mfcStorageTypes = listOf(StorageType.AM, StorageType.HT)
                    )
                ),
                arrayOf("WHEN the barcode is an invalid tote barcode with too many digits THEN return Unknown", "TTC015", BarcodeType.Unknown("TTC015")),
                arrayOf("WHEN the barcode is an invalid tote barcode with too few digits THEN return Unknown", "TTC0", BarcodeType.Unknown("TTC0")),
                arrayOf("WHEN the barcode is an invalid tote barcode with too many alphabetic chars THEN return Unknown", "TT01", BarcodeType.Unknown("TT01")),
                arrayOf("WHEN the barcode is an invalid tote barcode with too few alphabetic chars THEN return Unknown", "TTCA01", BarcodeType.Unknown("TTCA01")),
                // Invalid with 0 price
                arrayOf("WHEN the barcode is invalid because it has zero price with 13 characters", "0242178000000", BarcodeType.Unknown("0242178000000")),
                arrayOf("WHEN the barcode is invalid because it has zero price with 12 characters", "242178000000", BarcodeType.Unknown("242178000000")),

                // Invalid old manhattan barcodes
                arrayOf("WHEN the barcode is invalid because it has zero price with leading 002", "0022345600000", BarcodeType.Unknown("0022345600000")),
                arrayOf("WHEN the barcode is invalid because it has zero price with leading 001", "0012345600000", BarcodeType.Unknown("0012345600000")),
                arrayOf("WHEN the barcode is invalid because it has zero price with leading 004", "0042345600000", BarcodeType.Unknown("0042345600000")),
                arrayOf("WHEN the barcode is invalid because it has zero price with leading 006", "0062345600000", BarcodeType.Unknown("0062345600000")),
                arrayOf("WHEN the barcode is invalid because it has zero price with leading 007", "0072345600000", BarcodeType.Unknown("0072345600000")),
                // arrayOf("WHEN the GS1 barcode is invalid because it has zero weight with leading 004", "01000000002345600000000000", BarcodeType.Unknown("0423456000000")),
                // Imaginary invalid manhattan barcodes
                arrayOf("WHEN the barcode is invalid because it has zero price with leading 003", "0032345600000", BarcodeType.Unknown("0032345600000")),
                arrayOf("WHEN the barcode is invalid because it has zero price with leading 005", "0052345600000", BarcodeType.Unknown("0052345600000")),
                arrayOf("WHEN the barcode is invalid because it has zero price with leading 008", "0082345600000", BarcodeType.Unknown("0082345600000")),
                arrayOf("WHEN the barcode is invalid because it has zero price with leading 009", "0092345600000", BarcodeType.Unknown("0092345600000")),

                // Invalid
                arrayOf("WHEN the barcode is not a tote and has alphabetic characters THEN return Unknown", "a78000053463", BarcodeType.Unknown("a78000053463")),
                arrayOf("WHEN the barcode is empty THEN return Unknown", "", BarcodeType.Unknown("")),
                // > 13 characters failure cases
                arrayOf("WHEN the barcode is 14 characters return Unknown", "00012345600012", BarcodeType.Unknown("00012345600012")),
                arrayOf("WHEN the barcode is 15 characters return Unknown", "100012345600012", BarcodeType.PharmacyBag("100012345600012", "100012345600012", "", "")),
                arrayOf("WHEN the barcode is 16 characters return Unknown", "1200012345600012", BarcodeType.Unknown("1200012345600012")),
                // BAG
                arrayOf("WHEN the barcode is 7 digits - tote code - 6 digits characters return BAG", "1234567-AAA21-123456", BarcodeType.Bag(rawBarcode = "1234567-AAA21-123456", customerOrderNumber = "1234567", displayToteId = "AAA21", bagOrToteId = "123456")),
                arrayOf("WHEN the barcode is 8 digits - tote code - 6 digits characters return BAG", "12345678-AAA21-123456", BarcodeType.Bag(rawBarcode = "12345678-AAA21-123456", customerOrderNumber = "12345678", displayToteId = "AAA21", bagOrToteId = "123456")),
                arrayOf("WHEN the barcode is 9 digits - tote code - 6 digits characters return BAG", "123456789-AAA21-123456", BarcodeType.Bag(rawBarcode = "123456789-AAA21-123456", customerOrderNumber = "123456789", displayToteId = "AAA21", bagOrToteId = "123456")), // TODO remove test if regex is not needed

                // < 8 excluding 6 characters failure cases
                arrayOf("WHEN the barcode is 7 characters return Unknown", "0127890", BarcodeType.Unknown("0127890")),
                arrayOf("WHEN the barcode is 5 characters return Unknown", "12345", BarcodeType.Unknown("12345")),
                arrayOf("WHEN the barcode is 4 characters return Unknown", "1234", BarcodeType.Unknown("1234")),
                arrayOf("WHEN the barcode is 3 characters return Unknown", "123", BarcodeType.Unknown("123")),
                arrayOf("WHEN the barcode is 1 characters return Unknown", "12", BarcodeType.Unknown("12")),
                arrayOf("WHEN the barcode is 1 character return Unknown", "1", BarcodeType.Unknown("1")),
                arrayOf("WHEN 13 digit barcode has 9 zero's in front of last 4 digits", "0000000004261", BarcodeType.Unknown("0000000004261")),
                arrayOf("WHEN 13 digit barcode has 8 zero's before the last 5 digits", "0000000094899", BarcodeType.Unknown("0000000094899")),
                arrayOf("WHEN the barcode starts with 004 return Unknown", "0049406500000", BarcodeType.Unknown("0049406500000")),
                // Normal
                arrayOf("WHEN the barcode is a valid normal barcode THEN return Normal", "078000053463", BarcodeType.Item.Normal(catalogLookupUpc = "0007800005346", rawBarcode = "078000053463")),
                arrayOf("WHEN the barcode is a valid normal barcode THEN return Normal", "082000776591", BarcodeType.Item.Normal(catalogLookupUpc = "0008200077659", rawBarcode = "082000776591")),
                // Priced
                arrayOf("WHEN 12 digit barcode is priced THEN return Priced", "260813506315", BarcodeType.Item.Priced(plu = "60813", catalogLookupUpc = "0026081300000", rawPrice = "0631", rawBarcode = "0260813506315")),
                arrayOf("WHEN 13 digit barcode is priced THEN return Priced", "0260813506315", BarcodeType.Item.Priced(plu = "60813", catalogLookupUpc = "0026081300000", rawPrice = "0631", rawBarcode = "0260813506315")),
                arrayOf("WHEN 13 digit barcode is with start digit 022 priced THEN return Priced", "2235425149951", BarcodeType.Item.Priced(plu = "35425", catalogLookupUpc = "0023542500000", rawPrice = "14995", rawBarcode = "2235425149951")),

                // Weighted
                arrayOf("WHEN 12 digit barcode is weighted THEN return Weighted", "404235002561", BarcodeType.Item.Weighted(plu = "04235", catalogLookupUpc = "0040423500000", rawWeight = "00256", rawBarcode = "0404235002561")),
                arrayOf("WHEN 13 digit barcode is weighted THEN return Weighted", "0404235002561", BarcodeType.Item.Weighted(plu = "04235", catalogLookupUpc = "0040423500000", rawWeight = "00256", rawBarcode = "0404235002561")),
                arrayOf("WHEN 13 digit barcode2 is weighted THEN return Weighted", "0404131000500", BarcodeType.Item.Weighted(plu = "04131", catalogLookupUpc = "0040413100000", rawWeight = "00050", rawBarcode = "0404131000500")),
                arrayOf("WHEN 13 digit barcode3 is weighted THEN return Weighted", "0403151001252", BarcodeType.Item.Weighted(plu = "03151", catalogLookupUpc = "0040315100000", rawWeight = "00125", rawBarcode = "0403151001252")),
                arrayOf(
                    "GS1 barcode to 26 digit weighted barcode",
                    "01000000009407403203001995",
                    BarcodeType.Item.Weighted(plu = "94074", catalogLookupUpc = "0049407400000", rawWeight = "00199", rawBarcode = "0494074001995")
                ),
                // Short
                arrayOf("WHEN valid short w/ digit 7 = 0 THEN return Short", "01321207", BarcodeType.Item.Short(upcA = "013000002127", catalogLookupUpc = "0001300000212", rawBarcode = "01321207")),
                arrayOf("WHEN valid short w/ digit 7 = 1 THEN return Short", "05225619", BarcodeType.Item.Short(upcA = "052100002569", catalogLookupUpc = "0005210000256", rawBarcode = "05225619")),

                // Example shorts from http://www.barcodeisland.com/upce.phtml to cover all cases 0-9
                // ORIGINAL UPC-A  EQUIVALENT UPC-E  UPC-A EXAMPLE  UPC-E EQUIV.
                //     FORMAT          FORMAT
                //   AB00000HIJ        ABHIJ0         1200000789    127890
                //   AB10000HIJ        ABHIJ1         1210000789    127891
                //   AB20000HIJ        ABHIJ2         1220000789    127892

                //   AB300000IJ        AB3IJ3         1230000089    123893
                //   AB400000IJ        AB4IJ3         1240000089    124893
                //   AB500000IJ        AB5IJ3         1250000089    125893
                //   AB600000IJ        AB6IJ3         1260000089    126893
                //   AB700000IJ        AB7IJ3         1270000089    127893
                //   AB800000IJ        AB8IJ3         1280000089    128893
                //   AB900000IJ        AB9IJ3         1290000089    129893

                //   ABCD00000J        ABCDJ4         1291000009    129194
                //   ABCDE00005        ABCDE5         1291100005    129115
                //   ABCDE00006        ABCDE6         1291100006    129116
                //   ABCDE00007        ABCDE7         1291100007    129117
                //   ABCDE00008        ABCDE8         1291100008    129118
                //   ABCDE00009        ABCDE9         1291100009    129119
                arrayOf("WHEN valid short w/ digit 7 = 0 THEN return Short", "01278907", BarcodeType.Item.Short(upcA = "012000007897", catalogLookupUpc = "0001200000789", rawBarcode = "01278907")),
                arrayOf("WHEN valid short w/ digit 7 = 1 THEN return Short", "01278916", BarcodeType.Item.Short(upcA = "012100007896", catalogLookupUpc = "0001210000789", rawBarcode = "01278916")),
                arrayOf("WHEN valid short w/ digit 7 = 2 THEN return Short", "01278925", BarcodeType.Item.Short(upcA = "012200007895", catalogLookupUpc = "0001220000789", rawBarcode = "01278925")),

                arrayOf("WHEN valid short w/ digit 7 = 3, digit 3 = 3 THEN return Short", "01238935", BarcodeType.Item.Short(upcA = "012300000895", catalogLookupUpc = "0001230000089", rawBarcode = "01238935")),
                arrayOf("WHEN valid short w/ digit 7 = 3, digit 3 = 4 THEN return Short", "01248935", BarcodeType.Item.Short(upcA = "012400000894", catalogLookupUpc = "0001240000089", rawBarcode = "01248935")),
                arrayOf("WHEN valid short w/ digit 7 = 3, digit 3 = 5 THEN return Short", "01258935", BarcodeType.Item.Short(upcA = "012500000893", catalogLookupUpc = "0001250000089", rawBarcode = "01258935")),
                arrayOf("WHEN valid short w/ digit 7 = 3, digit 3 = 6 THEN return Short", "01268935", BarcodeType.Item.Short(upcA = "012600000892", catalogLookupUpc = "0001260000089", rawBarcode = "01268935")),
                arrayOf("WHEN valid short w/ digit 7 = 3, digit 3 = 7 THEN return Short", "01278935", BarcodeType.Item.Short(upcA = "012700000891", catalogLookupUpc = "0001270000089", rawBarcode = "01278935")),
                arrayOf("WHEN valid short w/ digit 7 = 3, digit 3 = 8 THEN return Short", "01288935", BarcodeType.Item.Short(upcA = "012800000890", catalogLookupUpc = "0001280000089", rawBarcode = "01288935")),
                arrayOf("WHEN valid short w/ digit 7 = 3, digit 3 = 9 THEN return Short", "01298935", BarcodeType.Item.Short(upcA = "012900000899", catalogLookupUpc = "0001290000089", rawBarcode = "01298935")),

                arrayOf("WHEN valid short w/ digit 7 = 4 THEN return Short", "01291944", BarcodeType.Item.Short(upcA = "012910000094", catalogLookupUpc = "0001291000009", rawBarcode = "01291944")),
                arrayOf("WHEN valid short w/ digit 7 = 5 THEN return Short", "01291157", BarcodeType.Item.Short(upcA = "012911000055", catalogLookupUpc = "0001291100005", rawBarcode = "01291157")),
                arrayOf("WHEN valid short w/ digit 7 = 6 THEN return Short", "01291162", BarcodeType.Item.Short(upcA = "012911000062", catalogLookupUpc = "0001291100006", rawBarcode = "01291162")),
                arrayOf("WHEN valid short w/ digit 7 = 7 THEN return Short", "01291179", BarcodeType.Item.Short(upcA = "012911000079", catalogLookupUpc = "0001291100007", rawBarcode = "01291179")),
                arrayOf("WHEN valid short w/ digit 7 = 8 THEN return Short", "01291186", BarcodeType.Item.Short(upcA = "012911000086", catalogLookupUpc = "0001291100008", rawBarcode = "01291186")),
                arrayOf("WHEN valid short w/ digit 7 = 9 THEN return Short", "01291193", BarcodeType.Item.Short(upcA = "012911000093", catalogLookupUpc = "0001291100009", rawBarcode = "01291193")),
                // generated each retrieved from server - infer the original Each
                arrayOf("WHEN catalog each 4 digit PLU THEN return Each", "000000042253", BarcodeType.Item.Each(plu = "4225", catalogLookupUpc = "0000000004225", rawBarcode = "4225", generatedBarcode = "000000042253", itemActivityDbId = null)),
                arrayOf("WHEN catalog each 5 digit PLU THEN return Each", "000000948890", BarcodeType.Item.Each(plu = "94889", catalogLookupUpc = "0000000094889", rawBarcode = "94889", generatedBarcode = "000000948890", itemActivityDbId = null)),
                arrayOf("WHEN catalog each 5 digit PLU alt 2 THEN return Each", "000000940627", BarcodeType.Item.Each(plu = "94062", catalogLookupUpc = "0000000094062", rawBarcode = "94062", generatedBarcode = "000000940627", itemActivityDbId = null)),
                arrayOf("WHEN catalog each 5 digit PLU alt 3 THEN return Each", "000000942256", BarcodeType.Item.Each(plu = "94225", catalogLookupUpc = "0000000094225", rawBarcode = "94225", generatedBarcode = "000000942256", itemActivityDbId = null)),
                // Bags
                arrayOf("WHEN the barcode is a valid bag barcode THEN return Bag", "17979187-QWE13-100101", BarcodeType.Bag(rawBarcode = "17979187-QWE13-100101", customerOrderNumber = "17979187", displayToteId = "QWE13", bagOrToteId = "100101")),
                arrayOf("WHEN the barcode is a valid mfc tote 1 barcode THEN return mfc tote", "99900400703744-30171945", BarcodeType.MfcTote(rawBarcode = "99900400703744-30171945", customerOrderNumber = "30171945", bagOrToteId = "99900400703744", displayToteId = "00703744")),
                arrayOf("WHEN the barcode is a valid mfc tote 2 barcode THEN return mfc tote", "12300500704833-12345678", BarcodeType.MfcTote(rawBarcode = "12300500704833-12345678", customerOrderNumber = "12345678", bagOrToteId = "12300500704833", displayToteId = "00704833")),
                arrayOf("WHEN the barcode is a valid non mfc tote barcode THEN return tote", "17979187-QWE13", BarcodeType.NonMfcTote(rawBarcode = "17979187-QWE13", customerOrderNumber = "17979187", displayToteId = "QWE13", bagOrToteId = "QWE13")),
            )
        }

        @Test
        fun inferBarcodeType_givenInput_returnsExpectedResult() {
            val sut = BarcodeMapperImplementation()
            val result = sut.inferBarcodeType(barcode, enableLogging = true)
            assertThat(result).isEqualTo(output)
        }
    }

    @RunWith(Parameterized::class)
    class GenerateEachBarcodeParameterizedTests(private val testName: String, private val barcode: String, private val output: BarcodeType) : BaseTest() {

        companion object {

            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun data() = listOf(
                arrayOf(
                    "WHEN a 4 digit PLU return appropriate generated barcode",
                    "4225",
                    BarcodeType.Item.Each(plu = "4225", catalogLookupUpc = "0000000004225", rawBarcode = "4225", generatedBarcode = "000000042253", itemActivityDbId = FAKE_ITEM_ACTIVITY_DB_ID)
                ),
                arrayOf(
                    "WHEN a 5 digit PLU with first digit 0 return appropriate generated barcode",
                    "04889",
                    BarcodeType.Item.Each(plu = "04889", catalogLookupUpc = "0000000004889", rawBarcode = "04889", generatedBarcode = "000000048897", itemActivityDbId = FAKE_ITEM_ACTIVITY_DB_ID)
                ),
                arrayOf(
                    "WHEN a 5 digit PLU return appropriate generated barcode",
                    "94889",
                    BarcodeType.Item.Each(plu = "94889", catalogLookupUpc = "0000000094889", rawBarcode = "94889", generatedBarcode = "000000948890", itemActivityDbId = FAKE_ITEM_ACTIVITY_DB_ID)
                ),
                arrayOf(
                    "WHEN a 5 digit PLU alt 2 return appropriate generated barcode",
                    "94062",
                    BarcodeType.Item.Each(plu = "94062", catalogLookupUpc = "0000000094062", rawBarcode = "94062", generatedBarcode = "000000940627", itemActivityDbId = FAKE_ITEM_ACTIVITY_DB_ID)
                ),
                arrayOf(
                    "WHEN a 5 digit PLU alt 3 return appropriate generated barcode",
                    "94225",
                    BarcodeType.Item.Each(plu = "94225", catalogLookupUpc = "0000000094225", rawBarcode = "94225", generatedBarcode = "000000942256", itemActivityDbId = FAKE_ITEM_ACTIVITY_DB_ID)
                ),
            )

            private const val FAKE_ITEM_ACTIVITY_DB_ID = 1660L
        }

        @Test
        fun generateEachBarcode_givenInput_returnsExpectedResult() {
            val sut = BarcodeMapperImplementation()
            val result = sut.generateEachBarcode(barcode, FAKE_ITEM_ACTIVITY_DB_ID)
            assertThat(result).isEqualTo(output)
        }
    }
}
/* ktlint-enable max-line-length */
