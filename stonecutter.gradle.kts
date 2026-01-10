plugins {
    id("dev.kikugie.stonecutter")
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
}

stonecutter active "1.21.1-forge"

stonecutter parameters {
    constants.match(
        node.metadata.project.substringAfter('-'),
        modProperties.versions.map { it.key.str }
    )
}