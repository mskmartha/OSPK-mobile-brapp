package com.albertsons.acupick.wifi.model

enum class Strength {
    ZERO,
    ONE,
    TWO,
    THREE,
    FOUR;

    fun weak(): Boolean = ZERO == this

    companion object {
        fun calculate(level: Int): Strength {
            val enumValues: Array<Strength> = enumValues()
            return enumValues[calculateSignalLevel(level, enumValues.size)]
        }

        fun reverse(strength: Strength): Strength {
            val enumValues: Array<Strength> = enumValues()
            return enumValues[enumValues.size - strength.ordinal - 1]
        }
    }
}
