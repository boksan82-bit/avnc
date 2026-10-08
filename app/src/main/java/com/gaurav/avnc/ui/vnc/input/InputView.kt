/*
 * Copyright (c) 2026  Gaurav Ujjwal.
 *
 * SPDX-License-Identifier:  GPL-3.0-or-later
 *
 * See COPYING.txt for more details.
 */

package com.gaurav.avnc.ui.vnc.input

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.PointerIcon
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import com.gaurav.avnc.viewmodel.VncViewModel

/**
 * This is a simple, transparent view to handle input events.
 * It acts as an edit box to handle key events.
 *
 * Voice typing is advertised as a text field so Gboard enables the mic.
 * Dictation is not injected as key events. It is handed to [voiceTextListener],
 * which reveals the local buffer. Single typed characters still go to the server.
 */
class InputView(context: Context?, attrs: AttributeSet? = null) : View(context, attrs) {

    private var inputHandler: InputHandler? = null

    /**
     * Receives IME text that looks like dictation rather than a key press.
     * Return true to consume it. [composing] is true for live partials.
     */
    var voiceTextListener: ((text: CharSequence, composing: Boolean) -> Boolean)? = null

    /** Called when the IME reports that a voice session started, before any text. */
    var voiceSessionListener: (() -> Unit)? = null

    /**
     * Input connection used for intercepting key events and voice commits.
     */
    inner class InputConnection : BaseInputConnection(this, false) {
        override fun sendKeyEvent(event: KeyEvent): Boolean {
            return inputHandler?.onKeyEvent(event) == true || super.sendKeyEvent(event)
        }

        override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
            if (text.isNullOrEmpty()) return true
            if (voiceTextListener?.invoke(text, false) == true) return true
            inputHandler?.sendText(text.toString())
            return true
        }

        override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean {
            if (!text.isNullOrEmpty())
                voiceTextListener?.invoke(text, true)
            return true
        }

        override fun finishComposingText(): Boolean = true

        override fun getTextBeforeCursor(n: Int, flags: Int): CharSequence = ""

        override fun getTextAfterCursor(n: Int, flags: Int): CharSequence = ""

        override fun performPrivateCommand(action: String?, data: Bundle?): Boolean {
            if (action?.contains("voice", ignoreCase = true) == true) {
                voiceSessionListener?.invoke()
                return true
            }
            return super.performPrivateCommand(action, data)
        }
    }

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        setWillNotDraw(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            defaultFocusHighlightEnabled = false
        }
    }

    /**
     * Should be called from [com.gaurav.avnc.ui.vnc.VncActivity.onCreate].
     */
    fun initialize(viewModel: VncViewModel, inputHandler: InputHandler) {
        this.inputHandler = inputHandler

        // Hide local cursor if requested and supported
        if (Build.VERSION.SDK_INT >= 24 && viewModel.pref.input.hideLocalCursor)
            pointerIcon = PointerIcon.getSystemIcon(context, PointerIcon.TYPE_NULL)
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        // TYPE_NULL is why Gboard shows "voice input is not supported here".
        outAttrs.inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE
        outAttrs.imeOptions = EditorInfo.IME_ACTION_NONE or
                EditorInfo.IME_FLAG_NO_EXTRACT_UI or
                EditorInfo.IME_FLAG_NO_FULLSCREEN
        return InputConnection()
    }

    override fun onCheckIsTextEditor(): Boolean {
        return true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return inputHandler?.onTouchEvent(event) == true
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        return inputHandler?.onGenericMotionEvent(event) == true
    }

    override fun onHoverEvent(event: MotionEvent): Boolean {
        return inputHandler?.onHoverEvent(event) == true
    }

    override fun onCapturedPointerEvent(event: MotionEvent): Boolean {
        return inputHandler?.onCapturedPointerEvent(event) == true
    }
}
