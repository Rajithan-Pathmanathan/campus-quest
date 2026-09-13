package com.campusquest.ui.creator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.campusquest.R
import com.campusquest.databinding.FragmentCreateGameBinding

class CreateGameFragment : Fragment() {

    private var _binding: FragmentCreateGameBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateGameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnAddCheckpoint.setOnClickListener {
            val bundle = Bundle().apply {
                putString("gameId", "TEMP_NEW_GAME")
            }
            findNavController().navigate(R.id.action_createGame_to_checkpointEditor, bundle)
        }

        binding.btnSaveDraft.setOnClickListener {
            Toast.makeText(requireContext(), "Quest draft saved successfully", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }

        binding.btnPublish.setOnClickListener {
            Toast.makeText(requireContext(), "Quest published to campus!", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
