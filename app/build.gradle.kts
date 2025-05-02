import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.api.BaseVariantOutput

plugins {
    id(Config.ApplyPlugins.ANDROID_APPLICATION)
    id(Config.ApplyPlugins.APP_DYNAMICS)
    id(Config.ApplyPlugins.JACOCO_ANDROID)
    kotlin(Config.ApplyPlugins.Kotlin.ANDROID)
    kotlin(Config.ApplyPlugins.Kotlin.KAPT)
    id(Config.ApplyPlugins.PARCELIZE)
    id(Config.ApplyPlugins.NAVIGATION_SAFE_ARGS_KOTLIN)
    id(Config.ApplyPlugins.GOOGLE_SERVICES)
    id(Config.ApplyPlugins.CRASHLYTICS)
    id(Config.ApplyPlugins.FIREBASE_PERF)
}

jacoco {
    toolVersion = Config.JACOCO_VERSION
}

// Prep BuildInfoManager to use its functions/properties later throughout this build script
BuildInfoManager.initialize(
    BuildInfoInput(
        appVersion = AppVersion(
            major = 2, minor = 30, patch = 0, hotfix = 0,
            showEmptyPatchNumberInVersionName = true,
            showHotFixIfNotEmpty = true,
            padZerosToNDigits = 2
        ),
        brandName = "Albertsons_AcuPick",
        rootProjectDir = rootDir
    )
)

// Some documentation on inner tags/blocks can be found with the below urls:
// android {...} DSL Reference:
// Android Gradle Plugin api: https://developer.android.com/reference/tools/gradle-api/4.1/classes
android {
    compileSdk = Config.AndroidSdkVersions.COMPILE_SDK
    buildToolsVersion = Config.AndroidSdkVersions.BUILD_TOOLS
    defaultConfig {
        minSdk = Config.AndroidSdkVersions.MIN_SDK
        targetSdk = Config.AndroidSdkVersions.TARGET_SDK
        versionCode = BuildInfoManager.APP_VERSION.versionCode
        versionName = BuildInfoManager.APP_VERSION.versionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    lint {
        disable += setOf("NullSafeMutableLiveData")
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = Config.ComplierExtension.KOTLIN
    }
    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    signingConfigs {
        getByName("debug") {
            // Use common debug keystore so all local builds can be shared between devs/QA
            storeFile = file("../keystore/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        create("release") {
            storeFile = file(System.getenv("KEYSTORE") ?: "error")
            storePassword = System.getenv()["KEYSTORE_PASSWORD"]
            keyAlias = System.getenv()["KEY_ALIAS"]
            keyPassword = System.getenv()["KEY_PASSWORD"]
        }
    }
    // See BEST_PRACTICES.md for comments on purpose of each build type/flavor/variant
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            // Disabling as leaving it enabled can cause the build to hang at the jacocoDebug task for 5+ minutes with no observed adverse effects when executing
            // the jacocoTest...UnitTestReport tasks. Stopping and restarting build would allow compilation/installation to complete.
            // Disable suggestion found at https://github.com/opendatakit/collect/issues/3262#issuecomment-546815946
            isTestCoverageEnabled = false
        }
        // Create debug minified buildtype to allow attaching debugger to minified build: https://medium.com/androiddevelopers/practical-proguard-rules-examples-5640a3907dc9
        create("debugMini") {
            initWith(getByName("debug"))
            setMatchingFallbacks("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    flavorDimensions("environment")
    // See BEST_PRACTICES.md for comments on purpose of each build type/flavor/variant
    productFlavors {
        create("internal") {
            applicationId = "com.albertsons.acupick.internal"
            versionNameSuffix = "-internal"
            buildConfigField("boolean", "INTERNAL", "true")
            buildConfigField("boolean", "PRODUCTION", "false")
            buildConfigField("Boolean", "USE_PRODUCTION_EAST_REGION", "null")
            buildConfigField("Boolean", "USE_PRODUCTION_CANARY", "null")
            dimension = "environment"
        }
        create("production") {
            applicationId = "com.albertsons.acupick"
            buildConfigField("boolean", "INTERNAL", "false")
            buildConfigField("boolean", "PRODUCTION", "true")
            buildConfigField("Boolean", "USE_PRODUCTION_EAST_REGION", "false")
            buildConfigField("Boolean", "USE_PRODUCTION_CANARY", "false")
            dimension = "environment"
        }
        create("productionEast") {
            applicationId = "com.albertsons.acupick"
            buildConfigField("boolean", "INTERNAL", "false")
            buildConfigField("boolean", "PRODUCTION", "true")
            buildConfigField("Boolean", "USE_PRODUCTION_EAST_REGION", "true")
            buildConfigField("Boolean", "USE_PRODUCTION_CANARY", "false")
            dimension = "environment"
        }
        create("productionCanary") {
            applicationId = "com.albertsons.acupick"
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
    applicationVariants.all {
        // Using a local val here since attempting to use a named lambda parameter would change the function signature from operating on applicationVariants.all (with an `Action` parameter)
        // to the Collections Iterable.`all` function. Same thing applies to outputs.all below
        val variant: ApplicationVariant = this
        val variantType = when {
            (variant.productFlavors.any { it.name.contains("canary", ignoreCase = true) }) -> VariantType.Canary
            (variant.productFlavors.any { it.name.contains("east", ignoreCase = true) }) -> VariantType.East
            else -> VariantType.West
        }
        BuildInfoManager.createBuildIdentifier(variant, variantType)
        variant.outputs.all {
            val baseVariantOutput: BaseVariantOutput = this
            BuildInfoManager.modifyVersionNameAndApkName(variant, baseVariantOutput, variantType)
        }
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.isReturnDefaultValues = true
    }
    // lint {
    //     ignore("NullSafeMutableLiveData")
    // }

    sourceSets {
        getByName("debug") {
            java {
                srcDirs("src/debug/java")
            }
        }
        getByName("release") {
            java {
                srcDirs("src/release/java")
            }
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
    implementation(project(":wifi"))
    implementation(project(mapOf("path" to ":data")))
    // TODO: Find a way to make sure we are aware of out-of-date versions of any static aars/jars in /libs. Manually check for any updates at/prior to dev signoff.
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    debugImplementation("androidx.compose.ui:ui-tooling:1.4.3")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Kotlin/coroutines
    kotlinDependencies()
    coroutineDependencies()
    preferenceKtxDependencies()

    // Twilio
    twilioDependencies()

    // AndroidX
    appCompatDependencies()
    constraintLayoutDependencies()
    datastoreDependencies()
    materialDependencies()
    lifecycleDependencies()
    navigationDependencies()
    constraintDependencies()
    cameraDependencies()

    // UI
    picassoDependencies()
    groupieDependencies()
    recyclerviewDependencies()
    swipeRefreshDependencies()
    lottieDependencies()
    shimmerLayoutDependencies()
    composeDependencies()
    // Utility
    liveEventDependencies()
    timberDependencies()
    processPhoenixDependencies()
    leakCanaryDependencies()

    // Misc
    koinDependencies()
    coreLibraryDesugaringDependencies()
    firebaseDependencies()
    mlKitDependencies()
    signaturePadDependencies()

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
