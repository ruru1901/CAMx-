package com.pausecard.app.overlay

import java.util.concurrent.atomic.AtomicBoolean

object OverlayStateManager {

    private val isShowing = AtomicBoolean(false)
    private var currentPackageName: String? = null

    fun isOverlayShowing(): Boolean = isShowing.get()

    fun markShowing(packageName: String) {
        isShowing.set(true)
        currentPackageName = packageName
    }

    fun markDismissed() {
        isShowing.set(false)
        currentPackageName = null
    }

    fun getCurrentPackage(): String? = currentPackageName
}
