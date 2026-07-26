import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.TestReport
import org.gradle.api.tasks.wrapper.Wrapper
import java.nio.file.Files

plugins {
    base
}

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
}

val buildModules = listOf(
    project(":engine:core"),
    project(":engine:gdx"),
    project(":desktop"),
    project(":game"),
)

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
    )
}

val aggregateTestReport = tasks.register<TestReport>("aggregateTestReport") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Publishes one HTML report containing every module test result."
    destinationDirectory.set(layout.buildDirectory.dir("reports/tests/aggregate"))
    testResults.from(buildModules.map { module -> module.tasks.withType<Test>() })
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
