package tel.schich.tinyjib.jib

import com.google.cloud.tools.jib.api.JavaContainerBuilder
import com.google.cloud.tools.jib.api.JibContainerBuilder
import com.google.cloud.tools.jib.api.buildplan.AbsoluteUnixPath
import com.google.cloud.tools.jib.api.buildplan.FileEntriesLayer
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

private const val JIB_CLASSPATH_FILE = "jib-classpath-file"
private const val JIB_MAIN_CLASS_FILE = "jib-main-class-file"

private fun writeFileConservatively(file: Path, content: String) {
    if (Files.exists(file)) {
        val oldContent = Files.readAllBytes(file).decodeToString()
        if (oldContent == content) {
            return
        }
    }
    Files.createDirectories(file.parent)
    Files.write(file, content.encodeToByteArray())
}

private fun JibContainerBuilder.addJvmArgFilesLayer(
    cacheDir: Path,
    appRoot: AbsoluteUnixPath,
    classpath: String,
    mainClass: String
) {
    val classpathFile: Path = cacheDir.resolve(JIB_CLASSPATH_FILE)
    val mainClassFile: Path = cacheDir.resolve(JIB_MAIN_CLASS_FILE)

    // It's perfectly fine to always generate a new temp file or rewrite an existing file. However,
    // fixing the source file path and preserving the file timestamp prevents polluting the Jib
    // layer cache space by not creating new cache selectors every time. (Note, however, creating
    // new selectors does not affect correctness at all.)
    writeFileConservatively(classpathFile, classpath)
    writeFileConservatively(mainClassFile, mainClass)

    val layer = FileEntriesLayer.builder()
        .setName(JavaContainerBuilder.LayerType.JVM_ARG_FILES.getName())
        .addEntry(classpathFile, appRoot.resolve(JIB_CLASSPATH_FILE))
        .addEntry(mainClassFile, appRoot.resolve(JIB_MAIN_CLASS_FILE))
        .build()

    addFileEntriesLayer(layer)
}

fun JibContainerBuilder.configureEntrypoint(
    cacheDir: Path,
    appRoot: String,
    entrypoint: List<String>?,
    mainClass: String,
    jvmFlags: List<String>,
    dependencies: List<Path>,
    extraClasspath: List<String>,
) {
    val appRoot = AbsoluteUnixPath.get(appRoot)
    val classpath = mutableListOf<String>()
    classpath.addAll(extraClasspath)
    classpath.add(appRoot.resolve("resources").toString())
    classpath.add(appRoot.resolve("classes").toString())

    val duplicates = dependencies.asSequence()
        .map { path -> path.fileName.toString() }
        .groupBy { it }
        .mapValues { it.value.size }
        .filter { (_, value) -> value > 1 }
        .map { (key) -> key }
        .toSet()

    val libsPath = appRoot.resolve("libs")
    for (jar in dependencies) {
        // Handle duplicates by appending filesize to the end of the file. This renaming logic
        // must be in sync with the code that does the same in the other place. See
        // https://github.com/GoogleContainerTools/jib/issues/3331
        val originalName = jar.fileName.toString()
        val jarName = if (originalName in duplicates) {
            "${jar.nameWithoutExtension}-${Files.size(jar)}.${jar.extension}"
        } else {
            originalName
        }
        classpath.add(libsPath.resolve(jarName).toString())
    }

    val classpathString = classpath.joinToString(":")
    addJvmArgFilesLayer(cacheDir, appRoot, classpathString, mainClass)

    val entrypoint = if (entrypoint != null) {
        if (entrypoint.size == 1 && entrypoint[0] == "INHERIT") {
            null
        } else {
            entrypoint
        }
    } else {
        buildList {
            add("java")
            addAll(jvmFlags)
            add("-cp")
            add("@" + appRoot.resolve(JIB_CLASSPATH_FILE))
            add(mainClass)
        }
    }
    setEntrypoint(entrypoint)

}