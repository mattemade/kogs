plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    jvm()
    js(IR) {
        binaries.library()
        browser {}
    }

    sourceSets {
        val commonMain by getting
        val jsMain by getting
        val jvmMain by getting
    }
}
