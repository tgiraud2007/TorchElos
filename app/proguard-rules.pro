# TorchElos Proguard Rules

# libsu (TopJohnWu)
-keep class com.topjohnwu.superuser.** { *; }
-keepclassmembers class * extends com.topjohnwu.superuser.ipc.RootService { *; }
