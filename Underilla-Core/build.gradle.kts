plugins {
    `java-library`
    id("org.sonarqube") version "4.4.1.3373"
}

group = "com.Jkantrell.mc"
version = "1.5.2"
java.sourceCompatibility = JavaVersion.VERSION_21

repositories {
    mavenCentral()
    mavenLocal()
    maven ("https://jitpack.io")
}


dependencies {
    // testImplementation platform("org.junit:junit-bom:5.9.1")
    // testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    api("com.github.HydrolienF:KntNBT:2.2.2")
}

tasks.test {
    useJUnitPlatform()
}

sonar {
  properties {
    property("sonar.projectKey", "mvndicraft_underilla")
    property("sonar.organization", "mvndicraft")
    property("sonar.host.url", "https://sonarcloud.io")
  }
}