plugins {
    `java-library`
}

description = "Isolated libGDX backend spike authorized by Issue #14."

val gdxVersion = providers.gradleProperty("gdxVersion").get()

sourceSets {
    main {
        resources {
            setSrcDirs(listOf(rootProject.file("assets")))
            include("spike/**")
        }
    }
}

dependencies {
    api(project(":engine:core"))
    api("com.badlogicgames.gdx:gdx:$gdxVersion")

    testImplementation(platform("org.junit:junit-bom:5.14.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

val assetServiceCwdSmoke = tasks.register<JavaExec>("assetServiceCwdSmoke") {
    group = "verification"
    description = "Loads a typed classpath asset from a real external working directory."
    dependsOn(tasks.named("testClasses"))
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("engine.incubator.gdx.assets.AssetServiceCwdProbe")

    val externalCwd = layout.buildDirectory.dir("tmp/asset-service-external-cwd")
    doFirst {
        val directory = externalCwd.get().asFile
        directory.mkdirs()
        workingDir(directory)
    }
}

tasks.named("test") {
    dependsOn(assetServiceCwdSmoke)
}
