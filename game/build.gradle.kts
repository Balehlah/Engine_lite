import org.gradle.api.tasks.JavaExec

plugins {
    application
}

description = "Reference game consuming the transitional desktop backend."

sourceSets {
    main {
        java {
            setSrcDirs(listOf(rootProject.file("src")))
            include("game/**/*.java")
        }
    }
}

dependencies {
    implementation(project(":engine:core"))
    implementation(
        project(
            mapOf(
                "path" to ":desktop",
                "configuration" to "legacyElements",
            ),
        ),
    )
}

application {
    mainClass = "game.test.Main"
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

tasks.register("legacyDemo") {
    group = "application"
    description = "Runs the transitional Java2D demo through the Gradle runtime classpath."
    dependsOn(tasks.named("run"))
}

tasks.register<JavaExec>("legacyDemoSmoke") {
    group = "verification"
    description = "Initializes and cleanly shuts down the transitional Java2D demo."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = application.mainClass
    args("--smoke")
}
