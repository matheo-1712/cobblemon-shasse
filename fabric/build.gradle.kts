plugins { id("fabric-loom") }
base { archivesName.set("cobblemon-shasse-fabric") }
kotlin.sourceSets.main { kotlin.srcDir("../common/src/main/kotlin") }
sourceSets.main { resources.srcDir("../common/src/main/resources") }
dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${property("fabric_loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${property("fabric_kotlin_version")}")
    modCompileOnly("maven.modrinth:cobblemon:${property("cobblemon_fabric_version")}")
    if (!providers.gradleProperty("withoutCobblemon").isPresent) {
        modRuntimeOnly("maven.modrinth:cobblemon:${property("cobblemon_fabric_version")}")
        // Loom does not add Cobblemon's nested libraries to the development classpath.
        // These are already shipped by Cobblemon in production; nothing is bundled in our JAR.
        runtimeOnly("org.graalvm.js:js:22.3.0")
        minecraftServerLibraries("com.ibm.icu:icu4j:71.1")
        runtimeOnly("org.mongodb:mongodb-driver-sync:4.10.2")
    }
}
loom { runs { named("server") { runDir("run"); programArg("nogui") } } }
tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") { expand("version" to project.version) }
}
java { withSourcesJar() }
