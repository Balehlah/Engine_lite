import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.TestReport
import org.gradle.api.tasks.wrapper.Wrapper
import org.gradle.jvm.toolchain.JavaToolchainService
import java.nio.file.Files
import java.util.jar.JarFile

plugins {
    base
}

val engineVersion = providers.gradleProperty("engineVersion").get()

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
        check(entriesByJar.getValue("engine-gdx").none { it.endsWith(".class") }) {
            "engine:gdx must remain empty until Issue #14 authorizes implementation."
        }
        check(entriesByJar.getValue("desktop").none { it.endsWith(".class") }) {
            "The desktop main JAR must not duplicate the transitional legacy backend."
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
                appendLine("engine-gdx=empty")
                appendLine("desktop-main=empty")
                appendLine("desktop-legacy=no-stable-api-duplication")
                appendLine("game=no-engine-classes")
            },
            Charsets.UTF_8,
        )
        logger.lifecycle("Inspected ${jars.size} JARs; report: ${reportFile.relativeTo(projectDir)}.")
    }
}

val verifyDistribution = tasks.register("verifyDistribution") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs the Issue #13 license, API and artifact gates."
    dependsOn(
        generateDependencyLicenseReport,
        verifyAssetAttribution,
        ":engine:core:apiCheck",
        inspectJars,
    )
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
                    .filterNot { it.startsWith("engine/api/") }
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
        ":desktop:test",
    )

    doLast {
        val testedModules = listOf(
            project(":engine:core"),
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
        logger.lifecycle("Verified JUnit XML and HTML reports for engine:core and desktop.")
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
        ":desktop:java25CompatibilityTest",
    )

    doLast {
        val testedModules = listOf(
            project(":engine:core"),
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
        logger.lifecycle("Verified Java 25 JUnit XML and HTML reports for engine:core and desktop.")
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
