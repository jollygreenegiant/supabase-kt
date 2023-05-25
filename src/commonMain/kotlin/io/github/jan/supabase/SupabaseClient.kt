package io.github.jan.supabase

import io.github.aakira.napier.Napier
import io.github.jan.supabase.annotiations.SupabaseInternal
import io.github.jan.supabase.network.KtorSupabaseHttpClient
import io.github.jan.supabase.plugins.PluginManager
import io.github.jan.supabase.plugins.SupabasePlugin
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine

/**
 * The main class to interact with Supabase.
 *
 * To add functionality, add plugins like **GoTrue** or **Functions** within the [SupabaseClientBuilder]
 */
sealed interface SupabaseClient {

    /**
     * The supabase url with either a http or https scheme.
     */
    val supabaseHttpUrl: String

    /**
     * The base supabase url without any scheme
     */
    val supabaseUrl: String

    /**
     * The api key for interacting with the supabase api
     */
    val supabaseKey: String

    /**
     * The plugin manager is used to manage installed plugins
     */
    val pluginManager: PluginManager

    /**
     * The http client used to interact with the supabase api
     */
    val httpClient: KtorSupabaseHttpClient

    /**
     * Whether [supabaseHttpUrl] uses https
     */
    val useHTTPS: Boolean

    /**
     * Releases all resources held by the [httpClient] and all plugins the [pluginManager]
     */
    suspend fun close()

}

internal class SupabaseClientImpl(
    override val supabaseUrl: String,
    override val supabaseKey: String,
    plugins: Map<String, (SupabaseClient) -> SupabasePlugin>,
    httpConfigOverrides: MutableList<HttpClientConfig<*>.() -> Unit>,
    override val useHTTPS: Boolean,
    requestTimeout: Long,
    httpEngine: HttpClientEngine?,
) : SupabaseClient {

    init {
        Napier.i {
            "SupabaseClient created! Please report any bugs you find."
        }
    }

    override val supabaseHttpUrl: String = if (useHTTPS) {
        "https://$supabaseUrl"
    } else {
        "http://$supabaseUrl"
    }

 //   override val coroutineContext = Dispatchers.Default + SupervisorJob()

    override val pluginManager = PluginManager(plugins.toList().associate { (key, value) ->
        key to value(this)
    })

    @OptIn(SupabaseInternal::class)
    override val httpClient = KtorSupabaseHttpClient(supabaseKey, httpConfigOverrides, requestTimeout, httpEngine)

    override suspend fun close() {
        httpClient.close()
        pluginManager.closeAllPlugins()
    }

}

