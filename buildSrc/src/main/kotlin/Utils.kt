import java.io.File

val File.exists: Boolean get() = this@exists.exists()
val File.readText: String get() = this@readText.readText()

val String.getLoader: Loader get() =
    when (this) {
        "fabric" -> Loader.FABRIC
        "neoforge" -> Loader.NEO_FORGE
        "forge" -> Loader.FORGE
        else -> error("Unknown loader '$this'!")
    }
