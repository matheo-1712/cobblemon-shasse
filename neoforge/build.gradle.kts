plugins { id("net.neoforged.moddev") }
base { archivesName.set("cobblemon-shasse-neoforge") }
kotlin.sourceSets.main { kotlin.srcDir("../common/src/main/kotlin") }
sourceSets.main { resources.srcDir("../common/src/main/resources") }
neoForge {
    version = property("neoforge_version").toString()
    runs {
        create("server") {
            server()
            gameDirectory.set(file("run"))
            programArgument("nogui")
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
