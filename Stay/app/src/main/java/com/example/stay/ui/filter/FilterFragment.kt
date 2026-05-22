package com.example.stay.ui.filter

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.stay.R
import com.example.stay.databinding.FragmentFilterBinding
import java.time.LocalDate
import java.util.Calendar
import java.util.Locale

class FilterFragment : Fragment() {

    private var _binding: FragmentFilterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set default date to today
        val today = LocalDate.now()
        binding.etDateFilter.setText(today.toString())
        
        // Show DatePicker when clicking the date field
        binding.etDateFilter.setOnClickListener {
            showDatePicker()
        }
        binding.etDateFilter.isFocusable = false // Prevent keyboard from showing

        binding.btnApplyFilter.setOnClickListener {
            val minPriceStr = binding.etMinPrice.text.toString().trim()
            val maxPriceStr = binding.etMaxPrice.text.toString().trim()
            val location = binding.etLocationFilter.text.toString().trim()
            val date = binding.etDateFilter.text.toString()
            val savePrefs = binding.cbSavePreferences.isChecked

            // Logic for strict filtering: 
            // If only one field is filled, treat it as an exact price match.
            // If both are empty, use full range 800-2000.
            val (minPrice, maxPrice) = when {
                minPriceStr.isNotEmpty() && maxPriceStr.isEmpty() -> {
                    val p = minPriceStr.toDoubleOrNull() ?: 800.0
                    p to p
                }
                minPriceStr.isEmpty() && maxPriceStr.isNotEmpty() -> {
                    val p = maxPriceStr.toDoubleOrNull() ?: 2000.0
                    p to p
                }
                minPriceStr.isEmpty() && maxPriceStr.isEmpty() -> {
                    800.0 to 2000.0
                }
                else -> {
                    val min = minPriceStr.toDoubleOrNull() ?: 800.0
                    val max = maxPriceStr.toDoubleOrNull() ?: 2000.0
                    min to max
                }
            }

            val handle = findNavController().previousBackStackEntry?.savedStateHandle
            handle?.set("minPrice", minPrice)
            handle?.set("maxPrice", maxPrice)
            handle?.set("location", location)
            handle?.set("date", date)
            handle?.set("savePrefs", savePrefs)
            handle?.set("filterTrigger", System.currentTimeMillis())
            
            findNavController().popBackStack()
        }

        binding.btnClearFilters.setOnClickListener {
            binding.etMinPrice.text.clear()
            binding.etMaxPrice.text.clear()
            binding.etLocationFilter.text.clear()
            binding.etDateFilter.setText(LocalDate.now().toString())
            binding.cbSavePreferences.isChecked = false
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            val formattedDate = String.format(Locale.US, "%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
            binding.etDateFilter.setText(formattedDate)
        }, year, month, day)

        datePickerDialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
