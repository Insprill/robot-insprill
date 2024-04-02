plugins {
    kotlin("jvm") version "1.9.20"
    kotlin("plugin.serialization") version "1.9.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("net.kyori.blossom") version "1.3.1"
    id("org.ajoberstar.grgit") version "5.2.2"
}

group = "net.insprill"
version = "${project.version}+${versionMetadata()}"

repositories {
    mavenCentral()
}

dependencies {
    // Discord
    implementation("dev.kord:kord-core-jvm:0.13.1")

    // Configuration
    implementation("com.sksamuel.hoplite:hoplite-core:2.7.5")
    implementation("com.sksamuel.hoplite:hoplite-yaml:2.7.5")
    implementation("com.sksamuel.hoplite:hoplite-datetime:2.7.5")

    // Web requests
    implementation("com.github.kittinunf.fuel:fuel:2.3.1")
    implementation("com.github.kittinunf.fuel:fuel-coroutines:2.3.1")
    implementation("com.github.kittinunf.fuel:fuel-kotlinx-serialization:2.3.1")

    // OCR
    implementation("org.bytedeco:tesseract-platform:5.2.0-1.5.8")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.3")
    implementation("io.github.oshai:kotlin-logging-jvm:6.0.3")
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

blossom {
    val clazz = "src/main/kotlin/net/insprill/robotinsprill/RobotInsprill.kt"
    fun repl(token: String, value: Any?) {
        replaceToken("\"{$token}\"", "\"$value\"", clazz)
    }
    repl("build.version", version)
}

fun versionMetadata(): String {
    // CI builds only
    val buildId = System.getenv("GITHUB_RUN_NUMBER")
    if (buildId != null) {
        return "build.${buildId}"
    }

    val head = grgit.head()
    var id = head.abbreviatedId

    // Flag the build if the build tree is dirty
    if (!grgit.status().isClean) {
        id += "-dirty"
    }

    return "rev.${id}"
}
