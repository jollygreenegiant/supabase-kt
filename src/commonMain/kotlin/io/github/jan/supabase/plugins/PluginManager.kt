package io.github.jan.supabase.plugins

import io.github.jan.supabase.SupabaseClientBuilder

/**
 * The plugin manager is used to manage installed plugins
 * @param installedPlugins A map of installed plugins. You can install plugins by using the [SupabaseClientBuilder.install] method
 */
class PluginManager(val installedPlugins: Map<String, SupabasePlugin>) {

    /**
     * Retrieve an installed plugin using it's [Provider] or null if no such plugin is installed
     */
    inline fun <reified Plugin: SupabasePlugin, Config, Provider : SupabasePluginProvider<Config, Plugin>> getPluginOrNull(provider: Provider): Plugin? {
        return installedPlugins[provider.key] as? Plugin
    }

    /**
     * Retrieve an installed plugin using it's [Provider]
     */
    inline fun <reified Plugin: SupabasePlugin, Config, Provider : SupabasePluginProvider<Config, Plugin>> getPlugin(provider: Provider): Plugin {
        return getPluginOrNull(provider) ?: error("Plugin ${provider.key} not installed or not of type ${Plugin::class.simpleName}. Consider installing ${Plugin::class.simpleName} within your SupabaseClientBuilder")
    }

    /**
     * Closes all installed plugins
     */
    suspend inline fun closeAllPlugins() {
        installedPlugins.values.forEach { it.close() }
    }

}