package rerebleue.cobblemon_shasse

import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.NbtIo
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.storage.LevelResource
import org.slf4j.LoggerFactory
import rerebleue.cobblemon_shasse.config.ConfigFile
import rerebleue.cobblemon_shasse.config.HuntConfig
import rerebleue.cobblemon_shasse.integration.CobblemonHooks
import rerebleue.cobblemon_shasse.integration.NaturalSpawnProperty
import rerebleue.cobblemon_shasse.method.MethodRegistry
import rerebleue.cobblemon_shasse.method.SwordShieldMethod
import rerebleue.cobblemon_shasse.storage.HuntSavedData
import java.nio.file.Files
import java.nio.file.Path

class ShasseRuntime(val server: MinecraftServer, configPath: Path) : AutoCloseable {
    private val configFile = ConfigFile(configPath)
    var config: HuntConfig = loadInitialConfig()
        private set
    val methods = MethodRegistry().apply { register(SwordShieldMethod()) }
    val storage: HuntSavedData
    private val hooks = CobblemonHooks(this)
    var ticks = 0L
        private set

    init {
        // Validate ourselves first: DimensionDataStorage catches malformed files and replaces them.
        // Fail startup instead of silently losing an existing player's statistics.
        val path = server.getWorldPath(LevelResource.ROOT).resolve("data").resolve("${HuntSavedData.NAME}.dat")
        if (Files.exists(path)) {
            val root = NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap())
            storage = HuntSavedData.decode(root.getCompound("data"))
        } else {
            storage = HuntSavedData()
        }
        server.overworld().dataStorage.set(HuntSavedData.NAME, storage)
        hooks.install()
        LOGGER.info("Cobblemon Shasse actif : mode {}, données par monde, version 1", config.swordShield.mode)
    }
    private fun loadInitialConfig(): HuntConfig = try { configFile.load() } catch (e: Exception) {
        LOGGER.error("Configuration invalide : shasse désactivée. Corriger le fichier puis /shinyhunt reload.", e)
        HuntConfig(enabled = false)
    }
    fun reload() {
        val next = configFile.load()
        config = next
        if (next.persistence.autosaveEnabled) storage.changed()
    }
    fun changed() {
        if (config.persistence.autosaveEnabled) storage.changed()
    }
    fun onServerThread(action: () -> Unit) {
        if (server.isSameThread) action() else server.execute(action)
    }
    fun tick() {
        ticks++
        hooks.tick()
    }
    override fun close() {
        hooks.close()
        // SERVER_STOPPING runs before Minecraft's final save.
        storage.changed()
        LOGGER.info("Cobblemon Shasse : statistiques préparées pour la sauvegarde finale")
    }
    companion object {
        val LOGGER = LoggerFactory.getLogger("cobblemon_shasse")
        private var initialized = false
        fun initialize() {
            if (!initialized) {
                NaturalSpawnProperty.register()
                initialized = true
            }
        }
    }
}
