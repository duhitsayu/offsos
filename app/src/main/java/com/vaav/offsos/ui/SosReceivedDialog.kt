package com.vaav.offsos.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.vaav.offsos.databinding.DialogSosReceivedBinding
import com.vaav.offsos.R

class SosReceivedDialog : DialogFragment() {

    private var _binding: DialogSosReceivedBinding? = null
    private val binding get() = _binding!!

    interface SosResponseListener {
        fun onRespondClicked(senderName: String)
        fun onSilenceClicked()
    }

    var listener: SosResponseListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogSosReceivedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val senderName = arguments?.getString(ARG_SENDER_NAME) ?: "UNKNOWN"
        val distance = arguments?.getString(ARG_DISTANCE) ?: "~0m away"

        binding.tvSosSender.text = "Signal from: $senderName"
        binding.tvSosDistance.text = distance

        binding.btnRespond.setOnClickListener {
            listener?.onRespondClicked(senderName)
            dismiss()
        }

        binding.btnSilence.setOnClickListener {
            listener?.onSilenceClicked()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "SosReceivedDialog"
        private const val ARG_SENDER_NAME = "sender_name"
        private const val ARG_DISTANCE = "distance"

        fun newInstance(senderName: String, distance: String): SosReceivedDialog {
            val fragment = SosReceivedDialog()
            val args = Bundle().apply {
                putString(ARG_SENDER_NAME, senderName)
                putString(ARG_DISTANCE, distance)
            }
            fragment.arguments = args
            return fragment
        }
    }
}
