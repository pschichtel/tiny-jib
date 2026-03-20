package tel.schich.tinyjib.util

data class ExecuteResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
)
