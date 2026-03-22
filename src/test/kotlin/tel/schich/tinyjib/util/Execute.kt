package tel.schich.tinyjib.util

import java.nio.file.Path
import java.nio.file.Paths
import kotlin.time.Duration

fun executeGradleDefaults(target: Path, args: List<String>, javaVersion: String, timeout: Duration): ExecuteResult {
    return executeGradle(target, args + listOf("--no-daemon", "--stacktrace"), javaVersion, timeout)
}

fun executeGradle(target: Path, args: List<String>, javaVersion: String, timeout: Duration): ExecuteResult {
    val javaHome = Paths.get(getProp("javaHome.$javaVersion"))
    return runCommand(listOf("./gradlew") + args, timeout) {
        directory(target.toFile())
        val env = environment()
        env.clear()
        env["JAVA_HOME"] = javaHome.toString()
    }
}
