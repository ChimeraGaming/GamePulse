package com.chimeragaming.gamepulse.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import com.chimeragaming.gamepulse.databinding.DialogGameCollectionSettingsBinding
import com.chimeragaming.gamepulse.utils.SharedPreferencesManager

class GameCollectionSettingsDialog(
    context: Context,
    private val onSave: () -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogGameCollectionSettingsBinding
    private val prefsManager = SharedPreferencesManager(context)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogGameCollectionSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    override fun onStart() {
        super.onStart()
        window?.let { dialogWindow ->
            val width = (context.resources.displayMetrics.widthPixels * 0.9f).toInt()
            dialogWindow.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun setupUI() {
        binding.enableEditSwitch.isChecked = prefsManager.gameCollectionEditEnabled
        binding.enableDeleteSwitch.isChecked = prefsManager.gameCollectionDeleteEnabled

        binding.closeButton.setOnClickListener {
            dismiss()
        }

        binding.saveButton.setOnClickListener {
            prefsManager.gameCollectionEditEnabled = binding.enableEditSwitch.isChecked
            prefsManager.gameCollectionDeleteEnabled = binding.enableDeleteSwitch.isChecked
            onSave()
            dismiss()
        }
    }
}
