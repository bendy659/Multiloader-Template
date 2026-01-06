plugins {
    `kotlin-dsl`
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
}

repositories {
    mavenCentral()
    gradlePluginPortal()

    maven("https://maven.architectury.dev")
    maven("https://maven.fabricmc.net/")
    maven("https://maven.minecraftforge.net/")
}

dependencies {
    constraints {
        implementation("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.9.0")
    }

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    implementation("architectury-plugin:architectury-plugin.gradle.plugin:3.4-SNAPSHOT")
    implementation("dev.architectury:architectury-loom:1.10-SNAPSHOT")
}

buildscript {
    dependencies {
        classpath("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.9.0")
    }
}