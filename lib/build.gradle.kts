/*
 * This file was generated by the Gradle 'init' task.
 *
 * This generated file contains a sample Java library project to get you started.
 * For more details on building Java & JVM projects, please refer to https://docs.gradle.org/8.14.2/userguide/building_java_projects.html in the Gradle documentation.
 */

plugins {
    // Apply the java-library plugin for API and implementation separation.
    `java-library`
    java
    jacoco
}

jacoco {
    toolVersion = "0.8.10"  // Use latest stable version
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport) // generate report after tests run
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)

    reports {
        html.required.set(true)
        xml.required.set(true)
    }

    val fileFilter = listOf(
        "**/*Test*"
    )

    classDirectories.setFrom(
        files(
            classDirectories.files.map {
                fileTree(it) {
                    exclude(fileFilter)
                }
            }
        )
    )

    sourceDirectories.setFrom(files("src/main/java"))
    additionalSourceDirs.setFrom(files("src/main/java"))
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    // Use JUnit test framework.
    implementation("org.junit.jupiter:junit-jupiter-api:5.9.1")

    // This dependency is used by the application.
    implementation("com.google.guava:guava:31.1-jre")
    implementation("org.glassfish.jersey.containers:jersey-container-grizzly2-http:3.1.3")
    implementation("org.glassfish.jersey.media:jersey-media-json-jackson:3.0.3")
    implementation("org.glassfish.jersey.inject:jersey-hk2:3.1.3")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")      // For writing tests
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")

    // Logging
    implementation("org.apache.logging.log4j:log4j-api:2.20.0")
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")

    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    // Dagger core
    implementation("com.google.dagger:dagger:2.51")
    annotationProcessor("com.google.dagger:dagger-compiler:2.51")

    // Cassandra
    implementation("com.datastax.oss:java-driver-core:4.17.0")
}

tasks.named<Jar>("jar") {
    archiveBaseName.set("HealthDAO")
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

