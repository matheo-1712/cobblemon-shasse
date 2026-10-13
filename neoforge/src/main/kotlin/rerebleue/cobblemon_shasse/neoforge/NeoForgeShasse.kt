package rerebleue.cobblemon_shasse.neoforge

import net.neoforged.api.distmarker.Dist
import net.neoforged.fml.common.Mod
import net.neoforged.fml.loading.FMLPaths
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.RegisterCommandsEvent
import net.neoforged.neoforge.event.server.ServerStartedEvent
import net.neoforged.neoforge.event.server.ServerStoppingEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent
import rerebleue.cobblemon_shasse.ShasseRuntime
import rerebleue.cobblemon_shasse.command.ShinyHuntCommands

@Mod(value = "cobblemon_shasse", dist = [Dist.DEDICATED_SERVER])
class NeoForgeShasse {
    private var runtime: ShasseRuntime? = null
    init {
        ShasseRuntime.initialize()
        NeoForge.EVENT_BUS.addListener<ServerStartedEvent> { event ->
            runtime = ShasseRuntime(event.server, FMLPaths.CONFIGDIR.get().resolve("cobblemon-shasse.json"))
        }
        NeoForge.EVENT_BUS.addListener<ServerStoppingEvent> { runtime?.close(); runtime = null }
        NeoForge.EVENT_BUS.addListener<ServerTickEvent.Post> { runtime?.tick() }
        NeoForge.EVENT_BUS.addListener<RegisterCommandsEvent> { event ->
            ShinyHuntCommands.register(event.dispatcher) { runtime }
        }
    }
}
