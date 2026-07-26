import org.gradle.api.tasks.bundling.Jar

plugins {
    `java-library`
}

description = "Desktop boundary and transitional Java2D backend."

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
    add(legacy.implementationConfigurationName, project(":engine:core"))
    testImplementation(platform("org.junit:junit-bom:5.14.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(project(":engine:core"))
    testImplementation(files(legacy.output))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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

tasks.named("test") {
    dependsOn(legacy.classesTaskName)
}

tasks.named("check") {
    dependsOn(legacy.classesTaskName)
}
