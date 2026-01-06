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

val subProject = project.name
val mcVersion = subProject.substringBefore('-')
val cLoader = subProject.substringAfter('-')
val props = modProperties

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
    when(cLoader) { Loader.FABRIC.str -> fabric(); Loader.NEO_FORGE.str -> neoForge(); Loader.FORGE.str -> forge() }
}

base { archivesName.set("${props.name}-$cLoader") }
/*
configurations.all {
    if (cLoader == "forge" || cLoader == "neoforge") {
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

    registrationFabric(cLoader)
    registrationNeoforge(cLoader)
    registrationForge(cLoader)

    registrationDependencies(props)
    setupDependencies(loom, mcVersion, cLoader)
}

loom {
    mixin {
        useLegacyMixinAp = true
        defaultRefmapName.set("${props.id}-refmap.json")
    }
    accessWidenerPath = rootProject.file("src/main/resources/${props.id}.accesswidener")

    decompilers { getByName("vineflower") { options.put("mark-corresponding-synthetics", "1") } }

    if (cLoader == "neoforge") neoForge {  }
    if (cLoader == "forge") forge { mixinConfigs("${props.id}-forge.mixins.json") }

    runConfigs.all {
        props.runningConfiguration.forEach { (env, args) ->
            if (env.str == environment) {
                args.forEach { (key, value) -> programArg("--$key=$value") }
            }
        }

        runDir("../../run/$mcVersion")
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
    configurations = listOf(shadowBundle)
    archiveClassifier = "dev-shadow"
    // Исключаем лишние мета-файлы из зависимостей
    exclude("META-INF/maven/**", "META-INF/gradle/**")
}

tasks.remapJar {
    injectAccessWidener = true
    inputFile = tasks.shadowJar.get().archiveFile
    archiveClassifier = null
    dependsOn(tasks.shadowJar)
}

tasks.jar { archiveClassifier = "dev" }

// Сборка и копирование в общую папку
val buildAndCollect = tasks.register<Copy>("buildAndCollect") {
    group = "versioned"
    from(tasks.remapJar.get().archiveFile, tasks.remapSourcesJar.get().archiveFile)
    into(rootProject.layout.buildDirectory.dir("libs/${props.version}/$cLoader"))
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
        "loader" to cLoader,
        "mod_author" to props.authors.joinToString(", "),
        "resourcepack_format" to resourcepackFormat(mcVersion),
        "mod_group" to props.group,
        "mod_mixins" to props.mixins,
        "mod_fabric_entrypoint_client" to props.fabricEntrypoints.client,
        "mod_fabric_entrypoint" to props.fabricEntrypoints.common,
        "mod_fabric_entrypoint_server" to props.fabricEntrypoints.server
    )

    inputs.properties(tokens)

    // Используем expand для поддержки ${mod_id}
    filesMatching(files) {
        expand(tokens)
    }
}

// Финальное имя файла
tasks.withType<AbstractArchiveTask>().configureEach {
    archiveFileName.set("${props.name}-$cLoader-$mcVersion-${props.version}.jar")
}