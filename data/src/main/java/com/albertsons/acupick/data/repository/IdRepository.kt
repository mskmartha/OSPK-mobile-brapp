package com.albertsons.acupick.data.repository

import android.content.SharedPreferences
import android.os.Parcelable
import androidx.annotation.Keep
import com.albertsons.acupick.data.R
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.parcelize.Parcelize

interface IdRepository : Repository {
    fun saveCompleteHandoff(orderNumber: String, identificationInfoPto: IdentificationInfoPto)
    fun loadCompleteHandoff(orderNumber: String): IdentificationInfoPto?
    fun removeCompleteHandoff(orderNumber: String): Boolean
    fun clear(): Boolean
}

@JsonClass(generateAdapter = true)
@Parcelize
data class IdentificationInfoPto(
    val identificationType: IdentificationTypePto,
    val name: String?,
    val dateOfBirth: String?,
    val identificationNumber: String?,
    val pickupPersonSignature: String?,
) : Parcelable

@Keep
enum class IdentificationTypePto(
    val label: Int,
    val serverValue: Int = label
) {
    DriversLicense(R.string.id_type_drivers_license, R.string.id_type_drivers_license_server),
    StateId(R.string.id_type_state_id),
    Passport(R.string.id_type_passport),
    MilitaryId(R.string.id_type_military_id),
    Other(R.string.id_type_other),
}

internal class IdRepositoryImplementation(
    moshi: Moshi,
    private val sharedPrefs: SharedPreferences,
) : IdRepository {

    private val identificationInfoPtoJsonAdapter = IdentificationInfoPtoJsonAdapter(moshi)

    override fun saveCompleteHandoff(orderNumber: String, identificationInfoPto: IdentificationInfoPto) {
        with(sharedPrefs.edit()) {
            val jsonObj = identificationInfoPtoJsonAdapter.toJson(identificationInfoPto)
            putString(orderNumber, jsonObj)
            commit()
        }
    }

    override fun loadCompleteHandoff(orderNumber: String): IdentificationInfoPto? {
        if (sharedPrefs.contains(orderNumber)) {
            val jsonObj = sharedPrefs.getString(orderNumber, String())!!
            return identificationInfoPtoJsonAdapter.fromJson(jsonObj)
        }
        return null
    }

    override fun removeCompleteHandoff(orderNumber: String) = sharedPrefs.edit().remove(orderNumber).commit()

    override fun clear() = sharedPrefs.edit().clear().commit()
}
