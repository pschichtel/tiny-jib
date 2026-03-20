package tel.schich.tinyjib.util

import org.intellij.lang.annotations.Language
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.isDirectory

fun generateProject(target: Path, qualifiedClassName: String, gradleVersion: String, config: String) {
    val pluginId = getProp("id")
    val version = getProp("version")
    val rootDir = Paths.get(getProp("rootDir"))
    val repoDir = URI(getProp("repoUri"))

    try {
        copyWrapper(target, rootDir)
        writeWrapperProperties(target, gradleVersion)
        writeBuildGradleKts(target, pluginId, version, config)
        writeSettingsGradleKts(target, repoDir)
        writeMainClass(target, qualifiedClassName)
    } finally {
        Files.walk(target).use {
            for (path in it) {
                println("Generated file: $path")
            }
        }
    }
}

private fun writeBuildGradleKts(target: Path, pluginId: String, version: String, @Language("kotlin") config: String) {
    val indentedConfig = config
        .trimIndent()
        .trimEnd()
        .split("\n")
        .map { it.trimEnd() }
        .joinToString("\n") { "            ${it.trimEnd()}" }
        .trimStart()

    @Language("kotlin")
    val content = """
        plugins {
            java
            id("${escapeKotlinString(pluginId)}") version "${escapeKotlinString(version)}"
        }

        group = "tinyjib"
        version = "1.0.0"

        tinyJib {
            $indentedConfig
        }
    """.trimIndent()
    writeContent(target.resolve("build.gradle.kts"), content)
}

private fun writeSettingsGradleKts(target: Path, repoUri: URI) {
    @Language("kotlin")
    val content = """
        rootProject.name = "test"
        pluginManagement {
            repositories {
                maven { 
                    url = uri("${escapeKotlinString(repoUri.toString())}") 
                }
                gradlePluginPortal() 
                mavenCentral()
            }
        }
    """.trimIndent()
    writeContent(target.resolve("settings.gradle.kts"), content)
}

private fun writeMainClass(target: Path, qualifiedClassName: String) {
    val simpleName = qualifiedClassName.substringAfterLast('.')
    val packageName = qualifiedClassName.substringBeforeLast('.', "")

    val packageDeclaration = if (packageName.isEmpty()) {
        ""
    } else {
        "package $packageName;\n\n"
    }

    @Language("java")
    val classDeclaration = """
        public class $simpleName {
            public static void main(String[] args) {
                System.out.println("Test executed!");
            }
        }
    """.trimIndent()

    val content = packageDeclaration + classDeclaration
    val javaBasePath = target.resolve("src/main/java")
    val classFileName = Paths.get("$simpleName.java")
    val filePath = if (packageName.isEmpty()) {
        javaBasePath.resolve(classFileName)
    } else {
        val packagePath = Paths.get(packageName.replace('.', File.separatorChar))
        javaBasePath.resolve(packagePath).resolve(classFileName)
    }

    writeContent(filePath, content)
}

private fun writeWrapperProperties(target: Path, gradleVersion: String) {
    @Language("properties")
    val content = """
        distributionBase=GRADLE_USER_HOME
        distributionPath=wrapper/dists
        distributionUrl=https\://services.gradle.org/distributions/gradle-${gradleVersion}-bin.zip
        networkTimeout=10000
        validateDistributionUrl=true
        zipStoreBase=GRADLE_USER_HOME
        zipStorePath=wrapper/dists
    """.trimIndent()
    writeContent(target.resolve("gradle/wrapper/gradle-wrapper.properties"), content)
}

private fun writeContent(target: Path, content: String) {
    val contentWithLineBreak = content.trim() + "\n"
    Files.createDirectories(target.parent)
    Files.write(target, contentWithLineBreak.toByteArray())
}

private fun copyWrapper(target: Path, rootDir: Path) {
    copyTo(rootDir.resolve("gradlew"), target)
    copyToRecursive(rootDir, Paths.get("gradle/wrapper"), target)
}

private fun copyTo(source: Path, targetDir: Path) {
    Files.createDirectories(targetDir)
    val target = targetDir.resolve(source.fileName)
    Files.copy(source, target)
}

private fun copyToRecursive(sourceBaseDir: Path, sourceDir: Path, targetDir: Path) {
    Files.walk(sourceBaseDir.resolve(sourceDir)).use {
        for (source in it) {
            val relativeSource = sourceBaseDir.relativize(source)
            val target = targetDir.resolve(relativeSource)

            if (source.isDirectory()) {
                Files.createDirectories(target)
            } else {
                Files.copy(source, target)
            }
        }
    }
}
