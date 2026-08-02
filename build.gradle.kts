import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.TestReport
import org.gradle.api.tasks.wrapper.Wrapper
import org.gradle.jvm.toolchain.JavaToolchainService
import java.nio.file.Files
import java.security.MessageDigest
import java.util.HexFormat
import java.util.jar.JarFile
import java.util.zip.ZipFile

plugins {
    base
}

val engineVersion = providers.gradleProperty("engineVersion").get()
val gdxVersion = providers.gradleProperty("gdxVersion").get()
val lwjglVersion = providers.gradleProperty("lwjglVersion").get()

fun sha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().buffered().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) {
                break
            }
            digest.update(buffer, 0, count)
        }
    }
    return HexFormat.of().formatHex(digest.digest())
}

fun sha256(bytes: ByteArray): String =
    HexFormat.of().formatHex(
        MessageDigest.getInstance("SHA-256").digest(bytes),
    )

providers.gradleProperty("isolatedBuildRoot").orNull?.let { configuredRoot ->
    val isolatedBuildRoot = file(configuredRoot)
    allprojects {
        val projectRelativePath = if (path == ":") {
            "root"
        } else {
            path.removePrefix(":").replace(':', File.separatorChar)
        }
        layout.buildDirectory.set(isolatedBuildRoot.resolve(projectRelativePath))
    }
}

allprojects {
    group = "io.github.balehlah.enginelite"
    version = engineVersion

    tasks.withType<Delete>().configureEach {
        doFirst {
            if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
                targetFiles.files
                    .filter(File::exists)
                    .forEach { target ->
                        target.walkBottomUp().forEach { entry ->
                            entry.setWritable(true, false)
                            runCatching {
                                Files.setAttribute(entry.toPath(), "dos:readonly", false)
                            }
                        }
                    }
            }
        }
    }
}

subprojects {
    apply(plugin = "base")

    repositories {
        mavenCentral()
    }

    pluginManager.withPlugin("java") {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion = JavaLanguageVersion.of(21)
            }
        }

        val sourceSets = extensions.getByType<SourceSetContainer>()
        val javaToolchains = extensions.getByType<JavaToolchainService>()

        tasks.register<Test>("java25CompatibilityTest") {
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            description = "Runs the module test suite on Java 25 while production bytecode stays on Java 21."
            testClassesDirs = sourceSets.named("test").get().output.classesDirs
            classpath = sourceSets.named("test").get().runtimeClasspath
            javaLauncher.set(
                javaToolchains.launcherFor {
                    languageVersion.set(JavaLanguageVersion.of(25))
                },
            )
            shouldRunAfter(tasks.named("test"))
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release = 21
    }

    tasks.withType<Javadoc>().configureEach {
        options.encoding = "UTF-8"
    }

    tasks.withType<Test>().configureEach {
        val randomSeed = providers.gradleProperty("testRandomSeed").orElse("11")
        val includedTags = providers.gradleProperty("includeTags").orNull
            ?.split(",")
            ?.map(String::trim)
            ?.filter(String::isNotEmpty)
            .orEmpty()
        val excludedTags = providers.gradleProperty("excludeTags").orNull
            ?.split(",")
            ?.map(String::trim)
            ?.filter(String::isNotEmpty)
            .orEmpty()

        useJUnitPlatform {
            if (includedTags.isNotEmpty()) {
                includeTags(*includedTags.toTypedArray())
            }
            if (excludedTags.isNotEmpty()) {
                excludeTags(*excludedTags.toTypedArray())
            }
        }

        systemProperty("file.encoding", "UTF-8")
        systemProperty("java.awt.headless", "true")
        systemProperty("junit.jupiter.execution.order.random.seed", randomSeed.get())
        systemProperty(
            "junit.jupiter.testclass.order.default",
            "org.junit.jupiter.api.ClassOrderer\$Random",
        )
        systemProperty(
            "junit.jupiter.testmethod.order.default",
            "org.junit.jupiter.api.MethodOrderer\$Random",
        )

        reports {
            junitXml.required.set(true)
            junitXml.isOutputPerTestCase = true
            html.required.set(true)
        }

        testLogging {
            events("passed", "skipped", "failed")
        }

        doFirst {
            if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
                listOf(
                    binaryResultsDirectory.get().asFile,
                    reports.junitXml.outputLocation.get().asFile,
                    reports.html.outputLocation.get().asFile,
                )
                    .filter(File::exists)
                    .forEach { output ->
                        output.walkBottomUp().forEach { entry ->
                            entry.setWritable(true, false)
                            runCatching {
                                Files.setAttribute(entry.toPath(), "dos:readonly", false)
                            }
                        }
                    }
            }
            logger.lifecycle("JUnit random seed for $path: ${randomSeed.get()}")
        }
    }

    tasks.withType<JavaExec>().configureEach {
        jvmArgs(
            "-Dfile.encoding=UTF-8",
            "-Dstdout.encoding=UTF-8",
            "-Dstderr.encoding=UTF-8",
        )
    }

    tasks.withType<Jar>().configureEach {
        manifest {
            attributes(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version.toString(),
            )
        }
        from(rootProject.layout.projectDirectory.file("LICENSE")) {
            into("META-INF")
        }
        from(rootProject.layout.projectDirectory.file("THIRD_PARTY_NOTICES.md")) {
            into("META-INF")
        }
        from(rootProject.layout.projectDirectory.file("assets/ATTRIBUTION.md")) {
            into("META-INF")
            rename { "ASSET_ATTRIBUTION.md" }
        }
    }
}

val buildModules = listOf(
    project(":engine:core"),
    project(":engine:gdx"),
    project(":desktop"),
    project(":game"),
)
val spikeApplicationName = "engine-lite-spike"

val dependencyLicenseCatalog = layout.projectDirectory.file("gradle/dependency-licenses.txt")
val toolingLicenseCatalog = layout.projectDirectory.file("gradle/tooling-licenses.txt")
val wrapperProperties = layout.projectDirectory.file("gradle/wrapper/gradle-wrapper.properties")
val ciWorkflow = layout.projectDirectory.file(".github/workflows/build.yml")
val dependencyLicenseReport = layout.buildDirectory.file("reports/licenses/dependencies.csv")

val generateDependencyLicenseReport = tasks.register("generateDependencyLicenseReport") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Resolves dependencies and records their origin and license."
    inputs.files(
        dependencyLicenseCatalog,
        toolingLicenseCatalog,
        wrapperProperties,
        ciWorkflow,
        fileTree(layout.projectDirectory) {
            include("**/*.gradle.kts", "gradle.properties")
            exclude("**/build/**", "**/.gradle/**")
        },
    )
    outputs.file(dependencyLicenseReport)
    outputs.upToDateWhen { false }

    doLast {
        fun catalogRows(file: File, fieldCount: Int): List<List<String>> =
            file.readLines(Charsets.UTF_8)
                .asSequence()
                .map(String::trim)
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .map { line ->
                    line.split('|').map(String::trim).also { fields ->
                        check(fields.size == fieldCount) {
                            "Invalid license catalog row in ${file.relativeTo(projectDir)}: $line"
                        }
                    }
                }
                .toList()

        val dependencyRows = catalogRows(dependencyLicenseCatalog.asFile, 5)
        val dependencyByCoordinates = dependencyRows.associateBy { it[0] }
        check(dependencyByCoordinates.size == dependencyRows.size) {
            "Duplicate coordinates in ${dependencyLicenseCatalog.asFile.relativeTo(projectDir)}."
        }

        val reportConfigurationNames = setOf(
            "runtimeClasspath",
            "testRuntimeClasspath",
            "legacyRuntimeClasspath",
            "gdxDesktopNativesSource",
        )
        val resolvedModules = allprojects
            .flatMap { candidateProject ->
                candidateProject.configurations
                    .filter {
                        it.isCanBeResolved &&
                            it.name in reportConfigurationNames
                    }
                    .flatMap { configuration ->
                        configuration.incoming.resolutionResult.allComponents
                            .mapNotNull { component ->
                                component.id as? ModuleComponentIdentifier
                            }
                    }
            }
            .distinctBy { "${it.group}:${it.module}:${it.version}" }
            .sortedWith(
                compareBy(
                    ModuleComponentIdentifier::getGroup,
                    ModuleComponentIdentifier::getModule,
                    ModuleComponentIdentifier::getVersion,
                ),
            )

        val resolvedCoordinates = resolvedModules
            .map { "${it.group}:${it.module}" }
            .toSet()
        val missingLicenses = resolvedCoordinates - dependencyByCoordinates.keys
        val staleLicenses = dependencyByCoordinates.keys - resolvedCoordinates
        check(missingLicenses.isEmpty() && staleLicenses.isEmpty()) {
            buildString {
                appendLine("Dependency license catalog is not synchronized.")
                appendLine("Missing: ${missingLicenses.ifEmpty { setOf("<none>") }}")
                appendLine("Stale: ${staleLicenses.ifEmpty { setOf("<none>") }}")
            }
        }

        val toolingRows = catalogRows(toolingLicenseCatalog.asFile, 7)
        val workflowCatalog = toolingRows
            .filter { it[0] == "workflow" }
            .map { "${it[1]}@${it[2]}" }
            .toSet()
        val workflowReferences = Regex("""uses:\s*([^\s@]+)@([^\s]+)""")
            .findAll(ciWorkflow.asFile.readText(Charsets.UTF_8))
            .map { "${it.groupValues[1]}@${it.groupValues[2]}" }
            .toSet()
        check(workflowReferences == workflowCatalog) {
            buildString {
                appendLine("CI action license catalog is not synchronized.")
                appendLine(
                    "Missing: ${(workflowReferences - workflowCatalog).ifEmpty { setOf("<none>") }}",
                )
                appendLine(
                    "Stale: ${(workflowCatalog - workflowReferences).ifEmpty { setOf("<none>") }}",
                )
            }
        }

        val distributionUrl = java.util.Properties().apply {
            wrapperProperties.asFile.inputStream().use(::load)
        }.getProperty("distributionUrl")
        val wrapperVersion = Regex("""gradle-([0-9.]+)-bin\.zip""")
            .find(distributionUrl)
            ?.groupValues
            ?.get(1)
            ?: error("Unable to extract the Gradle version from $distributionUrl")
        val catalogWrapperVersions = toolingRows
            .filter { it[0] == "wrapper" && it[1] == "gradle-wrapper" }
            .map { it[2] }
            .toSet()
        check(catalogWrapperVersions == setOf(wrapperVersion)) {
            "Gradle Wrapper $wrapperVersion is not synchronized with the tooling license catalog."
        }

        val reportFile = dependencyLicenseReport.get().asFile
        reportFile.parentFile.mkdirs()
        reportFile.writeText(
            buildString {
                appendLine("kind,coordinates,version,license,license_url,source_url,usage")
                resolvedModules.forEach { module ->
                    val coordinates = "${module.group}:${module.module}"
                    val license = dependencyByCoordinates.getValue(coordinates)
                    appendLine(
                        listOf(
                            "module",
                            coordinates,
                            module.version,
                            license[1],
                            license[2],
                            license[3],
                            license[4],
                        ).joinToString(","),
                    )
                }
                toolingRows.forEach { row ->
                    appendLine(
                        listOf(
                            row[0],
                            row[1],
                            row[2],
                            row[3],
                            row[4],
                            row[5],
                            row[6],
                        ).joinToString(","),
                    )
                }
            },
            Charsets.UTF_8,
        )
        logger.lifecycle(
            "Recorded ${resolvedModules.size} resolved modules and " +
                "${toolingRows.size} tooling entries in ${reportFile.relativeTo(projectDir)}.",
        )
    }
}

val verifyAssetAttribution = tasks.register("verifyAssetAttribution") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Rejects assets without an origin and license inventory entry."
    val assetsDirectory = layout.projectDirectory.dir("assets")
    val attributionFile = assetsDirectory.file("ATTRIBUTION.md")
    inputs.dir(assetsDirectory)

    doLast {
        val inventoryPaths = Regex("""\|\s*`([^`]+)`\s*\|""")
            .findAll(attributionFile.asFile.readText(Charsets.UTF_8))
            .map { it.groupValues[1].replace('\\', '/') }
            .toSet()
        val assetPaths = assetsDirectory.asFile
            .walkTopDown()
            .filter { it.isFile && it != attributionFile.asFile }
            .map { it.relativeTo(projectDir).invariantSeparatorsPath }
            .toSet()
        val missing = assetPaths - inventoryPaths
        val stale = inventoryPaths - assetPaths
        check(missing.isEmpty() && stale.isEmpty()) {
            buildString {
                appendLine("Asset attribution inventory is not synchronized.")
                appendLine("Missing: ${missing.ifEmpty { setOf("<none>") }}")
                appendLine("Stale: ${stale.ifEmpty { setOf("<none>") }}")
            }
        }
        logger.lifecycle("Verified asset attribution for ${assetPaths.size} distributable assets.")
    }
}

val inspectJars = tasks.register("inspectJars") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Inspects module JAR boundaries, version metadata and bundled notices."
    dependsOn(
        ":engine:core:jar",
        ":engine:gdx:jar",
        ":desktop:jar",
        ":desktop:legacyJar",
        ":game:jar",
    )
    val report = layout.buildDirectory.file("reports/jars/inspection.txt")
    outputs.file(report)
    outputs.upToDateWhen { false }

    doLast {
        fun jarFrom(projectPath: String, taskName: String): File =
            project(projectPath).tasks.named<Jar>(taskName).get().archiveFile.get().asFile

        val jars = linkedMapOf(
            "engine-core" to jarFrom(":engine:core", "jar"),
            "engine-gdx" to jarFrom(":engine:gdx", "jar"),
            "desktop" to jarFrom(":desktop", "jar"),
            "desktop-legacy" to jarFrom(":desktop", "legacyJar"),
            "game" to jarFrom(":game", "jar"),
        )
        val entriesByJar = jars.mapValues { (_, file) ->
            JarFile(file).use { jar ->
                val version = jar.manifest.mainAttributes.getValue("Implementation-Version")
                check(version == engineVersion) {
                    "${file.name} has Implementation-Version=$version; expected $engineVersion."
                }
                val entries = jar.entries().asSequence().map { it.name }.toSortedSet()
                val requiredNotices = setOf(
                    "META-INF/LICENSE",
                    "META-INF/THIRD_PARTY_NOTICES.md",
                    "META-INF/ASSET_ATTRIBUTION.md",
                )
                check(entries.containsAll(requiredNotices)) {
                    "${file.name} is missing notices: ${requiredNotices - entries}"
                }
                entries
            }
        }

        check("engine/api/EngineVersion.class" in entriesByJar.getValue("engine-core")) {
            "engine:core must package the stable engine.api baseline."
        }
        val engineGdxClasses = entriesByJar.getValue("engine-gdx")
            .filter { it.endsWith(".class") }
            .toSet()
        val requiredEngineGdxClasses = setOf(
            "engine/incubator/gdx/input/GdxInputAdapter.class",
            "engine/incubator/gdx/runtime/GdxGameRuntimeLoop.class",
            "engine/incubator/gdx/spike/LibGdxSpikeApplication.class",
            "engine/incubator/gdx/spike/SpikeRunConfiguration.class",
        )
        check(engineGdxClasses.containsAll(requiredEngineGdxClasses)) {
            "engine:gdx is missing spike classes: ${requiredEngineGdxClasses - engineGdxClasses}"
        }
        check(engineGdxClasses.all {
            it.startsWith("engine/incubator/gdx/spike/") ||
                it.startsWith("engine/incubator/gdx/input/") ||
                it.startsWith("engine/incubator/gdx/runtime/")
        }) {
            "engine:gdx may package only isolated spike/input/runtime implementations: " +
                engineGdxClasses.filterNot {
                    it.startsWith("engine/incubator/gdx/spike/") ||
                        it.startsWith("engine/incubator/gdx/input/") ||
                        it.startsWith("engine/incubator/gdx/runtime/")
                }
        }

        val desktopClasses = entriesByJar.getValue("desktop")
            .filter { it.endsWith(".class") }
            .toSet()
        check(
            "engine/incubator/gdx/spike/desktop/Lwjgl3SpikeLauncher.class" in desktopClasses,
        ) {
            "The desktop main JAR must package the LWJGL3 spike launcher."
        }
        check(
            desktopClasses.all {
                it.startsWith("engine/incubator/gdx/spike/desktop/")
            },
        ) {
            "The desktop main JAR must contain only launcher classes: " +
                desktopClasses.filterNot {
                    it.startsWith("engine/incubator/gdx/spike/desktop/")
                }
        }
        check(entriesByJar.getValue("desktop-legacy")
            .filter { it.endsWith(".class") }
            .none { it.startsWith("engine/api/") }
        ) {
            "The transitional desktop JAR must not duplicate stable API classes."
        }
        check(entriesByJar.getValue("game")
            .filter { it.endsWith(".class") }
            .none { it.startsWith("engine/") }
        ) {
            "The game JAR must not package engine implementation classes."
        }

        val spikeAssetsRoot = layout.projectDirectory.dir("assets/spike").asFile
        val requiredSpikeResources = spikeAssetsRoot
            .walkTopDown()
            .filter(File::isFile)
            .map { asset ->
                "spike/${asset.relativeTo(spikeAssetsRoot).invariantSeparatorsPath}"
            }
            .toSet()
        check(requiredSpikeResources.isNotEmpty()) {
            "The spike must package at least one owned acceptance asset."
        }
        check(
            entriesByJar.getValue("engine-gdx").containsAll(requiredSpikeResources),
        ) {
            "engine:gdx is missing spike resources: " +
                (requiredSpikeResources - entriesByJar.getValue("engine-gdx"))
        }
        check(
            entriesByJar.getValue("desktop").intersect(requiredSpikeResources).isEmpty(),
        ) {
            "Spike resources must have a single owner (engine:gdx), not desktop."
        }

        val reportFile = report.get().asFile
        reportFile.parentFile.mkdirs()
        reportFile.writeText(
            buildString {
                appendLine("Engine Lite JAR inspection")
                appendLine("version=$engineVersion")
                jars.forEach { (label, file) ->
                    val classes = entriesByJar.getValue(label).count { it.endsWith(".class") }
                    appendLine("$label=${file.name}; classes=$classes; notices=present")
                }
                appendLine("stable-api-owner=engine-core")
                appendLine("engine-gdx=isolated-spike-input-runtime-and-assets")
                appendLine("desktop-main=lwjgl3-launcher")
                appendLine("desktop-legacy=no-stable-api-duplication")
                appendLine("game=no-engine-classes")
                appendLine(
                    "spike-resources=${requiredSpikeResources.sorted().joinToString(",")}",
                )
            },
            Charsets.UTF_8,
        )
        logger.lifecycle("Inspected ${jars.size} JARs; report: ${reportFile.relativeTo(projectDir)}.")
    }
}

val distributionVerificationReport =
    layout.buildDirectory.file("reports/distribution/verification.txt")
val historicalMacOsAllowlist =
    layout.projectDirectory.file("gradle/macos-historical-allowlist.txt")
val supportedDesktopReferenceReport =
    layout.buildDirectory.file("reports/platform/macos-reference-audit.txt")

val verifySupportedDesktopReferences = tasks.register("verifySupportedDesktopReferences") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description =
        "Rejects current documentation that promises the superseded desktop platform."
    val documentation = fileTree(layout.projectDirectory) {
        include("*.md", "docs/**/*.md", "third_party/**/*.md")
        exclude("**/build/**", "**/.gradle/**")
    }
    inputs.files(documentation, historicalMacOsAllowlist)
    outputs.file(supportedDesktopReferenceReport)
    outputs.upToDateWhen { false }

    doLast {
        val historicalReferences = historicalMacOsAllowlist.asFile
            .readLines(Charsets.UTF_8)
            .asSequence()
            .map(String::trim)
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .map { line ->
                val fields = line.split('|').map(String::trim)
                check(fields.size == 3) {
                    "Invalid historical platform allowlist row: $line"
                }
                fields
            }
            .toList()
        historicalReferences.forEach { reference ->
            val file = layout.projectDirectory.file(reference[0]).asFile
            check(file.isFile) {
                "Historical platform allowlist points to a missing file: ${reference[0]}"
            }
            if (reference[1] != "*") {
                check(
                    file.readLines(Charsets.UTF_8).any {
                        it.contains(reference[1])
                    },
                ) {
                    "Historical marker ${reference[1]} is missing from ${reference[0]}."
                }
            }
        }

        val historicalByPath = historicalReferences.groupBy { it[0] }
        val restrictedReference = Regex(
            """(?i)macos|natives-macos|\.dylib|xstartonfirstthread|três sistemas operacionais""",
        )
        val negativeContractTerms = listOf(
            "não suport",
            "substitu",
            "remov",
            "exclu",
            "ausência",
            "proibid",
            "não entra",
            "não é",
            "upstream",
            "absent",
            "forbidden",
            "exclude",
            "must not",
            "does not",
            "not ship",
        )
        val findings = mutableListOf<String>()
        val violations = mutableListOf<String>()
        documentation.files.sortedBy(File::invariantSeparatorsPath).forEach { file ->
            val relativePath = file.relativeTo(projectDir).invariantSeparatorsPath
            file.readLines(Charsets.UTF_8).forEachIndexed { index, line ->
                if (!restrictedReference.containsMatchIn(line)) {
                    return@forEachIndexed
                }
                val historical = historicalByPath[relativePath].orEmpty().any {
                    it[1] == "*" || line.contains(it[1])
                }
                val negativeContract = negativeContractTerms.any {
                    line.contains(it, ignoreCase = true)
                }
                val classification = when {
                    historical -> "historical"
                    negativeContract -> "negative-contract"
                    else -> "VIOLATION"
                }
                findings += "$relativePath:${index + 1}|$classification|${line.trim()}"
                if (!historical && !negativeContract) {
                    violations += "$relativePath:${index + 1}: ${line.trim()}"
                }
            }
        }
        check(violations.isEmpty()) {
            "Current documentation contains an unsupported platform promise:\n" +
                violations.joinToString("\n")
        }

        val report = supportedDesktopReferenceReport.get().asFile
        report.parentFile.mkdirs()
        report.writeText(
            buildString {
                appendLine("Engine Lite desktop platform reference audit")
                appendLine("current-supported=Windows,Linux")
                appendLine("historical-allowlist=${historicalReferences.size}")
                appendLine("violations=0")
                findings.forEach { appendLine(it) }
            },
            Charsets.UTF_8,
        )
        logger.lifecycle(
            "Verified current Windows/Linux documentation; historical references: " +
                historicalReferences.size,
        )
    }
}

val verifyDistribution = tasks.register("verifyDistribution") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Builds and verifies the real installed and ZIP spike distributions."
    dependsOn(
        generateDependencyLicenseReport,
        verifyAssetAttribution,
        verifySupportedDesktopReferences,
        ":engine:core:apiCheck",
        ":desktop:buildSpikeDistribution",
        inspectJars,
    )
    outputs.file(distributionVerificationReport)
    outputs.upToDateWhen { false }

    doLast {
        val desktopProject = project(":desktop")
        val installDirectory = desktopProject.layout.buildDirectory
            .dir("install/$spikeApplicationName")
            .get()
            .asFile
        val desktopJar = desktopProject.tasks.named<Jar>("jar")
            .get()
            .archiveFile
            .get()
            .asFile
        val engineGdxJar = project(":engine:gdx").tasks.named<Jar>("jar")
            .get()
            .archiveFile
            .get()
            .asFile
        val zip = desktopProject.tasks.named<Zip>("distZip")
            .get()
            .archiveFile
            .get()
            .asFile
        val curatedGdxNativesJar = desktopProject.tasks
            .named<Jar>("curateGdxDesktopNatives")
            .get()
            .archiveFile
            .get()
            .asFile

        val requiredInstalledFiles = setOf(
            "LICENSE",
            "THIRD_PARTY_NOTICES.md",
            "assets/ATTRIBUTION.md",
            "third_party/README.md",
            "third_party/licenses/Apache-2.0.txt",
            "third_party/licenses/GLFW-Zlib.txt",
            "third_party/licenses/jemalloc-BSD-2-Clause.txt",
            "third_party/licenses/JLayer-LGPL-2.1.txt",
            "third_party/licenses/JOrbis-LGPL-2.0-or-later.txt",
            "third_party/licenses/LWJGL-BSD-3-Clause.txt",
            "third_party/licenses/OpenAL-Soft-LGPL-2.0-or-later.txt",
            "third_party/licenses/stb-MIT-or-Public-Domain.txt",
            "bin/$spikeApplicationName",
            "bin/$spikeApplicationName.bat",
            "lib/${desktopJar.name}",
            "lib/${engineGdxJar.name}",
            "lib/${curatedGdxNativesJar.name}",
        )
        val installedFiles = installDirectory
            .walkTopDown()
            .filter(File::isFile)
            .map { it.relativeTo(installDirectory).invariantSeparatorsPath }
            .toSet()
        check(installedFiles.containsAll(requiredInstalledFiles)) {
            "Installed distribution is missing: ${requiredInstalledFiles - installedFiles}"
        }
        val requiredLicenseFiles = requiredInstalledFiles.filter {
            it.startsWith("third_party/licenses/")
        }
        check(
            requiredLicenseFiles.all {
                installDirectory.resolve(it).length() > 0L
            },
        ) {
            "Installed third-party license texts must not be empty."
        }
        check(
            installedFiles.any {
                it.startsWith("lib/jlayer-") && it.endsWith(".jar")
            } &&
                installedFiles.any {
                    it.startsWith("lib/jorbis-") && it.endsWith(".jar")
                },
        ) {
            "The LWJGL3 audio backend requires the JLayer and JOrbis decoder JARs."
        }
        check(
            installedFiles.any {
                it.startsWith("lib/gdx-backend-lwjgl3-") && it.endsWith(".jar")
            },
        ) {
            "Installed distribution is missing the libGDX LWJGL3 backend."
        }
        val originalGdxDesktopNatives = installedFiles.filter {
            it.startsWith("lib/gdx-platform-") && it.endsWith("-natives-desktop.jar")
        }
        check(originalGdxDesktopNatives.isEmpty()) {
            "The uncurated libGDX desktop native JAR must not be distributed: " +
                originalGdxDesktopNatives
        }
        val curatedGdxNatives = installedFiles.filter {
            it == "lib/gdx-platform-$gdxVersion-natives-windows-linux.jar"
        }
        check(curatedGdxNatives.size == 1) {
            "Installed distribution must contain exactly one curated libGDX native JAR: " +
                curatedGdxNatives
        }
        check(
            installedFiles.any {
                it.startsWith("lib/lwjgl-") && it.endsWith(".jar")
            },
        ) {
            "Installed distribution is missing LWJGL runtime modules."
        }
        val requiredLwjglModules = listOf(
            "lwjgl",
            "lwjgl-glfw",
            "lwjgl-jemalloc",
            "lwjgl-openal",
            "lwjgl-opengl",
            "lwjgl-stb",
        )
        val requiredNativePlatforms = setOf(
            "natives-linux",
            "natives-linux-arm32",
            "natives-linux-arm64",
            "natives-windows",
            "natives-windows-x86",
        )
        val invalidLwjglNativeSets = requiredLwjglModules.mapNotNull { module ->
            val prefix = "lib/$module-$lwjglVersion-"
            val classifiers = installedFiles
                .filter { it.startsWith(prefix) && it.endsWith(".jar") }
                .map { it.removePrefix(prefix).removeSuffix(".jar") }
                .filter { it.startsWith("natives-") }
                .toSet()
            if (classifiers == requiredNativePlatforms) {
                null
            } else {
                "$module expected=$requiredNativePlatforms observed=$classifiers"
            }
        }
        check(invalidLwjglNativeSets.isEmpty()) {
            "Installed distribution has an invalid LWJGL native inventory: " +
                invalidLwjglNativeSets
        }

        val unixLauncher = installDirectory.resolve("bin/$spikeApplicationName")
        val windowsLauncher = installDirectory.resolve("bin/$spikeApplicationName.bat")
        check(
            unixLauncher.readText(Charsets.UTF_8)
                .contains("--enable-native-access=ALL-UNNAMED") &&
                windowsLauncher.readText(Charsets.UTF_8)
                    .contains("--enable-native-access=ALL-UNNAMED"),
        ) {
            "Both installed launchers must enable native access for LWJGL."
        }
        check(
            !unixLauncher.readText(Charsets.UTF_8).contains("-XstartOnFirstThread") &&
                !windowsLauncher.readText(Charsets.UTF_8).contains("-XstartOnFirstThread"),
        ) {
            "Platform-specific macOS launcher flags must not be distributed."
        }

        val forbiddenDistributedFiles = installedFiles.filter { path ->
            val normalized = path.lowercase()
            val fileName = normalized.substringAfterLast('/')
            normalized.contains("natives-macos") ||
                normalized.endsWith(".dylib") ||
                fileName == "opengl32.dll" ||
                fileName == "libgallium_wgl.dll"
        }
        check(forbiddenDistributedFiles.isEmpty()) {
            "Distribution contains forbidden macOS or Mesa files: $forbiddenDistributedFiles"
        }
        val jarPayloads = installedFiles
            .filter { it.startsWith("lib/") && it.endsWith(".jar") }
            .associateWith { path ->
                ZipFile(installDirectory.resolve(path)).use { archive ->
                    archive.entries().asSequence()
                        .filterNot { it.isDirectory }
                        .map { it.name }
                        .toSet()
                }
            }
        val forbiddenJarPayloads = jarPayloads.flatMap { (jar, entries) ->
            entries.filter { entry ->
                val normalized = entry.lowercase()
                normalized.contains("natives-macos") ||
                    normalized.endsWith(".dylib") ||
                    normalized.endsWith("opengl32.dll") ||
                    normalized.endsWith("libgallium_wgl.dll")
            }.map { entry -> "$jar!/$entry" }
        }
        check(forbiddenJarPayloads.isEmpty()) {
            "Distribution JARs contain forbidden macOS or Mesa payloads: " +
                forbiddenJarPayloads
        }
        val curatedPayload = jarPayloads.getValue(
            "lib/gdx-platform-$gdxVersion-natives-windows-linux.jar",
        ).filterNot { it.startsWith("META-INF/") }.toSet()
        val expectedCuratedPayload = setOf(
            "gdx.dll",
            "gdx64.dll",
            "libgdx64.so",
            "libgdxarm.so",
            "libgdxarm64.so",
            "libgdxriscv64.so",
        )
        check(curatedPayload == expectedCuratedPayload) {
            "Curated libGDX native JAR differs from its Windows/Linux allowlist: " +
                curatedPayload
        }

        check(zip.isFile && zip.length() > 0L) {
            "Distribution ZIP was not produced: $zip"
        }
        val zipRoot = "${zip.name.removeSuffix(".zip")}/"
        val zipFileHashes = ZipFile(zip).use { archive ->
            archive.entries().asSequence()
                .filterNot { it.isDirectory }
                .associate { entry ->
                    check(entry.name.startsWith(zipRoot)) {
                        "ZIP entry escapes the expected distribution root: ${entry.name}"
                    }
                    entry.name.removePrefix(zipRoot) to
                        archive.getInputStream(entry).use { sha256(it.readBytes()) }
                }
        }
        val zipEntries = zipFileHashes.keys.map { "$zipRoot$it" }.toSet()
        val requiredZipSuffixes = requiredInstalledFiles.map { "/$it" }.toSet()
        val missingZipFiles = requiredZipSuffixes.filterNot { suffix ->
            zipEntries.any { it.endsWith(suffix) }
        }
        check(missingZipFiles.isEmpty()) {
            "Distribution ZIP is missing: $missingZipFiles"
        }
        check(zipFileHashes.keys == installedFiles) {
            "ZIP and installed distribution inventories differ. ZIP-only: " +
                "${zipFileHashes.keys - installedFiles}; installed-only: " +
                "${installedFiles - zipFileHashes.keys}"
        }
        val installedFileHashes = installedFiles.associateWith { path ->
            sha256(installDirectory.resolve(path))
        }
        val mismatchedZipPayloads = installedFiles.filter { path ->
            zipFileHashes.getValue(path) != installedFileHashes.getValue(path)
        }
        check(mismatchedZipPayloads.isEmpty()) {
            "ZIP payloads differ from the verified installed files: $mismatchedZipPayloads"
        }

        val reportFile = distributionVerificationReport.get().asFile
        reportFile.parentFile.mkdirs()
        reportFile.writeText(
            buildString {
                appendLine("Engine Lite spike distribution verification")
                appendLine("version=$engineVersion")
                appendLine(
                    "installed=${installDirectory.absolutePath.replace('\\', '/')}",
                )
                appendLine("zip=${zip.absolutePath.replace('\\', '/')}")
                appendLine("zip-bytes=${zip.length()}")
                appendLine("main-class=engine.incubator.gdx.spike.desktop.Lwjgl3SpikeLauncher")
                appendLine("required-files=present")
                appendLine("third-party-license-texts=present")
                appendLine("audio-decoders=present")
                appendLine("libgdx-backend=present")
                appendLine("desktop-natives=curated-windows-linux")
                appendLine(
                    "desktop-natives-source-sha256=" +
                        "f4847981d27c6524a30f5665036ec8c11f48c8eda7610bb63f742de95ffe1970",
                )
                appendLine(
                    "desktop-natives-curated-sha256=" +
                        sha256(curatedGdxNativesJar),
                )
                appendLine("lwjgl-runtime=present")
                appendLine("lwjgl-linux-windows-natives=exact")
                appendLine("macos-natives=absent")
                appendLine("mesa-runtime=absent")
                appendLine("zip-installed-byte-parity=present")
                appendLine("native-access-enabled=true")
            },
            Charsets.UTF_8,
        )
        logger.lifecycle(
            "Verified installed distribution and ${zip.name}; report: " +
                reportFile.absolutePath,
        )
    }
}

tasks.wrapper {
    gradleVersion = "9.6.1"
    distributionType = Wrapper.DistributionType.BIN
    distributionSha256Sum = "9c0f7faeeb306cb14e4279a3e084ca6b596894089a0638e68a07c945a32c9e14"
    networkTimeout = 30_000
    validateDistributionUrl = true
}

val verifyNoBinInput = tasks.register("verifyNoBinInput") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Fails when a Gradle source set reads the removed legacy bin/ directory."

    doLast {
        val legacyBin = layout.projectDirectory.dir("bin").asFile.canonicalFile.toPath()
        val offenders = allprojects.flatMap { project ->
            val sourceSets = project.extensions.findByType(SourceSetContainer::class.java)
                ?: return@flatMap emptyList()

            sourceSets.flatMap { sourceSet ->
                sourceSet.allSource.srcDirs
                    .map { it.canonicalFile }
                    .filter { it.toPath().startsWith(legacyBin) }
                    .map { "${project.path}:${sourceSet.name} -> $it" }
            }
        }

        check(offenders.isEmpty()) {
            "The build must not read bin/. Offending source sets:\n${offenders.joinToString("\n")}"
        }

        logger.lifecycle("Verified: no Gradle source set reads bin/.")
    }
}

val verifyRootDoesNotPublishJar = tasks.register("verifyRootDoesNotPublishJar") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Ensures the root project remains an aggregator and has no monolithic jar task."

    doLast {
        check(tasks.findByName("jar") == null) {
            "The root project must not apply a JVM publication plugin or create a monolithic jar."
        }
        logger.lifecycle("Verified: the root project has no jar task.")
    }
}

val verifyLegacyClassParity = tasks.register("verifyLegacyClassParity") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Compares Gradle outputs with the class inventory captured before bin/ removal."
    dependsOn(
        ":engine:core:classes",
        ":desktop:legacyClasses",
        ":game:classes",
    )

    doLast {
        val inventory = layout.projectDirectory
            .file("gradle/legacy-class-baseline.txt")
            .asFile
            .readLines()
        val sourceCount = inventory
            .first { it.startsWith("# source-count:") }
            .substringAfter(":")
            .trim()
            .toInt()
        val expectedClasses = inventory
            .asSequence()
            .map(String::trim)
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .toSortedSet()
        val currentSourceCount = layout.projectDirectory
            .dir("src")
            .asFile
            .walkTopDown()
            .count { it.isFile && it.extension == "java" }
        val classRoots = listOf(
            project(":engine:core")
                .layout.buildDirectory.dir("classes/java/main").get().asFile,
            project(":desktop")
                .layout.buildDirectory.dir("classes/java/legacy").get().asFile,
            project(":game")
                .layout.buildDirectory.dir("classes/java/main").get().asFile,
        )
        val actualClasses = classRoots
            .filter { it.exists() }
            .flatMap { root ->
                root.walkTopDown()
                    .filter { it.isFile && it.extension == "class" }
                    .map { it.relativeTo(root).invariantSeparatorsPath }
                    .filterNot {
                        it.startsWith("engine/api/") ||
                            it.startsWith("engine/incubator/runtime/input/") ||
                            it.startsWith("engine/incubator/runtime/lifecycle/") ||
                            it.startsWith("engine/incubator/runtime/time/")
                    }
                    .toList()
            }
            .toSortedSet()

        check(currentSourceCount == sourceCount) {
            "Legacy source count changed: expected $sourceCount, found $currentSourceCount."
        }
        check(actualClasses == expectedClasses) {
            val missing = expectedClasses - actualClasses
            val unexpected = actualClasses - expectedClasses
            buildString {
                appendLine("Gradle output differs from the pre-removal bin/ inventory.")
                appendLine("Missing: ${missing.ifEmpty { setOf("<none>") }}")
                appendLine("Unexpected: ${unexpected.ifEmpty { setOf("<none>") }}")
            }
        }

        logger.lifecycle(
            "Verified legacy parity: $currentSourceCount sources -> ${actualClasses.size} classes.",
        )
    }
}

val test = tasks.register("test") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs all module tests and the Issue #10 build-boundary gates."
    dependsOn(
        buildModules.map { "${it.path}:test" },
        ":engine:core:verifyBackendIndependence",
        verifyNoBinInput,
        verifyRootDoesNotPublishJar,
        verifyLegacyClassParity,
        verifyDistribution,
    )
}

val aggregateTestReport = tasks.register<TestReport>("aggregateTestReport") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Publishes one HTML report containing every module test result."
    destinationDirectory.set(layout.buildDirectory.dir("reports/tests/aggregate"))
    testResults.from(
        buildModules.map { module ->
            module.tasks.withType<Test>().matching { it.name == "test" }
        },
    )
}

val verifyJUnitReports = tasks.register("verifyJUnitReports") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Fails unless the JUnit XML and HTML reports required by Issue #11 exist."
    dependsOn(
        ":engine:core:test",
        ":engine:gdx:test",
        ":desktop:test",
    )

    doLast {
        val testedModules = listOf(
            project(":engine:core"),
            project(":engine:gdx"),
            project(":desktop"),
        )
        val missingReports = testedModules.flatMap { module ->
            val buildDirectory = module.layout.buildDirectory.get().asFile
            val xmlDirectory = buildDirectory.resolve("test-results/test")
            val htmlIndex = buildDirectory.resolve("reports/tests/test/index.html")
            buildList {
                if (!xmlDirectory
                        .walkTopDown()
                        .any { it.isFile && it.extension.equals("xml", ignoreCase = true) }
                ) {
                    add("${module.path}: JUnit XML in $xmlDirectory")
                }
                if (!htmlIndex.isFile) {
                    add("${module.path}: HTML report at $htmlIndex")
                }
            }
        }

        check(missingReports.isEmpty()) {
            "Required JUnit reports were not published:\n${missingReports.joinToString("\n")}"
        }
        logger.lifecycle(
            "Verified JUnit XML and HTML reports for engine:core, engine:gdx and desktop.",
        )
    }
}

test.configure {
    dependsOn(verifyJUnitReports)
    finalizedBy(aggregateTestReport)
}

val verifyJava25Reports = tasks.register("verifyJava25Reports") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Fails unless the Java 25 compatibility reports required by ADR-001 exist."
    dependsOn(
        ":engine:core:java25CompatibilityTest",
        ":engine:gdx:java25CompatibilityTest",
        ":desktop:java25CompatibilityTest",
    )

    doLast {
        val testedModules = listOf(
            project(":engine:core"),
            project(":engine:gdx"),
            project(":desktop"),
        )
        val missingReports = testedModules.flatMap { module ->
            val buildDirectory = module.layout.buildDirectory.get().asFile
            val xmlDirectory = buildDirectory.resolve("test-results/java25CompatibilityTest")
            val htmlIndex = buildDirectory
                .resolve("reports/tests/java25CompatibilityTest/index.html")
            buildList {
                if (!xmlDirectory
                        .walkTopDown()
                        .any { it.isFile && it.extension.equals("xml", ignoreCase = true) }
                ) {
                    add("${module.path}: Java 25 JUnit XML in $xmlDirectory")
                }
                if (!htmlIndex.isFile) {
                    add("${module.path}: Java 25 HTML report at $htmlIndex")
                }
            }
        }

        check(missingReports.isEmpty()) {
            "Required Java 25 compatibility reports were not published:\n" +
                missingReports.joinToString("\n")
        }
        logger.lifecycle(
            "Verified Java 25 JUnit XML and HTML reports for " +
                "engine:core, engine:gdx and desktop.",
        )
    }
}

tasks.register("java25CompatibilityTest") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs all module tests on Java 25 and verifies their reports."
    dependsOn(
        buildModules.map { "${it.path}:java25CompatibilityTest" },
        verifyJava25Reports,
    )
}

tasks.named("check") {
    dependsOn(
        buildModules.map { "${it.path}:check" },
        test,
    )
}

tasks.named<Delete>("clean") {
    dependsOn(subprojects.map { "${it.path}:clean" })
}

tasks.register("run") {
    group = "application"
    description = "Runs the documented transitional legacy demo."
    dependsOn(":game:run")
}

tasks.register("legacyDemo") {
    group = "application"
    description = "Explicit transitional alias for the Java2D demo."
    dependsOn(":game:legacyDemo")
}

tasks.register("legacyDemoSmoke") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Initializes and cleanly shuts down the transitional Java2D demo."
    dependsOn(":game:legacyDemoSmoke")
}

val buildSpikeDistribution = tasks.register("buildSpikeDistribution") {
    group = "distribution"
    description = "Builds the canonical installed and ZIP libGDX/LWJGL3 spike distribution."
    dependsOn(":desktop:buildSpikeDistribution")
}

val smokeSpikeDistribution = tasks.register("smokeSpikeDistribution") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description =
        "Runs the installed spike package from an external CWD and hashes its evidence."
    dependsOn(":desktop:generateSpikeEvidenceManifest")
}

tasks.register("recordSpikeDistributionHash") {
    group = "distribution"
    description = "Records the canonical ZIP SHA-256 before the Java 21/25 smokes."
    dependsOn(":desktop:recordSpikeDistributionHash")
}

tasks.register("verifySpikeDistributionHash") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Verifies the canonical ZIP SHA-256 after the Java 21/25 smokes."
    dependsOn(":desktop:verifySpikeDistributionHash")
}

tasks.register("spikeDistribution") {
    group = "distribution"
    description = "Canonical alias for building the packaged libGDX/LWJGL3 spike."
    dependsOn(buildSpikeDistribution)
}

tasks.register("spikeDistributionSmoke") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Canonical alias for smoke-testing the packaged libGDX/LWJGL3 spike."
    dependsOn(smokeSpikeDistribution)
}
