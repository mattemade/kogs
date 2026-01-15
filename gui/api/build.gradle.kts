plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    jvm()
    js()

    sourceSets {
        val commonMain by getting
    }
}
