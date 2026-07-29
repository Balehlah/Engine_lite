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
