import com.palantir.gradle.gitversion.VersionDetails
import groovy.lang.Closure

plugins {
    kotlin("jvm") version "2.2.10"
    kotlin("plugin.serialization") version "2.2.10"
    id("com.gradleup.shadow") version "9.1.0"
    id("net.kyori.blossom") version "2.1.0"
    id("com.palantir.git-version") version "4.0.0"
}

group = "net.insprill"
version = "${project.version}${versionMetadata()}"

repositories {
    mavenCentral()
}

dependencies {
    // Discord
    implementation("dev.kord:kord-core-jvm:0.15.0")

    // Configuration
    implementation("com.sksamuel.hoplite:hoplite-core:2.9.0")
    implementation("com.sksamuel.hoplite:hoplite-yaml:2.9.0")
    implementation("com.sksamuel.hoplite:hoplite-datetime:2.9.0")

    // Web requests
    implementation("com.github.kittinunf.fuel:fuel:2.3.1")
    implementation("com.github.kittinunf.fuel:fuel-coroutines:2.3.1")
    implementation("com.github.kittinunf.fuel:fuel-kotlinx-serialization:2.3.1")

    // OCR
    implementation("org.bytedeco:tesseract-platform:5.2.0-1.5.8")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.13")
}

tasks {
    kotlin {
        jvmToolchain(21)
    }

    jar {
        enabled = false
    }

    shadowJar {
        archiveClassifier.set("")

        mergeServiceFiles()

        manifest {
            attributes["Main-Class"] = "net.insprill.robotinsprill.RobotInsprillKt"
        }
    }

    build {
        dependsOn(shadowJar)
    }
}

sourceSets {
    main {
        blossom {
            javaSources {
                property("buildVersion", version as String)
            }
        }
    }
}

fun versionMetadata(): String {
    // CI builds only
    val buildId = System.getenv("GITHUB_RUN_NUMBER")
    if (buildId != null) {
        return "+build.${buildId}"
    }

    val versionDetails: Closure<VersionDetails> by extra
    var id = versionDetails().gitHash
    val git = (extra["versionDetails"] as Closure<*>)() as VersionDetails
    var id = git.gitHash

    // Flag the build if the build tree is dirty
    if (!git.isCleanTag) {
        id += "-dirty"
    }

    return "+rev.${id}"
}
