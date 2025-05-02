import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.dsl.DependencyHandler

// Provides dependencies that can be used throughout the project build.gradle files

// https://github.com/JetBrains/kotlin/blob/master/ChangeLog.md
// https://github.com/JetBrains/kotlin/releases/latest
// https://plugins.jetbrains.com/plugin/6954-kotlin
// https://kotlinlang.org/docs/reference/whatsnew16.html
// https://blog.jetbrains.com/kotlin/2021/11/kotlin-1-6-0-is-released/
// TODO: Update corresponding buildSrc/build.gradle.kts value when updating this version!
private const val KOTLIN_VERSION = "1.8.0"
private const val KOTLIN_COROUTINES_VERSION = "1.7.3"
private const val NAVIGATION_VERSION = "2.4.1"

/**
 * Provides the source of truth for version/configuration information to any gradle build file (project and app module build.gradle.kts)
 */
object Config {
    // https://github.com/JLLeitschuh/ktlint-gradle/blob/master/CHANGELOG.md
    // https://github.com/JLLeitschuh/ktlint-gradle/releases
    const val KTLINT_GRADLE_VERSION = "10.2.0"

    // https://github.com/pinterest/ktlint/blob/master/CHANGELOG.md
    // https://github.com/pinterest/ktlint/releases
    const val KTLINT_VERSION = "0.43.0"

    // 1. Execute `./gradlew jacocoTestReport` OR `./gradlew jacocoTestInternalDebugUnitTestReport`
    // 2. Execute `open app/build/jacoco/jacocoHtml/index.html` or the `Open Jacoco Report` AS Run Configuration
    // http://www.jacoco.org/jacoco/trunk/doc/
    // https://github.com/jacoco/jacoco/releases
    const val JACOCO_VERSION = "0.8.10"
    object ComplierExtension {
        const val KOTLIN = "1.4.0"
    }
    /**
     * Called from root project buildscript block in the project root build.gradle.kts
     */
    object BuildScriptPlugins {
        // https://developer.android.com/studio/releases/gradle-plugin
        // TODO: Update corresponding buildSrc/build.gradle.kts value when updating this version!
        const val ANDROID_GRADLE = "com.android.tools.build:gradle:7.4.0"
        const val KOTLIN_GRADLE = "org.jetbrains.kotlin:kotlin-gradle-plugin:$KOTLIN_VERSION"
        const val R8_TOOLS = "com.android.tools:r8:3.3.75"

        // Gradle version plugin; use dependencyUpdates task to view third party dependency updates via `./gradlew dependencyUpdates` or AS Gradle -> [project]] -> Tasks -> help -> dependencyUpdates
        // https://github.com/ben-manes/gradle-versions-plugin/releases
        const val GRADLE_VERSIONS = "com.github.ben-manes:gradle-versions-plugin:0.48.0"

        // https://github.com/arturdm/jacoco-android-gradle-plugin/releases
        // const val JACOCO_ANDROID = "com.dicedmelon.gradle:jacoco-android:0.1.4"
        // As the dicedmelon plugin doesn't support gradle 6 yet, using the hiya ported plugin. See https://github.com/arturdm/jacoco-android-gradle-plugin/pull/75#issuecomment-565222643
            const val JACOCO_ANDROID = "com.hiya:jacoco-android:0.2"
        const val NAVIGATION_SAFE_ARGS_GRADLE = "androidx.navigation:navigation-safe-args-gradle-plugin:$NAVIGATION_VERSION"

        // https://developers.google.com/android/guides/google-services-plugin
        // https://github.com/google/play-services-plugins/releases
        const val GOOGLE_SERVICES = "com.google.gms:google-services:4.3.15"

        // https://firebase.google.com/support/release-notes/android#crashlytics_gradle_plugin_v2-3-0
        const val CRASHLYTICS_GRADLE = "com.google.firebase:firebase-crashlytics-gradle:2.9.0"

        // End User monitoring (EUM) Overview: https://docs.appdynamics.com/display/PRO45/End+User+Monitoring
        // Mobile Real User Monitoring (RUM) Overview (EUM Child section): https://docs.appdynamics.com/display/PRO45/Mobile+Real+User+Monitoring (and children pages)
        // Setup: https://docs.appdynamics.com/display/PRO45/Instrument+an+Android+Application+Manually
        // Customizations: https://docs.appdynamics.com/display/PRO45/Customize+the+Android+Instrumentation
        // Latest javadocs: https://sdkdocs.appdynamics.com/javadocs/android-sdk/latest/
        // Version specific javadocs: https://sdkdocs.appdynamics.com/javadocs/android-sdk/YEAR-MONTH-VERSION/ ex: https://sdkdocs.appdynamics.com/javadocs/android-sdk/21.11/
        // Releases: https://docs.appdynamics.com/display/PRO45/Past+Agent+Releases#PastAgentReleases-AndroidAgent
        const val APP_DYNAMICS_GRADLE = "com.appdynamics:appdynamics-gradle-plugin:21.11.1"

        const val FIREBASE_PERFORMANCE = "com.google.firebase:perf-plugin:1.4.2"
    }

    /**
     * Called in non-root project modules via id()[org.gradle.kotlin.dsl.PluginDependenciesSpecScope.id]
     * or kotlin()[org.gradle.kotlin.dsl.kotlin] (the PluginDependenciesSpec.kotlin extension function) in a build.gradle.kts
     */
    object ApplyPlugins {
        const val ANDROID_APPLICATION = "com.android.application"
        const val ANDROID_LIBRARY = "com.android.library"
        const val GRADLE_VERSIONS = "com.github.ben-manes.versions"
        const val KT_LINT = "org.jlleitschuh.gradle.ktlint"

        // const val JACOCO_ANDROID = "jacoco-android"
        // As the dicedmelon plugin doesn't support gradle 6 yet, using the hiya ported plugin. See https://github.com/arturdm/jacoco-android-gradle-plugin/pull/75#issuecomment-565222643
        const val JACOCO_ANDROID = "com.hiya.jacoco-android"
        const val NAVIGATION_SAFE_ARGS_KOTLIN = "androidx.navigation.safeargs.kotlin"
        const val GOOGLE_SERVICES = "com.google.gms.google-services"
        const val CRASHLYTICS = "com.google.firebase.crashlytics"

        /** AppDynamicsEndUserMonitoring */
        const val APP_DYNAMICS = "adeum"
        const val FIREBASE_PERF = "com.google.firebase.firebase-perf"
        const val JAVA_LIB = "java-library"
        const val KOTLIN_LIB = "kotlin"

        const val PARCELIZE = "kotlin-parcelize"

        object Kotlin {
            const val ANDROID = "android"
            const val KAPT = "kapt"
        }
    }

    // What each version represents - https://medium.com/androiddevelopers/picking-your-compilesdkversion-minsdkversion-targetsdkversion-a098a0341ebd
    object AndroidSdkVersions {
        const val COMPILE_SDK = 33

        // https://developer.android.com/studio/releases/build-tools
        const val BUILD_TOOLS = "31.0.0"
        const val MIN_SDK = 27

        // https://developer.android.com/about/versions/12/behavior-changes-12
        const val TARGET_SDK = 31
    }

    // Gradle versions plugin configuration: https://github.com/ben-manes/gradle-versions-plugin#revisions
    fun isNonStable(version: String): Boolean {
        val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
        val regex = "^[0-9,.v-]+(-r)?$".toRegex()
        val isStable = stableKeyword || regex.matches(version)
        return isStable.not()
    }
}

/**
 * Dependency Version definitions with links to source (if open source)/release notes. Defines the version in one place for multiple dependencies that use the same version.
 * Use [DependencyHandler] extension functions below that provide logical groupings of dependencies including appropriate configuration accessors.
 */
private object Libraries {
    //// AndroidX
    // All androidx dependencies version table: https://developer.android.com/jetpack/androidx/versions#version-table
    // https://developer.android.com/jetpack/androidx/releases/core
    // https://developer.android.com/kotlin/ktx#core-packages
    // https://developer.android.com/jetpack/androidx/releases/
    // https://developer.android.com/kotlin/ktx

    // https://developer.android.com/jetpack/androidx/releases/core
    const val CORE_KTX = "androidx.core:core-ktx:1.8.0"

    // Lifecycle
    // https://developer.android.com/jetpack/androidx/releases/lifecycle
    private const val LIFECYCLE_VERSION = "2.4.1"
    const val LIFECYCLE_LIVEDATA_KTX = "androidx.lifecycle:lifecycle-livedata-ktx:$LIFECYCLE_VERSION"
    const val LIFECYCLE_VIEWMODEL_KTX = "androidx.lifecycle:lifecycle-viewmodel-ktx:$LIFECYCLE_VERSION"
    const val LIFECYCLE_LIVEDATA_CORE_KTX = "androidx.lifecycle:lifecycle-livedata-core-ktx:$LIFECYCLE_VERSION"
    const val LIFECYCLE_COMMON_JAVA8 = "androidx.lifecycle:lifecycle-common-java8:$LIFECYCLE_VERSION"

    // https://developer.android.com/jetpack/androidx/releases/appcompat
    const val APP_COMPAT = "androidx.appcompat:appcompat:1.4.0"

    // https://developer.android.com/jetpack/androidx/releases/constraintlayout
    const val CONSTRAINT_LAYOUT = "androidx.constraintlayout:constraintlayout:2.1.2"

    // Datastore
    // https://developer.android.com/jetpack/androidx/releases/datastore
    const val DATASTORE = "androidx.datastore:datastore-preferences:1.0.0"

    // Navigation
    // https://developer.android.com/jetpack/androidx/releases/navigation
    const val NAVIGATION_FRAGMENT_KTX = "androidx.navigation:navigation-fragment-ktx:$NAVIGATION_VERSION"
    const val NAVIGATION_UI_KTX = "androidx.navigation:navigation-ui-ktx:$NAVIGATION_VERSION"

    // Preference
    // https://developer.android.com/jetpack/androidx/releases/preference
    const val PREFERENCE_KTX = "androidx.preference:preference-ktx:1.1.1"

    // https://security.googleblog.com/2020/02/data-encryption-on-android-with-jetpack.html
    // https://developer.android.com/topic/security/data
    // https://developer.android.com/jetpack/androidx/releases/security
    const val SECURITY_CRYPTO = "androidx.security:security-crypto:1.0.0"

    //// Material
    // https://github.com/material-components/material-components-android/releases
    const val MATERIAL = "com.google.android.material:material:1.4.0"

    //// Kotlin
    const val KOTLIN_STDLIB_JDK7 = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:$KOTLIN_VERSION"
    const val KOTLIN_REFLECT = "org.jetbrains.kotlin:kotlin-reflect:$KOTLIN_VERSION"

    //// Coroutines + Flow
    // Flow docs: https://kotlinlang.org/docs/reference/coroutines/flow.html
    // https://github.com/Kotlin/kotlinx.coroutines/blob/master/CHANGES.md
    // https://github.com/Kotlin/kotlinx.coroutines/releases
    const val KOTLINX_COROUTINES_CORE = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$KOTLIN_COROUTINES_VERSION"
    const val KOTLINX_COROUTINES_ANDROID = "org.jetbrains.kotlinx:kotlinx-coroutines-android:$KOTLIN_COROUTINES_VERSION"

    //// Koin
    // https://github.com/InsertKoinIO/koin/blob/master/CHANGELOG.md
    // https://github.com/InsertKoinIO/koin/tags
    private const val KOIN_VERSION = "3.4.1"
    const val KOIN_ANDROID = "io.insert-koin:koin-android:$KOIN_VERSION"
    const val KOIN_ANDROID_TEST = "io.insert-koin:koin-test-junit4:$KOIN_VERSION"

    //// Core Library Desugaring
    // https://developer.android.com/studio/write/java8-support#library-desugaring
    // https://developer.android.com/studio/write/java8-support-table
    // https://github.com/google/desugar_jdk_libs
    // https://github.com/google/desugar_jdk_libs/blob/master/VERSION.txt
    private const val DESUGAR_VERSION = "1.1.5"
    const val CORE_LIBRARY_DESUGARING = "com.android.tools:desugar_jdk_libs:$DESUGAR_VERSION"

    private const val TWILIO_VERSION = "6.0.4"
    const val TWILIO = "com.twilio:conversations-android:$TWILIO_VERSION"

    //// Firebase
    // https://firebase.google.com/support/release-notes/android
    /** B.O.M. used to control versions of all firebase libraries to make sure they work together. */
    private const val FIREBASE_BOM_VERSION = "29.0.0"
    const val FIREBASE_BOM = "com.google.firebase:firebase-bom:$FIREBASE_BOM_VERSION"
    const val FIREBASE_ANALYTICS = "com.google.firebase:firebase-analytics-ktx"
    const val FIREBASE_CRASHLYTICS = "com.google.firebase:firebase-crashlytics-ktx"
    const val FIREBASE_PERFORMANCE = "com.google.firebase:firebase-perf-ktx"
    const val FIREBASE_MESSAGING = "com.google.firebase:firebase-messaging-ktx:"
    const val FIREBASE_MESSAGING_DIRECTBOOT = "com.google.firebase:firebase-messaging-directboot:"


    //// Retrofit
    // javadoc: https://square.github.io/retrofit/2.x/retrofit/
    // https://github.com/square/retrofit/blob/master/CHANGELOG.md
    // https://github.com/square/retrofit/releases
    private const val RETROFIT_VERSION = "2.9.0"
    const val RETROFIT = "com.squareup.retrofit2:retrofit:$RETROFIT_VERSION"
    const val RETROFIT_SCALARS_CONVERTER = "com.squareup.retrofit2:converter-scalars:$RETROFIT_VERSION"
    const val RETROFIT_MOSHI_CONVERTER = "com.squareup.retrofit2:converter-moshi:$RETROFIT_VERSION"

    //// OkHttp
    // 3 to 4 upgrade guide: https://square.github.io/okhttp/upgrading_to_okhttp_4/
    // https://github.com/square/okhttp/blob/master/CHANGELOG.md
    // https://github.com/square/okhttp/releases
    const val OKHTTP = "com.squareup.okhttp3:okhttp:4.11.0"

    //// Moshi
    // https://github.com/square/moshi/blob/master/CHANGELOG.md
    // https://github.com/square/moshi/releases
    private const val MOSHI_VERSION = "1.15.0"
    // Note: DO NOT USE moshi-kotlin as it uses reflection via `KotlinJsonAdapterFactory`. Instead, rely on moshi and the kapt `moshi-kotlin-codegen` dependency AND annotate relevant classes with @JsonClass(generateAdapter = true)
    const val MOSHI = "com.squareup.moshi:moshi:$MOSHI_VERSION"
    // https://github.com/square/moshi/blob/master/adapters/README.md
    const val MOSHI_ADAPTERS = "com.squareup.moshi:moshi-adapters:$MOSHI_VERSION"
    const val MOSHI_KOTLIN_CODEGEN = "com.squareup.moshi:moshi-kotlin-codegen:$MOSHI_VERSION"

    //// UI
    // https://github.com/square/picasso/blob/master/CHANGELOG.md
    // https://github.com/square/picasso/releases
    const val PICASSO = "com.squareup.picasso:picasso:2.71828"

    // NOTE: groupie-databinding is deprecated. groupie-viewbinding is the replacement and should be used with both viewbinding and databinding. See https://github.com/lisawray/groupie#note
    // https://github.com/lisawray/groupie/releases
    private const val GROUPIE_VERSION = "2.9.0"
    const val GROUPIE = "com.github.lisawray.groupie:groupie:$GROUPIE_VERSION"
    const val GROUPIE_VIEWBINDING = "com.github.lisawray.groupie:groupie-viewbinding:$GROUPIE_VERSION"

    // Recycler View Helper tools
    private const val RECYCLERVIEW_SELECTION_VERSION = "1.1.0"
    const val RECYCLERVIEW_SELECTION = "androidx.recyclerview:recyclerview-selection:$RECYCLERVIEW_SELECTION_VERSION"

    // Recycler view Stick Header
    private const val STICKY_HEADER_VERSION = "1.0.1"
    const val STICKY_HEADER = "com.github.qiujayen:sticky-layoutmanager:$STICKY_HEADER_VERSION"

    //// Utility
    // Blog: https://proandroiddev.com/livedata-with-single-events-2395dea972a8
    // https://github.com/hadilq/LiveEvent/releases
    const val LIVE_EVENT = "com.github.hadilq.liveevent:liveevent:1.2.0"

    // https://github.com/JakeWharton/timber/blob/master/CHANGELOG.md
    // https://github.com/JakeWharton/timber/releases
    const val TIMBER = "com.jakewharton.timber:timber:5.0.1"

    // https://github.com/JakeWharton/ProcessPhoenix/blob/master/CHANGELOG.md
    // https://github.com/JakeWharton/ProcessPhoenix/releases
    const val PROCESS_PHOENIX = "com.jakewharton:process-phoenix:2.1.2"

    // Commons codec - used for base64 operations (no android framework requirement)
    // https://github.com/apache/commons-codec/blob/master/RELEASE-NOTES.txt
    // https://github.com/apache/commons-codec/releases
    const val COMMONS_CODEC = "commons-codec:commons-codec:1.15"

    // https://square.github.io/leakcanary/changelog/
    // https://github.com/square/leakcanary/releases
    // Just use on debugImplementation builds
    // TODO: Keep an eye on plumber-android possible future usage in release builds: https://square.github.io/leakcanary/changelog/#plumber-android-is-a-new-artifact-that-fixes-known-android-leaks
    const val LEAK_CANARY = "com.squareup.leakcanary:leakcanary-android:2.7"

    // Chucker
    // https://medium.com/@cortinico/introducing-chucker-18f13a51b35d
    // https://github.com/ChuckerTeam/chucker/blob/develop/CHANGELOG.md
    // https://github.com/ChuckerTeam/chucker/releases
    private const val CHUCKER_VERSION = "3.5.2"
    const val CHUCKER = "com.github.ChuckerTeam.Chucker:library:$CHUCKER_VERSION"
    const val CHUCKER_NO_OP = "com.github.ChuckerTeam.Chucker:library-no-op:$CHUCKER_VERSION"

    // https://developer.android.com/jetpack/androidx/releases/swiperefreshlayout
    private const val REFRESH_VERSION = "1.2.0-alpha01"
    const val SWIPE_REFRESH = "androidx.swiperefreshlayout:swiperefreshlayout:$REFRESH_VERSION"

    // Lottie
    // http://airbnb.io/lottie/#/android
    // https://github.com/airbnb/lottie-android/releases
    private const val LOTTIE_VERSION = "4.2.2"
    private const val LOTTIE_COMPOSE_VERSION = "5.2.0"
    const val LOTTIE = "com.airbnb.android:lottie:$LOTTIE_VERSION"
    const val LOTTIE_COMPOSE = "com.airbnb.android:lottie-compose:$LOTTIE_COMPOSE_VERSION"

    // Shimmer Layout
    // http://facebook.github.io/shimmer-android/
    private const val SHIMMER_VERSION = "0.5.0"
    const val SHIMMER = "com.facebook.shimmer:shimmer:$SHIMMER_VERSION"

    // Jetpack Compose
    const val COMPOSE_UI_VERSION = "1.2.0"
    const val COMPOSE_ACTIVITY_VERSION = "1.3.1"
    const val COMPOSE_VIEWMODEL_VERSION = "2.6.0"
    const val RUNTIME_KTX_VERSION = "2.3.1"
    const val COMPOSE_COIL_VERSION = "2.4.0"
    const val COMPOSE_LIFECYCLE_VERSION = "2.6.0"
    const val COMPOSE_RUNTIME_LIVEDATA = "androidx.compose.runtime:runtime-livedata"
    const val COMPOSE_LIFECYCLE = "androidx.lifecycle:lifecycle-runtime-compose:$COMPOSE_LIFECYCLE_VERSION"
    const val LIFECYCLE_RUNTIME_KTX = "androidx.lifecycle:lifecycle-runtime-ktx:$RUNTIME_KTX_VERSION"
    const val COMPOSE_ACTIVITY = "androidx.activity:activity-compose:$COMPOSE_ACTIVITY_VERSION"
    const val COMPOSE_UI = "androidx.compose.ui:ui:$COMPOSE_UI_VERSION"
    const val COMPOSE_UI_TOOLING = "androidx.compose.ui:ui-tooling-preview:$COMPOSE_UI_VERSION"
    const val COMPOSE_MATERIAL = "androidx.compose.material:material:$COMPOSE_UI_VERSION"
    const val COMPOSE_COIL = "io.coil-kt:coil-compose:$COMPOSE_COIL_VERSION"
    const val COMPOSE_CONSTRAINT_LAYOUT = "androidx.constraintlayout:constraintlayout-compose:1.0.1"
    const val COMPOSE_VIEWMODEL = "androidx.lifecycle:lifecycle-viewmodel-compose:$COMPOSE_VIEWMODEL_VERSION"

    // CameraX
    private const val CAMERAX_VERSION = "1.1.0"
    const val CAMERAX_CORE = "androidx.camera:camera-core:$CAMERAX_VERSION"
    const val CAMERAX_CAMERA2 = "androidx.camera:camera-camera2:$CAMERAX_VERSION"
    const val CAMERAX_VIEW = "androidx.camera:camera-view:$CAMERAX_VERSION"
    const val CAMERAX_LIFECYCLE = "androidx.camera:camera-lifecycle:$CAMERAX_VERSION"

    // MlKit - Barcode Scanning
    // https://developers.google.com/ml-kit/vision/barcode-scanning/android
    private const val MLKIT_BARCODE_SCANNING_VERSION = "17.0.3"
    const val MLKIT_BARCODE_SCANNING = "com.google.mlkit:barcode-scanning:$MLKIT_BARCODE_SCANNING_VERSION"

    // SignaturePad
    private const val SIGNATUREPAD_VERSION = "1.3.1"
    const val SIGNATUREPAD = "com.github.gcacace:signature-pad:$SIGNATUREPAD_VERSION"
}

/**
 * test and/or androidTest specific dependencies.
 * Use [DependencyHandler] extension functions below that provide logical groupings of dependencies including appropriate configuration accessors.
 */
private object TestLibraries {
    // https://github.com/junit-team/junit4/releases
    // https://github.com/junit-team/junit4/blob/master/doc/ReleaseNotes4.13.md
    // https://github.com/junit-team/junit4/blob/main/doc/ReleaseNotes4.13.2.md
    const val JUNIT = "junit:junit:4.13.2"
    const val JUNIT_EXT = "androidx.test.ext:junit:1.1.3"

    // https://github.com/robolectric/robolectric
    const val ROBOLECTRIC = "org.robolectric:robolectric:4.7.3"

    // main site: https://google.github.io/truth/
    // comparison to other assertion libs: https://google.github.io/truth/comparison
    // benefits: https://google.github.io/truth/benefits
    // javadocs: https://truth.dev/api/1.1.3/index.html?overview-summary.html
    // https://github.com/google/truth/releases
    const val TRUTH = "com.google.truth:truth:1.1.3"

    // https://github.com/mockito/mockito-kotlin/wiki/Mocking-and-verifying
    // https://github.com/mockito/mockito-kotlin/releases
    const val MOCKITO_KOTLIN = "org.mockito.kotlin:mockito-kotlin:4.0.0"

    //// AndroidX - testing
    // https://developer.android.com/jetpack/androidx/releases/arch
    const val ARCH_CORE_TESTING = "androidx.arch.core:core-testing:2.1.0"
    const val ESPRESSO_CORE = "androidx.test.espresso:espresso-core:3.4.0"

    //// Kotlinx Coroutine - Testing
    // https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-test/
    const val KOTLINX_COROUTINE_TESTING = "org.jetbrains.kotlinx:kotlinx-coroutines-test:$KOTLIN_COROUTINES_VERSION"

    // Turbine - small emission testing lib for flows (hot or cold)
    // https://github.com/cashapp/turbine/blob/trunk/CHANGELOG.md
    // https://github.com/cashapp/turbine/releases
    const val TURBINE = "app.cash.turbine:turbine:0.7.0"
}

//// Dependency Groups - to be used inside dependencies {} block instead of declaring all necessary lines for a particular dependency
//// See DependencyHandlerUtils.kt to define DependencyHandler extension functions for types not handled (ex: compileOnly).
//// More info in BEST_PRACTICES.md -> Build section
fun DependencyHandler.kotlinDependencies() {
    implementation(Libraries.KOTLIN_STDLIB_JDK7)
    implementation(Libraries.KOTLIN_REFLECT)
}

fun DependencyHandler.coroutineDependencies() {
    implementation(Libraries.KOTLINX_COROUTINES_CORE)
    implementation(Libraries.KOTLINX_COROUTINES_ANDROID)
}

fun DependencyHandler.koinDependencies() {
    implementation(Libraries.KOIN_ANDROID)
    implementation(Libraries.KOIN_ANDROID_TEST)
}

fun DependencyHandler.coreLibraryDesugaringDependencies() {
    coreLibraryDesugaring(Libraries.CORE_LIBRARY_DESUGARING)
}

fun DependencyHandler.firebaseDependencies() {
    implementation(platform(Libraries.FIREBASE_BOM))
    implementation(Libraries.FIREBASE_ANALYTICS)
    implementation(Libraries.FIREBASE_CRASHLYTICS)
    implementation(Libraries.FIREBASE_PERFORMANCE)
    implementation(Libraries.FIREBASE_MESSAGING)
    implementation(Libraries.FIREBASE_MESSAGING_DIRECTBOOT)
}

fun DependencyHandler.retrofitDependencies() {
    implementation(Libraries.OKHTTP)
    implementation(Libraries.RETROFIT)
    implementation(Libraries.RETROFIT_SCALARS_CONVERTER)
    implementation(Libraries.RETROFIT_MOSHI_CONVERTER)
}

fun DependencyHandler.moshiDependencies() {
    api(Libraries.MOSHI)
    implementation(Libraries.MOSHI_ADAPTERS)
    kapt(Libraries.MOSHI_KOTLIN_CODEGEN)
}

fun DependencyHandler.appCompatDependencies() {
    implementation(Libraries.APP_COMPAT)
}

fun DependencyHandler.constraintLayoutDependencies() {
    implementation(Libraries.CONSTRAINT_LAYOUT)
}

fun DependencyHandler.datastoreDependencies() {
    implementation(Libraries.DATASTORE)
}

fun DependencyHandler.lifecycleDependencies() {
    implementation(Libraries.LIFECYCLE_LIVEDATA_KTX)
    implementation(Libraries.LIFECYCLE_VIEWMODEL_KTX)
    implementation(Libraries.LIFECYCLE_LIVEDATA_CORE_KTX)
    implementation(Libraries.LIFECYCLE_COMMON_JAVA8)
}

fun DependencyHandler.navigationDependencies() {
    implementation(Libraries.NAVIGATION_FRAGMENT_KTX)
    implementation(Libraries.NAVIGATION_UI_KTX)
}

fun DependencyHandler.materialDependencies() {
    implementation(Libraries.MATERIAL)
}

fun DependencyHandler.coreKtxDependencies() {
    implementation(Libraries.CORE_KTX)
}

fun DependencyHandler.preferenceKtxDependencies() {
    implementation(Libraries.PREFERENCE_KTX)
}

fun DependencyHandler.twilioDependencies() {
    implementation(Libraries.TWILIO)
}

fun DependencyHandler.securityCryptoDependencies() {
    implementation(Libraries.SECURITY_CRYPTO)
}

fun DependencyHandler.picassoDependencies() {
    implementation(Libraries.PICASSO)
}

fun DependencyHandler.groupieDependencies() {
    implementation(Libraries.GROUPIE)
    implementation(Libraries.GROUPIE_VIEWBINDING)
}

fun DependencyHandler.recyclerviewDependencies() {
    implementation(Libraries.RECYCLERVIEW_SELECTION)
    implementation(Libraries.STICKY_HEADER)
}

fun DependencyHandler.timberDependencies() {
    implementation(Libraries.TIMBER)
}

fun DependencyHandler.processPhoenixDependencies() {
    implementation(Libraries.PROCESS_PHOENIX)
}

fun DependencyHandler.liveEventDependencies() {
    implementation(Libraries.LIVE_EVENT)
}

fun DependencyHandler.commonsCodecDependencies() {
    implementation(Libraries.COMMONS_CODEC)
}

fun DependencyHandler.leakCanaryDependencies() {
    debugImplementation(Libraries.LEAK_CANARY) // note the debugImplementation usage (no releaseImplementation) - intentionally bypass adding to internalRelease build unless QA has a reason to need it
}

fun DependencyHandler.chuckerDependencies(devConfigurations: List<Configuration>, productionConfigurations: List<Configuration>) {
    // Only add dependency for dev configurations in the list
    devConfigurations.forEach { devConfiguration: Configuration ->
        add(devConfiguration.name, Libraries.CHUCKER)
    }
    // Production configuration is a no-op
    productionConfigurations.forEach { productionConfiguration: Configuration ->
        add(productionConfiguration.name, Libraries.CHUCKER_NO_OP) // note the releaseImplementation no-op
    }
}

fun DependencyHandler.constraintDependencies() {
    implementation(Libraries.CONSTRAINT_LAYOUT)
}

// Test specific dependency groups
fun DependencyHandler.junitDependencies() {
    testImplementation(TestLibraries.JUNIT)
    testImplementation(TestLibraries.JUNIT_EXT)
}

fun DependencyHandler.robolectricDependencies() {
    testImplementation(TestLibraries.ROBOLECTRIC)
}

fun DependencyHandler.mockitoKotlinDependencies() {
    testImplementation(TestLibraries.MOCKITO_KOTLIN)
}

fun DependencyHandler.truthDependencies() {
    testImplementation(TestLibraries.TRUTH)
}

fun DependencyHandler.archCoreTestingDependencies() {
    testImplementation(TestLibraries.ARCH_CORE_TESTING)
}

fun DependencyHandler.kotlinxCoroutineTestingDependencies() {
    testImplementation(TestLibraries.KOTLINX_COROUTINE_TESTING)
}

fun DependencyHandler.espressoDependencies() {
    androidTestImplementation(TestLibraries.ESPRESSO_CORE)
}

fun DependencyHandler.swipeRefreshDependencies() {
    implementation(Libraries.SWIPE_REFRESH)
}

fun DependencyHandler.lottieDependencies() {
    implementation(Libraries.LOTTIE)
    implementation(Libraries.LOTTIE_COMPOSE)
}

fun DependencyHandler.shimmerLayoutDependencies() {
    implementation(Libraries.SHIMMER)
}

fun DependencyHandler.composeDependencies() {
    implementation(Libraries.COMPOSE_RUNTIME_LIVEDATA)
    implementation(Libraries.LIFECYCLE_RUNTIME_KTX)
    implementation(Libraries.COMPOSE_LIFECYCLE)
    implementation(Libraries.COMPOSE_ACTIVITY)
    implementation(Libraries.COMPOSE_VIEWMODEL)
    implementation(Libraries.COMPOSE_UI)
    implementation(Libraries.COMPOSE_UI_TOOLING)
    implementation(Libraries.COMPOSE_MATERIAL)
    implementation(Libraries.COMPOSE_CONSTRAINT_LAYOUT)
    implementation(Libraries.COMPOSE_COIL)
}
fun DependencyHandler.turbineDependencies() {
    testImplementation(TestLibraries.TURBINE)
}

fun DependencyHandler.mlKitDependencies() {
    implementation(Libraries.MLKIT_BARCODE_SCANNING)
}

fun DependencyHandler.cameraDependencies() {
    implementation(Libraries.CAMERAX_CAMERA2)
    implementation(Libraries.CAMERAX_CORE)
    implementation(Libraries.CAMERAX_LIFECYCLE)
    implementation(Libraries.CAMERAX_VIEW)
}

fun DependencyHandler.signaturePadDependencies() {
    implementation(Libraries.SIGNATUREPAD)
}