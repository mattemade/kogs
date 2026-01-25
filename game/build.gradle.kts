import org.apache.tools.ant.taskdefs.condition.Os
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.ir.JsIrBinary

plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    tasks.withType<JavaExec> { jvmArgs("--enable-preview", "--enable-native-access=ALL-UNNAMED") }
    jvm {
        //compilations.all { kotlinOptions.jvmTarget = "21" }
        compilations {
            val main by getting

            val mainClassName = (findProperty("jvm.mainClass") as? String)?.plus("Kt")
            if (mainClassName == null) {
                project.logger.log(
                    LogLevel.ERROR,
                    "Property 'jvm.mainClass' has either changed or has not been set. Check 'gradle.properties' and ensure it is properly set!"
                )
            }
            tasks {
                register<Copy>("copyResources") {
                    group = "package"
                    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                    dependsOn(named("jvmProcessResources"))
                    from(main.output.resourcesDir)
                    destinationDir = File("${layout.buildDirectory.asFile.get()}/publish")
                }
                register<Jar>("packageFatJar") {
                    group = "package"
                    archiveClassifier.set("all")
                    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                    dependsOn(named("jvmJar"))
                    dependsOn(named("copyResources"))
                    manifest { attributes["Main-Class"] = mainClassName }
                    destinationDirectory.set(File("${layout.buildDirectory.asFile.get()}/publish/"))
                    from(
                        main.runtimeDependencyFiles.map { if (it.isDirectory) it else zipTree(it) },
                        main.output.classesDirs
                    )
                    doLast {
                        logger.lifecycle(
                            "[LittleKt] The packaged jar is available at: ${outputs.files.first().parent}"
                        )
                    }
                }
                if (Os.isFamily(Os.FAMILY_MAC)) {
                    register<JavaExec>("jvmRun") {
                        jvmArgs("-XstartOnFirstThread")
                        mainClass.set(mainClassName)
                        kotlin {
                            val mainCompile = targets["jvm"].compilations["main"]
                            dependsOn(mainCompile.compileAllTaskName)
                            classpath(
                                { mainCompile.output.allOutputs.files },
                                (configurations["jvmRuntimeClasspath"])
                            )
                        }
                    }
                }
            }
        }
    }
    js {
        binaries.executable()
        browser {
            testTask { useKarma { useChromeHeadless() } }
            commonWebpackConfig {
                devServer =
                    (devServer
                        ?: org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
                            .DevServer())
                        .copy(
                            open = mapOf("app" to mapOf("name" to "chrome")),
                        )
            }
        }
/*        binaries.withType<JsIrBinary>().all {
            this.linkTask.configure {
                kotlinOptions {
                    sourceMap = false
                }
            }
        }*/

        this.attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)

        //compilations.all { kotlinOptions.sourceMap = false }
    }

    val multiplatformDependencies = listOf(
        ":game-utils",
        ":fmod",
        ":concurrency",
    )

    sourceSets {
        val commonMain by getting {
            dependencies {
                //implementation(project(":game-utils"))
                multiplatformDependencies.forEach {
                    implementation(project(it))
                }
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.websockets)
                implementation(libs.ktor.client.cio)
            }
        }
        val jvmTest by getting
        val jsMain by getting {
            dependencies {
                implementation(libs.kotlinx.html.js)
            }
        }
        val jsTest by getting
    }

    // ensure resources of JS dependencies are copied are copied
    multiplatformDependencies.forEach { dependency ->
        tasks.named<ProcessResources>("jsProcessResources") {
            val libResources = project(dependency)
                .tasks.named<ProcessResources>("jsProcessResources")

            from(libResources.map { it.destinationDir })
        }
    }

}
