package tel.schich.tinyjib.util

import java.io.ByteArrayOutputStream
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.concurrent.thread
import kotlin.time.Duration

fun executeGradleDefaults(target: Path, args: List<String>, javaVersion: String, timeout: Duration): ExecuteResult {
    return executeGradle(target, args + listOf("--no-daemon", "--stacktrace"), javaVersion, timeout)
}

fun executeGradle(target: Path, args: List<String>, javaVersion: String, timeout: Duration): ExecuteResult {
    val javaHome = Paths.get(getProp("javaHome.$javaVersion"))
    val process = ProcessBuilder(listOf("./gradlew") + args)
        .directory(target.toFile())
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .apply {
            val env = environment()
            env.clear()
            env["JAVA_HOME"] = javaHome.toString()
        }
        .start()

    val stdoutBytes = ByteArrayOutputStream()
    val stdoutReader = thread {
        process.inputStream.copyTo(stdoutBytes, 8192)
    }

    val stderrBytes = ByteArrayOutputStream()
    val stderrReader = thread {
        process.errorStream.copyTo(stderrBytes, 8192)
    }

    val completedBeforeTimeout = process.waitFor(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
    if (!completedBeforeTimeout) {
        throw TimeoutException("Gradle execution timed out after $timeout")
    }

    stdoutReader.join()
    stderrReader.join()

    val stdout = stdoutBytes.toByteArray().decodeToString()
    val stderr = stderrBytes.toByteArray().decodeToString()

    return ExecuteResult(process.exitValue(), stdout, stderr)
}
