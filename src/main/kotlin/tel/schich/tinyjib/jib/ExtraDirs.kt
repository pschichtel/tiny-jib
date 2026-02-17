package tel.schich.tinyjib.jib

import com.google.cloud.tools.jib.api.JavaContainerBuilder
import com.google.cloud.tools.jib.api.JibContainerBuilder
import com.google.cloud.tools.jib.api.buildplan.AbsoluteUnixPath
import com.google.cloud.tools.jib.api.buildplan.FileEntriesLayer
import com.google.cloud.tools.jib.api.buildplan.FilePermissions
import com.google.cloud.tools.jib.api.buildplan.ModificationTimeProvider
import com.google.cloud.tools.jib.filesystem.DirectoryWalker
import tel.schich.tinyjib.TinyJibExtension
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.PathMatcher
import java.nio.file.Paths
import java.util.function.Predicate
import kotlin.collections.mapValues
import kotlin.collections.orEmpty

private fun determinePermissions(
    path: AbsoluteUnixPath,
    extraDirectoryPermissions: Map<String, FilePermissions>,
    permissionsPathMatchers: Iterable<Pair<PathMatcher, FilePermissions>>,
): FilePermissions? {
    // The check is only for optimization. (`permissionsPathMatchers` is constructed from the map.)
    val permissions = extraDirectoryPermissions[path.toString()]
    if (permissions != null) {
        return permissions
    }

    // Check for matching globs
    val localPath = Paths.get(path.toString())
    for ((key, value) in permissionsPathMatchers) {
        if (key.matches(localPath)) {
            return value
        }
    }
    return null
}

private fun extraDirectoryLayerConfiguration(
    sourceDirectory: Path,
    targetDirectory: AbsoluteUnixPath,
    includes: MutableList<String>,
    excludes: MutableList<String>,
    extraDirectoryPermissions: Map<String, FilePermissions>,
    modificationTimeProvider: ModificationTimeProvider
): FileEntriesLayer {
    fun matchPath(path: String): PathMatcher {
        return FileSystems.getDefault().getPathMatcher("glob:$path")
    }

    val permissionsPathMatchers = extraDirectoryPermissions.map { (path, permissions) ->
        matchPath(path) to permissions
    }

    val walker = DirectoryWalker(sourceDirectory).filterRoot()

    fun matchers(patterns: List<String>): Sequence<Predicate<Path>> = patterns.asSequence()
        .map { path ->
            val matcher = matchPath(path)
            Predicate { path ->
                matcher.matches(sourceDirectory.relativize(path))
            }
        }

    // add exclusion filters
    matchers(excludes)
        .map { it.negate() }
        .forEach { p -> walker.filter(p) }
    // add an inclusion filter
    if (includes.isNotEmpty()) {
        matchers(includes)
            .reduce { acc, predicate -> acc.or(predicate) }
            .let { walker.filter(it) }
    }

    // walk the source tree and add layer entries
    return FileEntriesLayer.builder().apply {
        setName(JavaContainerBuilder.LayerType.EXTRA_FILES.getName())

        walker.walk { localPath ->
            val pathOnContainer = targetDirectory
                .resolve(sourceDirectory.relativize(localPath))

            val modificationTime = modificationTimeProvider.get(localPath, pathOnContainer)
            val permissions = determinePermissions(pathOnContainer, extraDirectoryPermissions, permissionsPathMatchers)
            if (permissions != null) {
                addEntry(localPath, pathOnContainer, permissions, modificationTime)
            } else {
                addEntry(localPath, pathOnContainer, modificationTime)
            }
        }
    }.build()
}

fun JibContainerBuilder.configureExtraDirectoryLayers(extension: TinyJibExtension, modificationTimeProvider: ModificationTimeProvider) {
    val extraDirs = extension.extraDirectories
    val permissions = extraDirs.permissions.get()
        .mapValues { FilePermissions.fromOctalString(it.value) }
    for (path in extraDirs.paths.orNull.orEmpty()) {
        val from = path.from.get().toPath()
        if (Files.exists(from)) {
            val layer = extraDirectoryLayerConfiguration(
                from,
                AbsoluteUnixPath.get(path.into.get()),
                path.includes.get(),
                path.excludes.get(),
                permissions,
                modificationTimeProvider,
            )
            addFileEntriesLayer(layer)
        }
    }
}
