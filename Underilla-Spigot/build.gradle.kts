plugins {
    `java-library`
    id("io.github.goooler.shadow") version "8.1.7"
    id("io.papermc.paperweight.userdev") version "1.7.1" // paperweight // Check for new versions at https://plugins.gradle.org/plugin/io.papermc.paperweight.userdev
    `maven-publish` // Add ./gradlew publishToMavenLocal
    id("xyz.jpenilla.run-paper") version "2.3.0"
}

group = "com.jkantrell.mc.underilla.spigot"
version = "1.5.2"
description="Generate vanilla cave in custom world."

repositories {
    mavenLocal()
    mavenCentral()
    maven ("https://repo.papermc.io/repository/maven-public/")
    maven ("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven ("https://oss.sonatype.org/content/groups/public/")
    maven ("https://jitpack.io")
}


dependencies {
    // compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT") // without paperweight
    paperweight.paperDevBundle("1.20.6-R0.1-SNAPSHOT") // paperweight
    implementation("com.jkantrell:Yamlizer:main-SNAPSHOT")
    implementation(project(":Underilla-Core"))
}

// tasks.build.dependsOn tasks.reobfJar // paperweight
// tasks.build.dependsOn tasks.shadowJar // without paperweight

java {
    // Configure the java toolchain. This allows gradle to auto-provision JDK 21 on systems that only have JDK 8 installed for example.
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks {
    shadowJar {
        minimize()
        val prefix = "${project.group}.lib"
        sequenceOf(
            "co.aikar",
            "org.bstats",
            "jakarta.annotation",
        ).forEach { pkg ->
            relocate(pkg, "$prefix.$pkg")
        }
    }

    assemble {
        // dependsOn(shadowJar) // Not needed, probably because reobfJar depends on shadowJar
        dependsOn(reobfJar)
    }

    processResources {
        val props = mapOf(
            "name" to project.name,
            "version" to project.version,
            "description" to project.description,
            "apiVersion" to "1.20",
            "group" to project.group
        )
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("1.20.6")
    }
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

// Break Yamlizer.
// paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION