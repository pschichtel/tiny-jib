package tel.schich.tinyjib.params

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

class BaseImageParameters @Inject constructor(objectFactory: ObjectFactory) : ImageParams {
    @get:Optional
    @get:Nested
    override val auth: AuthParameters = objectFactory.newInstance()

    @Input
    val image: Property<String> = objectFactory.property()

    @get:Optional
    @get:Nested
    override val credHelper: CredHelperParameters = objectFactory.newInstance()

    @get:Optional
    @get:Nested
    val platforms: ListProperty<PlatformParameters> = objectFactory.listProperty()
}
