package com.timewgui.domain.system

import java.io.File

/**
 * Manages a macOS Launch Agent plist for starting the app at login.
 * Creates/removes `~/Library/LaunchAgents/com.timewgui.plist`.
 */
object LaunchAtLogin {

    private const val PLIST_NAME = "com.timewgui.plist"

    private val plistFile: File
        get() {
            val home = System.getProperty("user.home")
            return File(home, "Library/LaunchAgents/$PLIST_NAME")
        }

    fun enable() {
        val appPath = detectAppPath() ?: return
        val plistDir = plistFile.parentFile
        if (!plistDir.exists()) plistDir.mkdirs()

        val plistContent = """
            |<?xml version="1.0" encoding="UTF-8"?>
            |<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
            |<plist version="1.0">
            |<dict>
            |    <key>Label</key>
            |    <string>com.timewgui</string>
            |    <key>ProgramArguments</key>
            |    <array>
            |        <string>$appPath</string>
            |    </array>
            |    <key>RunAtLoad</key>
            |    <true/>
            |    <key>KeepAlive</key>
            |    <false/>
            |</dict>
            |</plist>
        """.trimMargin()

        plistFile.writeText(plistContent)
    }

    fun disable() {
        if (plistFile.exists()) {
            plistFile.delete()
        }
    }

    fun isEnabled(): Boolean = plistFile.exists()

    private fun detectAppPath(): String? {
        // Prefer the process command (works for packaged .app bundles)
        ProcessHandle.current().info().command().orElse(null)?.let { return it }
        // Fallback to java.class.path (works for JAR execution)
        val classPath = System.getProperty("java.class.path") ?: return null
        return if (classPath.endsWith(".jar")) "java -jar $classPath" else null
    }
}
