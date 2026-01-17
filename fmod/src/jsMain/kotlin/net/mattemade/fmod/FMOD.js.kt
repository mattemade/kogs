package net.mattemade.fmod

actual fun systemCreate(): Long {
    println("create system")
    return 0L
}

actual fun systemRelease(system: Long) {
    println("release system $system")
}