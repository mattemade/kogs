plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    jvm()
    js {
        browser {
            binaries.library()
        }
    }

    sourceSets {
        val commonMain by getting
        val jsMain by getting
    }
}
