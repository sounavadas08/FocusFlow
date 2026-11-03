package com.example.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class FocusAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return

        // 1. First check if the application is completely restricted / hard-blocked
        if (!InterceptManager.checkAppAccess(this, packageName)) {
            return
        }

        // 2. Perform deep multi-signal inspection for Reels / Shorts
        when (packageName) {
            "com.instagram.android" -> {
                if (isInstagramReelsActive(event)) {
                    Log.d(TAG, "Intercepted Instagram Reels via event signal or view tree")
                    InterceptManager.onSectionDetected(this, packageName, "Instagram Reels")
                }
            }
            "com.google.android.youtube" -> {
                if (isYouTubeShortsActive(event)) {
                    Log.d(TAG, "Intercepted YouTube Shorts via event signal or view tree")
                    InterceptManager.onSectionDetected(this, packageName, "YouTube Shorts")
                }
            }
            "com.zhiliaoapp.musically", "com.ss.android.ugc.trill" -> {
                Log.d(TAG, "Intercepted TikTok feed")
                InterceptManager.onSectionDetected(this, packageName, "TikTok Feed")
            }
        }
    }

    private fun isInstagramReelsActive(event: AccessibilityEvent): Boolean {
        // Fast signal 1: Check class name
        val className = event.className?.toString() ?: ""
        if (className.contains("Clips", ignoreCase = true) ||
            className.contains("ReelViewer", ignoreCase = true) ||
            className.contains("ClipsViewer", ignoreCase = true)
        ) {
            return true
        }

        // Fast signal 2: Check event text and content description
        val contentDesc = event.contentDescription?.toString() ?: ""
        if (contentDesc.equals("Reels", ignoreCase = true) || contentDesc.contains("Reels tab", ignoreCase = true)) {
            return true
        }
        if (event.text.any { it.toString().contains("Reels", ignoreCase = true) }) {
            return true
        }

        // Deep signal 3: Check active window node hierarchy
        val rootNode = rootInActiveWindow ?: event.source ?: return false
        return try {
            val hasReelsViewId = rootNode.findAccessibilityNodeInfosByViewId("com.instagram.android:id/reel_viewer_title").isNotEmpty() ||
                rootNode.findAccessibilityNodeInfosByViewId("com.instagram.android:id/clips_video_container").isNotEmpty() ||
                rootNode.findAccessibilityNodeInfosByViewId("com.instagram.android:id/clips_viewer").isNotEmpty() ||
                rootNode.findAccessibilityNodeInfosByViewId("com.instagram.android:id/clips_swipe_refresh_container").isNotEmpty() ||
                rootNode.findAccessibilityNodeInfosByViewId("com.instagram.android:id/clips_media_container").isNotEmpty() ||
                rootNode.findAccessibilityNodeInfosByViewId("com.instagram.android:id/feed_clips").isNotEmpty()

            if (hasReelsViewId) return true

            // Check selected navigation tab or text nodes
            val reelsTextNodes = rootNode.findAccessibilityNodeInfosByText("Reels")
            val hasSelectedReelsTab = reelsTextNodes.any { it.isSelected || it.isClickable }

            hasSelectedReelsTab || recursiveSearchForText(rootNode, "Reels")
        } catch (e: Exception) {
            false
        }
    }

    private fun isYouTubeShortsActive(event: AccessibilityEvent): Boolean {
        val className = event.className?.toString() ?: ""
        if (className.contains("Shorts", ignoreCase = true) || className.contains("Reel", ignoreCase = true)) {
            return true
        }

        val contentDesc = event.contentDescription?.toString() ?: ""
        if (contentDesc.contains("Shorts", ignoreCase = true)) {
            return true
        }

        val rootNode = rootInActiveWindow ?: event.source ?: return false
        return try {
            val hasShortsViewId = rootNode.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/shorts_player").isNotEmpty() ||
                rootNode.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/reel_player_fragment").isNotEmpty() ||
                rootNode.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/reel_recycler").isNotEmpty() ||
                rootNode.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/shorts_container").isNotEmpty()

            if (hasShortsViewId) return true

            val shortsNodes = rootNode.findAccessibilityNodeInfosByText("Shorts")
            shortsNodes.any { it.isSelected } || recursiveSearchForText(rootNode, "Shorts")
        } catch (e: Exception) {
            false
        }
    }

    private fun recursiveSearchForText(node: AccessibilityNodeInfo, target: String, depth: Int = 0): Boolean {
        if (depth > 6) return false
        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""
        if (text.contains(target, ignoreCase = true) || desc.contains(target, ignoreCase = true)) {
            if (node.isSelected || node.isFocused || node.isClickable) return true
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (recursiveSearchForText(child, target, depth + 1)) {
                return true
            }
        }
        return false
    }

    override fun onInterrupt() {
        Log.w(TAG, "FocusAccessibilityService interrupted")
    }

    companion object {
        private const val TAG = "FocusAccessibility"
    }
}
