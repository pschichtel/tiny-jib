package tel.schich.tinyjib.jib

import com.google.cloud.tools.jib.api.buildplan.AbsoluteUnixPath
import com.google.cloud.tools.jib.api.buildplan.ModificationTimeProvider
import java.nio.file.Path
import java.time.Instant
import java.time.format.DateTimeFormatter

const val CONSTANT_MOD_TIME = "EPOCH_PLUS_SECOND"

class SimpleModificationTimeProvider(modTime: String) : ModificationTimeProvider {
    private val modTimeInstant: Instant = if (modTime == CONSTANT_MOD_TIME) {
        Instant.ofEpochSecond(1)
    } else {
        DateTimeFormatter.ISO_DATE_TIME.parse(modTime, Instant::from)
    }

    override fun get(sourcePath: Path?, destinationPath: AbsoluteUnixPath?): Instant = modTimeInstant
}
