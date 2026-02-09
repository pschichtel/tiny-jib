package tel.schich.tinyjib.params

import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.kotlin.dsl.newInstance

abstract class ImageParams(objectFactory: ObjectFactory) {
    @get:Optional
    @get:Nested
    val auth: AuthParameters = objectFactory.newInstance()

    @get:Optional
    @get:Nested
    val credHelper: CredHelperParameters = objectFactory.newInstance()
}