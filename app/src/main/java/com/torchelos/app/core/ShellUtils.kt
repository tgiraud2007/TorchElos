package com.torchelos.app.core

import android.util.Log
import com.topjohnwu.superuser.Shell
import java.io.BufferedReader
import java.io.InputStreamReader

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
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine()
            process.waitFor()
            line?.contains("uid=0") == true
        } catch (e: Exception) {
            false
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
