package com.torchelos.app.core

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.Log

class Camera2TorchEngine(private val context: Context) : TorchEngine {

    companion object {
        private const val TAG = "Camera2TorchEngine"
    }

    private val cameraManager: CameraManager by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    private var cameraIdWithFlash: String? = null
    private var maxStrength: Int = 1
    private var defaultStrength: Int = 1

    init {
        detectFlashCamera()
    }

    private fun detectFlashCamera() {
        try {
            for (id in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)

                if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraIdWithFlash = id

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        maxStrength = characteristics.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
                        defaultStrength = characteristics.get(CameraCharacteristics.FLASH_INFO_STRENGTH_DEFAULT_LEVEL) ?: 1
                    }
                    Log.d(TAG, "Back flash camera found ID: $id, maxStrength: $maxStrength")
                    break
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting flash camera", e)
        }
    }

    override fun isAvailable(): Boolean = cameraIdWithFlash != null

    override fun getMaxLevel(): Int = maxStrength
    override fun getMinLevel(): Int = 1
    override fun getDefaultLevel(): Int = defaultStrength

    override fun turnOn(level: Int): Boolean {
        val camId = cameraIdWithFlash ?: return false
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && maxStrength > 1) {
                val clamped = level.coerceIn(1, maxStrength)
                cameraManager.turnOnTorchWithStrengthLevel(camId, clamped)
            } else {
                cameraManager.setTorchMode(camId, true)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error turnOn Camera2", e)
            false
        }
    }

    override fun setStrength(level: Int): Boolean {
        val camId = cameraIdWithFlash ?: return false
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && maxStrength > 1) {
                val clamped = level.coerceIn(1, maxStrength)
                cameraManager.turnOnTorchWithStrengthLevel(camId, clamped)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setStrength Camera2", e)
            false
        }
    }

    override fun turnOff(): Boolean {
        val camId = cameraIdWithFlash ?: return false
        return try {
            cameraManager.setTorchMode(camId, false)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error turnOff Camera2", e)
            false
        }
    }
}
