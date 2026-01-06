import net.fabricmc.loom.api.LoomGradleExtensionAPI
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

object DependsManager {
    private val LOGGER = LogManager("Depend-Manager")

    private val QUEUE_DEPENDS: MutableList<Dependency> = mutableListOf()
    private val QUEUE_MAPPINGS: MutableList<Mapping> = mutableListOf()

    val Dependency.addToQueueDepends: Unit get() = QUEUE_DEPENDS.addLast(this@addToQueueDepends)
    val Mapping.addToMappingQueue: Unit get() = QUEUE_MAPPINGS.addLast(this@addToMappingQueue)

    fun cleanup() { QUEUE_DEPENDS.clear(); QUEUE_MAPPINGS.clear() }

    fun Project.registrationMappings(props: ModProperties) =
        props.mappings.forEach { it.addToMappingQueue }

    fun Project.registrationDependencies(props: ModProperties) =
        props.depends.forEach { it.addToQueueDepends }

    fun Project.registrationFabric(cLoader: String) {
        if (cLoader != Loader.FABRIC.str) return

        Dependency(
            notation = "net.fabricmc:fabric-loader:{version}",
            side = Loader.FABRIC,
            type = DependencyType.MOD_IMPLEMENTATION
        ).addToQueueDepends

        Dependency(
            notation = "net.fabricmc.fabric-api:fabric-api:{version}+{mc}",
            side = Loader.FABRIC,
            type = DependencyType.MOD_IMPLEMENTATION
        ).addToQueueDepends

        Dependency(
            //notation = "net.fabricmc:fabric-language-kotlin:{version}",
            notation = "net.fabricmc:fabric-language-kotlin:{version}+kotlin.{kotlin}",
            side = Loader.FABRIC,
            type = DependencyType.MOD_IMPLEMENTATION,
            include = true
        ).addToQueueDepends
    }

    fun Project.registrationNeoforge(cLoader: String) {
        if (cLoader != Loader.NEO_FORGE.str) return

        Dependency(
            notation = "net.neoforged:neoforge:{version}",
            excludeTags = "beta, alpha, snapshot",
            side = Loader.NEO_FORGE,
            type = DependencyType.NEO_FORGE
        ).addToQueueDepends

        Dependency(
            notation = "thedarkcolour:kotlinforforge-neoforge:${modProperties.kotlinForForgeVersion}",
            side = Loader.NEO_FORGE,
            type = DependencyType.MOD_IMPLEMENTATION
        ).addToQueueDepends
    }

    fun Project.registrationForge(cLoader: String) {
        if (cLoader != Loader.FORGE.str) return

        Dependency(
            notation = "net.minecraftforge:forge:{mc}-{version}",
            side = Loader.FORGE,
            type = DependencyType.FORGE
        ).addToQueueDepends

        Dependency(
            notation = "thedarkcolour:kotlinforforge:${modProperties.kotlinForForgeVersion}",
            side = Loader.FORGE,
            type = DependencyType.MOD_IMPLEMENTATION
        ).addToQueueDepends
    }

    fun Project.setupDependencies(loom: LoomGradleExtensionAPI, mcVersion: String, cLoader: String) {
        LOGGER.info("Starting initialization Dependencies...")

        // Matching dependencies library //
        val forGlobal = mutableListOf<Dependency>()
        val forFabric = mutableListOf<Dependency>()
        val forNeoforge = mutableListOf<Dependency>()
        val forForge = mutableListOf<Dependency>()

        QUEUE_DEPENDS.forEach {
            if (it.type == DependencyType.MAPPING) return@forEach

            when (it.side) {
                null -> forGlobal.add(it)
                Loader.FABRIC -> forFabric.add(it)
                Loader.NEO_FORGE -> forNeoforge.add(it)
                Loader.FORGE -> forForge.add(it)
            }
        }

        LOGGER.info("Registration:\n |- Global: ${forGlobal.size}\n |- For 'fabric': ${forFabric.size}\n |- For 'neoforge': ${forNeoforge.size}\n \\- For 'forge': ${forForge.size}")

        dependencies {
            // Registration Global //
            LOGGER.info("Registration Global...")
            forGlobal.forEach { setupDepend(mcVersion, it) }

            // Registration mappings //
            LOGGER.info("Registration 'mappings'...")
            add("mappings", loom.layered {
                QUEUE_MAPPINGS.forEach {
                    when (it) {
                        Mapping.OFFICIAL_MOJANG_MAPPINGS -> {
                            LOGGER.info("Setup 'Official Mojang' mapping...")
                            officialMojangMappings()
                        }
                        Mapping.PARCHMENT_MC -> {
                            LOGGER.info("Setup 'ParchmentMC' mapping...")

                            val dep = "org.parchmentmc.data:parchment-{mc}:{version}".dependency
                            val resolve = MavenResolver.resolve(project.rootDir, mcVersion, dep)

                            if (resolve == null)
                                LOGGER.error("Could not resolve 'ParchmentMC' mapping for '$mcVersion'!")
                            else
                                parchment("$resolve@zip")
                        }
                        Mapping.YARN -> {
                            LOGGER.info("Setup 'Yarn' mapping...")

                            val dep = "net.fabricmc:yarn:{version}".dependency
                            val resolve = MavenResolver.resolve(project.rootDir, mcVersion, dep)

                            if (resolve == null)
                                LOGGER.error("Could not resolve 'Yarn' mapping for '$mcVersion'!")
                            else
                                mappings("$resolve:v2")
                        }
                    }
                }
            })

            // Registration 'fabric' //
            LOGGER.info("Registration '$cLoader'...")
            when(cLoader) {
                Loader.FABRIC.str -> forFabric.forEach { setupDepend(mcVersion, it) }
                Loader.NEO_FORGE.str -> forNeoforge.forEach { setupDepend(mcVersion, it) }
                Loader.FORGE.str -> forForge.forEach { setupDepend(mcVersion, it) }
                else -> LOGGER.warn("Unknown loader '$cLoader'")
            }
        }
    }

    private fun Project.setupDepend(mcVersion: String, dependency: Dependency) {
        val configuration = dependency.type.str
        val resolve = MavenResolver.resolve(project.rootDir, mcVersion, dependency)
        if (resolve == null) {
            LOGGER.error("Could not resolve '${dependency.notation}'!")
            return
        }

        LOGGER.info("Setup dependency '$resolve")
        dependencies.add(configuration, resolve)

        if (dependency.include) {
            LOGGER.info("\\- Include...")
            dependencies.add("include", resolve)
        }
        if (dependency.exclude) {
            LOGGER.info("\\- Exclude...")
            dependencies.add("exclude", resolve)
        }
    }

    private val String.dependency: Dependency get() = Dependency(this)
}