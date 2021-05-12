import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack

plugins {
    kotlin("multiplatform") version "1.5.0"
    kotlin("plugin.serialization") version "1.5.0"
    application
}

group = "world.amplus"
version = "1.0-SNAPSHOT"

repositories {
    jcenter()
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-js-wrappers") }
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven") }
}

distributions {
    main {
        contents {
            into ("www") {
                from("www")
            }
            into ("www/genjs") {
                from("build/distributions/amplus_packed.js")
                from("build/distributions/amplus_packed.js.map")
            }
            into("lib") {
                from("build/libs")
            }
        }
    }
}

kotlin {

    js(LEGACY) {
        binaries.executable()
        browser {
            commonWebpackConfig {
                cssSupport.enabled = true
                devServer =
                    org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.DevServer(
                        //contentBase  = listOf("/Users/ehubbard/dev/ampy/www")
                        contentBase  = listOf("${projectDir}/www")

                    )

//                devServer?.proxy?.apply {
//                    this.plus("/socket", )
//                }

            }
        }
    }
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
        testRuns["test"].executionTask.configure {
            useJUnit()
        }
        withJava()
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.2.0")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting {
            dependencies {

                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0-RC")
                implementation("com.google.code.gson:gson:2.8.6")
                implementation("io.ktor:ktor-server-netty:1.5.2")
                implementation("io.ktor:ktor-html-builder:1.5.2")
                implementation("io.ktor:ktor-websockets:1.5.2")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.2.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.2.0")
                implementation("ch.qos.logback:logback-classic:1.0.13")
                implementation("net.sf.trove4j:trove4j:2.1.0")
                implementation("com.github.davidmoten:hilbert-curve:0.2.2")
                implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.2")
                implementation("com.google.api-client:google-api-client:1.31.2")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
        val jsMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.2.0")
                implementation(npm("three", "^0.120.0", generateExternals = false))
                implementation(npm("stats-js", "1.0.1", generateExternals = false))

            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}

application {
    mainClassName = "world.amplus.server.ServerKt"
}

tasks.getByName<KotlinWebpack>("jsBrowserProductionWebpack") {
    outputFileName = "amplus_packed.js"
}

tasks.getByName<Jar>("jvmJar") {
    dependsOn(tasks.getByName("jsBrowserProductionWebpack"))
    val jsBrowserProductionWebpack = tasks.getByName<KotlinWebpack>("jsBrowserProductionWebpack")
    from(File(jsBrowserProductionWebpack.destinationDirectory, jsBrowserProductionWebpack.outputFileName))
}

tasks.getByName<JavaExec>("run") {
    dependsOn(tasks.getByName<Jar>("jvmJar"))
    classpath(tasks.getByName<Jar>("jvmJar"))
}
