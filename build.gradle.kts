import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import pl.allegro.tech.build.axion.release.domain.PredefinedVersionCreator

plugins {
  `java-gradle-plugin`
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.serialization).apply(false)
  alias(libs.plugins.tapmoc)
  alias(libs.plugins.pluginPublish)
  alias(libs.plugins.axionRelease)
  alias(libs.plugins.detekt)
}


class WorkaroundExtension {
  var compilerVersion: String? = null
}
open class WorkaroundSerializationGradleSubplugin :
  KotlinCompilerPluginSupportPlugin {
  private val workaroundExtension = WorkaroundExtension()

  companion object {
    const val SERIALIZATION_GROUP_NAME = "org.jetbrains.kotlin"
    const val SERIALIZATION_ARTIFACT_NAME = "kotlin-serialization-compiler-plugin-embeddable"
  }

  override fun apply(target: Project) {
    super.apply(target)
    target.extensions.add("workaroundExtension", workaroundExtension)
  }
  override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

  override fun applyToCompilation(
    kotlinCompilation: KotlinCompilation<*>
  ): Provider<List<SubpluginOption>> =
    kotlinCompilation.target.project.provider { emptyList() }

  override fun getPluginArtifact(): SubpluginArtifact =
    SubpluginArtifact(SERIALIZATION_GROUP_NAME, SERIALIZATION_ARTIFACT_NAME, workaroundExtension.compilerVersion!!)

  override fun getCompilerPluginId() = "org.jetbrains.kotlinx.serialization"
}

plugins.apply(WorkaroundSerializationGradleSubplugin::class.java)
plugins.apply("org.jetbrains.kotlin.jvm")

extensions.getByType(WorkaroundExtension::class.java).compilerVersion = libs.versions.kotlin.compiler.get()
tapmoc {
  gradle("8.0.0")
}

kotlin {
  @OptIn(ExperimentalBuildToolsApi::class, ExperimentalKotlinGradlePluginApi::class)
  compilerVersion.set(libs.versions.kotlin.compiler.get())
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
  implementation(libs.serialization.json)
  implementation(libs.jibCore)
  implementation(libs.guava)
  compileOnly(libs.gradle.api)
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

tasks.check {
  dependsOn(tasks.detektMain, tasks.detektTest)
}

detekt {
  parallel = true
  buildUponDefaultConfig = true
  config.setFrom(files(project.rootDir.resolve("detekt.yml")))
}
