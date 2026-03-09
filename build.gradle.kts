plugins {
    kotlin("jvm") version "2.0.21"
    id("fabric-loom") version "1.8-SNAPSHOT"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21"
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/")
    maven("https://maven.cobblemon.com/releases/")
    maven("https://m2.dv8tion.net/releases/")
    maven("https://s01.oss.sonatype.org/content/repositories/releases/")
}

dependencies {
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    mappings("net.fabricmc:yarn:${project.property("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_version")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${project.property("fabric_kotlin_version")}")
    
    // Cobblemon
    modImplementation("com.cobblemon:fabric:${project.property("cobblemon_version")}")
    
    // SQLite
    implementation("org.xerial:sqlite-jdbc:${project.property("sqlite_version")}")
    include("org.xerial:sqlite-jdbc:${project.property("sqlite_version")}")
    
    // JDA Discord
    implementation("net.dv8tion:JDA:${project.property("jda_version")}") {
        exclude(module = "opus-java")
    }
    include("net.dv8tion:JDA:${project.property("jda_version")}") {
        exclude(module = "opus-java")
    }
    
    // Kotlin Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    include("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    
    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    include("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
}

tasks {
    processResources {
        inputs.property("version", project.version)
        filteringCharset = "UTF-8"
        filesMatching("fabric.mod.json") {
            expand(mapOf("version" to project.version))
        }
    }
    
    withType<JavaCompile>().configureEach {
        options.release = 21
    }
    
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().all {
        kotlinOptions.jvmTarget = "21"
    }
    
    jar {
        from("LICENSE")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    withSourcesJar()
}
