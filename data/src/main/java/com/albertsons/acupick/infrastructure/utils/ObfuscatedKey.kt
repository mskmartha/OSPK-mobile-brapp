package com.albertsons.acupick.infrastructure.utils

import android.annotation.SuppressLint

@SuppressLint
class ObfuscatedKey(val key: String = "") {
    val A: ObfuscatedKey
        get() = ObfuscatedKey(key + "A")
    val B: ObfuscatedKey
        get() = ObfuscatedKey(key + "B")
    val C: ObfuscatedKey
        get() = ObfuscatedKey(key + "C")
    val D: ObfuscatedKey
        get() = ObfuscatedKey(key + "D")
    val E: ObfuscatedKey
        get() = ObfuscatedKey(key + "E")
    val F: ObfuscatedKey
        get() = ObfuscatedKey(key + "F")
    val G: ObfuscatedKey
        get() = ObfuscatedKey(key + "G")
    val H: ObfuscatedKey
        get() = ObfuscatedKey(key + "H")
    val I: ObfuscatedKey
        get() = ObfuscatedKey(key + "I")
    val J: ObfuscatedKey
        get() = ObfuscatedKey(key + "J")
    val K: ObfuscatedKey
        get() = ObfuscatedKey(key + "K")
    val L: ObfuscatedKey
        get() = ObfuscatedKey(key + "L")
    val M: ObfuscatedKey
        get() = ObfuscatedKey(key + "M")
    val N: ObfuscatedKey
        get() = ObfuscatedKey(key + "N")
    val O: ObfuscatedKey
        get() = ObfuscatedKey(key + "O")
    val P: ObfuscatedKey
        get() = ObfuscatedKey(key + "P")
    val Q: ObfuscatedKey
        get() = ObfuscatedKey(key + "Q")
    val R: ObfuscatedKey
        get() = ObfuscatedKey(key + "R")
    val S: ObfuscatedKey
        get() = ObfuscatedKey(key + "S")
    val T: ObfuscatedKey
        get() = ObfuscatedKey(key + "T")
    val U: ObfuscatedKey
        get() = ObfuscatedKey(key + "U")
    val V: ObfuscatedKey
        get() = ObfuscatedKey(key + "V")
    val W: ObfuscatedKey
        get() = ObfuscatedKey(key + "W")
    val X: ObfuscatedKey
        get() = ObfuscatedKey(key + "X")
    val Y: ObfuscatedKey
        get() = ObfuscatedKey(key + "Y")
    val Z: ObfuscatedKey
        get() = ObfuscatedKey(key + "Z")
    val _a: ObfuscatedKey
        get() = ObfuscatedKey(key + "a")
    val _b: ObfuscatedKey
        get() = ObfuscatedKey(key + "b")
    val _c: ObfuscatedKey
        get() = ObfuscatedKey(key + "c")
    val _d: ObfuscatedKey
        get() = ObfuscatedKey(key + "d")
    val _e: ObfuscatedKey
        get() = ObfuscatedKey(key + "e")
    val _f: ObfuscatedKey
        get() = ObfuscatedKey(key + "f")
    val _g: ObfuscatedKey
        get() = ObfuscatedKey(key + "g")
    val _h: ObfuscatedKey
        get() = ObfuscatedKey(key + "h")
    val _i: ObfuscatedKey
        get() = ObfuscatedKey(key + "i")
    val _j: ObfuscatedKey
        get() = ObfuscatedKey(key + "j")
    val _k: ObfuscatedKey
        get() = ObfuscatedKey(key + "k")
    val _l: ObfuscatedKey
        get() = ObfuscatedKey(key + "l")
    val _m: ObfuscatedKey
        get() = ObfuscatedKey(key + "m")
    val _n: ObfuscatedKey
        get() = ObfuscatedKey(key + "n")
    val _o: ObfuscatedKey
        get() = ObfuscatedKey(key + "o")
    val _p: ObfuscatedKey
        get() = ObfuscatedKey(key + "p")
    val _q: ObfuscatedKey
        get() = ObfuscatedKey(key + "q")
    val _r: ObfuscatedKey
        get() = ObfuscatedKey(key + "r")
    val _s: ObfuscatedKey
        get() = ObfuscatedKey(key + "s")
    val _t: ObfuscatedKey
        get() = ObfuscatedKey(key + "t")
    val _u: ObfuscatedKey
        get() = ObfuscatedKey(key + "u")
    val _v: ObfuscatedKey
        get() = ObfuscatedKey(key + "v")
    val _w: ObfuscatedKey
        get() = ObfuscatedKey(key + "w")
    val _x: ObfuscatedKey
        get() = ObfuscatedKey(key + "x")
    val _y: ObfuscatedKey
        get() = ObfuscatedKey(key + "y")
    val _z: ObfuscatedKey
        get() = ObfuscatedKey(key + "z")
    val n0: ObfuscatedKey
        get() = ObfuscatedKey(key + "0")
    val n1: ObfuscatedKey
        get() = ObfuscatedKey(key + "1")
    val n2: ObfuscatedKey
        get() = ObfuscatedKey(key + "2")
    val n3: ObfuscatedKey
        get() = ObfuscatedKey(key + "3")
    val n4: ObfuscatedKey
        get() = ObfuscatedKey(key + "4")
    val n5: ObfuscatedKey
        get() = ObfuscatedKey(key + "5")
    val n6: ObfuscatedKey
        get() = ObfuscatedKey(key + "6")
    val n7: ObfuscatedKey
        get() = ObfuscatedKey(key + "7")
    val n8: ObfuscatedKey
        get() = ObfuscatedKey(key + "8")
    val n9: ObfuscatedKey
        get() = ObfuscatedKey(key + "9")

    fun literal(c: Char): ObfuscatedKey {
        return ObfuscatedKey(key + c)
    }
}
