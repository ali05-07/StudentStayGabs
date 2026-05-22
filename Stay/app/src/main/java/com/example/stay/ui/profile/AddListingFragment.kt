package com.example.stay.ui.profile

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.stay.data.local.AppDatabase
import com.example.stay.data.local.entity.Listing
import com.example.stay.databinding.FragmentAddListingBinding
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar
import java.util.Locale

class AddListingFragment : Fragment() {

    private var _binding: FragmentAddListingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddListingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = AppDatabase.getDatabase(requireContext())
        val listingDao = db.listingDao()
        val sharedPrefs = requireContext().getSharedPreferences("StayPrefs", Context.MODE_PRIVATE)
        val userEmail = sharedPrefs.getString("userEmail", "") ?: ""

        // Default date
        binding.etAddDate.setText(LocalDate.now().toString())

        binding.etAddDate.setOnClickListener {
            showDatePicker()
        }

        binding.btnSubmitListing.setOnClickListener {
            val title = binding.etTitle.text.toString()
            val priceStr = binding.etPrice.text.toString()
            val location = binding.etLocation.text.toString()
            val amenities = binding.etAmenities.text.toString()
            val depositStr = binding.etDeposit.text.toString()
            val date = binding.etAddDate.text.toString()
            val imageUrl = binding.etImageUrl.text.toString()

            if (title.isEmpty() || priceStr.isEmpty() || location.isEmpty() || depositStr.isEmpty()) {
                Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val price = priceStr.toDoubleOrNull() ?: 0.0
            val deposit = depositStr.toDoubleOrNull() ?: 0.0
            
            viewLifecycleOwner.lifecycleScope.launch {
                val newListing = Listing(
                    title = title,
                    price = price,
                    location = location,
                    type = "Custom Listing",
                    amenities = amenities,
                    availabilityDate = date,
                    depositAmount = deposit,
                    imageUrl = imageUrl,
                    distanceToCampus = 1.5,
                    status = "Available",
                    ownerEmail = userEmail
                )
                
                listingDao.insert(newListing)
                Toast.makeText(context, "Property listed successfully!", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            val formattedDate = String.format(Locale.US, "%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
            binding.etAddDate.setText(formattedDate)
        }, year, month, day)

        datePickerDialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
