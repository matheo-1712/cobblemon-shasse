package rerebleue.cobblemon_shasse.fabric

import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.loader.api.FabricLoader
import rerebleue.cobblemon_shasse.ShasseRuntime
import rerebleue.cobblemon_shasse.command.ShinyHuntCommands

class FabricShasse : DedicatedServerModInitializer {
    private var runtime: ShasseRuntime? = null
    override fun onInitializeServer() {
        ShasseRuntime.initialize()
        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            runtime = ShasseRuntime(server, FabricLoader.getInstance().configDir.resolve("cobblemon-shasse.json"))
        }
        ServerLifecycleEvents.SERVER_STOPPING.register { runtime?.close(); runtime = null }
        ServerTickEvents.END_SERVER_TICK.register { runtime?.tick() }
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            ShinyHuntCommands.register(dispatcher) { runtime }
        }
    }
}
