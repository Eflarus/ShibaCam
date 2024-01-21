package ru.eflarus.shibacam.fragments

import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.coroutines.launch
import ru.eflarus.shibacam.GridGalleryAdapter
import ru.eflarus.shibacam.R
import ru.eflarus.shibacam.databinding.FragmentGalleryBinding
import java.io.File

class GalleryFragment : Fragment() {
    private lateinit var binding: FragmentGalleryBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGalleryBinding.inflate(inflater, container, false)
        val appName = requireContext().resources.getString(R.string.app_name)
        val directory =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES + "/" + appName)

        val files = directory.listFiles() as Array<File>


        binding.switchToPhotoBtn.setOnClickListener {
            lifecycleScope.launch{
                Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
                    GalleryFragmentDirections.actionGalleryFragmentToCapturePhotoFragment()
                )
            }
        }

        binding.backButton.setOnClickListener {
            Navigation.findNavController(requireActivity(), R.id.fragment_container)
                .navigateUp()
        }

        binding.switchToVideoBtn.setOnClickListener {
            lifecycleScope.launch{
                Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
                    GalleryFragmentDirections.actionGalleryFragmentToCaptureVideoFragment()
                )
            }
        }




        Log.d("TAG", files.joinToString())
        binding.galleryRecycler.layoutManager =
            GridLayoutManager(binding.root.context, 3)
        binding.galleryRecycler.adapter = GridGalleryAdapter(files.reversedArray())


        return binding.root
    }
}
