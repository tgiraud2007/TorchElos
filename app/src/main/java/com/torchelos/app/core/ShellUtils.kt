package com.torchelos.app.core

import android.util.Log
import com.topjohnwu.superuser.Shell

data class ShellResult(
    val isSuccess: Boolean,
    val output: String
)

object ShellUtils {
    private const val TAG = "ShellUtils"

    const val ROOT_NONE = "Not detected"
    const val ROOT_KERNELSU = "KernelSU"
    const val ROOT_MAGISK = "Magisk"
    const val ROOT_APATCH = "APatch"
    const val ROOT_GRANTED = "Root granted"

    @Volatile
    private var rootShell: Shell? = null

    @Volatile
    private var rootConfirmed = false

    fun isRootAvailable(): Boolean {
        if (rootConfirmed) return true

        val available = libsuRootState() == true || suBinaryRootState()
        if (available) {
            rootConfirmed = true
        }
        return available
    }

    private fun libsuRootState(): Boolean? = try {
        Shell.isAppGrantedRoot()
    } catch (e: Exception) {
        Log.w(TAG, "Root state check failed", e)
        null
    }

    private fun suBinaryRootState(): Boolean = checkSuBinary()

    fun detectRootSolution(): String {
        if (!isRootAvailable()) return ROOT_NONE

        val version = execSu("su -v").output.ifBlank { execSu("su -V").output }.uppercase()
        return when {
            version.contains("KSU") || version.contains("KERNELSU") -> ROOT_KERNELSU
            version.contains("MAGISK") -> ROOT_MAGISK
            version.contains("APATCH") -> ROOT_APATCH
            else -> detectRootByFiles()
        }
    }

    fun execSu(command: String): ShellResult {
        val shell = obtainRootShell()
        if (shell != null) {
            try {
                val result = shell.newJob().add(command).exec()
                if (result.isSuccess) {
                    return ShellResult(true, result.out.joinToString("\n"))
                }
                Log.w(TAG, "libsu command failed, falling back to direct su")
            } catch (e: Exception) {
                Log.w(TAG, "libsu execution failed, falling back to direct su", e)
            }
        }
        return execSuDirect(command)
    }

    private fun obtainRootShell(): Shell? {
        rootShell?.let { return it }
        return try {
            val shell = Shell.getShell()
            if (shell.isRoot) {
                rootShell = shell
                shell
            } else {
                Log.w(TAG, "libsu shell is not privileged, using direct su")
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to obtain root shell", e)
            null
        }
    }

    private fun checkSuBinary(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val line = process.inputStream.bufferedReader().use { it.readLine() }
            process.waitFor()
            line?.contains("uid=0") == true
        } catch (e: Exception) {
            false
        }
    }

    private fun detectRootByFiles(): String {
        if (execSu("test -d /data/adb/ksu").isSuccess) return ROOT_KERNELSU
        if (execSu("test -f /data/adb/ap/bin/apd").isSuccess) return ROOT_APATCH
        if (execSu("test -d /data/adb/magisk").isSuccess) return ROOT_MAGISK
        return ROOT_GRANTED
    }

    private fun execSuDirect(command: String): ShellResult {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
            val error = process.errorStream.bufferedReader().use { it.readText() }.trim()
            val exitCode = process.waitFor()
            val success = exitCode == 0
            ShellResult(success, if (success) output else error.ifEmpty { output })
        } catch (e: Exception) {
            Log.e(TAG, "Direct su execution failed: $command", e)
            ShellResult(false, e.message.orEmpty())
        }
    }
}
