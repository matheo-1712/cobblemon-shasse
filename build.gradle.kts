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

tasks.register("prepareLocalTestServers") {
    group = "verification"
    description = "Prépare deux serveurs locaux isolés pour les clients de développement."
    doLast {
        val acceptedEula = file("fabric/run/eula.txt").takeIf {
            it.exists() && Regex("(?m)^eula\\s*=\\s*true\\s*$").containsMatchIn(it.readText())
        }
        listOf("fabric" to 25565, "neoforge" to 25566).forEach { (loader, port) ->
            val directory = file("$loader/run-test-server").apply { mkdirs() }
            val settings = directory.resolve("server.properties")
            if (!settings.exists()) {
                settings.writeText("""
                    server-ip=127.0.0.1
                    server-port=$port
                    online-mode=false
                    enforce-secure-profile=false
                    gamemode=creative
                    difficulty=normal
                    view-distance=4
                    simulation-distance=4
                    spawn-protection=0
                    motd=Cobblemon Shasse - Test local $loader
                    level-name=world
                """.trimIndent() + "\n")
            }
            // Reuse the user's existing acceptance; never accept or replace an EULA on their behalf.
            val targetEula = directory.resolve("eula.txt")
            if (acceptedEula != null && !targetEula.exists()) acceptedEula.copyTo(targetEula)
            // Local offline test identity, explicitly authorized as an operator by the user.
            val playerName = "RereBleue"
            val playerUuid = java.util.UUID.nameUUIDFromBytes("OfflinePlayer:$playerName".toByteArray(Charsets.UTF_8))
            val opsFile = directory.resolve("ops.json")
            @Suppress("UNCHECKED_CAST")
            val existingOps = if (opsFile.exists()) {
                groovy.json.JsonSlurper().parseText(opsFile.readText()) as? List<Map<String, Any?>>
                    ?: error("Fichier ops.json invalide : $opsFile")
            } else emptyList()
            val operator = mapOf("uuid" to playerUuid.toString(), "name" to playerName,
                "level" to 4, "bypassesPlayerLimit" to false)
            val updatedOps = existingOps.filterNot { it["uuid"] == playerUuid.toString() } + operator
            opsFile.writeText(groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(updatedOps)) + "\n")
        }
    }
}
