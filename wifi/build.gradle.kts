plugins {
    id(Config.ApplyPlugins.ANDROID_LIBRARY)
    id(Config.ApplyPlugins.JACOCO_ANDROID)
    id(Config.ApplyPlugins.PARCELIZE)
    kotlin(Config.ApplyPlugins.Kotlin.ANDROID)
    kotlin(Config.ApplyPlugins.Kotlin.KAPT)
}

jacoco {
    toolVersion = Config.JACOCO_VERSION
}

android {
    compileSdk = Config.AndroidSdkVersions.COMPILE_SDK
    buildToolsVersion = Config.AndroidSdkVersions.BUILD_TOOLS

    defaultConfig {
        minSdk = Config.AndroidSdkVersions.MIN_SDK
        targetSdk = Config.AndroidSdkVersions.TARGET_SDK
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }

    buildTypes {
        getByName("release") {
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
        getByName("debug") {
            // Disabling as leaving it enabled can cause the build to hang at the jacocoDebug task for 5+ minutes with no observed adverse effects when executing
            // the jacocoTest...UnitTestReport tasks. Stopping and restarting build would allow compilation/installation to complete.
            // Disable suggestion found at https://github.com/opendatakit/collect/issues/3262#issuecomment-546815946
            isTestCoverageEnabled = false
        }
        // Create debug minified buildtype to allow attaching debugger to minified build: https://medium.com/androiddevelopers/practical-proguard-rules-examples-5640a3907dc9
        create("debugMini") {
            initWith(getByName("debug"))
            setMatchingFallbacks("debug")
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }
    flavorDimensions("environment")
    // See BEST_PRACTICES.md for comments on purpose of each build type/flavor/variant
    productFlavors {
        create("internal") {
            buildConfigField("boolean", "INTERNAL", "true")
            buildConfigField("boolean", "PRODUCTION", "false")
            buildConfigField("Boolean", "USE_PRODUCTION_EAST_REGION", "null")
            buildConfigField("Boolean", "USE_PRODUCTION_CANARY", "null")
            dimension = "environment"
        }
        create("production") {
            buildConfigField("boolean", "INTERNAL", "false")
            buildConfigField("boolean", "PRODUCTION", "true")
            buildConfigField("Boolean", "USE_PRODUCTION_EAST_REGION", "false")
            buildConfigField("Boolean", "USE_PRODUCTION_CANARY", "false")
            dimension = "environment"
        }
        create("productionEast") {
            buildConfigField("boolean", "INTERNAL", "false")
            buildConfigField("boolean", "PRODUCTION", "true")
            buildConfigField("Boolean", "USE_PRODUCTION_EAST_REGION", "true")
            buildConfigField("Boolean", "USE_PRODUCTION_CANARY", "false")
            dimension = "environment"
        }
        create("productionCanary") {
            buildConfigField("boolean", "INTERNAL", "false")
            buildConfigField("boolean", "PRODUCTION", "true")
            buildConfigField("Boolean", "USE_PRODUCTION_EAST_REGION", "false")
            buildConfigField("Boolean", "USE_PRODUCTION_CANARY", "true")
            dimension = "environment"
        }
    }
    variantFilter {
        // Gradle ignores any variants that satisfy the conditions listed below. `productionDebug` has no value for this project.
        if (setOf("productionDebug", "productionDebugMini", "productionEastDebug", "productionEastDebugMini", "productionCanaryDebug", "productionCanaryDebugMini").contains(name)) {
            ignore = true
        }
    }
}

// Declare configurations per variant to use in the dependencies block below. More info: https://guides.gradle.org/migrating-build-logic-from-groovy-to-kotlin/#custom_configurations_and_dependencies
private val internalDebugImplementation: Configuration by configurations.creating { extendsFrom(configurations["debugImplementation"]) }
private val internalDebugMiniImplementation: Configuration by configurations.creating { extendsFrom(configurations["debugImplementation"]) }
private val internalReleaseImplementation: Configuration by configurations.creating { extendsFrom(configurations["releaseImplementation"]) }
private val productionReleaseImplementation: Configuration by configurations.creating { extendsFrom(configurations["releaseImplementation"]) }
private val productionEastReleaseImplementation: Configuration by configurations.creating { extendsFrom(configurations["releaseImplementation"]) }
private val productionCanaryReleaseImplementation: Configuration by configurations.creating { extendsFrom(configurations["releaseImplementation"]) }
/** List of all buildable release configurations */
val releaseConfigurations: List<Configuration> = listOf(productionReleaseImplementation, productionEastReleaseImplementation, productionCanaryReleaseImplementation)
/** List of all buildable dev configurations */
val devConfigurations: List<Configuration> = listOf(internalDebugImplementation, internalDebugMiniImplementation, internalReleaseImplementation)

dependencies {
    implementation(project(":domain"))
    // TODO: Find a way to make sure we are aware of out-of-date versions of any static aars/jars in /libs. Manually check for any updates at/prior to dev signoff.
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    // Kotlin/coroutines
    kotlinDependencies()
    coroutineDependencies()
    preferenceKtxDependencies()

    // AndroidX
    coreKtxDependencies()
    securityCryptoDependencies()

    koinDependencies()

    coreLibraryDesugaringDependencies()

    firebaseDependencies()

    // Networking/parsing
    retrofitDependencies()
    moshiDependencies()

    // Utility
    liveEventDependencies()
    timberDependencies()
    commonsCodecDependencies()
    chuckerDependencies(devConfigurations = devConfigurations, productionConfigurations = releaseConfigurations)

    // Test
    junitDependencies()
    robolectricDependencies()
    mockitoKotlinDependencies()
    truthDependencies()
    archCoreTestingDependencies()
    kotlinxCoroutineTestingDependencies()
    turbineDependencies()

    // Android Test
    espressoDependencies()
}
