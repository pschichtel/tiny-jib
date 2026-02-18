package tel.schich.tinyjib.jib

import kotlinx.serialization.Serializable

@Serializable
data class ImageMetadataOutput(
    val image: String,
    val imageId: String,
    val imageDigest: String,
    val tags: List<String>,
    val imagePushed: Boolean,
)
