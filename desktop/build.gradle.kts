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
import java.util.zip.ZipFile
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
val isLinux = System.getProperty("os.name").startsWith("Linux", ignoreCase = true)
val spikeApplicationName = "engine-lite-spike"
val spikeMainClass = "engine.incubator.gdx.spike.desktop.Lwjgl3SpikeLauncher"
val lwjglModules = listOf(
    "lwjgl",
    "lwjgl-glfw",
    "lwjgl-jemalloc",
    "lwjgl-openal",
    "lwjgl-opengl",
    "lwjgl-stb",
)
val supportedLwjglNativeClassifiers = listOf(
    "natives-linux",
    "natives-linux-arm32",
    "natives-linux-arm64",
    "natives-windows",
    "natives-windows-x86",
)
val gdxDesktopNativesSourceSha256 =
    "f4847981d27c6524a30f5665036ec8c11f48c8eda7610bb63f742de95ffe1970"
val supportedGdxNativeEntries = setOf(
    "gdx.dll",
    "gdx64.dll",
    "libgdx64.so",
    "libgdxarm.so",
    "libgdxarm64.so",
    "libgdxriscv64.so",
)
val completeGdxNativePayload = supportedGdxNativeEntries + setOf(
    "libgdx64.dylib",
    "libgdxarm64.dylib",
)

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

val gdxDesktopNativesSource = configurations.create("gdxDesktopNativesSource") {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
    description = "Pinned source JAR used to curate libGDX natives for Windows and Linux."
}

val curatedGdxNativeReport =
    layout.buildDirectory.file("reports/distribution/gdx-platform-natives-curated.txt")
val curateGdxDesktopNatives = tasks.register<Jar>("curateGdxDesktopNatives") {
    group = "distribution"
    description =
        "Produces a reproducible libGDX native JAR containing only Windows and Linux payloads."
    archiveBaseName.set("gdx-platform")
    archiveVersion.set(gdxVersion)
    archiveClassifier.set("natives-windows-linux")
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    outputs.file(curatedGdxNativeReport)

    val sourceJar = providers.provider {
        val files = gdxDesktopNativesSource.resolve()
        check(files.size == 1) {
            "Expected one libGDX desktop native source JAR; found $files"
        }
        files.single()
    }
    from(sourceJar.map { zipTree(it) }) {
        include(*supportedGdxNativeEntries.toTypedArray())
    }
    from(
        rootProject.layout.projectDirectory.file(
            "third_party/gdx-platform-1.14.2-natives-desktop.provenance",
        ),
    ) {
        into("META-INF")
        rename { "ENGINE-LITE-NATIVE-PROVENANCE.txt" }
    }
    manifest {
        attributes(
            "Implementation-Title" to "libGDX desktop natives curated for Engine Lite",
            "Implementation-Version" to gdxVersion,
            "Engine-Lite-Supported-Platforms" to "Windows,Linux",
            "Engine-Lite-Source-SHA-256" to gdxDesktopNativesSourceSha256,
        )
    }

    doFirst {
        val source = sourceJar.get()
        check(sha256(source) == gdxDesktopNativesSourceSha256) {
            "libGDX desktop natives source checksum mismatch for $source."
        }
        val sourcePayload = ZipFile(source).use { archive ->
            archive.entries().asSequence()
                .filterNot { it.isDirectory || it.name == "META-INF/MANIFEST.MF" }
                .map { it.name }
                .toSet()
        }
        check(sourcePayload == completeGdxNativePayload) {
            "Unexpected libGDX desktop native payload. Expected " +
                "$completeGdxNativePayload, found $sourcePayload."
        }
    }

    doLast {
        val curatedJar = archiveFile.get().asFile
        val curatedPayload = ZipFile(curatedJar).use { archive ->
            archive.entries().asSequence()
                .filterNot { it.isDirectory || it.name.startsWith("META-INF/") }
                .map { it.name }
                .toSet()
        }
        check(curatedPayload == supportedGdxNativeEntries) {
            "Curated libGDX native payload differs from the Windows/Linux allowlist: " +
                curatedPayload
        }
        val report = curatedGdxNativeReport.get().asFile
        report.parentFile.mkdirs()
        report.writeText(
            buildString {
                appendLine("source.coordinates=com.badlogicgames.gdx:gdx-platform:$gdxVersion")
                appendLine("source.classifier=natives-desktop")
                appendLine("source.sha256=$gdxDesktopNativesSourceSha256")
                appendLine("curated.file=${curatedJar.name}")
                appendLine("curated.sha256=${sha256(curatedJar)}")
                appendLine(
                    "curated.entries=${supportedGdxNativeEntries.sorted().joinToString(",")}",
                )
                appendLine("excluded.entries=libgdx64.dylib,libgdxarm64.dylib")
                appendLine("license=Apache-2.0")
            },
            Charsets.UTF_8,
        )
    }
}

dependencies {
    implementation(project(":engine:gdx"))
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion") {
        exclude(group = "org.lwjgl")
    }
    add(
        gdxDesktopNativesSource.name,
        "com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop",
    )
    runtimeOnly(files(curateGdxDesktopNatives.flatMap { it.archiveFile }))

    lwjglModules.forEach { module ->
        implementation("org.lwjgl:$module:$lwjglVersion") {
            because("LWJGL 3.4.1 or newer is required for the Java 25 compatibility gate.")
        }
        supportedLwjglNativeClassifiers.forEach { classifier ->
            runtimeOnly("org.lwjgl:$module:$lwjglVersion:$classifier")
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
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
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
    dependsOn(
        legacyJar,
        curateGdxDesktopNatives,
    )
}

val installDist = tasks.named<Sync>("installDist")
val distZip = tasks.named<Zip>("distZip")
val spikeArchiveHashRecord =
    layout.buildDirectory.file("reports/distribution/spike-archive.sha256")

val buildSpikeDistribution = tasks.register("buildSpikeDistribution") {
    group = "distribution"
    description = "Builds the installed tree and canonical ZIP for the libGDX/LWJGL3 spike."
    dependsOn(
        installDist,
        distZip,
    )
}

val recordSpikeDistributionHash = tasks.register("recordSpikeDistributionHash") {
    group = "distribution"
    description = "Records the canonical ZIP hash before any runtime smoke."
    dependsOn(distZip)
    inputs.file(distZip.flatMap { it.archiveFile })
    outputs.file(spikeArchiveHashRecord)
    outputs.upToDateWhen { false }

    doLast {
        val archive = distZip.get().archiveFile.get().asFile
        val record = spikeArchiveHashRecord.get().asFile
        record.parentFile.mkdirs()
        record.writeText(
            "${sha256(archive)}  ${archive.name}\n",
            Charsets.UTF_8,
        )
    }
}

val verifySpikeDistributionHash = tasks.register("verifySpikeDistributionHash") {
    group = "verification"
    description = "Fails if the canonical ZIP changed after the runtime smokes."
    outputs.upToDateWhen { false }

    doLast {
        val archive = distZip.get().archiveFile.get().asFile
        val record = spikeArchiveHashRecord.get().asFile
        check(archive.isFile && record.isFile) {
            "The ZIP and its pre-smoke hash record must exist before integrity verification."
        }
        val expected = record.readText(Charsets.UTF_8).trim().substringBefore(' ')
        val observed = sha256(archive)
        check(observed == expected) {
            "The canonical ZIP changed after smoke execution. Expected $expected, " +
                "observed $observed."
        }
        logger.lifecycle("Verified unchanged canonical ZIP SHA-256: $observed")
    }
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
            val archiveHashBefore = sha256(archive)
            val recordedHash = spikeArchiveHashRecord.get().asFile
                .takeIf(File::isFile)
                ?.readText(Charsets.UTF_8)
                ?.trim()
                ?.substringBefore(' ')
            check(recordedHash == null || recordedHash == archiveHashBefore) {
                "The ZIP differs from its pre-smoke hash. Expected $recordedHash, " +
                    "observed $archiveHashBefore."
            }
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
                isLinux -> listOf(
                    "xvfb-run",
                    "--auto-servernum",
                    "--server-args=-screen 0 1920x1080x24",
                    "/bin/sh",
                    launcher.absolutePath,
                ) + smokeArguments
                else -> error(
                    "The spike smoke supports only Windows and Linux. " +
                        "Detected ${System.getProperty("os.name")}.",
                )
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
            val glRenderer = probeContents.lineSequence()
                .firstOrNull { " gl.renderer=" in it }
                ?.substringAfter(" gl.renderer=")
                ?.trim()
                ?: error("probe.log does not record gl.renderer.")
            val requireLlvmPipe = System.getenv("ENGINE_LITE_REQUIRE_LLVMPIPE")
                ?.let { it == "1" || it.equals("true", ignoreCase = true) }
                ?: false
            if (requireLlvmPipe) {
                check(glRenderer.contains("llvmpipe", ignoreCase = true)) {
                    "Windows CI requires Mesa llvmpipe; observed renderer: $glRenderer"
                }
                check(
                    System.getenv("GALLIUM_DRIVER").equals(
                        "llvmpipe",
                        ignoreCase = true,
                    ) && System.getenv("LIBGL_ALWAYS_SOFTWARE") == "1",
                ) {
                    "Windows CI must force GALLIUM_DRIVER=llvmpipe and " +
                        "LIBGL_ALWAYS_SOFTWARE=1."
                }
            }
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

            val archiveHashAfter = sha256(archive)
            check(archiveHashAfter == archiveHashBefore) {
                "The smoke modified the canonical ZIP. Before $archiveHashBefore, " +
                    "after $archiveHashAfter."
            }
            val mesaEnvironment = linkedMapOf(
                "mesa.version" to "ENGINE_LITE_MESA_VERSION",
                "mesa.source.url" to "ENGINE_LITE_MESA_SOURCE_URL",
                "mesa.source.commit" to "ENGINE_LITE_MESA_SOURCE_COMMIT",
                "mesa.archive.sha256" to "ENGINE_LITE_MESA_ARCHIVE_SHA256",
                "mesa.opengl32.sha256" to "ENGINE_LITE_MESA_OPENGL32_SHA256",
                "mesa.libgallium_wgl.sha256" to
                    "ENGINE_LITE_MESA_LIBGALLIUM_WGL_SHA256",
                "mesa.license" to "ENGINE_LITE_MESA_LICENSE",
            )
            if (requireLlvmPipe) {
                val missingMesaMetadata = mesaEnvironment.filterValues {
                    System.getenv(it).isNullOrBlank()
                }
                check(missingMesaMetadata.isEmpty()) {
                    "Windows CI Mesa metadata is incomplete: ${missingMesaMetadata.values}"
                }
            }
            absoluteEvidenceDirectory.resolve("runner.properties")
                .toFile()
                .writeText(
                    buildString {
                        appendLine("archive=${archive.name}")
                        appendLine("archive.sha256.before=$archiveHashBefore")
                        appendLine("archive.sha256.after=$archiveHashAfter")
                        appendLine("archive.integrity=PASS")
                        appendLine("cwd.external=PASS")
                        appendLine("cwd.observed=$observedRealDirectory")
                        appendLine("java.variant=$variant")
                        appendLine("openal.null=PASS")
                        appendLine("gl.renderer=$glRenderer")
                        appendLine("mesa.enabled=$requireLlvmPipe")
                        mesaEnvironment.forEach { (property, environment) ->
                            appendLine(
                                "$property=" +
                                    (System.getenv(environment) ?: "not-applicable"),
                            )
                        }
                    },
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
            "timing.log",
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
        val timingLog = evidenceByName.getValue("timing.log").readText(Charsets.UTF_8)
        val timingLines = timingLog.lineSequence().filter(String::isNotBlank).toList()
        check(timingLines.size == 2) {
            "Timing log must contain exactly one policy and one metrics line; " +
                "found ${timingLines.size}."
        }
        val timingPolicy = timingLines[0].substringAfter(' ', missingDelimiterValue = "")
        val timingMetrics = timingLines[1].substringAfter(' ', missingDelimiterValue = "")
        check(timingPolicy.isNotEmpty() && timingMetrics.isNotEmpty()) {
            "Timing log lines must contain an ISO-8601 timestamp and a payload."
        }
        val requiredTimingPolicy = setOf(
            "fixed.updates-per-second=60.0",
            "fixed.dt-seconds=0.016666666666666666",
            "fixed.step-nanos=16666667",
            "clamp-nanos=250000000",
            "max-catch-up=5",
        )
        check(requiredTimingPolicy.all { it in timingPolicy }) {
            "Timing log is missing fixed-timestep policy evidence: " +
                requiredTimingPolicy.filterNot { it in timingPolicy }
        }
        fun timingMetric(name: String): String {
            val match = Regex("(?:^|;)$name=([^;\\r\\n]+)")
                .find(timingMetrics)
                ?: error("Timing log is missing metric '$name'.")
            return match.groupValues[1]
        }
        val timingFrames = timingMetric("frames").toLong()
        val timingUpdates = timingMetric("updates").toLong()
        val timingAlpha = timingMetric("alpha").toDouble()
        val timingClampedFrames = timingMetric("clamped-frames").toLong()
        val timingClampedNanos = timingMetric("clamped-wall-nanos").toLong()
        val timingCatchUpHits = timingMetric("catch-up-limit-hits").toLong()
        val timingCatchUpDiscarded = timingMetric("catch-up-discarded-nanos").toLong()
        val timingInactiveNanos = timingMetric("inactive-wall-nanos").toLong()
        check(
            timingFrames > 0L
                && timingUpdates > 0L
                && timingAlpha >= 0.0
                && timingAlpha < 1.0
                && timingClampedFrames >= 0L
                && timingClampedNanos >= 0L
                && timingCatchUpHits >= 0L
                && timingCatchUpDiscarded >= 0L
                && timingInactiveNanos >= 0L,
        ) {
            "Timing metrics are outside their required boundaries: " +
                "frames=$timingFrames, updates=$timingUpdates, alpha=$timingAlpha, " +
                "clampedFrames=$timingClampedFrames, clampedNanos=$timingClampedNanos, " +
                "catchUpHits=$timingCatchUpHits, " +
                "catchUpDiscarded=$timingCatchUpDiscarded, " +
                "inactiveNanos=$timingInactiveNanos"
        }
        val runner = Properties()
        evidenceByName.getValue("runner.properties")
            .inputStream()
            .use { runner.load(it) }
        check(
            runner.getProperty("cwd.external") == "PASS"
                && runner.getProperty("openal.null") == "PASS"
                && runner.getProperty("archive.integrity") == "PASS"
                && runner.getProperty("archive.sha256.before")
                    == runner.getProperty("archive.sha256.after"),
        ) {
            "The package runner did not validate CWD, OpenAL and ZIP integrity: " +
                runner
        }
        if (runner.getProperty("mesa.enabled").toBoolean()) {
            check(
                runner.getProperty("gl.renderer")
                    .contains("llvmpipe", ignoreCase = true),
            ) {
                "Windows evidence did not record the required llvmpipe renderer."
            }
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

tasks.register("recordSpikeArchiveHash") {
    group = "distribution"
    description = "Alias for recording the canonical ZIP hash before smoke execution."
    dependsOn(recordSpikeDistributionHash)
}

tasks.register("verifySpikeArchiveHash") {
    group = "verification"
    description = "Alias for verifying the canonical ZIP hash after smoke execution."
    dependsOn(verifySpikeDistributionHash)
}

tasks.named("test") {
    dependsOn(legacy.classesTaskName)
}

tasks.named("check") {
    dependsOn(legacy.classesTaskName)
}
