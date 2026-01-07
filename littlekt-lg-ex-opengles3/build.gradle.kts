import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

plugins {
    //alias(libs.plugins.android.library)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlin.serialization)
    //id("module.publication")
}

kotlin {
    /*androidTarget {
        publishLibraryVariants("release", "debug")
    }*/
    jvm {
        /*compilations.all {
            kotlinOptions.jvmTarget = "17"
        }*/
        testRuns["test"].executionTask.configure {
            useJUnit()
        }
    }
    js(KotlinJsCompilerType.IR) {
        browser {
            binaries.executable()
            testTask(Action {
                useKarma {
                    useChromeHeadless()
                }
            })
        }

        this.attributes.attribute(
            KotlinPlatformType.attribute,
            KotlinPlatformType.js
        )

        compilations.all {
            //kotlinOptions.sourceMap = true
        }
    }

    /*@OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        compilations.all {
            kotlinOptions {
                freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
            }
        }
        binaries.executable()
    }*/

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.atomicfu)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(libs.mp3.decoder)

                implementation(libs.lwjgl.core)
                implementation(libs.lwjgl.glfw)
                implementation(libs.lwjgl.opengles)
                implementation(libs.lwjgl.openal)
                implementation(libs.lwjgl.stb)

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
                        runtimeOnly("${libs.lwjgl.glfw.get()}:$platform")
                        runtimeOnly("${libs.lwjgl.opengles.get()}:$platform")
                        runtimeOnly("${libs.lwjgl.openal.get()}:$platform")
                        runtimeOnly("${libs.lwjgl.stb.get()}:$platform")
                    }
            }
        }
        val jvmTest by getting
        val jsMain by getting
        val jsTest by getting
        /*val wasmJsMain by getting
        val wasmJsTest by getting
        val androidMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.android)
            }
        }
        val androidUnitTest by getting*/

        val jvmAndroidMain = maybeCreate("jvmAndroidMain")

        jvmAndroidMain.dependsOn(commonMain)
        //androidMain.dependsOn(jvmAndroidMain)
        jvmMain.dependsOn(jvmAndroidMain)
        //androidUnitTest.dependsOn(commonTest)
        jvmTest.dependsOn(commonTest)
        jsTest.dependsOn(commonTest)

        all {
            languageSettings.apply {
                progressiveMode = true
                optIn("kotlin.contracts.ExperimentalContracts")
                optIn("kotlin.time.ExperimentalTime")
            }
        }
    }
}

/*android {
    namespace = "com.littlekt.core"
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    compileSdk = (findProperty("android.compileSdk") as String).toInt()

    defaultConfig {
        minSdk = (findProperty("android.minSdk") as String).toInt()
        targetSdk = (findProperty("android.targetSdk") as String).toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}*/
