package com.vaav.offsos.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.vaav.offsos.databinding.FragmentSettingsBottomSheetBinding

class SettingsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentSettingsBottomSheetBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedPrefs: SharedPreferences
    
    // Callback interface to communicate with MainActivity
    interface SettingsListener {
        fun onFlareClicked()
        fun onStrobeClicked()
        fun onDownloadMapClicked()
        fun onSentinelModeChanged(enabled: Boolean)
    }

    var listener: SettingsListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPrefs = requireContext().getSharedPreferences("OFFSOS_PREFS", Context.MODE_PRIVATE)

        // Load current values
        binding.etName.setText(sharedPrefs.getString("codename", ""))
        binding.etChannel.setText(sharedPrefs.getString("channel", "PUBLIC"))
        binding.swSentinel.isChecked = sharedPrefs.getBoolean("sentinel_mode", false)
        binding.cbPrivacy.isChecked = sharedPrefs.getBoolean("incognito_mode", false)

        // Save on dismiss/text change
        binding.etName.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) savePrefs()
        }
        binding.etChannel.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) savePrefs()
        }

        binding.swSentinel.setOnCheckedChangeListener { _, isChecked ->
            savePrefs()
            listener?.onSentinelModeChanged(isChecked)
        }

        binding.cbPrivacy.setOnCheckedChangeListener { _, _ ->
            savePrefs()
        }

        binding.btnFlare.setOnClickListener {
            listener?.onFlareClicked()
            dismiss()
        }

        binding.btnStrobe.setOnClickListener {
            listener?.onStrobeClicked()
            dismiss()
        }

        binding.btnDownloadMap.setOnClickListener {
            listener?.onDownloadMapClicked()
            dismiss()
        }
    }

    private fun savePrefs() {
        sharedPrefs.edit().apply {
            putString("codename", binding.etName.text.toString())
            putString("channel", binding.etChannel.text.toString())
            putBoolean("sentinel_mode", binding.swSentinel.isChecked)
            putBoolean("incognito_mode", binding.cbPrivacy.isChecked)
            apply()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        savePrefs() // Ensure we save when closing
        _binding = null
    }

    companion object {
        const val TAG = "SettingsBottomSheet"
    }
}
