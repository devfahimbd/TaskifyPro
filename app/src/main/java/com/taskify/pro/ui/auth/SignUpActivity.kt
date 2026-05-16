package com.taskify.pro.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.taskify.pro.R
import com.taskify.pro.databinding.ActivitySignUpBinding
import com.taskify.pro.ui.task.MainActivity
import com.taskify.pro.utils.Resource
import com.taskify.pro.utils.ValidationUtils
import com.taskify.pro.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Sign-up screen — creates a new Firebase account.
 *
 * On successful registration the user is automatically signed in and
 * navigated to [MainActivity].
 */
@AndroidEntryPoint
class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.btnSignUp.setOnClickListener { attemptSignUp() }
        binding.tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun attemptSignUp() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()

        // Validate all fields
        val emailError = ValidationUtils.validateEmail(email)
        val passwordError = ValidationUtils.validatePassword(password)
        val confirmError = ValidationUtils.validateConfirmPassword(password, confirmPassword)

        binding.tilEmail.error = emailError
        binding.tilPassword.error = passwordError
        binding.tilConfirmPassword.error = confirmError

        if (emailError != null || passwordError != null || confirmError != null) return

        viewModel.signUp(email, password)
    }

    private fun observeViewModel() {
        viewModel.signUpState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> showLoading(true)
                is Resource.Success -> {
                    showLoading(false)
                    // Firebase automatically signs in the user after sign-up.
                    startActivity(
                        Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                    )
                    finish()
                }
                is Resource.Error -> {
                    showLoading(false)
                    Snackbar.make(binding.root, resource.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSignUp.isEnabled = !isLoading
    }
}
