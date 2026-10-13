import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    base
    kotlin("jvm") version "2.2.20" apply false
    id("fabric-loom") version "1.11.8" apply false
    id("net.neoforged.moddev") version "2.0.147" apply false
}
allprojects {
    group = rootProject.property("mod_group")!!
    version = rootProject.property("mod_version")!!
    repositories {
        mavenCentral()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases")
        maven("https://thedarkcolour.github.io/KotlinForForge/")
        exclusiveContent {
            forRepository { maven("https://api.modrinth.com/maven") }
            filter { includeGroup("maven.modrinth") }
        }
    }
}
subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        jvmToolchain(21)
        compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
    }
    tasks.withType<JavaCompile>().configureEach { options.release.set(21) }
    tasks.withType<AbstractArchiveTask>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }
    tasks.withType<Jar>().configureEach { from(rootProject.file("LICENSE")) }
}
tasks.named("build") { dependsOn(":common:build", ":fabric:build", ":neoforge:build") }
tasks.named("check") { dependsOn(":common:check", ":fabric:check", ":neoforge:check") }
