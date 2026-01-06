import MavenResolver.DEFAULT_REPOSITORIES
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileNotFoundException

// Data //

@Serializable
data class EntrypointData(
    val client: String,
    val common: String = "",
    val server: String = ""
)

@Serializable
enum class Mapping {
    @SerialName("official_mojang_mappings") OFFICIAL_MOJANG_MAPPINGS,
    @SerialName("parchment_mc") PARCHMENT_MC,
    @SerialName("yarn") YARN
}

@Serializable
enum class Loader(val str: String) {
    @SerialName("fabric") FABRIC("fabric"),
    @SerialName("neoforge") NEO_FORGE("neoforge"),
    @SerialName("forge") FORGE("forge")
}

@Serializable
enum class DependencyType(val str: String) {
    @SerialName("mod_runtime") MOD_RUNTIME("modRuntimeOnly"),
    @SerialName("runtime") RUNTIME("runtimeOnly"),

    @SerialName("mod_implementation") MOD_IMPLEMENTATION("modImplementation"),
    @SerialName("implementation") IMPLEMENTATION("implementation"),

    @SerialName("mapping") MAPPING("mapping"),
    @SerialName("forge") FORGE("forge"),
    @SerialName("neoforge") NEO_FORGE("neoForge")
}

@Serializable
data class Repository(
    val url: String,
    @SerialName("include_group") val includeGroup: String? = null,
    @SerialName("include_group_by_regex") val includeGroupByRegex: String? = null
)

@Serializable
data class Dependency(
    val notation: String,
    val repo: String? = null,
    val side: Loader? = null,
    val type: DependencyType = DependencyType.MOD_IMPLEMENTATION,

    val include: Boolean = false,
    val exclude: Boolean = false,

    @SerialName("exclude_tags") val excludeTags: String = "",

    @SerialName("version_override")
    val versionOverride: Map<String, String>? = null
)

@Serializable
enum class RunningSide(val str: String) {
    @SerialName("all") ALL("all"),
    @SerialName("common") COMMON("common"),

    @SerialName("client") CLIENT("client"),
    @SerialName("server") SERVER("string"),
}

@Serializable
data class ModProperties(
    val name: String,
    val id: String,
    val version: String,
    val description: String,
    val authors: List<String>,
    val license: String,

    val versions: Map<Loader, List<String>>,

    val side: Map<RunningSide, Boolean>,

    @SerialName("fabric_entrypoints")
    val fabricEntrypoints: EntrypointData,

    val mixins: String? = null,

    val mappings: List<Mapping> = listOf(Mapping.OFFICIAL_MOJANG_MAPPINGS),
    val group: String,

    val repositories: List<Repository> = emptyList(),
    val depends: List<Dependency> = emptyList(),

    @SerialName("running_configuration")
    val runningConfiguration: Map<RunningSide, Map<String, String>> = emptyMap(),

    @SerialName("kotlin")
    val kotlinVersion: String = "2.0.21",

    @SerialName("kotlin_for_forge")
    val kotlinForForgeVersion: String,
)

// Parsing //

private const val MOD_PROPERTIES_FILE = "ModProperties.json"

private val JSON = Json { ignoreUnknownKeys=true; isLenient=true; encodeDefaults=true }

val modProperties: ModProperties by lazy {
    val file = File(MOD_PROPERTIES_FILE)
    if (!file.exists) {
        throw FileNotFoundException("File 'ModProperties.json' NOT exists!")
        println("Downloading new...")

        downloadModPropertiesJson()

        println("Let's again!")
    }

    JSON.decodeFromString<ModProperties>(file.readText)
}

val allRepositories: List<Repository> get() = DEFAULT_REPOSITORIES + modProperties.repositories

private fun downloadModPropertiesJson() {}

fun resourcepackFormat(mcVersion: String): Int = when(mcVersion) {
    "1.21.11" -> 75
    "1.21.10", "1.21.9" -> 69
    "1.21.8", "1.21.7" -> 64
    "1.21.6" -> 63
    "1.21.5" -> 55
    "1.21.4" -> 46
    "1.21.3", "1.21.2" -> 42
    "1.21.1", "1.21" -> 34
    "1.20.6", "1.20.5" -> 32
    "1.20.4", "1.20.3" -> 22
    "1.20.2" -> 18
    "1.20.1", "1.20" -> 15
    "1.19.2", "1.19.1", "1.19" -> 9
    "1.18.2", "1.18.1", "1.18" -> 8
    "1.17.1", "1.17" -> 7
    "1.16.5", "1.16.4", "1.16.3", "1.16.2" -> 6
    "1.16.1", "1.16", "1.15.2", "1.15.1", "1.15" -> 5
    "1.14.4", "1.14.3", "1.14.2", "1.14.1", "1.14", "1.13.2", "1.13.1", "1.13" -> 4
    "1.12.2", "1.12.1", "1.12", "1.11.2", "1.11.1", "1.11" -> 3
    "1.10.2", "1.10.1", "1.10", "1.9.4", "1.9.3", "1.9.2", "1.9.1", "1.9" -> 2
    "1.8.9", "1.8.8", "1.8.7", "1.8.6", "1.8.5", "1.8.4", "1.8.3", "1.8.2", "1.8.1", "1.8", "1.7.10", "1.7.7", "1.7.6", "1.7.5", "1.7.4", "1.7.2", "1.6.4", "1.6.2", "1.6.1" -> 1

    else -> 0
}
