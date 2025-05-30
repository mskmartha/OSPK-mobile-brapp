package com.albertsons.acupick.wifi.vendor

import android.content.Context
import android.content.res.Resources
import com.albertsons.acupick.wifi.utils.readFile
import com.albertsons.acupick.wifi.R
import java.util.Locale
import java.util.TreeMap

class VendorService(private val context: Context) {
    private val vendorData: VendorData by lazy { load(context.resources) }

    fun findVendorName(address: String = ""): String =
        vendorData.macs[address.clean()].orEmpty()

    fun findMacAddresses(vendorName: String = ""): List<String> =
        vendorData.vendors[vendorName.uppercase(Locale.getDefault())].orEmpty()

    fun findVendors(vendorName: String = ""): List<String> {
        val name = vendorName.uppercase(Locale.getDefault())
        return vendorData.vendors.filterKeys { filter(it, name) }.keys.toList()
    }

    internal fun findMacs(): List<String> = vendorData.macs.keys.toList()

    private fun filter(source: String, filter: String): Boolean =
        source.contains(filter) || macContains(source, filter)

    private fun macContains(source: String, filter: String): Boolean =
        findMacAddresses(source).any { it.contains(filter) }

    private fun load(resources: Resources): VendorData {
        val macs: MutableMap<String, String> = TreeMap()
        val vendors: MutableMap<String, List<String>> = TreeMap()
        readFile(resources, R.raw.vendors)
            .split("\n")
            .map { it.split("|").toTypedArray() }
            .filter { it.size == 2 }
            .forEach { it ->
                val name = it[0]
                val results: List<String> = it[1].chunked(MAX_SIZE)
                results.forEach { macs[it] = name }
                vendors[name] = results.map { it.toMacAddress() }
            }
        return VendorData(vendors, macs)
    }

    private class VendorData(val vendors: Map<String, List<String>>, val macs: Map<String, String>)
}
