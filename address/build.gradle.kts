/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    id("io.github.neodix42.java-conventions")
}

dependencies {
    compileOnly(project(":utils"))
    implementation("commons-codec:commons-codec:1.15")
    implementation("org.purejava:tweetnacl-java:1.1.2")
    testImplementation("junit:junit:4.13.2")
    annotationProcessor("org.projectlombok:lombok:1.18.24")
    testImplementation("org.projectlombok:lombok:1.18.24")
    testImplementation("ch.qos.logback:logback-classic:1.2.11")
    testImplementation("org.assertj:assertj-core:3.23.1")
}

description = "TON Java address"
