package tel.schich.tinyjib.params

import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional

abstract class ImageParams(objectFactory: ObjectFactory) {
    @get:Optional
    @get:Nested
    val auth: AuthParameters = objectFactory.newInstance(AuthParameters::class.java)

    @get:Optional
    @get:Nested
    val credHelper: CredHelperParameters = objectFactory.newInstance(CredHelperParameters::class.java)

    fun auth(block: AuthParameters.() -> Unit): Unit = auth.block()
    fun credHelper(block: CredHelperParameters.() -> Unit): Unit = credHelper.block()
}
