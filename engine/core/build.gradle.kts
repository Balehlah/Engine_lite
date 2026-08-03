import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.tasks.bundling.Jar
import org.gradle.jvm.toolchain.JavaToolchainService
import java.util.jar.JarFile

plugins {
    `java-library`
}

description = "Backend-neutral Engine Lite core extracted from the legacy source tree."

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.14.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

sourceSets {
    main {
        java {
            setSrcDirs(
                listOf(
                    rootProject.file("src"),
                    project.file("src/main/java"),
                ),
            )
            include(
                "engine/api/**/*.java",
                "engine/core/Timer.java",
                "engine/incubator/assets/**/*.java",
                "engine/incubator/runtime/input/**/*.java",
                "engine/incubator/runtime/lifecycle/**/*.java",
                "engine/incubator/runtime/time/**/*.java",
                "engine/math/Vector2.java",
                "engine/util/**/*.java",
            )
        }
    }
}

val javaToolchains = extensions.getByType<JavaToolchainService>()
val java21Launcher = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(21))
}
val publicApiBaseline = rootProject.layout.projectDirectory
    .file("gradle/public-api-baseline.txt")
val publicApiDependencies = rootProject.layout.projectDirectory
    .file("gradle/public-api-dependencies.txt")
val currentApiReport = layout.buildDirectory.file("reports/api/current.txt")
val boundaryReport = layout.buildDirectory.file("reports/api/boundaries.txt")
val engineCoreJar = tasks.named<Jar>("jar")

fun runJdkTool(executable: File, arguments: List<String>): String {
    val process = ProcessBuilder(listOf(executable.absolutePath) + arguments)
        .redirectErrorStream(true)
        .start()
    val output = process.inputStream
        .bufferedReader(Charsets.UTF_8)
        .use { it.readText() }
    val exitCode = process.waitFor()
    check(exitCode == 0) {
        "${executable.name} failed with exit code $exitCode:\n$output"
    }
    return output.replace("\r\n", "\n")
}

fun jdkExecutable(tool: String): File {
    val suffix = if (
        System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
    ) {
        ".exe"
    } else {
        ""
    }
    return java21Launcher.get().metadata.installationPath
        .file("bin/$tool$suffix")
        .asFile
}

fun renderPublicApi(jarFile: File): String {
    val candidateClasses = JarFile(jarFile).use { jar ->
        jar.entries()
            .asSequence()
            .map { it.name }
            .filter {
                it.startsWith("engine/api/") &&
                    it.endsWith(".class") &&
                    !it.endsWith("package-info.class") &&
                    !it.endsWith("module-info.class")
            }
            .map { it.removeSuffix(".class").replace('/', '.') }
            .sorted()
            .toList()
    }

    val javap = jdkExecutable("javap")
    val stableSignatures = candidateClasses.mapNotNull { className ->
        val signature = runJdkTool(
            javap,
            listOf("-classpath", jarFile.absolutePath, "-protected", "-s", className),
        )
            .lineSequence()
            .filterNot { it.startsWith("Compiled from ") }
            .joinToString("\n")
            .trim()
        val declaration = signature.lineSequence().firstOrNull()?.trim().orEmpty()
        if (declaration.startsWith("public ") || declaration.startsWith("protected ")) {
            className to signature
        } else {
            null
        }
    }
    check(stableSignatures.isNotEmpty()) {
        "No public or protected classes were found under engine.api.* in ${jarFile.name}."
    }

    return buildString {
        appendLine("# Engine Lite public API baseline")
        appendLine("# JDK 21 javap -protected -s; update only with coordinator approval.")
        stableSignatures.forEach { (className, signature) ->
            appendLine()
            appendLine("## $className")
            appendLine(signature)
        }
    }.replace("\r\n", "\n")
}

val apiDump = tasks.register("apiDump") {
    group = "build setup"
    description = "Writes the reviewed engine.api.* signature baseline."
    dependsOn(engineCoreJar)
    inputs.file(engineCoreJar.flatMap { it.archiveFile })
    outputs.file(publicApiBaseline)

    doLast {
        val baselineFile = publicApiBaseline.asFile
        baselineFile.parentFile.mkdirs()
        baselineFile.writeText(
            renderPublicApi(engineCoreJar.get().archiveFile.get().asFile),
            Charsets.UTF_8,
        )
        logger.lifecycle(
            "Wrote public API baseline to ${baselineFile.relativeTo(rootProject.projectDir)}.",
        )
    }
}

val verifyPublicApiBoundaries = tasks.register("verifyPublicApiBoundaries") {
    group = "verification"
    description = "Rejects internal/prototype signature leaks and unapproved api dependencies."
    dependsOn(engineCoreJar)
    inputs.files(
        publicApiDependencies,
        engineCoreJar.flatMap { it.archiveFile },
    )
    outputs.file(boundaryReport)

    doLast {
        val jarFile = engineCoreJar.get().archiveFile.get().asFile
        val jdepsOutput = runJdkTool(
            jdkExecutable("jdeps"),
            listOf(
                "--api-only",
                "-verbose:class",
                "-filter:none",
                "-include",
                """engine\.api\..*""",
                jarFile.absolutePath,
            ),
        )
        val signatureDependencies = jdepsOutput
            .lineSequence()
            .map(String::trim)
            .filter { line ->
                line.contains("->") &&
                    line.substringBefore("->").trim().startsWith("engine.api.")
            }
            .map { line ->
                val fields = line.substringAfter("->")
                    .trim()
                    .split(Regex("""\s+"""))
                fields.first() to fields.getOrElse(1) { "<unknown-module>" }
            }
            .distinct()
            .sortedBy { it.first }
            .toList()
        val forbiddenSignatureDependencies = signatureDependencies
            .filterNot { (type, module) ->
                type.startsWith("engine.api.") ||
                    (module.startsWith("java.") && !type.startsWith("jdk.internal."))
            }
            .map { it.first }
        check(forbiddenSignatureDependencies.isEmpty()) {
            "Stable API signatures leak non-stable types: $forbiddenSignatureDependencies"
        }

        val allowedApiDependencies = publicApiDependencies.asFile
            .readLines(Charsets.UTF_8)
            .map(String::trim)
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .toSet()
        val declaredApiDependencies = configurations.named("api").get().dependencies
            .map { dependency ->
                when (dependency) {
                    is ProjectDependency -> "project:${dependency.path}"
                    else -> "${dependency.group}:${dependency.name}"
                }
            }
            .toSet()
        check(declaredApiDependencies == allowedApiDependencies) {
            buildString {
                appendLine("Gradle api dependencies are not synchronized with the contract.")
                appendLine(
                    "Unapproved: ${
                        (declaredApiDependencies - allowedApiDependencies)
                            .ifEmpty { setOf("<none>") }
                    }",
                )
                appendLine(
                    "Stale: ${
                        (allowedApiDependencies - declaredApiDependencies)
                            .ifEmpty { setOf("<none>") }
                    }",
                )
            }
        }

        val reportFile = boundaryReport.get().asFile
        reportFile.parentFile.mkdirs()
        reportFile.writeText(
            buildString {
                appendLine("stable-package=engine.api.*")
                appendLine(
                    "signature-dependencies=${
                        signatureDependencies
                            .joinToString(",") { (type, module) -> "$type@$module" }
                            .ifEmpty { "<none>" }
                    }",
                )
                appendLine(
                    "gradle-api-dependencies=${
                        declaredApiDependencies.joinToString(",").ifEmpty { "<none>" }
                    }",
                )
                appendLine("internal-or-prototype-leaks=none")
            },
            Charsets.UTF_8,
        )
        logger.lifecycle(
            "Verified public API boundaries; report: " +
                reportFile.relativeTo(rootProject.projectDir),
        )
    }
}

val apiCheck = tasks.register("apiCheck") {
    group = "verification"
    description = "Fails when engine.api.* differs from the reviewed signature baseline."
    dependsOn(engineCoreJar, verifyPublicApiBoundaries)
    inputs.files(
        publicApiBaseline,
        engineCoreJar.flatMap { it.archiveFile },
    )
    outputs.file(currentApiReport)

    doLast {
        val current = renderPublicApi(engineCoreJar.get().archiveFile.get().asFile)
        val reportFile = currentApiReport.get().asFile
        reportFile.parentFile.mkdirs()
        reportFile.writeText(current, Charsets.UTF_8)

        val expected = publicApiBaseline.asFile
            .readText(Charsets.UTF_8)
            .replace("\r\n", "\n")
        check(current == expected) {
            val expectedLines = expected.lines()
            val currentLines = current.lines()
            val firstDifference = (0 until maxOf(expectedLines.size, currentLines.size))
                .firstOrNull { index ->
                    expectedLines.getOrNull(index) != currentLines.getOrNull(index)
                }
                ?: 0
            buildString {
                appendLine("Stable API differs from gradle/public-api-baseline.txt.")
                appendLine("First difference at line ${firstDifference + 1}:")
                appendLine("Expected: ${expectedLines.getOrNull(firstDifference) ?: "<missing>"}")
                appendLine("Actual:   ${currentLines.getOrNull(firstDifference) ?: "<missing>"}")
                appendLine(
                    "Run :engine:core:apiDump only for an approved compatibility change.",
                )
            }
        }
        logger.lifecycle("Verified stable API against gradle/public-api-baseline.txt.")
    }
}

val verifyBackendIndependence = tasks.register("verifyBackendIndependence") {
    group = "verification"
    description = "Rejects AWT, Swing, libGDX and LWJGL references from engine:core."

    val coreSources = sourceSets.main.get().allJava
    inputs.files(coreSources)

    doLast {
        val forbiddenReferences = listOf(
            "java.awt",
            "javax.swing",
            "com.badlogic.gdx",
            "org.lwjgl",
        )
        val sourceOffenders = coreSources.files.flatMap { source ->
            val contents = source.readText(Charsets.UTF_8)
            forbiddenReferences
                .filter(contents::contains)
                .map { reference -> "${source.relativeTo(rootProject.projectDir)} -> $reference" }
        }
        val dependencyOffenders = configurations
            .flatMap { configuration -> configuration.dependencies }
            .map { dependency -> "${dependency.group.orEmpty()}:${dependency.name}" }
            .filter { notation ->
                notation.contains("gdx", ignoreCase = true) ||
                    notation.contains("lwjgl", ignoreCase = true) ||
                    notation.contains("awt", ignoreCase = true)
            }
            .distinct()

        check(sourceOffenders.isEmpty() && dependencyOffenders.isEmpty()) {
            buildString {
                appendLine("engine:core must remain independent of AWT and graphics backends.")
                appendLine("Source references: ${sourceOffenders.ifEmpty { listOf("<none>") }}")
                appendLine("Dependencies: ${dependencyOffenders.ifEmpty { listOf("<none>") }}")
            }
        }

        logger.lifecycle(
            "Verified engine:core boundary across ${coreSources.files.size} source files.",
        )
    }
}

tasks.named("test") {
    dependsOn(verifyBackendIndependence)
}

tasks.named("check") {
    dependsOn(
        verifyBackendIndependence,
        apiCheck,
    )
}
