pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases")
        gradlePluginPortal()
        mavenCentral()
    }
}
rootProject.name = "cobblemon-shasse"
include("common", "fabric", "neoforge")
