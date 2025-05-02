package com.albertsons.acupick.infrastructure.utils

import android.app.ActivityManager
import android.content.Context
import java.io.File
import java.io.FileFilter
import java.io.RandomAccessFile
import java.util.regex.Pattern

object DiagnosticsUtils {
    /**
     * return current cpu usage (0 to 100) guessed from core frequencies
     */
    val cpuUsageFromFreq: Int
        get() = getCpuUsage(coresUsageGuessFromFreq)

    /**
     * @return total cpu usage (from 0 to 100) from cores usage array
     * @param coresUsage must come from getCoresUsageXX().
     */
    fun getCpuUsage(coresUsage: IntArray): Int {
        // compute total cpu usage from each core as the total cpu usage given by /proc/stat seems
        // not considering offline cores: i.e. 2 cores, 1 is offline, total cpu usage given by /proc/stat
        // is equal to remaining online core (should be remaining online core / 2).
        var cpuUsage = 0
        if (coresUsage.size < 2) return 0
        for (i in 1 until coresUsage.size) {
            if (coresUsage[i] > 0) cpuUsage += coresUsage[i]
        }
        return cpuUsage / (coresUsage.size - 1)
    }

    /**
     * guess core usage using core frequency (e.g. all core at min freq => 0% usage;
     *   all core at max freq => 100%)
     *
     * This function returns the current cpu usage (not the average usage since last call).
     *
     * @return array of cores usage
     *   array size = nbcore +1 as the first element is for global cpu usage
     *   array element: 0 => cpu at 0% ; 100 => cpu at 100%
     */
    @get:Synchronized
    val coresUsageGuessFromFreq: IntArray
        get() {
            initCoresFreq()
            val nbCores = mCoresFreq!!.size + 1
            val coresUsage = IntArray(nbCores)
            coresUsage[0] = 0
            for (i in mCoresFreq!!.indices) {
                coresUsage[i + 1] = mCoresFreq!![i].curUsage
                coresUsage[0] += coresUsage[i + 1]
            }
            if (mCoresFreq!!.size > 0) coresUsage[0] /= mCoresFreq!!.size
            return coresUsage
        }

    fun initCoresFreq() {
        if (mCoresFreq == null) {
            val nbCores = nbCores
            mCoresFreq = ArrayList()
            for (i in 0 until nbCores) {
                mCoresFreq!!.add(CoreFreq(i))
            }
        }
    }

    private fun getCurCpuFreq(coreIndex: Int): Int {
        return readIntegerFile("/sys/devices/system/cpu/cpu$coreIndex/cpufreq/scaling_cur_freq")
    }

    private fun getMinCpuFreq(coreIndex: Int): Int {
        return readIntegerFile("/sys/devices/system/cpu/cpu$coreIndex/cpufreq/cpuinfo_min_freq")
    }

    private fun getMaxCpuFreq(coreIndex: Int): Int {
        return readIntegerFile("/sys/devices/system/cpu/cpu$coreIndex/cpufreq/cpuinfo_max_freq")
    }

    // return 0 if any pb occurs
    private fun readIntegerFile(path: String): Int {
        var ret = 0
        try {
            val reader = RandomAccessFile(path, "r")
            ret = try {
                val line = reader.readLine()
                line.toInt()
            } finally {
                reader.close()
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return ret
    }

    /**
     * Gets the number of cores available in this device, across all processors.
     * Requires: Ability to peruse the filesystem at "/sys/devices/system/cpu"
     * @return The number of cores, or 1 if failed to get result
     */
    val nbCores: Int
        get() {
            // private class to display only CPU devices in the directory listing
            class CpuFilter : FileFilter {
                override fun accept(pathname: File): Boolean {
                    // Check if filename is "cpu", followed by one or more digits
                    return Pattern.matches("cpu[0-9]+", pathname.name)
                }
            }
            return try {
                // get directory containing CPU info
                val dir = File("/sys/devices/system/cpu/")
                // filter to only list the devices we care about
                val files = dir.listFiles(CpuFilter())
                // return the number of cores (virtual CPU devices)
                files.size
            } catch (e: Exception) {
                // default to return 1 core
                1
            }
        }

    // current cores frequencies
    private var mCoresFreq: ArrayList<CoreFreq>? = null // compute usage

    private class CoreFreq constructor(var num: Int) {
        var cur = 0
        var min = 0
        var max = 0

        fun updateCurFreq() {
            cur = getCurCpuFreq(num)
            // min & max cpu could not have been properly initialized if core was offline
            if (min == 0) min = getMinCpuFreq(num)
            if (max == 0) max = getMaxCpuFreq(num)
        }
        // if (cur == min)
        //     cpuUsage = 2; // consider lowest freq as 2% usage (usually core is offline if 0%)
        // else
        //     return usage from 0 to 100
        val curUsage: Int
            get() {
                updateCurFreq()
                var cpuUsage = 0
                if (max - min > 0 && max > 0 && cur > 0) {
                    // if (cur == min)
                    //     cpuUsage = 2; // consider lowest freq as 2% usage (usually core is offline if 0%)
                    // else
                    cpuUsage = (cur - min) * 100 / (max - min)
                }
                return cpuUsage
            }

        init {
            min = getMinCpuFreq(num)
            max = getMaxCpuFreq(num)
        }
    }

    /**
     * Get memory usage
     */
    fun getMemoryUsage(context: Context): Pair<Double, Double> {
        return try {
            // Declaring and Initializing the ActivityManager, and fetch memory info
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfo)

            // Fetching the available and total memory and converting into Gigabytes
            val availableMemory = memInfo.availMem.toDouble() / (1024 * 1024 * 1024)
            val totalMemory = memInfo.totalMem.toDouble() / (1024 * 1024 * 1024)

            Pair(availableMemory, totalMemory)
        } catch (e: Exception) {
            Pair(-1.0, -1.0)
        }
    }
}
