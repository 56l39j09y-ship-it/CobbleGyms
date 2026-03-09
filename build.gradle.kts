plugins {
    id("fabric-loom") version "1.7-SNAPSHOT"
    id("org.jetbrains.kotlin.jvm") version "2.0.0"
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String

base {
    archivesName.set(project.property("archives_base_name") as String)
}

val targetJavaVersion = 21

java {
    toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    withSourcesJar()
}

repositories {
    maven("https://maven.fabricmc.net/")
    maven("https://maven.impactdevelopment.dev/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://cursemaven.com")
    maven {
        name = "Cobblemon"
        url = uri("https://maven.cobblemon.com/releases")
    }
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    mappings("net.fabricmc:yarn:${project.property("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_version")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${project.property("kotlin_loader_version")}")
    modCompileOnly("com.cobblemon:cobblemon-fabric:${project.property("cobblemon_version")}")
}

tasks.processResources {
    inputs.property("version", project.version)
    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") {
        expand(mapOf("version" to project.version))
    }
}

kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(targetJavaVersion)
}
