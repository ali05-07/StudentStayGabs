package com.example.stay.ui.payment

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.stay.R
import com.example.stay.data.local.AppDatabase
import com.example.stay.databinding.FragmentPaymentBinding
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

class PaymentFragment : Fragment() {

    private var _binding: FragmentPaymentBinding? = null
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
        _binding = FragmentPaymentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (listingId == -1L) {
            Toast.makeText(context, "Error processing payment", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        val db = AppDatabase.getDatabase(requireContext())
        val listingDao = db.listingDao()

        viewLifecycleOwner.lifecycleScope.launch {
            val listing = listingDao.getListingById(listingId)
            listing?.let {
                // the money format
                val formattedAmount = String.format(Locale.getDefault(), "BWP %,.0f", it.depositAmount)
                binding.tvPaymentAmount.text = String.format("Amount to Pay: %s", formattedAmount)

                binding.btnConfirmPayment.setOnClickListener { _ ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        // Simulate payment processing
                        val sharedPrefs = requireContext().getSharedPreferences("StayPrefs", Context.MODE_PRIVATE)
                        val userEmail = sharedPrefs.getString("userEmail", "")
                        
                        val updatedListing = it.copy(
                            status = "Reserved",
                            reservedByEmail = userEmail
                        )
                        listingDao.update(updatedListing)

                        val refNumber = UUID.randomUUID().toString().substring(0, 8).uppercase(Locale.getDefault())
                        showReceiptNotification(it.title, refNumber)

                        Toast.makeText(context, "Payment Successful! Ref: $refNumber", Toast.LENGTH_LONG).show()
                        findNavController().navigate(R.id.action_paymentFragment_to_homeFragment)
                    }
                }
            }
        }
    }

    private fun showReceiptNotification(title: String, ref: String) {
        val context = context ?: return
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "payment_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Payments", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Reservation Confirmed")
            .setContentText("Receipt for $title: #$ref")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
