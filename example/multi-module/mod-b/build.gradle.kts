plugins {
    id("org.jetbrains.kotlin.jvm") version "2.3.10"
    id("tel.schich.tinyjib") version "0.2.5-SNAPSHOT"
}

repositories {
    mavenCentral()
}

tinyJib {
    from {
        image = "docker.io/library/eclipse-temurin:11-alpine"
    }
    container {
        mainClass = "tel.schich.tinyjib.example.modb.MainKt"
    }
    to {
        image = "ghcr.io/pschichtel/tinyjib/example/modb:latest"
    }
}
