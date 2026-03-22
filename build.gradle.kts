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

/**
 * Because we are using BTA, the kotlinc version is different from the KGP version.
 *
 * By default, KGP uses the KGP version for compiler plugins ([source](https://github.com/Jetbrains/kotlin/blob/bd48ae1608136d3185e61b441e75f0dda7ace7a6/libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/SubpluginEnvironment.kt#L99)).
 *
 * In order for us to control the compiler plugin version, we fork the kotlinx.serialization plugin below.
 *
 * See https://youtrack.jetbrains.com/issue/KT-81629/
 */
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

  testImplementation(gradleTestKit())
  testImplementation(libs.junit.api)
  testRuntimeOnly(libs.junit.jupiter.engine)
  testRuntimeOnly(libs.junit.platform.launcher)
}

val pluginId = "tel.schich.tinyjib"

gradlePlugin {
  displayName
  website = "https://github.com/pschichtel/tiny-jib"
  vcsUrl = "https://github.com/pschichtel/tiny-jib"
  plugins {
    create("tinyJibPlugin") {
      id = pluginId
      implementationClass = "tel.schich.tinyjib.TinyJibPlugin"
      displayName = "Tiny Jib Gradle Plugin"
      description = "A heavily simplified version of Google's Jib plugin"
      tags = listOf("container", "jib")
    }
  }
}

val testRepoName = "testRepo"
val testRepoDir = project.layout.buildDirectory.dir("test-repo")

publishing {
  repositories {
    maven {
      name = testRepoName
      url = testRepoDir.get().asFile.toURI()
    }
  }
}

tasks.test {
  testLogging {
    showStandardStreams = true
  }

  val publishTasks = tasks
    .withType(PublishToMavenRepository::class)
    .matching { it.repository.name == testRepoName }

  dependsOn(publishTasks)

  useJUnitPlatform()
  systemProperty("tinyjib.id", pluginId)
  systemProperty("tinyjib.version", project.version)
  systemProperty("tinyjib.rootDir", project.rootDir.absolutePath)
  systemProperty("tinyjib.repoUri", testRepoDir.get().asFile.toURI().toString())
  systemProperty("tinyjib.gradleVersion", gradle.gradleVersion)

  for (version in listOf(8, 11, 17, 21, 25)) {
    val javaHome = javaToolchains.launcherFor {
      languageVersion.set(JavaLanguageVersion.of(version))
    }.get().metadata.installationPath.asFile.absolutePath
    systemProperty("tinyjib.javaHome.$version", javaHome)
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
