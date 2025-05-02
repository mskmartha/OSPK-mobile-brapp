package com.albertsons.acupick.test

import java.lang.reflect.Field
import java.lang.reflect.Modifier

@Throws(Exception::class)
/** Sets the value of a static final field. Inspired by https://proandroiddev.com/build-version-in-unit-testing-4e963940dae7 */
fun Field.setFinalStatic(newValue: Any) {
    isAccessible = true

    val modifiersField = Field::class.java.getDeclaredField("modifiers")
    modifiersField.isAccessible = true
    modifiersField.setInt(this, modifiers and Modifier.FINAL.inv())

    set(null, newValue)
}

fun <T : Any> T.getPrivateProperty(variableName: String): Any? {
    val fields = this.javaClass.declaredFields
    val field = fields.first { it.name == variableName }
    field.isAccessible = true
    return field.get(this)
}

fun <T : Any> T.setPrivateProperty(variableName: String, newVal: T) {
    val field = this.javaClass.declaredFields.first { it.name == variableName }
    field.isAccessible = true
    field.set(this, newVal)
}

fun <T : Any> T.runPrivateMethod(methodName: String): Any? {
    val method = this.javaClass.getDeclaredMethod(methodName)
    method.isAccessible = true
    return method.invoke(this)
}

fun <T : Any> T.runPrivateMethodWithParams(methodName: String, vararg params: Any): Any? {
    val method = this.javaClass.declaredMethods.first { it.name == methodName }
    method.isAccessible = true
    return method.invoke(this, *params)
}

suspend fun <T : Any> T.runSuspendPrivateMethodWithParams(methodName: String, vararg params: Any): Any? {
    val method = this.javaClass.declaredMethods.first { it.name == methodName }
    method.isAccessible = true
    return kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn { cont ->
        method.invoke(this, *params, cont)
    }
}
