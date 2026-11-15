plugins {
    kotlin("jvm") version "1.9.24"
    `java-library`
    `maven-publish`
}

group = "com.ground"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    api("com.squareup.okhttp3:okhttp:4.12.0")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}

java {
    withSourcesJar()
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name.set("google-play-scraper-kotlin")
                description.set("Kotlin/JVM port of the google-play-scraper library")
                url.set("https://github.com/diegoHeartless/google-play-scraper-kotlin")
            }
        }
    }
}
