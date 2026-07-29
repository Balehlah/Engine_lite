import org.gradle.api.distribution.DistributionContainer
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip
import org.gradle.jvm.application.tasks.CreateStartScripts
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.HexFormat
import java.util.Properties
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream
import javax.imageio.ImageIO

plugins {
    application
    `java-library`
}

description = "LWJGL3 spike launcher and transitional Java2D desktop backend."

val gdxVersion = providers.gradleProperty("gdxVersion").get()
val lwjglVersion = providers.gradleProperty("lwjglVersion").get()
val isWindows = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
val isMacOs = System.getProperty("os.name").startsWith("Mac", ignoreCase = true)
val spikeApplicationName = "engine-lite-spike"
val spikeMainClass = "engine.incubator.gdx.spike.desktop.Lwjgl3SpikeLauncher"

val legacy = sourceSets.create("legacy") {
    java {
        setSrcDirs(listOf(rootProject.file("src")))
        include("engine/**/*.java")
        exclude(
            "engine/core/Timer.java",
            "engine/math/Vector2.java",
            "engine/util/**/*.java",
        )
    }
}

dependencies {
    implementation(project(":engine:gdx"))
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")
    runtimeOnly("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")

    constraints {
        listOf(
            "lwjgl",
            "lwjgl-glfw",
            "lwjgl-jemalloc",
            "lwjgl-openal",
            "lwjgl-opengl",
            "lwjgl-stb",
        ).forEach { module ->
            implementation("org.lwjgl:$module:$lwjglVersion") {
                because("LWJGL 3.4.1 or newer is required for the Java 25 compatibility gate.")
            }
        }
    }

    add(legacy.implementationConfigurationName, project(":engine:core"))
    testImplementation(platform("org.junit:junit-bom:5.14.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(project(":engine:core"))
    testImplementation(files(legacy.output))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    applicationName = spikeApplicationName
    mainClass = spikeMainClass
    applicationDefaultJvmArgs = buildList {
        add("--enable-native-access=ALL-UNNAMED")
        if (isMacOs) {
            add("-XstartOnFirstThread")
        }
    }
}

tasks.named<CreateStartScripts>("startScripts") {
    doLast {
        fun replaceClasspath(script: File, pattern: Regex, replacement: String) {
            val original = script.readText(Charsets.UTF_8)
            val matches = pattern.findAll(original).count()
            check(matches == 1) {
                "Expected one generated CLASSPATH declaration in $script; found $matches."
            }
            script.writeText(
                pattern.replace(original) { replacement },
                Charsets.UTF_8,
            )
        }

        replaceClasspath(
            windowsScript,
            Regex("""(?m)^set CLASSPATH=.*$"""),
            """set CLASSPATH=%APP_HOME%\lib\*""",
        )
        replaceClasspath(
            unixScript,
            Regex("""(?m)^CLASSPATH=.*$"""),
            """CLASSPATH=${'$'}APP_HOME/lib/*""",
        )
    }
}

extensions.configure<DistributionContainer> {
    named("main") {
        contents {
            from(rootProject.layout.projectDirectory.file("LICENSE"))
            from(rootProject.layout.projectDirectory.file("THIRD_PARTY_NOTICES.md"))
            from(rootProject.layout.projectDirectory.file("assets/ATTRIBUTION.md")) {
                into("assets")
            }
            from(rootProject.layout.projectDirectory.dir("third_party")) {
                into("third_party")
            }
            from(rootProject.layout.projectDirectory.file("LICENSE")) {
                into("third_party/licenses")
                rename { "Apache-2.0.txt" }
            }
        }
    }
}

val legacyJar = tasks.register<Jar>("legacyJar") {
    group = "build"
    description = "Packages the transitional Java2D backend without creating a root monolith."
    archiveClassifier.set("legacy")
    from(legacy.output)
}

configurations.create("legacyElements") {
    isCanBeConsumed = true
    isCanBeResolved = false
    extendsFrom(configurations[legacy.implementationConfigurationName])
    outgoing.artifact(legacyJar)
}

tasks.named("assemble") {
    dependsOn(legacyJar)
}

val installDist = tasks.named<Sync>("installDist")
val distZip = tasks.named<Zip>("distZip")

val buildSpikeDistribution = tasks.register("buildSpikeDistribution") {
    group = "distribution"
    description = "Builds the installed tree and canonical ZIP for the libGDX/LWJGL3 spike."
    dependsOn(
        installDist,
        distZip,
    )
}

val spikeSmokeVariant = providers.gradleProperty("spikeSmokeVariant")
    .orElse("java21")
    .map { variant ->
        require(Regex("""[a-z0-9][a-z0-9-]*""").matches(variant)) {
            "spikeSmokeVariant must contain only lowercase letters, digits and hyphens: $variant"
        }
        variant
    }
val smokeEvidenceDirectory = layout.buildDirectory.dir(
    spikeSmokeVariant.map { variant -> "reports/spike/$variant" },
)
val repositoryDirectory = rootProject.projectDir
    .toPath()
    .toAbsolutePath()
    .normalize()
val smokeTimeoutSeconds = providers.gradleProperty("spikeSmokeTimeoutSeconds")
    .map(String::toLong)
    .orElse(45L)

val smokeSpikeDistribution = tasks.register("smokeSpikeDistribution") {
    group = "verification"
    description =
        "Extracts and runs the distribution ZIP from a temporary CWD and captures smoke evidence."
    dependsOn(distZip)
    inputs.file(distZip.flatMap { it.archiveFile })
    outputs.dir(smokeEvidenceDirectory)
    outputs.upToDateWhen { false }

    doLast {
        val evidenceDirectory = smokeEvidenceDirectory.get().asFile
        check(
            evidenceDirectory.toPath()
                .toAbsolutePath()
                .normalize()
                .startsWith(layout.buildDirectory.get().asFile.toPath()),
        ) {
            "Refusing to clear evidence outside the desktop build directory: " +
                evidenceDirectory
        }
        evidenceDirectory.deleteRecursively()
        check(evidenceDirectory.mkdirs() || evidenceDirectory.isDirectory) {
            "Unable to create smoke evidence directory: $evidenceDirectory"
        }
        val absoluteEvidenceDirectory = evidenceDirectory.toPath()
            .toAbsolutePath()
            .normalize()
        check(absoluteEvidenceDirectory.isAbsolute) {
            "Smoke evidence directory must be absolute: $absoluteEvidenceDirectory"
        }

        val temporaryWorkingDirectory = Files.createTempDirectory(
            "engine-lite-spike-cwd-",
        ).toFile()
        val extractedPackageDirectory = Files.createTempDirectory(
            "engine-lite-spike-package-",
        ).toFile()
        check(
            !temporaryWorkingDirectory.toPath()
                .toAbsolutePath()
                .normalize()
                .startsWith(repositoryDirectory),
        ) {
            "Smoke CWD must be outside the repository: $temporaryWorkingDirectory"
        }

        var spawnedProcess: Process? = null
        try {
            val extractionRoot = extractedPackageDirectory.toPath()
                .toAbsolutePath()
                .normalize()
            val archive = distZip.get().archiveFile.get().asFile
            ZipInputStream(archive.inputStream().buffered()).use { zipInput ->
                while (true) {
                    val entry = zipInput.nextEntry ?: break
                    val target = extractionRoot.resolve(entry.name).normalize()
                    check(target.startsWith(extractionRoot)) {
                        "Distribution ZIP entry escapes extraction root: ${entry.name}"
                    }
                    if (entry.isDirectory) {
                        Files.createDirectories(target)
                    } else {
                        Files.createDirectories(target.parent)
                        Files.copy(
                            zipInput,
                            target,
                            StandardCopyOption.REPLACE_EXISTING,
                        )
                    }
                    zipInput.closeEntry()
                }
            }
            val launcherName =
                "$spikeApplicationName${if (isWindows) ".bat" else ""}"
            val launcherCandidates = extractedPackageDirectory
                .walkTopDown()
                .filter(File::isFile)
                .filter {
                    it.name == launcherName && it.parentFile.name == "bin"
                }
                .toList()
            check(launcherCandidates.size == 1) {
                "Expected exactly one launcher named $launcherName in ${archive.name}; " +
                    "found $launcherCandidates"
            }
            val launcher = launcherCandidates.single()
            val smokeArguments = listOf(
                "--smoke",
                "--evidence-dir=$absoluteEvidenceDirectory",
            )
            val command = when {
                isWindows -> listOf(
                    "cmd.exe",
                    "/d",
                    "/c",
                    "call",
                    launcher.absolutePath,
                ) + smokeArguments
                isMacOs -> listOf(
                    "/bin/sh",
                    launcher.absolutePath,
                ) + smokeArguments
                else -> listOf(
                    "xvfb-run",
                    "--auto-servernum",
                    "--server-args=-screen 0 1920x1080x24",
                    "/bin/sh",
                    launcher.absolutePath,
                ) + smokeArguments
            }

            logger.lifecycle(
                "Running ${archive.name} from $temporaryWorkingDirectory with evidence at " +
                    absoluteEvidenceDirectory,
            )
            val processStdout = absoluteEvidenceDirectory
                .resolve("process.stdout.log")
                .toFile()
            val processStderr = absoluteEvidenceDirectory
                .resolve("process.stderr.log")
                .toFile()
            val processBuilder = ProcessBuilder(command)
                .directory(temporaryWorkingDirectory)
                .redirectOutput(processStdout)
                .redirectError(processStderr)
                .apply {
                    environment()["ALSOFT_DRIVERS"] = "null"
                    environment()["ALSOFT_LOGLEVEL"] = "3"
                    environment()["ALSOFT_LOGFILE"] =
                    absoluteEvidenceDirectory.resolve("openal.log").toString()
                }
            val process = processBuilder.start()
            spawnedProcess = process
            val completed = process.waitFor(smokeTimeoutSeconds.get(), TimeUnit.SECONDS)
            if (!completed) {
                process.destroy()
                if (!process.waitFor(5, TimeUnit.SECONDS)) {
                    process.destroyForcibly()
                    process.waitFor(5, TimeUnit.SECONDS)
                }
                error(
                    "Installed spike exceeded the ${smokeTimeoutSeconds.get()} second " +
                        "smoke timeout. Evidence: $absoluteEvidenceDirectory",
                )
            }
            if (processStdout.length() == 0L) {
                processStdout.writeText("(no stdout)\n", Charsets.UTF_8)
            }
            if (processStderr.length() == 0L) {
                processStderr.writeText("(no stderr)\n", Charsets.UTF_8)
            }
            if (process.exitValue() != 0) {
                logger.error(
                    "Packaged spike stdout:\n" +
                        processStdout.readText(Charsets.UTF_8),
                )
                logger.error(
                    "Packaged spike stderr:\n" +
                        processStderr.readText(Charsets.UTF_8),
                )
            }
            check(process.exitValue() == 0) {
                "Installed spike exited with code ${process.exitValue()}. " +
                    "Evidence: $absoluteEvidenceDirectory"
            }

            val probeLog = absoluteEvidenceDirectory.resolve("probe.log").toFile()
            check(probeLog.isFile) {
                "The spike did not record probe.log."
            }
            val probeContents = probeLog.readText(Charsets.UTF_8)
            val observedWorkingDirectory = probeContents.lineSequence()
                .firstOrNull { " cwd=" in it }
                ?.substringAfter(" cwd=")
                ?.let(Path::of)
                ?.toAbsolutePath()
                ?.normalize()
                ?: error("probe.log does not record the runtime CWD.")
            val expectedWorkingDirectory = temporaryWorkingDirectory.toPath()
                .toAbsolutePath()
                .normalize()
            val observedRealDirectory = observedWorkingDirectory.toRealPath()
            val expectedRealDirectory = expectedWorkingDirectory.toRealPath()
            check(Files.isSameFile(observedRealDirectory, expectedRealDirectory)) {
                "Packaged spike CWD was $observedWorkingDirectory; expected " +
                    "$expectedWorkingDirectory."
            }
            check(!observedRealDirectory.startsWith(repositoryDirectory.toRealPath())) {
                "Packaged spike ran inside the repository: $observedRealDirectory"
            }

            val variant = spikeSmokeVariant.get()
            val expectedJavaMajor =
                Regex("""(?:^|-)java(21|25)(?:-|$)""")
                    .find(variant)
                    ?.groupValues
                    ?.get(1)
            if (expectedJavaMajor != null) {
                check(
                    Regex(
                        """(?m)^\S+\s+java\.version=${expectedJavaMajor}(?:[.\s]|$)""",
                    ).containsMatchIn(probeContents),
                ) {
                    "Smoke variant $variant did not run on Java $expectedJavaMajor."
                }
            }

            val openAlLog = absoluteEvidenceDirectory.resolve("openal.log").toFile()
            check(openAlLog.isFile) {
                "OpenAL Soft did not create openal.log."
            }
            val openAlContents = openAlLog.readText(Charsets.UTF_8)
            check(
                Regex("""(?i)initialized backend\s+"null"""")
                    .containsMatchIn(openAlContents),
            ) {
                "OpenAL Soft did not initialize the required null backend."
            }

            absoluteEvidenceDirectory.resolve("runner.properties")
                .toFile()
                .writeText(
                    "archive=${archive.name}\n"
                        + "cwd.external=PASS\n"
                        + "cwd.observed=$observedRealDirectory\n"
                        + "java.variant=$variant\n"
                        + "openal.null=PASS\n",
                    Charsets.UTF_8,
                )
        } finally {
            spawnedProcess?.takeIf { it.isAlive }?.let { process ->
                process.destroy()
                if (!process.waitFor(5, TimeUnit.SECONDS)) {
                    process.destroyForcibly()
                    process.waitFor(5, TimeUnit.SECONDS)
                }
            }
            temporaryWorkingDirectory.deleteRecursively()
            extractedPackageDirectory.deleteRecursively()
        }
    }
}

val spikeEvidenceManifest = layout.buildDirectory.file(
    spikeSmokeVariant.map { variant -> "reports/spike/manifests/$variant.sha256" },
)

val generateSpikeEvidenceManifest = tasks.register("generateSpikeEvidenceManifest") {
    group = "verification"
    description = "Hashes the distribution ZIP and every smoke artifact with SHA-256."
    dependsOn(smokeSpikeDistribution, distZip)
    inputs.file(distZip.flatMap { it.archiveFile })
    inputs.dir(smokeEvidenceDirectory)
    outputs.file(spikeEvidenceManifest)
    outputs.upToDateWhen { false }

    doLast {
        val evidenceFiles = smokeEvidenceDirectory.get().asFile
            .walkTopDown()
            .filter(File::isFile)
            .sortedBy(File::invariantSeparatorsPath)
            .toList()
        check(evidenceFiles.isNotEmpty()) {
            "The smoke run did not produce evidence in ${smokeEvidenceDirectory.get().asFile}."
        }
        val screenshots = evidenceFiles.filter {
            it.extension.equals("png", ignoreCase = true)
        }
        val requiredScreenshots = setOf(
            "viewport-640x360.png",
            "viewport-800x600.png",
            "viewport-1280x720.png",
        )
        check(
            screenshots.map(File::getName).toSet() == requiredScreenshots,
        ) {
            "The smoke run must produce the three viewport PNGs; found " +
                screenshots.map(File::getName)
        }
        screenshots.forEach { screenshot ->
            val dimensions = Regex("""viewport-(\d+)x(\d+)\.png""")
                .matchEntire(screenshot.name)
                ?: error("Unexpected viewport evidence name: ${screenshot.name}")
            val image = ImageIO.read(screenshot)
                ?: error("Unable to decode viewport evidence: $screenshot")
            val expectedWidth = dimensions.groupValues[1].toInt()
            val expectedHeight = dimensions.groupValues[2].toInt()
            check(image.width == expectedWidth && image.height == expectedHeight) {
                "${screenshot.name} is ${image.width}x${image.height}; expected " +
                    "${expectedWidth}x$expectedHeight."
            }
        }
        val requiredEvidenceFiles = setOf(
            "lifecycle.log",
            "probe.log",
            "viewport.log",
            "dispose.log",
            "openal.log",
            "process.stderr.log",
            "process.stdout.log",
            "runner.properties",
            "summary.properties",
        )
        val evidenceByName = evidenceFiles.associateBy(File::getName)
        check(evidenceByName.keys.containsAll(requiredEvidenceFiles)) {
            "The smoke run is missing required evidence: " +
                (requiredEvidenceFiles - evidenceByName.keys)
        }
        val emptyEvidence = evidenceFiles.filter { it.length() == 0L }
        check(emptyEvidence.isEmpty()) {
            "Smoke evidence files must not be empty: ${emptyEvidence.map(File::getName)}"
        }
        check("failure.log" !in evidenceByName) {
            "The spike recorded failure evidence: ${evidenceByName.getValue("failure.log")}"
        }

        val summary = Properties()
        evidenceByName.getValue("summary.properties")
            .inputStream()
            .use { summary.load(it) }
        check(summary.getProperty("result") == "PASS") {
            "Smoke summary did not report PASS: ${summary.getProperty("result")}"
        }
        check(summary.getProperty("fixtures") == "3") {
            "Smoke summary did not complete all three fixtures."
        }
        check(summary.getProperty("input.events").toInt() > 0) {
            "Smoke summary did not observe an input event."
        }
        val disposableCount = summary.getProperty("disposables").toInt()
        val summaryMetadata = setOf(
            "result",
            "fixtures",
            "input.events",
            "disposables",
        )
        val disposableEntries = summary.stringPropertyNames() - summaryMetadata
        check(
            disposableEntries.size == disposableCount &&
                disposableEntries.all { summary.getProperty(it) == "1" },
        ) {
            "Every owned Disposable must be released exactly once: " +
                disposableEntries.associateWith { summary.getProperty(it) }
        }

        val probeLog = evidenceByName.getValue("probe.log").readText(Charsets.UTF_8)
        val requiredProbeResults = setOf(
            "asset.sprite.type=Internal",
            "tiled=PASS",
            "audio=PASS",
            "input.processor=PASS",
            "input.backend-event=PASS",
        )
        check(requiredProbeResults.all { it in probeLog }) {
            "Probe log is missing PASS evidence: " +
                requiredProbeResults.filterNot { it in probeLog }
        }
        val runner = Properties()
        evidenceByName.getValue("runner.properties")
            .inputStream()
            .use { runner.load(it) }
        check(
            runner.getProperty("cwd.external") == "PASS"
                && runner.getProperty("openal.null") == "PASS",
        ) {
            "The package runner did not validate external CWD and OpenAL null: " +
                runner
        }
        val viewportLog = evidenceByName.getValue("viewport.log")
            .readText(Charsets.UTF_8)
        val requiredViewportResults = setOf(
            "640x360=PASS",
            "800x600=PASS",
            "1280x720=PASS",
            "golden=PASS",
        )
        check(requiredViewportResults.all { it in viewportLog }) {
            "Viewport log is missing PASS evidence: " +
                requiredViewportResults.filterNot { it in viewportLog }
        }
        val lifecycleLog = evidenceByName.getValue("lifecycle.log")
            .readText(Charsets.UTF_8)
        check(
            "dispose.end;result=PASS" in lifecycleLog &&
                "result=FAIL" !in lifecycleLog,
        ) {
            "Lifecycle log did not end with a clean PASS."
        }

        val filesToHash = listOf(distZip.get().archiveFile.get().asFile) + evidenceFiles
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

        val manifest = spikeEvidenceManifest.get().asFile
        manifest.parentFile.mkdirs()
        manifest.writeText(
            filesToHash.joinToString(separator = "\n", postfix = "\n") { file ->
                val rootPath = rootProject.projectDir.toPath()
                    .toAbsolutePath()
                    .normalize()
                val filePath = file.toPath().toAbsolutePath().normalize()
                val manifestPath = if (filePath.startsWith(rootPath)) {
                    rootPath.relativize(filePath).toString()
                } else {
                    filePath.toString()
                }.replace('\\', '/')
                "${sha256(file)}  $manifestPath"
            },
            Charsets.UTF_8,
        )
        logger.lifecycle(
            "Recorded SHA-256 for ${filesToHash.size} artifacts in " +
                manifest.absolutePath,
        )
    }
}

tasks.register("spikeDistribution") {
    group = "distribution"
    description = "Canonical alias for the packaged libGDX/LWJGL3 spike."
    dependsOn(buildSpikeDistribution)
}

tasks.register("spikeDistributionSmoke") {
    group = "verification"
    description = "Canonical alias for smoke-testing the installed spike package."
    dependsOn(generateSpikeEvidenceManifest)
}

tasks.named("test") {
    dependsOn(legacy.classesTaskName)
}

tasks.named("check") {
    dependsOn(legacy.classesTaskName)
}
