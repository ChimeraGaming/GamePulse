package com.chimeragaming.gamepulse.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Point
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import com.chimeragaming.gamepulse.databinding.DialogGameCollectionSettingsBinding

/**
 * Settings dialog for Game Collection
 * v0.3: Basic implementation for future settings
 */
class GameCollectionSettingsDialog(
    context: Context,
    private val onSave: () -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogGameCollectionSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogGameCollectionSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    // Set dialog width to 90% of screen
    override fun onStart() {
        super.onStart()
        window?.let { dialogWindow ->
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display = windowManager.defaultDisplay
            val size = Point()
            display.getSize(size)

            val width = (size.x * 0.9).toInt()
            dialogWindow.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun setupUI() {
        binding.closeButton.setOnClickListener {
            dismiss()
        }

        // More settings will be added later
        binding.settingsPlaceholderText.text = "⚙️ Collection settings coming soon!\n\n" +
                "Future features:\n" +
                "• Auto-detect games\n" +
                "• Custom categories\n" +
                "• Export statistics\n" +
                "• Screenshot?"
    }
}