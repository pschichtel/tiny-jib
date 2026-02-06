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

  testImplementation("junit:junit:4.13.2")
  testImplementation("com.google.guava:guava:32.1.2-jre")
  testImplementation("pl.pragmatists:JUnitParams:1.1.1")
  testImplementation("com.google.truth:truth:1.1.5")
  testImplementation("com.google.truth.extensions:truth-java8-extension:1.1.5")
  testImplementation("org.mockito:mockito-core:4.11.0")
  testImplementation("org.slf4j:slf4j-api:2.0.7")
  testImplementation("com.github.stefanbirkner:system-rules:1.19.0")
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
