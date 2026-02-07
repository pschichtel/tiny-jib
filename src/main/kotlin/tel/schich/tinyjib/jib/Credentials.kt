package tel.schich.tinyjib.jib

import com.google.cloud.tools.jib.api.Credential
import com.google.cloud.tools.jib.api.ImageReference
import com.google.cloud.tools.jib.api.RegistryImage
import com.google.cloud.tools.jib.frontend.CredentialRetrieverFactory
import com.google.cloud.tools.jib.gradle.AuthParameters
import com.google.cloud.tools.jib.gradle.CredHelperParameters
import org.gradle.api.Task
import tel.schich.tinyjib.jib.adaptLogs
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.collections.orEmpty

private val DOCKER_CONFIG_FILE = Paths.get("config.json")
// for Kubernetes: https://github.com/GoogleContainerTools/jib/issues/2260
private val KUBERNETES_DOCKER_CONFIG_FILE = Paths.get(".dockerconfigjson")
private val LEGACY_DOCKER_CONFIG_FILE = Paths.get(".dockercfg")
// for Podman: https://www.mankier.com/5/containers-auth.json
private val XDG_AUTH_FILE = Paths.get("containers").resolve("auth.json")

fun getCredentials(params: AuthParameters): Credential? {
    val username = params.username
    if (username.isNullOrBlank()) {
        return null
    }
    val password = params.password
    if (password.isNullOrBlank()) {
        return null
    }
    return Credential.from(username, password)
}

private fun addConfigBasedRetrievers(credRetrieverFactory: CredentialRetrieverFactory, image: RegistryImage) {

    fun getEnv(name: String): Path? {
        return System.getenv(name)?.ifEmpty { null }?.let(Paths::get)
    }
    fun getProp(name: String): Path? {
        return System.getProperty(name)?.ifEmpty { null }?.let(Paths::get)
    }

    fun getDockerFiles(configDir: Path) = listOf(
        configDir.resolve(DOCKER_CONFIG_FILE),
        configDir.resolve(KUBERNETES_DOCKER_CONFIG_FILE),
        configDir.resolve(LEGACY_DOCKER_CONFIG_FILE)
    )

    val dockerConfigFiles = buildList {
        getEnv("XDG_RUNTIME_DIR")?.let {
            add(it.resolve(XDG_AUTH_FILE))
        }

        getEnv("XDG_CONFIG_HOME")?.let {
            add(it.resolve(XDG_AUTH_FILE))
        }

        for (home in listOfNotNull(getProp("user.home"), getEnv("HOME"))) {
            add(home.resolve(".config").resolve(XDG_AUTH_FILE))
            addAll(getDockerFiles(home.resolve(".docker")))
        }

        getEnv("DOCKER_CONFIG")?.let {
            addAll(getDockerFiles(it))
        }
    }

    for (path in dockerConfigFiles) {
        val retriever = if (path.endsWith(LEGACY_DOCKER_CONFIG_FILE)) {
            credRetrieverFactory.legacyDockerConfig(path)
        } else {
            credRetrieverFactory.dockerConfig(path)
        }
        image.addCredentialRetriever(retriever)
    }
}

fun Task.configureCredentialRetrievers(imageRef: ImageReference, image: RegistryImage, authParams: AuthParameters?, credHelperParams: CredHelperParameters?) {
    val credHelperEnv = credHelperParams?.environment.orEmpty()
    val credHelperFactory = CredentialRetrieverFactory.forImage(imageRef, adaptLogs(), credHelperEnv)
    if (authParams != null) {
        getCredentials(authParams)?.let {
            image.addCredentialRetriever(credHelperFactory.known(it, authParams.authDescriptor))
        }
    }
    credHelperParams?.helper?.let { helperName ->
        val helperBinaryPath = Paths.get(helperName)
        if (Files.isExecutable(helperBinaryPath)) {
            image.addCredentialRetriever(credHelperFactory.dockerCredentialHelper(helperBinaryPath))
        } else {
            image.addCredentialRetriever(credHelperFactory.dockerCredentialHelper("docker-credential-$helperName"))
        }
    }

    addConfigBasedRetrievers(credHelperFactory, image)

    image.addCredentialRetriever(credHelperFactory.wellKnownCredentialHelpers())
    image.addCredentialRetriever(credHelperFactory.googleApplicationDefaultCredentials())
}