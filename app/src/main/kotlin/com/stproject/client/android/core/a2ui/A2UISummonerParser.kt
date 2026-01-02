package com.stproject.client.android.core.a2ui

import com.google.gson.JsonObject
import com.stproject.client.android.domain.model.A2UISummonerState

object A2UISummonerParser {
    fun fromDataModel(dataModel: JsonObject): A2UISummonerState? {
        val surfaceId = dataModel.readString("surfaceId") ?: return null
        val contents = dataModel.getAsJsonArray("contents") ?: return null
        var gold: Int? = null
        var actions: List<String> = emptyList()
        for (item in contents) {
            val entry = runCatching { item.asJsonObject }.getOrNull() ?: continue
            val key = entry.readString("key") ?: continue
            if (key != "summoner") continue
            val summonerEntries = entry.getAsJsonArray("valueMap") ?: continue
            for (summonerItem in summonerEntries) {
                val summonerEntry = runCatching { summonerItem.asJsonObject }.getOrNull() ?: continue
                when (summonerEntry.readString("key")) {
                    "gold" -> {
                        gold =
                            summonerEntry.readInt("valueNumber")
                                ?: summonerEntry.readInt("literalNumber")
                                ?: summonerEntry.readInt("valueString")
                    }
                    "actions" -> {
                        actions = parseActions(summonerEntry)
                    }
                }
            }
        }
        if (gold == null && actions.isEmpty()) return null
        return A2UISummonerState(surfaceId = surfaceId, gold = gold, actions = actions)
    }

    private fun parseActions(entry: JsonObject): List<String> {
        val items = entry.getAsJsonArray("valueMap") ?: return emptyList()
        val pairs = mutableListOf<Pair<Int, String>>()
        for (item in items) {
            val obj = runCatching { item.asJsonObject }.getOrNull() ?: continue
            val label =
                obj.readString("valueString")
                    ?: obj.readString("literalString")
                    ?: continue
            val rawKey = obj.readString("key") ?: ""
            val idx = rawKey.removePrefix("a").toIntOrNull() ?: pairs.size
            val trimmed = label.trim()
            if (trimmed.isNotEmpty()) {
                pairs.add(idx to trimmed)
            }
        }
        return pairs.sortedBy { it.first }.map { it.second }.take(6)
    }

    private fun JsonObject.readString(key: String): String? =
        runCatching { get(key)?.takeIf { !it.isJsonNull }?.asString }.getOrNull()

    private fun JsonObject.readInt(key: String): Int? {
        val element = get(key) ?: return null
        if (element.isJsonNull) return null
        val primitive = element.asJsonPrimitive ?: return null
        return when {
            primitive.isNumber -> primitive.asDouble.toInt()
            primitive.isString -> primitive.asString.toIntOrNull()
            else -> null
        }
    }
}
