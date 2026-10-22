/*
 * Copyright (C) 2014 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package helium314.keyboard.keyboard.internal

import helium314.keyboard.keyboard.Key
import helium314.keyboard.keyboard.PopupKeysPanel
import helium314.keyboard.keyboard.PointerTracker

interface DrawingProxy {
    fun onKeyPressed(key: Key, withPreview: Boolean)
    fun onKeyReleased(key: Key, withAnimation: Boolean)
    fun showPopupKeysKeyboard(key: Key, tracker: PointerTracker): PopupKeysPanel?
    /** Shows the LeanType-only language list above the language key, or null if nothing to show. */
    fun showLanguagePopupKeyboard(key: Key, tracker: PointerTracker): PopupKeysPanel? = null
    fun startWhileTypingAnimation(fadeInOrOut: Int)
    fun showSlidingKeyInputPreview(tracker: PointerTracker?)
    fun showGestureTrail(tracker: PointerTracker, showsFloatingPreviewText: Boolean)
    fun dismissGestureFloatingPreviewTextWithoutDelay()

    companion object {
        const val FADE_IN = 0
        const val FADE_OUT = 1
    }
}
