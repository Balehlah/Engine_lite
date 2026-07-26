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
            setSrcDirs(listOf(rootProject.file("src")))
            include(
                "engine/core/Timer.java",
                "engine/math/Vector2.java",
                "engine/util/**/*.java",
            )
        }
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
    dependsOn(verifyBackendIndependence)
}
