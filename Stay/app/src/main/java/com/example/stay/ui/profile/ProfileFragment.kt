package com.example.stay.ui.profile

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.stay.R
import com.example.stay.data.local.AppDatabase
import com.example.stay.databinding.FragmentProfileBinding
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPrefs = requireContext().getSharedPreferences("StayPrefs", Context.MODE_PRIVATE)
        val email = sharedPrefs.getString("userEmail", "") ?: ""

        if (email.isNotEmpty()) {
            val db = AppDatabase.getDatabase(requireContext())
            viewLifecycleOwner.lifecycleScope.launch {
                val user = db.userDao().getUserByEmail(email)
                user?.let {
                    binding.tvProfileName.text = it.username
                    binding.tvProfileEmail.text = it.email
                    binding.tvProfileRole.text = "Role: ${it.role}"

                    if (it.role == "Provider") {
                        binding.layoutProviderActions.visibility = View.VISIBLE
                        binding.layoutStudentActions.visibility = View.GONE
                    } else {
                        binding.layoutProviderActions.visibility = View.GONE
                        binding.layoutStudentActions.visibility = View.VISIBLE
                    }
                }
            }
        }

        binding.btnAddListing.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_addListingFragment)
        }

        binding.btnMyListings.setOnClickListener {
            val bundle = bundleOf("mode" to "MY_LISTINGS")
            findNavController().navigate(R.id.homeFragment, bundle)
        }

        binding.btnProviderMessages.setOnClickListener {
            findNavController().navigate(R.id.chatFragment)
        }

        binding.btnProviderEarnings.setOnClickListener {
            val bundle = bundleOf("mode" to "MY_LISTINGS") // Shows their houses with 'Reserved' status
            findNavController().navigate(R.id.homeFragment, bundle)
            Toast.makeText(context, "Showing reserved properties and collected deposits.", Toast.LENGTH_LONG).show()
        }

        binding.btnLogout.setOnClickListener {
            sharedPrefs.edit().clear().apply()
            findNavController().navigate(R.id.loginFragment)
        }

        binding.btnSavedListings.setOnClickListener {
            val bundle = bundleOf("mode" to "FAVORITES")
            findNavController().navigate(R.id.homeFragment, bundle)
        }

        binding.btnMyReservations.setOnClickListener {
            val bundle = bundleOf("mode" to "MY_RESERVATIONS")
            findNavController().navigate(R.id.homeFragment, bundle)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
