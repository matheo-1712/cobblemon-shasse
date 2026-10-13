package rerebleue.cobblemon_shasse.config

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.nio.file.Files
import java.nio.file.Path

/** Explicit parsing avoids Gson creating null values in non-null Kotlin fields. */
class ConfigFile(private val path: Path) {
    fun load(): HuntConfig {
        if (!Files.exists(path)) {
            Files.createDirectories(path.parent)
            Files.writeString(path, GsonBuilder().setPrettyPrinting().create().toJson(HuntConfig()) + "\n")
            return HuntConfig()
        }
        return parse(Files.readString(path))
    }

    companion object {
        fun parse(json: String): HuntConfig {
            val root = JsonParser.parseString(json).asJsonObject
            val defaults = HuntConfig()
            val s = root.obj("swordShield")
            val d = defaults.swordShield
            return HuntConfig(
                root.bool("enabled", true),
                SwordShieldConfig(
                    s.bool("enabled", d.enabled),
                    HuntMode.valueOf(s.text("mode", d.mode.name)),
                    s.long("maxCountForBonus", d.maxCountForBonus),
                    s.bool("countWildDefeats", d.countWildDefeats),
                    s.bool("countWildCaptures", d.countWildCaptures),
                    s.bool("allowTrainerBattles", d.allowTrainerBattles),
                    s.bool("allowArtificialSpawns", d.allowArtificialSpawns),
                    Attribution.valueOf(s.text("attribution", d.attribution.name)),
                    if (s.has("rollTiers")) s.getAsJsonArray("rollTiers").map {
                        val tier = it.asJsonObject
                        RollTier(tier.long("count", -1), Math.toIntExact(tier.long("extraRolls", -1)))
                    } else d.rollTiers,
                    if (s.has("brilliantTiers")) s.getAsJsonArray("brilliantTiers").map {
                        val tier = it.asJsonObject
                        BrilliantTier(tier.long("count", -1), tier.get("probability").asDouble)
                    } else d.brilliantTiers
                ),
                CaptureChainConfig(root.obj("captureChain").bool("enabled", false)),
                PersistenceConfig(root.obj("persistence").bool("autosaveEnabled", true))
            ).validate()
        }
        private fun JsonObject.obj(key: String) = if (has(key)) get(key).asJsonObject else JsonObject()
        private fun JsonObject.bool(key: String, default: Boolean): Boolean {
            if (!has(key)) return default
            val value = get(key).asJsonPrimitive
            require(value.isBoolean) { "$key doit être un booléen" }
            return value.asBoolean
        }
        private fun JsonObject.text(key: String, default: String): String {
            if (!has(key)) return default
            val value = get(key).asJsonPrimitive
            require(value.isString) { "$key doit être une chaîne" }
            return value.asString
        }
        private fun JsonObject.long(key: String, default: Long): Long {
            if (!has(key)) return default
            val value = get(key).asJsonPrimitive
            require(value.isNumber) { "$key doit être un entier" }
            return value.asBigDecimal.longValueExact()
        }
    }
}
