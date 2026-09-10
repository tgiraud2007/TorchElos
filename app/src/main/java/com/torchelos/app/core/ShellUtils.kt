package com.torchelos.app.core

import android.util.Log
import com.topjohnwu.superuser.Shell
import java.util.concurrent.TimeUnit

data class ShellResult(
    val isSuccess: Boolean,
    val output: String
)

object ShellUtils {
    private const val TAG = "ShellUtils"
    private const val SU_TIMEOUT_SECONDS = 10L

    const val ROOT_NONE = "Not detected"
    const val ROOT_KERNELSU = "KernelSU"
    const val ROOT_MAGISK = "Magisk"
    const val ROOT_APATCH = "APatch"
    const val ROOT_GRANTED = "Root granted"

    @Volatile
    private var rootShell: Shell? = null

    fun isRootAvailable(): Boolean =
        libsuRootState() == true || suBinaryRootState()

    fun detectRootSolution(): String {
        if (!isRootAvailable()) return ROOT_NONE

        val version = execSu("su -v")
            .takeIf { it.isSuccess && it.output.isNotBlank() }
            ?.output
            ?: execSu("su -V").output
        val normalized = version.uppercase()
        return when {
            normalized.contains("KSU") || normalized.contains("KERNELSU") -> ROOT_KERNELSU
            normalized.contains("MAGISK") -> ROOT_MAGISK
            normalized.contains("APATCH") -> ROOT_APATCH
            else -> detectRootByFiles()
        }
    }

    fun execSu(command: String): ShellResult {
        val shell = obtainRootShell() ?: return execSuDirect(command)
        return try {
            val result = shell.newJob().add(command).exec()
            ShellResult(result.isSuccess, result.out.joinToString("\n"))
        } catch (e: Exception) {
            Log.w(TAG, "libsu execution failed, falling back to direct su", e)
            execSuDirect(command)
        }
    }

    fun execSuAll(vararg commands: String): Boolean =
        commands.all { execSu(it).isSuccess }

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

    private fun libsuRootState(): Boolean? = try {
        Shell.isAppGrantedRoot()
    } catch (e: Exception) {
        Log.w(TAG, "Root state check failed", e)
        null
    }

    private fun suBinaryRootState(): Boolean {
        val result = runSuCommand("id")
        return result.isSuccess && result.output.contains("uid=0")
    }

    private fun detectRootByFiles(): String {
        if (execSu("test -d /data/adb/ksu").isSuccess) return ROOT_KERNELSU
        if (execSu("test -f /data/adb/ap/bin/apd").isSuccess) return ROOT_APATCH
        if (execSu("test -d /data/adb/magisk").isSuccess) return ROOT_MAGISK
        return ROOT_GRANTED
    }

    private fun execSuDirect(command: String): ShellResult = runSuCommand(command)

    private fun runSuCommand(command: String): ShellResult {
        var process: Process? = null
        return try {
            process = ProcessBuilder("su", "-c", command)
                .redirectErrorStream(true)
                .start()

            val output = StringBuilder()
            val reader = Thread {
                try {
                    process.inputStream.bufferedReader().use { output.append(it.readText()) }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to read su output", e)
                }
            }
            reader.start()

            if (!process.waitFor(SU_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                reader.interrupt()
                return ShellResult(false, "su command timed out")
            }
            reader.join(1000)
            ShellResult(process.exitValue() == 0, output.toString().trim())
        } catch (e: Exception) {
            Log.e(TAG, "Direct su execution failed: $command", e)
            process?.destroyForcibly()
            ShellResult(false, e.message.orEmpty())
        }
    }
}
