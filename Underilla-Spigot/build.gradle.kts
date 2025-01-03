import io.papermc.hangarpublishplugin.model.Platforms

plugins {
    `java-library`
    id("io.github.goooler.shadow") version "8.1.7"
    // paperweight.userdev 2.0.0-beta.8 isn't working to have a Underilla-Spigot-1.6.9.jar which is the reobf jar.
    id("io.papermc.paperweight.userdev") version "1.7.7" // paperweight // Check for new versions at https://plugins.gradle.org/plugin/io.papermc.paperweight.userdev
    `maven-publish` // Add ./gradlew publishToMavenLocal
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("io.papermc.hangar-publish-plugin") version "0.1.2"
}

group = "com.jkantrell.mc.underilla.spigot"
version = "1.6.15"
description="Generate vanilla cave in custom world."
val mainMinecraftVersion = "1.21.3"
val supportedMinecraftVersions = "1.21.3 - 1.21.4"

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
    paperweight.paperDevBundle("$mainMinecraftVersion-R0.1-SNAPSHOT")
    implementation("com.jkantrell:Yamlizer:main-SNAPSHOT")
    implementation(project(":Underilla-Core"))
    implementation("fr.formiko.mc.biomeutils:biomeutils:1.1.8")
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
            "fr.formiko.mc.biomeutils",
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
            "apiVersion" to "1.21",
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
        minecraftVersion(mainMinecraftVersion)
        // https://hangar.papermc.io/pop4959/Chunky
        downloadPlugins {
            hangar("Chunky", "1.4.28")
        }
    }
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

// Break Yamlizer.
// paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION


tasks.register("echoVersion") {
    doLast {
        println("${project.version}")
    }
}

tasks.register("echoReleaseName") {
    doLast {
        println("${project.version} [${supportedMinecraftVersions}]")
    }
}

val versionString: String = version as String
val isRelease: Boolean = !versionString.contains("SNAPSHOT")

hangarPublish { // ./gradlew publishPluginPublicationToHangar
    publications.register("plugin") {
        version.set(project.version as String)
        channel.set(if (isRelease) "Release" else "Snapshot")
        // id.set(project.name as String)
        id.set("Underilla")
        apiKey.set(System.getenv("HANGAR_API_TOKEN"))
        platforms {
            register(Platforms.PAPER) {
                jar.set(tasks.shadowJar.flatMap { it.archiveFile })
                // externalDownloadUrl.set("https://github.com/HydrolienF/Underilla/releases/download/1.6.14/Underilla-Spigot-1.6.14.jar")

                // Set platform versions from gradle.properties file
                val versions: List<String> = supportedMinecraftVersions.replace(" ", "").split(",")
                platformVersions.set(versions)
            }
        }
    }
}