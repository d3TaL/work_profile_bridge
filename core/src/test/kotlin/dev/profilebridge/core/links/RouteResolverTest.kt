package dev.profilebridge.core.links

import org.junit.Assert.*
import org.junit.Test

class RouteResolverTest {
    private fun link(host: String, path: String = "/") = (SafeLink.parse("https://$host$path") as LinkParseResult.Accepted).link
    private fun rule(id: String, host: String = "example.invalid", sub: Boolean = true, path: String = "") =
        Route(id, host, sub, path, "endpoint", "example.target.app")
    @Test fun `matches subdomain boundaries`() {
        assertEquals(RouteResolution.NoMatch, RouteResolver.resolve(link("evilexample.invalid"), listOf(rule("a"))))
        assertTrue(RouteResolver.resolve(link("sub.example.invalid"), listOf(rule("a"))) is RouteResolution.Match)
        assertEquals(RouteResolution.NoMatch, RouteResolver.resolve(link("sub.example.invalid"), listOf(rule("a", sub = false))))
    }
    @Test fun `exact host wins`() {
        val result = RouteResolver.resolve(link("sub.example.invalid", "/items/1"), listOf(rule("broad", path = "/items/"), rule("exact", host = "sub.example.invalid"))) as RouteResolution.Match
        assertEquals("exact", result.route.id)
    }
    @Test fun `path specificity is case sensitive`() {
        val result = RouteResolver.resolve(link("example.invalid", "/items/1"), listOf(rule("root"), rule("specific", path = "/items/"), rule("wrong", path = "/ITEMS/"))) as RouteResolution.Match
        assertEquals("specific", result.route.id)
    }
    @Test fun `equal priority is ambiguous regardless of input order`() {
        val rules = listOf(rule("b"), rule("a"))
        val expected = RouteResolution.Ambiguous(listOf("a", "b"))
        assertEquals(expected, RouteResolver.resolve(link("example.invalid"), rules))
        assertEquals(expected, RouteResolver.resolve(link("example.invalid"), rules.reversed()))
    }
}
