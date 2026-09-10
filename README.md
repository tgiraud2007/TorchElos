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
  <i>Full 1 to 500 Linear Intensity Range, Zero-Flash Nightlight Startup, and Bidirectional LineageOS Quick Settings Synchronization.</i>
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

## 📖 Background

On the POCO F5 and Redmi Note 12 Turbo (Snapdragon 7+ Gen 2), standard Android flashlight apps cannot adjust brightness because the OEM camera HAL does not expose multi-level brightness controls (`FLASH_INFO_STRENGTH_MAXIMUM_LEVEL = 1`). Furthermore, standard system drivers fire an intense factory pulse upon activation, blinding your eyes when all you need is a soft nightlight.

---

## 💡 How TorchElos Works

TorchElos bypasses the restricted Camera HAL by interfacing directly with the Qualcomm PMIC kernel drivers via root (`sysfs`):

* **Direct Hardware DAC Control:** Directly drives the LED current across the full **1 to 500 mA** hardware range, delivering true linear brightness control.
* **Zero-Flash Soft Startup:** Suppresses the harsh factory ignition pulse, enabling the light to turn on directly at your chosen intensity (such as an ultra-dim level 1 nightlight).
* **Two-Way Quick Settings Sync:** Seamlessly synchronizes state with the official Android Quick Settings flashlight tile.
* **100% Non-Destructive & Safe:** Operates entirely in volatile kernel memory (RAM). No system files or vendor partitions are ever modified, and standard camera calibrations are automatically restored when turning off.

---

## ✨ Features

- **Ultra-Lightweight Footprint:**
  - Highly optimized with R8 minification and resource shrinking (~1.9 MB).
- **Full 1 to 500 Range:**
  - `1 / 500` : Ultra-soft nightlight — easy on your eyes in complete darkness.
  - `50 / 500` : Eco illumination for reading and navigation.
  - `130 / 500` : Standard balanced flashlight (factory default intensity).
  - `500 / 500` : Maximum hardware turbo.
- **Intuitive Controls:**
  - Smooth intensity slider.
  - Direct numeric entry dialog (tap the level badge to enter exact values like `42` or `360`).
  - One-tap quick presets: **Nightlight (1)**, **Eco (50)**, **Standard (130)**, **Turbo (500)**.
- **Zero-Friction Launch:**
  - Launches instantly without requiring camera runtime permissions.
- **Quick Settings Integration:**
  - Dedicated custom tile (`TorchTileService`) with real-time level readout.
  - Synchronized with the stock system flashlight toggle.
- **Hardware Telemetry & Status:**
  - Automatic detection of your root solution (**KernelSU**, **Magisk**, **APatch**), device model, flash PMIC, and active ROM.

---

## 📱 Compatibility & Requirements

| Requirement | Supported |
|---|---|
| **Target Device** | **POCO F5 (`marble`)** / **Redmi Note 12 Turbo** |
| **Processor** | Qualcomm Snapdragon 7+ Gen 2 (SM7475) |
| **Android Version** | **Android 13, 14, 15, 16+** (API 33 - 36) |
| **ROM Support** | **LineageOS, crDroid, PixelOS, and all AOSP ROMs**, as well as **Stock rooted HyperOS / MIUI** |
| **Root Solution** | **KernelSU**, **Magisk**, or **APatch** (Required) |

---

## 🚀 Installation

1. Download the latest **`TorchElos-v1.1.0-beta.apk`** from the [Releases](https://github.com/tgiraud2007/TorchElos/releases/latest) section.
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
