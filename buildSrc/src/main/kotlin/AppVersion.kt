/**
 * Represents the app version. Provides [versionCode] to generate the versionCode used as well as [versionName] to generate the versionName.
 *
 * Note the restrictions on [major]/[minor]/[patch]/[hotfix]
 *
 * See https://confluence.bottlerocketapps.com/display/BKB/Android+Coding+Standards#AndroidCodingStandards-VersionCodes
 */
data class AppVersion(
    /** Must be >= 0 and < 100 */
    val major: Int,
    /** Must be >= 0 and < 100 */
    val minor: Int,
    /** Must be >= 0 and < 100 */
    val patch: Int,
    /** Must be >= 0 and < 10 */
    val hotfix: Int,
    /** If true, [versionName] can look like `1.0.0`, If false, [versionName] will look like `1.0`. Usually project/client dependant */
    val showEmptyPatchNumberInVersionName: Boolean,
    /**
     * If true, [versionName] can look like `1.2.2.1`, If false, [versionName] will look like `1.2.2`. Note that an empty (0) hotfix value will never be shown even when true (won't see `1.2.2.0`).
     * Usually project/client dependant
     */
    val showHotFixIfNotEmpty: Boolean = false,
    /** If > 1, [versionName] can look like `01.02.02` (example uses 2), If == 1, [versionName] will look like `1.2.2`. Usually project/client dependant */
    val padZerosToNDigits: Int = 1
) {
    init {
        check(major in 0 until 100) { "major version number must be >= 0 and < 100" }
        check(minor in 0 until 100) { "minor version number must be >= 0 and < 100" }
        check(patch in 0 until 100) { "patch version number must be >= 0 and < 100" }
        check(hotfix in 0 until 10) { "hotfix version number must be >= 0 and < 10" }
    }

    fun logString(): String {
        return "$this, versionCode=$versionCode, versionName=$versionName"
    }

    /**
     * Returns value to use for android.versionCode
     *
     * Ex:
     * *  `100000`
     * * `1001030`
     * *  `204000`
     * *  `500001`
     */
    val versionCode: Int
        get() = (major * 100000) + (minor * 1000) + (patch * 10) + hotfix

    /**
     * Returns value to use for android.versionName.
     *
     * Shows non-zero [hotfix] only when [showHotFixIfNotEmpty] is true.
     * Shows [patch] of 0 1) when [showEmptyPatchNumberInVersionName] is true OR 2) the hotfix number is shown.
     *
     * EX:
     * * `1.0.0` (or `1.0`)
     * * `10.1.3`
     * * `2.4.0` (or `2.4`)
     * * `5.0.0` (or `5.0`)
     * * `1.2.2.1` (or `1.2.2`)
     */
    // TODO: Find a way to unit test this logic
    val versionName: String
        get() {
            val showHotfixNumber = showHotFixIfNotEmpty && hotfix > 0
            val showPatchNumber = patch >= 1 || (showEmptyPatchNumberInVersionName && patch == 0) || showHotfixNumber
            // Note: Order matters in this when block
            return when {
                showHotfixNumber -> "${major.pad()}.${minor.pad()}.${patch.pad()}.${hotfix.pad()}"
                showPatchNumber -> "${major.pad()}.${minor.pad()}.${patch.pad()}"
                else -> "${major.pad()}.${minor.pad()}"
            }
        }

    private fun Int.pad() = "$this".padStart(padZerosToNDigits, '0')
}
