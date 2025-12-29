package ru.benos.gradle

import kotlinx.serialization.json.Json
import org.gradle.internal.impldep.kotlinx.serialization.SerialName
import org.gradle.internal.impldep.kotlinx.serialization.Serializable
import java.io.File
import java.io.FileNotFoundException

// Data //

@Serializable
enum class Mapping {
    @SerialName("official_mojang_mappings")
    OFFICIAL_MOJANG_MAPPINGS,
    @SerialName("parchment_mc")
    PARCHMENT_MC,
    YARN
}

@Serializable
enum class Loader(val loaderName: String) {
    FABRIC("fabric"),
    @SerialName("neoforge")
    NEO_FORGE("neoforge"),
    FORGE("forge")
}

@Serializable
data class Dependency(
    val id: String,
    @SerialName("version_override")
    val versionOverride: Map<String, String>? = null
)

@Serializable
data class ModProperties(
    val name: String,
    val id: String,
    val version: String,
    val description: String,
    val authors: List<String>,

    val mappings: List<Mapping> = listOf(Mapping.OFFICIAL_MOJANG_MAPPINGS, Mapping.PARCHMENT_MC),
    val group: String,

    val depends: Map<Loader, List<Dependency>> = emptyMap()
)

// Parsing //

private const val MOD_PROPERTIES_FILE = "ModProperties.json"

private val JSON = Json { ignoreUnknownKeys=true; isLenient=true; encodeDefaults=true }

val modProperties: ModProperties by lazy {
    val file = File(MOD_PROPERTIES_FILE)
    if (!file.exists) {
        FileNotFoundException("File 'ModProperties.json' NOT exists!")
        println("Downloading new...")

        downloadModPropertiesJson()
        modProperties
    }

    JSON.decodeFromString<ModProperties>(file.readText)
}

private fun downloadModPropertiesJson() {}