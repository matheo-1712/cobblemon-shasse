plugins { id("fabric-loom") }
base { archivesName.set("cobblemon-shasse-common") }
dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${property("fabric_loader_version")}")
    // Cobblemon implements Fabric API interfaces; they must be present during remapping.
    modCompileOnly("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")
    modImplementation("maven.modrinth:cobblemon:${property("cobblemon_fabric_version")}")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
}
tasks.test { useJUnitPlatform() }
java { withSourcesJar() }
