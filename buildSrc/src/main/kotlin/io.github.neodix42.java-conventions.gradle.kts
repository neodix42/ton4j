/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    `java-library`
    `maven-publish`
}

repositories {
    mavenLocal()
    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
}

group = "io.github.neodix42"
version = "0.0.3"
java.sourceCompatibility = JavaVersion.VERSION_11

java {
    withSourcesJar()
}


publishing {
    repositories {
        maven {
            name = "Ammer-Tech"
            url = uri("https://maven.pkg.github.com/Ammer-Tech/publications")
            credentials {
                username = (project.findProperty("gpr.user") ?: System.getenv("USERNAME")).toString()
                password = (project.findProperty("gpr.key") ?: System.getenv("TOKEN")).toString()
            }
        }
    }
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}
