package tel.schich.tinyjib.util

internal fun getProp(name: String): String {
    val fullName = "tinyjib.$name"
    return System.getProperty(fullName, null)?.trim()?.ifEmpty { null }
        ?: error("Property $fullName not set!")
}

internal fun escapeKotlinString(s: String): String {
    return s.replace("\\", "\\\\").replace("\"", "\\\"")
}
