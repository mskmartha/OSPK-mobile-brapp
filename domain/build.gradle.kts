plugins {
    id(Config.ApplyPlugins.JAVA_LIB)
    id(Config.ApplyPlugins.KOTLIN_LIB)
}

dependencies {
    kotlinDependencies()
    coroutineDependencies()
    // Test
    junitDependencies()
    mockitoKotlinDependencies()
    truthDependencies()
}
