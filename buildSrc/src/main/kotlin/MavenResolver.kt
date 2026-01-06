import org.gradle.api.Project
import org.gradle.kotlin.dsl.repositories
import org.w3c.dom.Document
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.time.Duration.Companion.hours

object MavenResolver {
    private val CACHE_TTL = 12.hours.inWholeMilliseconds
    private const val CACHE_DIR = ".gradle/maven-resolver-cache"

    val DEFAULT_REPOSITORIES: List<Repository> = listOf(
        "https://maven.parchmentmc.org".repository,
        "https://maven.fabricmc.net/".repository,
        "https://maven.neoforged.net/releases/".repository,
        "https://maven.minecraftforge.net/".repository,
        "https://thedarkcolour.github.io/KotlinForForge/".repository
    )

    fun Project.initializationRepositories() =
        repositories { allRepositories.forEach {
            maven {
                url = URI.create(it.url)
                content {
                    if (it.includeGroup != null) includeGroup(it.includeGroup)
                    if (it.includeGroupByRegex != null) includeGroupByRegex(it.includeGroupByRegex)
                }
            }
        } }

    fun resolve(rootDir: File, mcVersion: String, dep: Dependency): String? {
        val baseMc = mcVersion.substringBeforeLast('.')
        val mcAttempts = listOf(mcVersion, baseMc).distinct()
        val excludeList = dep.excludeTags.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        // Объединяем: личный репо + репозитории из JSON + дефолтные
        val searchRepos = allRepositories.map { it.url }.distinct()

        for (mc in mcAttempts) {
            val parts = dep.notation.split(':')
            if (parts.size != 3) continue

            val group = parts[0]
            val artifact = parts[1].replace("{mc}", mc)
            val verTemplate = parts[2]

            // Оверрайд
            dep.versionOverride?.get(mc)?.let {
                val sufix = verTemplate
                    .replace("{version}", it)
                    .replace("{mc}", mc)

                return "$group:$artifact:$sufix"
            }

            var doc: Document? = null
            for (repo in searchRepos) {
                doc = getMetadata(repo, group, artifact, rootDir)
                if (doc != null) break
            }
            if (doc == null) continue

            val versionNodes = doc.getElementsByTagName("version")
            val versions = (0 until versionNodes.length).map { versionNodes.item(it).textContent }

            // --- БЕЗОПАСНЫЙ REGEX ---
            // Создаем паттерн, заменяя токены на группы захвата
            // {version}+{mc} -> ^(.+)\+1\.21\.1$
            val patternString = "^" + verTemplate
                .replace(".", "\\.")
                .replace("+", "\\+")
                .replace("-", "\\-")
                .replace("{version}", "(.+)")
                .replace("{mc}", Regex.escape(mc))
                .replace("{kotlin}", modProperties.kotlinVersion) + "$"

            val patternRegex = Regex(patternString)
            val bestVer = versions.filter { v ->
                // 1. Должно подходить под Regex (шаблон {version})
                if (!patternRegex.matches(v)) return@filter false

                // 2. Версия не должна содержать ни одного слова из списка исключений
                excludeList.none { keyword -> v.contains(keyword, ignoreCase = true) }
            }.lastOrNull() // Берем самую свежую из "чистых"

            if (bestVer != null) return "$group:$artifact:$bestVer"
        }
        return null
    }

    private fun getMetadata(repo: String, group: String, artifact: String, rootDir: File): Document? {
        val cacheDir = File(rootDir, CACHE_DIR).apply { if (!exists()) mkdirs() }
        val cacheFile = File(cacheDir, "$group.$artifact.xml")
        val url = "${repo.trimEnd('/')}/${group.replace('.', '/')}/$artifact/maven-metadata.xml"

        val needsDownload = !cacheFile.exists() || (System.currentTimeMillis() - cacheFile.lastModified() > CACHE_TTL)

        if (needsDownload) {
            try {
                URI(url).toURL().openStream().use { input ->
                    Files.copy(input, cacheFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
            } catch (e: Exception) { return null }
        }

        return try {
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(cacheFile).apply {
                documentElement.normalize()
            }
        } catch (e: Exception) { null }
    }

    val String.repository: Repository get() = Repository(this)
}