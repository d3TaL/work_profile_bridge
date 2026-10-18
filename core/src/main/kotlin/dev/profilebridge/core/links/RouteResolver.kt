package dev.profilebridge.core.links

import java.util.Locale

data class Route(
    val id: String,
    val host: String,
    val includeSubdomains: Boolean,
    val rawPathPrefix: String,
    val endpointId: String,
    val targetPackage: String,
    val priority: Int = 0,
) {
    init {
        require(id.isNotBlank() && endpointId.isNotBlank())
        val parsed = SafeLink.parse("https://$host/")
        require(parsed is LinkParseResult.Accepted && parsed.link.host == host.lowercase(Locale.ROOT))
        require(rawPathPrefix.isEmpty() || rawPathPrefix.startsWith('/'))
        require(targetPackage.matches(Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")))
    }
}
sealed interface RouteResolution {
    data object NoMatch : RouteResolution
    data class Match(val route: Route) : RouteResolution
    data class Ambiguous(val routeIds: List<String>) : RouteResolution
}
object RouteResolver {
    fun resolve(link: SafeLink, routes: List<Route>): RouteResolution {
        val matching = routes.filter {
            val host = it.host.lowercase(Locale.ROOT)
            (link.host == host || it.includeSubdomains && link.host.endsWith(".$host")) &&
                link.rawPath.startsWith(it.rawPathPrefix)
        }
        if (matching.isEmpty()) return RouteResolution.NoMatch
        val order = compareBy<Route>(
            { if (link.host == it.host.lowercase(Locale.ROOT)) 1 else 0 },
            { it.rawPathPrefix.length }, { it.priority },
        )
        val best = matching.maxWith(order)
        val tied = matching.filter { order.compare(it, best) == 0 }
        return if (tied.size == 1) RouteResolution.Match(best)
        else RouteResolution.Ambiguous(tied.map { it.id }.sorted())
    }
}
