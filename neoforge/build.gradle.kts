plugins { id("net.neoforged.moddev") }
base { archivesName.set("cobblemon-shasse-neoforge") }
kotlin.sourceSets.main { kotlin.srcDir("../common/src/main/kotlin") }
sourceSets.main { resources.srcDir("../common/src/main/resources") }
val testClient = sourceSets.create("testClient") {
    runtimeClasspath = sourceSets.main.get().runtimeClasspath - sourceSets.main.get().output
}
neoForge {
    version = property("neoforge_version").toString()
    runs {
        create("client") {
            client()
            sourceSet.set(testClient)
            loadedMods.set(emptySet())
            gameDirectory.set(file("run-client"))
            ideName.set("NeoForge - Client Cobblemon sans Shasse")
            programArgument("--username")
            programArgument(providers.gradleProperty("testPlayer").getOrElse("RereBleue"))
            jvmArgument("-Xmx3G")
        }
        create("server") {
            server()
            gameDirectory.set(file("run"))
            programArgument("nogui")
        }
        create("localTestServer") {
            server()
            gameDirectory.set(file("run-test-server"))
            ideName.set("NeoForge - Serveur de test local")
            programArgument("nogui")
            jvmArgument("-Xmx3G")
            taskBefore(rootProject.tasks.named("prepareLocalTestServers"))
        }
    }
    mods { create("cobblemon_shasse") { sourceSet(sourceSets.main.get()) } }
}
dependencies {
    compileOnly("maven.modrinth:cobblemon:${property("cobblemon_neoforge_version")}")
    if (!providers.gradleProperty("withoutCobblemon").isPresent) {
        runtimeOnly("maven.modrinth:cobblemon:${property("cobblemon_neoforge_version")}")
    }
    implementation("thedarkcolour:kotlinforforge-neoforge:${property("kotlin_for_forge_version")}")
}
tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("META-INF/neoforge.mods.toml") { expand("version" to project.version) }
}
java { withSourcesJar() }
tasks.named<JavaExec>("runServer") { standardInput = System.`in` }
tasks.named<JavaExec>("runLocalTestServer") { standardInput = System.`in` }
tasks.named<JavaExec>("runClient") {
    doFirst {
        check(testClient.runtimeClasspath.files.intersect(sourceSets.main.get().output.files).isEmpty()) {
            "Le client de test doit exclure les fichiers de Shasse"
        }
    }
}
