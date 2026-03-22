package tel.schich.tinyjib.util

import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.concurrent.thread
import kotlin.time.Duration

fun runCommand(command: List<String>, timeout: Duration, customize: ProcessBuilder.() -> Unit = {}): ExecuteResult {
    val process = ProcessBuilder(command)
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .apply {
            customize()
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
        throw TimeoutException("Command $command timeout out after $timeout")
    }

    stdoutReader.join()
    stderrReader.join()

    val stdout = stdoutBytes.toByteArray().decodeToString()
    val stderr = stderrBytes.toByteArray().decodeToString()

    return ExecuteResult(process.exitValue(), stdout, stderr)
}
