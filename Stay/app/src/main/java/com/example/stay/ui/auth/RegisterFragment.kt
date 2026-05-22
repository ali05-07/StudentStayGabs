package com.example.stay.ui.auth

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.stay.R
import com.example.stay.data.local.AppDatabase
import com.example.stay.data.local.entity.User
import com.example.stay.databinding.FragmentRegisterBinding
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = AppDatabase.getDatabase(requireContext())
        val userDao = db.userDao()

        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString()
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()
            val role = if (binding.rbStudent.isChecked) "Student" else "Provider"

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch {
                val existingUser = userDao.getUserByEmail(email)
                if (existingUser != null) {
                    Toast.makeText(context, "Email already registered", Toast.LENGTH_SHORT).show()
                } else {
                    val newUser = User(username = name, email = email, password = password, role = role)
                    userDao.insert(newUser)
                    
                    // Persistent Login: Save session state
                    val sharedPrefs = requireContext().getSharedPreferences("StayPrefs", Context.MODE_PRIVATE)
                    sharedPrefs.edit().apply {
                        putBoolean("isLoggedIn", true)
                        putString("userEmail", email)
                        putString("userRole", role)
                        apply()
                    }

                    Toast.makeText(context, "Registration successful as $role", Toast.LENGTH_SHORT).show()
                    // Fixed navigation action
                    findNavController().navigate(R.id.action_registerFragment_to_homeFragment)
                }
            }
        }

        binding.tvLogin.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
