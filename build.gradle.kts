plugins {
    `java-library`
    `maven-publish`
    id("io.github.0ffz.github-packages") version "1.2.1"
    id("io.papermc.hangar-publish-plugin") version "0.1.2"
}

repositories {
    gradlePluginPortal()
    mavenLocal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven { githubPackage("apdevteam/movecraft")(this) }
}

dependencies {
    api("org.jetbrains:annotations-java5:24.1.0")
    compileOnly("io.papermc.paper:paper-api:1.18.2-R0.1-SNAPSHOT")
    compileOnly("net.countercraft:movecraft:8.0.0_beta-5_dev-1")
    compileOnly("net.countercraft.movecraft.combat:movecraft-combat:+")
}

group = "net.countercraft.movecraft"
version = "1.0.0_gradle"
description = "Movecraft-Overheat"
java.toolchain.languageVersion = JavaLanguageVersion.of(17)

tasks.jar {
    archiveBaseName.set("Movecraft-Overheat")
    archiveClassifier.set("")
    archiveVersion.set("")
}

tasks.processResources {
    from(rootProject.file("LICENSE.md"))
    filesMatching("*.yml") {
        expand(mapOf("projectVersion" to project.version))
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "net.countercraft.movecraft"
            artifactId = "movecraft-overheat"
            version = "${project.version}"

            artifact(tasks.jar)
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/drfiveminusmint/movecraft-overheat")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
