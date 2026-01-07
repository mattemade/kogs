package net.mattemade.utils.releasing

interface HasContext<ReleasingContext> {
    val context: Map<Any, ReleasingContext>
}
