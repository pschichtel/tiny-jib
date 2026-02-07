plugins {
  `kotlin-dsl`
  `java-gradle-plugin`
  id("com.gradle.plugin-publish") version "1.3.1"
}

group = "tel.schich.tinyjib"
version = "0.1.0"

java.toolchain {
  languageVersion = JavaLanguageVersion.of(8)
}

repositories {
  mavenCentral()
  gradlePluginPortal()
}

dependencies {
  implementation("com.google.cloud.tools:jib-core:0.28.1")
  implementation("com.google.guava:guava:32.1.2-jre")
  implementation("com.fasterxml.jackson.core:jackson-databind:2.21.0")
}

gradlePlugin {
  displayName
  website = "https://github.com/pschichtel/tiny-jib"
  vcsUrl = "https://github.com/pschichtel/tiny-jib"
  plugins {
    create("tinyJibPlugin") {
      id = "tel.schich.tinyjib"
      implementationClass = "tel.schich.dockcross.TinyJibPlugin"
      displayName = "Tiny Jib Gradle Plugin"
      description = "A heavily simplified version of Google's Jib plugin"
      tags = listOf("container", "jib")
    }
  }
}
