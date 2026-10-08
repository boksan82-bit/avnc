/*
 * Copyright (c) 2026  Gaurav Ujjwal.
 *
 * SPDX-License-Identifier:  GPL-3.0-or-later
 *
 * See COPYING.txt for more details.
 */

package com.gaurav.avnc.ui.vnc

import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.core.view.isVisible
import com.gaurav.avnc.ui.vnc.input.InputHandler

/**
 * Local dictation buffer. Hidden until Gboard actually starts voice input,
 * because the mic button itself is not observable.
 */
class VoiceInputBar(
        private val activity: VncActivity,
        private val inputHandler: InputHandler
) {
    private val bar = activity.binding.voiceInputBar
    private val field: EditText = activity.binding.voiceBuffer

    fun install() {
        val inputView = activity.binding.inputView
        inputView.voiceSessionListener = { reveal() }
        inputView.voiceTextListener = { text, composing -> onImeText(text, composing) }
        activity.binding.voiceSendBtn.setOnClickListener { sendBuffer() }
        field.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendBuffer()
                true
            } else {
                false
            }
        }
    }

    private fun onImeText(text: CharSequence, composing: Boolean): Boolean {
        val voice = composing || text.length > 1 || bar.isVisible
        if (!voice) return false
        reveal()
        field.setText(text)
        field.setSelection(field.text?.length ?: 0)
        if (!composing) field.requestFocus()
        return true
    }

    private fun reveal() {
        if (!bar.isVisible) bar.isVisible = true
    }

    private fun sendBuffer() {
        val text = field.text?.toString().orEmpty()
        if (text.isNotEmpty()) inputHandler.sendText(text)
        field.text?.clear()
        bar.isVisible = false
        activity.binding.inputView.requestFocus()
    }
}
