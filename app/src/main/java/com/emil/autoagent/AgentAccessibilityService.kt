package com.emil.autoagent

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AgentAccessibilityService : AccessibilityService() {

    companion object {
        var instance: AgentAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {
        instance = null
    }

    // Ekrandakı istənilən yazını tapıb üzərinə klikləyir
    fun clickByText(targetText: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val nodes = rootNode.findAccessibilityNodeInfosByText(targetText)
        for (node in nodes) {
            if (node.isClickable) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            }
            var parent = node.parent
            while (parent != null) {
                if (parent.isClickable) {
                    parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    return true
                }
                parent = parent.parent
            }
            // Əgər element kliklənən deyilsə, birbaşa koordinatına toxunur
            val rect = android.graphics.Rect()
            node.getBoundsInScreen(rect)
            if (!rect.isEmpty) {
                tapCoordinate(rect.exactCenterX(), rect.exactCenterY())
                return true
            }
        }
        return false
    }

    // Koordinata toxunuş
    fun tapCoordinate(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 120))
            .build()
        dispatchGesture(gesture, null, null)
    }

    // Ekranı sürüşdürmək (Swipe)
    fun swipeScreen(direction: String) {
        val path = Path()
        when (direction.lowercase()) {
            "aşağı", "asagi", "down" -> {
                path.moveTo(500f, 1600f)
                path.lineTo(500f, 400f)
            }
            "yuxarı", "yuxari", "up" -> {
                path.moveTo(500f, 400f)
                path.lineTo(500f, 1600f)
            }
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 350))
            .build()
        dispatchGesture(gesture, null, null)
    }

    // Mətn sahəsinə yazı yazmaq
    fun inputText(text: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val focusNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focusNode != null) {
            val args = Bundle()
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            focusNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            return true
        }
        return false
    }

    // Sistem əmrləri (Geri, Ana ekran)
    fun systemAction(action: String) {
        when (action.lowercase()) {
            "geri", "back" -> performGlobalAction(GLOBAL_ACTION_BACK)
            "ana ekran", "home" -> performGlobalAction(GLOBAL_ACTION_HOME)
            "bildirişlər", "notifications" -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
        }
    }
}
