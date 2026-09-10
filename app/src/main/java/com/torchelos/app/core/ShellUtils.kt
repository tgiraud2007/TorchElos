package com.torchelos.app.core

import android.util.Log
import com.topjohnwu.superuser.Shell

object ShellUtils {
    private const val TAG = "TorchShellUtils"

    fun isRootAvailable(): Boolean {
        return try {
            Shell.isAppGrantedRoot() == true || checkSuBinary()
        } catch (e: Exception) {
            Log.w(TAG, "Erreur vérification root", e)
            false
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

    fun detectRootSolution(): String {
        if (!isRootAvailable()) return "Not detected"
        return try {
            val res = execSu("su -v 2>/dev/null || su -V 2>/dev/null")
            val output = res.output.uppercase()
            when {
                output.contains("MAGISK") -> "Operational (Magisk)"
                output.contains("KSU") || output.contains("KERNELSU") -> "Operational (KernelSU)"
                output.contains("APATCH") -> "Operational (APatch)"
                else -> {
                    val ksuCheck = execSu("test -f /system/bin/ksud -o -d /data/adb/ksu && echo KSU")
                    if (ksuCheck.output.contains("KSU")) {
                        "Operational (KernelSU)"
                    } else {
                        val apatchCheck = execSu("test -f /data/adb/ap/bin/apd && echo APATCH")
                        if (apatchCheck.output.contains("APATCH")) {
                            "Operational (APatch)"
                        } else {
                            val magiskCheck = execSu("test -d /data/adb/magisk && echo MAGISK")
                            if (magiskCheck.output.contains("MAGISK")) {
                                "Operational (Magisk)"
                            } else {
                                "Operational (Root granted)"
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            "Operational (Root granted)"
        }
    }

    fun execSu(command: String): ShellResult {
        return try {
            // Méthode 1 : via libsu
            val result = Shell.cmd(command).exec()
            if (result.isSuccess) {
                ShellResult(true, result.out.joinToString("\n"), result.code)
            } else {
                // Fallback direct Runtime su
                execSuDirect(command)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Erreur libsu, tentative direct su: ${e.message}")
            execSuDirect(command)
        }
    }

    private fun execSuDirect(command: String): ShellResult {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val output = process.inputStream.bufferedReader().readText().trim()
            val error = process.errorStream.bufferedReader().readText().trim()
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                ShellResult(true, output, 0)
            } else {
                ShellResult(false, if (error.isNotEmpty()) error else output, exitCode)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur exécution direct su: $command", e)
            ShellResult(false, e.message ?: "Erreur inconnue", -1)
        }
    }
}

data class ShellResult(
    val isSuccess: Boolean,
    val output: String,
    val exitCode: Int
)
