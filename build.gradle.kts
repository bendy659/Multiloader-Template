@file:OptIn(ExperimentalStdlibApi::class)

import DependsManager.registrationDependencies
import DependsManager.registrationFabric
import DependsManager.registrationForge
import DependsManager.registrationMappings
import DependsManager.registrationNeoforge
import DependsManager.setupDependencies
import MavenResolver.initializationRepositories
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("dev.architectury.loom")
    id("architectury-plugin")
    id("com.github.johnrengelman.shadow")

    kotlin("jvm") version modProperties.kotlinVersion
    kotlin("plugin.serialization") version modProperties.kotlinVersion
}

val subProject: String        = project.name
val mcVersion:  String        = subProject.substringBefore('-')
val cLoader:    String        = subProject.substringAfter('-')
val loader:     Loader        = cLoader.getLoader
val props:      ModProperties = modProperties

group = props.group
version = props.version

val javaVersion = if (stonecutter.eval(mcVersion, ">=1.20.5")) JavaVersion.VERSION_21 else JavaVersion.VERSION_17

buildscript {
    dependencies {
        // Принудительно обновляем библиотеку метаданных для всех плагинов сборки
        classpath("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.9.0")
    }
}

configurations.all { resolutionStrategy { force("org.jetbrains.kotlin:kotlin-metadata-jvm:${props.kotlinVersion}") } }

architectury {
    minecraft = mcVersion
    when(loader) {
        Loader.FABRIC -> fabric()
        Loader.NEO_FORGE -> neoForge()
        Loader.FORGE -> forge()
    }
}

base { archivesName = "${props.name}-${loader.str}" }
/*
configurations.all {
    if (loader.str == "forge" || loader.str == "neoforge") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
    }
}
*/
java {
    withSourcesJar()
    targetCompatibility = javaVersion
    sourceCompatibility = javaVersion
}

initializationRepositories()

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")

    DependsManager.cleanup()
    registrationMappings(props)

    registrationFabric(loader.str)
    registrationNeoforge(loader.str)
    registrationForge(loader.str)

    registrationDependencies(props)
    setupDependencies(loom, mcVersion, loader.str)
}

loom {
    mixin {
        useLegacyMixinAp = true
        defaultRefmapName.set("${props.id}-refmap.json")
    }
    accessWidenerPath = rootProject.file("src/main/resources/${props.id}.accesswidener")

    decompilers { getByName("vineflower") { options.put("mark-corresponding-synthetics", "1") } }

    when (loader) {
        Loader.NEO_FORGE -> neoForge { }
        Loader.FORGE -> forge { mixinConfigs("${props.id}-forge.mixins.json") }
        else -> { }
    }

    runConfigs.all {
        props.runningConfiguration.forEach { (env, args) ->
            if (env.str == environment) {
                args.forEach { (key, value) -> programArg("--$key=$value") }
            }
        }

        runDir("../../run")
    }
}

// Настройка компиляции
tasks.withType<JavaCompile>().configureEach { options.release.set(javaVersion.majorVersion.toInt()) }

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        val jvmVersion = when(javaVersion) {
            JavaVersion.VERSION_21 -> JvmTarget.JVM_21
            JavaVersion.VERSION_17 -> JvmTarget.JVM_17
            else -> null
        }

        jvmTarget = jvmVersion
    }
}

// Shadow Jar логика
val shadowBundle: Configuration by configurations.creating { isCanBeConsumed = false; isCanBeResolved = true }

tasks.shadowJar {
    archiveClassifier = "dev-shadow"
    configurations = listOf(shadowBundle)
    from(sourceSets.main.get().output)
    exclude("META-INF/maven/**", "META-INF/gradle/**")
}

tasks.remapJar {
    injectAccessWidener = true
    inputFile = tasks.shadowJar.flatMap { it.archiveFile }
    archiveClassifier = null
}

tasks.jar { archiveClassifier = "dev" }

// Сборка и копирование в общую папку
val buildAndCollect = tasks.register<Copy>("buildAndCollect") {
    group = "versioned"
    from(
        tasks.remapJar.flatMap { it.archiveFile },
        tasks.remapSourcesJar.flatMap { it.archiveFile }
    )
    into(rootProject.layout.buildDirectory.dir("libs/${loader.str}"))
    dependsOn(tasks.build)
}

// Ярлыки для активной версии в IDE
if (stonecutter.current.isActive) {
    rootProject.tasks.register("buildActive") {
        group = "project"
        dependsOn(buildAndCollect)
    }
    rootProject.tasks.register("runActive") {
        group = "project"
        dependsOn(tasks.named("runClient"))
    }
}

tasks.processResources {
    val excludePattern = mutableListOf<String>()

    if (loader != Loader.FABRIC) { excludePattern += "fabric.mod.json"; excludePattern += "*.accesswidener" }
    if (loader != Loader.FORGE) excludePattern += "META-INF/mods.toml"
    if (loader != Loader.NEO_FORGE) excludePattern += "META-INF/neoforge.mods.toml"

    Loader.values().forEach {
        val lName = it.str
        if (lName != loader.str) excludePattern += "${props.id}-$lName.mixins.json"
    }

    exclude(excludePattern)

    val files = listOf(
        "pack.mcmeta",
        "fabric.mod.json", "META-INF/*.toml",
        "*.mixins.json",
        "*.common"
    )
    val tokens = mapOf(
        "mod_name" to props.name,
        "mod_id" to props.id,
        "mod_desc" to props.description,
        "mod_version" to props.version,
        "mod_license" to props.license,
        "mc_version" to mcVersion,
        "loader" to loader.str,
        "mod_author" to props.authors.joinToString(", "),
        "resourcepack_format" to resourcepackFormat(mcVersion),
        "mod_group" to props.group,
        "mod_mixins" to props.mixins,
        "mod_fabric_entrypoint_client" to props.fabricEntrypoints.client,
        "mod_fabric_entrypoint" to props.fabricEntrypoints.common,
        "mod_fabric_entrypoint_server" to props.fabricEntrypoints.server
    )

    inputs.properties(tokens)
    filesMatching(files) { expand(tokens) }
}