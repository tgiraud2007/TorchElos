# 🔦 TorchElos

<p align="center">
  <a href="https://github.com/tgiraud2007/TorchElos/releases/latest"><img src="https://img.shields.io/github/v/release/tgiraud2007/TorchElos?include_prereleases&style=for-the-badge&color=FFA726" alt="Latest Release" /></a>
  <a href="https://android.com"><img src="https://img.shields.io/badge/Android-13%20to%2016%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android Version" /></a>
  <a href="https://github.com/tgiraud2007/TorchElos"><img src="https://img.shields.io/badge/Device-POCO%20F5%20(marble)-007ACC?style=for-the-badge" alt="Target Device" /></a>
  <a href="https://kernelsu.org"><img src="https://img.shields.io/badge/Root-KernelSU%20%7C%20Magisk%20%7C%20APatch-E53935?style=for-the-badge" alt="Root Solution" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-blue?style=for-the-badge" alt="License" /></a>
</p>

<h3 align="center">
  <b>Advanced Hardware-Level Flashlight Intensity Controller for POCO F5 (<code>marble</code>)</b>
</h3>

<p align="center">
  <i>Full 1 to 500 Linear Intensity Range, Zero-Flash Stealth Nightlight Startup, and Bidirectional LineageOS Quick Settings Synchronization.</i>
</p>

---

> [!IMPORTANT]
> **🧪 PUBLIC BETA — ROOT ACCESS REQUIRED**
> * **TorchElos is currently in public Beta.**
> * **ROOT ACCESS IS MANDATORY:** You must have **KernelSU**, **Magisk**, or **APatch** installed. Without Root permissions, Android prevents apps from directly controlling the PMIC kernel sysfs registers.
> * Engineered specifically for **POCO F5 (`marble`)** and **Redmi Note 12 Turbo** (Snapdragon 7+ Gen 2) on Android 13 to 16+.

---

## ⚠️ Disclaimer & Hardware Warning

> [!CAUTION]
> **USE AT YOUR OWN RISK — NO LIABILITY**
> 
> * **Do NOT run intensity level 500 (100% / Plein phare) continuously for extended periods.**  
>   At level 500, the dual PMIC LED drivers inject **500 mA** of continuous electrical current into the flash module. This generates significant heat and can cause thermal degradation, reduce LED lifespan, or cause hardware damage to the flash module if left running unattended.
> * We strongly recommend using **level 500 only for short bursts** when maximum illumination is required. For daily use, levels between **1 and 250** provide more than enough illumination while remaining cool and energy-efficient.
> * **The author and contributors of TorchElos assume NO responsibility or liability** for any hardware damage, overheated/burnt LED units, battery degradation, software crashes, or any other issues that may occur on your device.

---

## 📖 The Problem on POCO F5 (`marble`)

On Xiaomi's Snapdragon 7+ Gen 2 platform (POCO F5 / Redmi Note 12 Turbo) running AOSP or LineageOS:
1. **Camera HAL Limitation:** The OEM Camera HAL declares `CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL = 1`. Because of this, standard Android 13+ torch apps (such as *FlashDim*) **cannot control brightness** and remain stuck at level 1.
2. **The 65 mA Blinding Spike:** Qualcomm CamX hardcodes `overrideFlashTorchCurrent=130` in `/vendor/etc/camera/camxoverridesettings.txt` (divided between both LEDs = 65 mA per diode). Whenever the standard system turns on the torch, CamX fires an immediate 65 mA pulse. If you wanted a dim nightlight to check on a baby or read at night, your eyes were blinded before any dimming could occur.

---

## 💡 How TorchElos Solves It

TorchElos bypasses the restricted Camera HAL and speaks directly to the Qualcomm PMIC (PM8350C) `leds-qti-flash` kernel drivers via `sysfs`:

* **Direct Sysfs DAC Writes:** Directly drives `/sys/class/leds/led:torch_0` and `/sys/class/leds/led:torch_3` with millivolt/milliamp accuracy from **1 to 500**.
* **Zero-Flash Stealth Ignition:** Before requesting `CameraManager.setTorchMode(true)`, TorchElos temporarily detaches the Qualcomm CamX V4L2 triggers (`switch0_trigger`, `torch0_trigger`, `torch3_trigger`). When CamX fires its 65 mA event, it drops into the void, allowing TorchElos to power up the LEDs **directly at level 1** without any intermediate spike.
* **Bidirectional Stock Tile Sync:** Intercepts system torch events so that tapping the official LineageOS Quick Settings tile automatically applies your saved custom intensity level.
* **100% Non-Destructive & Safe:** No files in `/system` or `/vendor` are ever touched. When the torch is off or when opening the Camera app (Aperture, GCam), standard kernel triggers are automatically restored, ensuring 100% stock photo and video flash calibration.

---

## ✨ Features

- **Fine-Grained 1 to 500 Range:**
  - `1 / 500` : Ultra-dim nightlight (Veilleuse douce) — will not blind you at night.
  - `50 / 500` : Indoor reading & navigation.
  - `250 / 500` : Standard bright flashlight.
  - `500 / 500` : Maximum hardware turbo (Plein phare).
- **Interactive Controls:**
  - Smooth intensity slider.
  - Step buttons (`-10`, `-1`, `+1`, `+10`).
  - Direct numeric keypad input (click the level number to type exact values like `42` or `360`).
  - Quick Presets: **1 (Nightlight)**, **10% (Eco)**, **25% (Balanced)**, **50% (Standard)**, **75% (Bright)**, **100% (Turbo)**.
- **LineageOS Quick Settings Tile:**
  - Dedicated custom tile (`TorchTileService`) with real-time subtitle feedback (`Nightlight (1/500)`, `250 / 500 (50%)`, etc.).
  - Two-way synchronization with the stock AOSP/LineageOS flashlight tile.
- **Hardware Telemetry:**
  - Real-time sysfs feedback showing the actual current register read from the physical PMIC.

---

## 📱 Compatibility & Requirements

| Requirement | Supported |
|---|---|
| **Target Device** | **POCO F5 (`marble`)** / **Redmi Note 12 Turbo** |
| **Processor** | Qualcomm Snapdragon 7+ Gen 2 (SM7475) |
| **Android Version** | **Android 13, 14, 15, 16+** (API 33 - 36) |
| **ROM Support** | LineageOS, crDroid, PixelOS, and other AOSP-based custom ROMs |
| **Root Solution** | **KernelSU**, **Magisk**, or **APatch** (Required) |

---

## 🚀 Installation

1. Download the latest **`TorchElos-v1.0.0-beta.apk`** from the [Releases](https://github.com/tgiraud2007/TorchElos/releases/latest) section.
2. Install the APK on your device.
3. Open **TorchElos** and **Grant Superuser / Root permissions** when prompted (by KernelSU, Magisk, or APatch).
4. *(Optional)* Add the **Torch** tile to your Quick Settings panel.

---

## 🛠️ Building from Source

Prerequisites: Android Studio Ladybug / Meerkat or command-line Gradle with JDK 21 and Android SDK 35+.

```bash
# Clone the repository
git clone https://github.com/tgiraud2007/TorchElos.git
cd TorchElos

# Build the Debug APK
./gradlew assembleDebug

# The generated APK will be located at:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
