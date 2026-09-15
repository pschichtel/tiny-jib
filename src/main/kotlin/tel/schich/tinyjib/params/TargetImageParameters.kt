package tel.schich.tinyjib.params

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import javax.inject.Inject

abstract class TargetImageParameters @Inject constructor(objectFactory: ObjectFactory) : ImageParams(objectFactory) {
    @get:Input
    abstract val image: Property<String>

    @get:Input
    @get:Optional
    abstract val tags: SetProperty<String>

    @get:Input
    abstract val format: Property<ImageFormat>

    enum class ImageFormat { OCI, Docker }

    init {
        format.convention(ImageFormat.OCI)
    }
}
