package com.torchelos.app.core

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log

class Camera2TorchEngine(context: Context) : TorchEngine {

    companion object {
        private const val TAG = "Camera2TorchEngine"
    }

    private data class FlashCamera(
        val id: String,
        val maxStrength: Int,
        val defaultStrength: Int
    )

    private val cameraManager: CameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private val flashCamera: FlashCamera? = detectBackFlashCamera()

    override fun isAvailable(): Boolean = flashCamera != null

    override fun getMaxLevel(): Int = flashCamera?.maxStrength ?: 1

    override fun getDefaultLevel(): Int = flashCamera?.defaultStrength ?: 1

    override fun turnOn(level: Int): Boolean {
        val camera = flashCamera ?: return false
        return try {
            if (camera.maxStrength > 1) {
                cameraManager.turnOnTorchWithStrengthLevel(
                    camera.id,
                    level.coerceIn(1, camera.maxStrength)
                )
            } else {
                cameraManager.setTorchMode(camera.id, true)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "turnOn failed", e)
            false
        }
    }

    override fun setStrength(level: Int): Boolean {
        val camera = flashCamera ?: return false
        if (camera.maxStrength <= 1) return false
        return try {
            cameraManager.turnOnTorchWithStrengthLevel(
                camera.id,
                level.coerceIn(1, camera.maxStrength)
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "setStrength failed", e)
            false
        }
    }

    override fun turnOff(): Boolean {
        val camera = flashCamera ?: return false
        return try {
            cameraManager.setTorchMode(camera.id, false)
            true
        } catch (e: Exception) {
            Log.e(TAG, "turnOff failed", e)
            false
        }
    }

    private fun detectBackFlashCamera(): FlashCamera? {
        return try {
            cameraManager.cameraIdList.firstNotNullOfOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val hasFlash =
                    characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (!hasFlash || facing != CameraCharacteristics.LENS_FACING_BACK) {
                    return@firstNotNullOfOrNull null
                }

                val maxStrength = characteristics
                    .get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
                val defaultStrength = characteristics
                    .get(CameraCharacteristics.FLASH_INFO_STRENGTH_DEFAULT_LEVEL) ?: 1
                FlashCamera(id, maxStrength, defaultStrength)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Flash camera detection failed", e)
            null
        }
    }
}
