package com.taskify.pro.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.taskify.pro.R
import com.taskify.pro.databinding.ActivityLoginBinding
import com.taskify.pro.ui.task.MainActivity
import com.taskify.pro.utils.Resource
import com.taskify.pro.utils.ValidationUtils
import com.taskify.pro.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Login screen — email / password authentication.
 *
 * Uses ViewBinding for type-safe view access and observes [AuthViewModel]
 * LiveData to react to sign-in results, loading states, and errors.
 */
@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // If the user is already authenticated, skip this screen.
        if (viewModel.isUserLoggedIn()) {
            navigateToMain()
            return
        }

        setupClickListeners()
        observeViewModel()
    }

    // -----------------------------------------------------------------------
    //  Click listeners
    // -----------------------------------------------------------------------

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener { attemptLogin() }
        binding.tvSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
        binding.tvForgotPassword.setOnClickListener { showForgotPasswordDialog() }
    }

    // -----------------------------------------------------------------------
    //  Validation & submission
    // -----------------------------------------------------------------------

    private fun attemptLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()

        // Client-side validation
        val emailError = ValidationUtils.validateEmail(email)
        val passwordError = ValidationUtils.validatePassword(password)

        binding.tilEmail.error = emailError
        binding.tilPassword.error = passwordError

        if (emailError != null || passwordError != null) return

        viewModel.signIn(email, password)
    }

    // -----------------------------------------------------------------------
    //  ViewModel observation
    // -----------------------------------------------------------------------

    private fun observeViewModel() {
        viewModel.signInState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> showLoading(true)
                is Resource.Success -> {
                    showLoading(false)
                    navigateToMain()
                    finish()
                }
                is Resource.Error -> {
                    showLoading(false)
                    Snackbar.make(binding.root, resource.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    //  Forgot password dialog
    // -----------------------------------------------------------------------

    private fun showForgotPasswordDialog() {
        val input = android.widget.EditText(this).apply {
            hint = getString(R.string.email)
            setPadding(48, 32, 48, 32)
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Reset Password")
            .setMessage("Enter your email address to receive a password reset link.")
            .setView(input)
            .setPositiveButton("Send") { _, _ ->
                val email = input.text.toString().trim()
                if (email.isBlank()) {
                    Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                viewModel.sendPasswordReset(email)
                observeResetState()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun observeResetState() {
        viewModel.resetState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> showLoading(true)
                is Resource.Success -> {
                    showLoading(false)
                    Snackbar.make(
                        binding.root,
                        getString(R.string.reset_password_sent),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
                is Resource.Error -> {
                    showLoading(false)
                    Snackbar.make(binding.root, resource.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    //  Helpers
    // -----------------------------------------------------------------------

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
    }

    private fun navigateToMain() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
    }
}
