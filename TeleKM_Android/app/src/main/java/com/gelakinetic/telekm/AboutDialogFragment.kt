package com.gelakinetic.telekm

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class AboutDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        return builder.setTitle(R.string.about_title) // TODO embed app name, version
                .setMessage(R.string.about_message) // TODO be humble
                .setPositiveButton(R.string.about_ok) { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
    }
}
