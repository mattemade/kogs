package net.mattemade.fmod

import org.lwjgl.PointerBuffer
import org.lwjgl.fmod.FMOD.FMOD_VERSION
import org.lwjgl.fmod.FMODStudio.FMOD_Studio_System_Create
import org.lwjgl.fmod.FMODStudio.FMOD_Studio_System_Release

actual fun systemCreate(): Long {
    val buffer = PointerBuffer.allocateDirect(1)
    FMOD_Studio_System_Create(buffer, FMOD_VERSION)
    return buffer.get()
}

actual fun systemRelease(system: Long) {
    FMOD_Studio_System_Release(system)
}