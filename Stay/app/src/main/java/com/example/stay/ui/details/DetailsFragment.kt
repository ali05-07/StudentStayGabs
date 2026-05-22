package com.example.stay.ui.details

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.stay.R
import com.example.stay.data.local.AppDatabase
import com.example.stay.data.local.entity.Listing
import com.example.stay.databinding.FragmentDetailsBinding
import kotlinx.coroutines.launch
import java.util.Locale

class DetailsFragment : Fragment() {

    private var _binding: FragmentDetailsBinding? = null
    private val binding get() = _binding!!
    
    private var listingId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        listingId = arguments?.getLong("listingId") ?: -1
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (listingId == -1L) {
            Toast.makeText(context, "Error loading details", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        val sharedPrefs = requireContext().getSharedPreferences("StayPrefs", Context.MODE_PRIVATE)
        val userRole = sharedPrefs.getString("userRole", "Student")
        val userEmail = sharedPrefs.getString("userEmail", "")

        val db = AppDatabase.getDatabase(requireContext())
        val listingDao = db.listingDao()

        viewLifecycleOwner.lifecycleScope.launch {
            val listing = listingDao.getListingById(listingId)
            listing?.let {
                if (_binding != null) {
                    setupUI(it, userRole, userEmail)
                }
            }
        }
    }

    private fun setupUI(listing: Listing, userRole: String?, userEmail: String?) {
        binding.tvDetailsTitle.text = listing.title
        binding.tvDetailsPrice.text = String.format(Locale.getDefault(), "BWP %,.0f", listing.price)
        binding.tvDetailsLocation.text = String.format("Location: %s", listing.location)
        binding.tvDetailsAmenities.text = String.format("Amenities: %s", listing.amenities)
        binding.tvDetailsAvailability.text = String.format("Available from: %s", listing.availabilityDate)
        binding.tvDetailsDeposit.text = String.format(Locale.getDefault(), "Required Deposit: BWP %,.0f", listing.depositAmount)
        binding.tvDistance.text = String.format(Locale.getDefault(), "Distance to Campus: %.1f km", listing.distanceToCampus)

        if (listing.imageUrl.isNotEmpty()) {
            Glide.with(this).load(listing.imageUrl).placeholder(android.R.drawable.ic_menu_gallery).into(binding.ivDetails)
        }

        // Role-based actions
        if (userRole == "Provider") {
            binding.layoutStudentActionsDetails.visibility = View.GONE
            binding.layoutProviderActionsDetails.visibility = View.VISIBLE
            binding.tvManagementStatus.text = "Status: ${listing.status}"
            if (listing.reservedByEmail != null) {
                binding.tvManagementStatus.text = "Reserved by: ${listing.reservedByEmail}"
            }
        } else {
            binding.layoutStudentActionsDetails.visibility = View.VISIBLE
            binding.layoutProviderActionsDetails.visibility = View.GONE
            
            binding.btnChat.setOnClickListener {
                val bundle = bundleOf("listingId" to listingId)
                findNavController().navigate(R.id.action_detailsFragment_to_chatFragment, bundle)
            }

            if (listing.status == "Reserved") {
                binding.btnReserve.isEnabled = false
                binding.btnReserve.text = "Already Reserved"
                binding.btnReserve.alpha = 0.5f
            } else {
                binding.btnReserve.setOnClickListener {
                    val bundle = bundleOf("listingId" to listing.id)
                    findNavController().navigate(R.id.action_detailsFragment_to_paymentFragment, bundle)
                }
            }
        }

        binding.btnShowRoute.setOnClickListener {
            try {
                val gmmIntentUri = Uri.parse("google.navigation:q=University+of+Botswana,Gaborone")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                startActivity(mapIntent)
            } catch (e: Exception) {
                Toast.makeText(context, "Google Maps not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
