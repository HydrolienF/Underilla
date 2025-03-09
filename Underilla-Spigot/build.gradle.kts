import io.papermc.hangarpublishplugin.model.Platforms

plugins {
    `java-library`
    id("io.github.goooler.shadow") version "8.1.7"
    // paperweight.userdev 2.0.0-beta.8 isn't working to have a Underilla-Spigot-1.6.9.jar which is the reobf jar.
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.12" // paperweight // Check for new versions at https://plugins.gradle.org/plugin/io.papermc.paperweight.userdev
    `maven-publish` // Add ./gradlew publishToMavenLocal
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("io.papermc.hangar-publish-plugin") version "0.1.2"
    id("org.sonarqube") version "5.0.0.4638"
}

group = "com.jkantrell.mc.underilla"
version = "2.1.0"
description="Generate vanilla cave in custom world."
val mainMinecraftVersion = "1.21.4"
val supportedMinecraftVersions = "1.21.3 - 1.21.4"
val voidWorldGeneratorVersion = "1.3.2"
val chunkyVersion = "1.4.28"

repositories {
    mavenLocal()
    mavenCentral()
    maven ("https://repo.papermc.io/repository/maven-public/")
    maven ("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven ("https://oss.sonatype.org/content/groups/public/")
    maven ("https://jitpack.io")
    maven("https://repo.codemc.io/repository/maven-public/") // For Chunky
}


dependencies {
    // compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT") // without paperweight
    paperweight.paperDevBundle("$mainMinecraftVersion-R0.1-SNAPSHOT")
    implementation("fr.formiko.mc.biomeutils:biomeutils:1.1.8")
    implementation("com.github.FormikoLudo:Utils:0.0.9")
    implementation("org.bstats:bstats-bukkit:3.1.0")
    implementation("com.github.HydrolienF:KntNBT:2.2.2")
    compileOnly("fr.formiko.mc.voidworldgenerator:voidworldgenerator:$voidWorldGeneratorVersion")
    compileOnly("org.popcraft:chunky-common:$chunkyVersion")
}

// tasks.build.dependsOn tasks.reobfJar // paperweight
// tasks.build.dependsOn tasks.shadowJar // without paperweight

java {
    // Configure the java toolchain. This allows gradle to auto-provision JDK 21 on systems that only have JDK 8 installed for example.
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withJavadocJar()
    withSourcesJar()
}


tasks {
    shadowJar {
        minimize()
        val prefix = "${project.group}.lib"
        sequenceOf(
            "co.aikar",
            "org.bstats",
            "fr.formiko.mc.biomeutils",
            "fr.formiko.utils",
        ).forEach { pkg ->
            relocate(pkg, "$prefix.$pkg")
        }
        archiveFileName.set("${project.name}-${project.version}.jar")
        exclude("javax/**")
        exclude("assets/**")
        exclude("com/google/**")
        exclude("org/checkerframework/**")
        exclude("org/apache/**")
    }

    assemble {
        dependsOn(shadowJar) // Not needed, probably because reobfJar depends on shadowJar
        // dependsOn(reobfJar)
    }

    processResources {
        val props = mapOf(
            "name" to project.name,
            "version" to project.version,
            "description" to project.description,
            "apiVersion" to "1.21.3",
            "group" to project.group,
            "voidWorldGeneratorVersion" to voidWorldGeneratorVersion,
            "chunkyVersion" to chunkyVersion
        )
        inputs.properties(props)
        filesMatching(listOf("paper-plugin.yml", "config.yml")) {
            expand(props)
        }
    }
    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion(mainMinecraftVersion)
        // https://hangar.papermc.io/pop4959/Chunky
        // downloadPlugins {
        //     hangar("Chunky", "$chunkyVersion")
        //     github("HydrolienF", "VoidWorldGenerator", "$voidWorldGeneratorVersion", "VoidWorldGenerator-${voidWorldGeneratorVersion}.jar")
        // }
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
                // jar.set(File("build/libs/${project.name}-${project.version}.jar"))
                // externalDownloadUrl.set("https://github.com/HydrolienF/Underilla/releases/download/1.6.14/Underilla-Spigot-1.6.14.jar")

                // Set platform versions from gradle.properties file
                val versions: List<String> = supportedMinecraftVersions.replace(" ", "").split(",")
                platformVersions.set(versions)
            }
        }
    }
}

sonar {
    properties {
        property("sonar.organization", "mvndicraft")
        property("sonar.projectKey", project.name)
        //property("sonar.host.url", "https://sonarcloud.io/")
        property("sonar.host.url", "http://mvndi.formiko.fr:9000/")
    }
}

publishing {
  publications {
    create<MavenPublication>("mavenJava") {
      from(components["java"])

      artifactId = project.name.lowercase()
      pom {
        packaging = "jar"
        url.set("https://github.com/HydrolienF/${project.name}")
        inceptionYear.set("2024")
        description = project.description
        licenses {
          license {
            name.set("MIT license")
            url.set("https://github.com/HydrolienF/${project.name}/blob/master/LICENSE.md")
          }
        }
        developers {
          developer {
            id.set("hydrolienf")
            name.set("HydrolienF")
            email.set("hydrolien.f@gmail.com")
          }
        }
        scm {
          connection.set("scm:git:git@github.com:HydrolienF/${project.name}.git")
          developerConnection.set("scm:git:ssh:git@github.com:HydrolienF/${project.name}.git")
          url.set("https://github.com/HydrolienF/${project.name}")
        }
      }
    }
  }
  repositories {
    maven {
        url = layout.buildDirectory.dir("staging-deploy").get().asFile.toURI()

    }
  }
}

// Custom signing task using gpg -ab
val signWithGpg = tasks.register("signWithGpg") {
    dependsOn("publishMavenJavaPublicationToMavenRepository")
    group = "signing"
    description = "Sign the publication using gpg -ab"
    val filesToSign = fileTree("${buildDir}/staging-deploy/${project.group.toString().lowercase().replace('.', '/')}/${project.name.lowercase()}/${project.version}") {
        include("**/*.jar", "**/*.module", "**/*.pom")
    }
    doFirst {
        filesToSign.forEach { file ->
            val command = listOf("gpg", "-ab", "--output", "${file.absolutePath}.asc", file.absolutePath)
            println("Executing command: ${command.joinToString(" ")}")
            exec {
                commandLine = command
            }
        }
    }
}

tasks.register<Zip>("zipStagingDeploy") {
    dependsOn("signWithGpg")
    dependsOn("publishMavenJavaPublicationToMavenRepository")
    from(layout.buildDirectory.dir("staging-deploy"))
    archiveFileName.set("staging-deploy-${project.name}-${project.version}.zip")
    destinationDirectory.set(layout.buildDirectory)
}