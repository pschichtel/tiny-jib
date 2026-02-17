import pl.allegro.tech.build.axion.release.domain.PredefinedVersionCreator

plugins {
  `kotlin-dsl`
  `java-gradle-plugin`
  alias(libs.plugins.pluginPublish)
  alias(libs.plugins.axionRelease)
}

scmVersion {
  tag {
    prefix = "v"
  }
  nextVersion {
    suffix = "SNAPSHOT"
    separator = "-"
  }
  versionCreator = PredefinedVersionCreator.SIMPLE.versionCreator
}

group = "tel.schich.tinyjib"
version = scmVersion.version

java.toolchain {
  languageVersion = JavaLanguageVersion.of(8)
}

repositories {
  mavenCentral()
  gradlePluginPortal()
}

dependencies {
  implementation(libs.jibCore)
  implementation(libs.guava)
  implementation(libs.jacksonDatabind)
}

gradlePlugin {
  displayName
  website = "https://github.com/pschichtel/tiny-jib"
  vcsUrl = "https://github.com/pschichtel/tiny-jib"
  plugins {
    create("tinyJibPlugin") {
      id = "tel.schich.tinyjib"
      implementationClass = "tel.schich.tinyjib.TinyJibPlugin"
      displayName = "Tiny Jib Gradle Plugin"
      description = "A heavily simplified version of Google's Jib plugin"
      tags = listOf("container", "jib")
    }
  }
}

tasks.jar {
  manifest {
    attributes("Implementation-Version" to version.toString())
  }
}
