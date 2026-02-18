import pl.allegro.tech.build.axion.release.domain.PredefinedVersionCreator

plugins {

  `java-gradle-plugin`
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.tapmoc)
  alias(libs.plugins.pluginPublish)
  alias(libs.plugins.axionRelease)
  alias(libs.plugins.detekt)
}

tapmoc {
  gradle("8.0.0")
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

repositories {
  mavenCentral()
  gradlePluginPortal()
}

dependencies {
  implementation(libs.jibCore)
  implementation(libs.guava)
  implementation(libs.jacksonDatabind)
  compileOnly(libs.gradle.api)
}

/**
 * java-gradle-plugin adds `gradleApi()` to the `api` dependencies, which isn't ideal because:
 * - the user distribution provides the Gradle version.
 * - compiling against an older version of the Gradle API can help us detect invalid API usages.
 *
 * So we remove `gradleApi()` here and instead pull the Nokee redistributed artifact as a compileOnly
 * dependency
 */
configurations.named("api").configure {
  dependencies.removeIf {
    it is FileCollectionDependency
  }
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

detekt {
  parallel = true
  buildUponDefaultConfig = true
  config.setFrom(files(project.rootDir.resolve("detekt.yml")))
}
