package ru.eflarus.shibacam.fragments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import kotlinx.coroutines.launch
import ru.eflarus.shibacam.R
import ru.eflarus.shibacam.databinding.FragmentPermissionsBinding

private var PERMISSIONS_REQUIRED = arrayOf(
    Manifest.permission.CAMERA,
    Manifest.permission.RECORD_AUDIO
)


class PermissionsFragment : Fragment() {
    private var binding: FragmentPermissionsBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            val permissionList = PERMISSIONS_REQUIRED.toMutableList()
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            PERMISSIONS_REQUIRED = permissionList.toTypedArray()
        }

        if (!hasPermissions(requireContext())) {
            activityResultLauncher.launch(PERMISSIONS_REQUIRED)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentPermissionsBinding.inflate(inflater, container, false).also {
            if (hasPermissions(requireContext())) {
                navigateToCapture()
            } else {
                Log.e(
                    PermissionsFragment::class.java.simpleName,
                    "Re-requesting permissions ..."
                )
                activityResultLauncher.launch(PERMISSIONS_REQUIRED)
            }
        }.root
    }

    companion object {
        /** Convenience method used to check if all permissions required by this app are granted */
        fun hasPermissions(context: Context) = PERMISSIONS_REQUIRED.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions())
        { permissions ->
            // Handle Permission granted/rejected
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in PERMISSIONS_REQUIRED && !it.value)
                    permissionGranted = false
            }
            if (permissionGranted && permissions.isNotEmpty()) {
                navigateToCapture()
            }
            if (!permissionGranted) {
                Toast.makeText(context, "Permission request denied", Toast.LENGTH_LONG).show()
            }
        }

    private fun navigateToCapture() {
        lifecycleScope.launch {
            Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
                PermissionsFragmentDirections.actionPermissionsFragmentToPhotoCapture()
            )
        }
    }
}