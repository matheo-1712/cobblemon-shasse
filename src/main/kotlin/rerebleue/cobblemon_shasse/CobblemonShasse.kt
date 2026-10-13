package rerebleue.cobblemon_shasse

import net.fabricmc.api.ModInitializer
import net.minecraft.resources.ResourceLocation
import org.slf4j.LoggerFactory

object CobblemonShasse : ModInitializer {
	const val MOD_ID: String = "cobblemon-shasse"

	private val LOGGER = LoggerFactory.getLogger(MOD_ID)

	override fun onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Hello Fabric world!")
	}

	fun id(path: String): ResourceLocation
		= ResourceLocation.fromNamespaceAndPath(MOD_ID, path)
}
