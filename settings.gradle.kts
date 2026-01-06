import groovy.json.JsonSlurper

// Generate project structure //

val propsFile = File(rootDir, "ModProperties.json")
if (propsFile.exists()) {
    val props = JsonSlurper().parse(propsFile) as Map<String, Any>
    val versionsData = props["versions"] as? Map<String, List<String>> ?: emptyMap()

    val allowedNames = versionsData.flatMap { (loader, mcVers) ->
        mcVers.map { "$it-$loader" }
    }.toSet()

    val versionsDir = File(rootDir, "versions")
    if (!versionsDir.exists()) versionsDir.mkdirs()

    allowedNames.forEach {
        val dir = File(versionsDir, it)
        if (!dir.exists()) { dir.mkdirs(); println("[Dir-Manager | INFO 📂] Created: $it") }
    }

    // Create src/main directories //
    val srcGroup = (props["group"] as String).replace('.', '/')
    val srcModId = props["id"]

    listOf(
        "src/main/java/$srcGroup/$srcModId", "src/main/kotlin/$srcGroup/$srcModId",
        "src/main/resources/assets/$srcModId", "src/main/resources/data/$srcModId"
    ).forEach {
        val dir = File(rootDir, it)
        if (!dir.exists()) { dir.mkdirs(); println("[Dir-Manager | INFO 📂] Created: $it") }
    }

    // 3. Удаляем лишние папки (которых нет в JSON)
    versionsDir.listFiles()?.filter { it.isDirectory }?.forEach { dir ->
        if (dir.name !in allowedNames) {
            // ИСПОЛЬЗУЕМ ЭТО: Удаляет сразу и со всем мусором внутри
            val deleted = dir.deleteRecursively()
            if (deleted) {
                println("[Dir-Manager | INFO 🗑️] Deleted obsolete directory: ${dir.name}")
            }
        }
    }
}

// ========================== //

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()

        // Stonecutter DEV //
        maven("https://maven.kikugie.dev/releases")

        // Architectury DEV //
        maven("https://maven.architectury.dev")

        // Fabric //
        maven("https://maven.fabricmc.net/")

        // NeoForge //
        maven("https://maven.neoforged.net/releases/")

        // Forge //
        maven("https://maven.minecraftforge.net/")
    }
}

plugins {
    // Stonecutter DEV //
    id("dev.kikugie.stonecutter") version "0.7.11"
}

stonecutter {
    centralScript = "build.gradle.kts"
    kotlinController = true

    // Generate version //
    // Just create a folder in 'versions' with name 'version-loader' and "Sync All Gradle Projects"
    // Example: 'versions/1.21.1-fabric'
    // Thx 'TheHollowHorizon' for that idea!
    create(rootProject) {
        rootProject.projectDir.resolve("versions")
            .listFiles()
            .filter { it.isDirectory }
            .forEach {
                val name = it.name
                val loader = name.substringAfterLast('-')

                version(it.name)

                gradle.beforeProject { if (this.name == name) extensions.extraProperties["loom.platform"] = loader }
            }
    }
}