package com.torchelos.app.core

interface TorchEngine {
    fun isAvailable(): Boolean
    fun getMaxLevel(): Int
    fun getMinLevel(): Int = 1
    fun getDefaultLevel(): Int

    fun turnOn(level: Int): Boolean
    fun setStrength(level: Int): Boolean
    fun turnOff(): Boolean
}
