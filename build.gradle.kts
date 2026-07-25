import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.wrapper.Wrapper
import java.nio.file.Files

plugins {
    base
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
        systemProperty("file.encoding", "UTF-8")
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
            layout.projectDirectory.dir("engine/core/build/classes/java/main").asFile,
            layout.projectDirectory.dir("desktop/build/classes/java/legacy").asFile,
            layout.projectDirectory.dir("game/build/classes/java/main").asFile,
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
