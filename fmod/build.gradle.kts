import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

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
        val commonMain by getting {}
        val jsMain by getting {}
        val jvmMain by getting {
            dependencies {
                implementation(libs.lwjgl.core)
                implementation(libs.lwjgl.fmod)

                listOf(
                    "natives-windows",
                    "natives-windows-arm64",
                    "natives-linux",
                    "natives-linux-arm64",
                    "natives-macos",
                    "natives-macos-arm64"
                )
                    .forEach { platform ->
                        runtimeOnly("${libs.lwjgl.core.get()}:$platform")
                    }
            }
            resources.srcDir("src/jvmMain/resources")
        }
    }
}
