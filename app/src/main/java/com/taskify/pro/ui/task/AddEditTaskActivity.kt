package com.taskify.pro.ui.task

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.taskify.pro.R
import com.taskify.pro.databinding.ActivityAddEditTaskBinding
import com.taskify.pro.utils.Resource
import com.taskify.pro.utils.ValidationUtils
import com.taskify.pro.viewmodel.TaskViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar
import java.util.Date

/**
 * Screen for adding a new task or editing an existing one.
 *
 * The mode (add vs. edit) is determined by whether an EXTRA_TASK_ID is
 * present in the launching Intent. The date/time pickers use Material-style
 * dialogs and combine into a single [Date] object for the reminder.
 */
@AndroidEntryPoint
class AddEditTaskActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditTaskBinding
    private val viewModel: TaskViewModel by viewModels()

    // Are we editing an existing task?
    private var isEditMode = false
    private var editTaskId: String? = null

    // Calendar holds the selected date and time.
    private val selectedCalendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        // Check if we're editing an existing task.
        editTaskId = intent.getStringExtra(MainActivity.EXTRA_TASK_ID)
        isEditMode = !editTaskId.isNullOrEmpty()

        if (isEditMode) {
            binding.toolbar.title = getString(R.string.edit_task)
            prefillFields()
        }

        setupDatePicker()
        setupTimePicker()
        setupSaveButton()
    }

    // -----------------------------------------------------------------------
    //  Pre-fill for edit mode
    // -----------------------------------------------------------------------

    private fun prefillFields() {
        intent.getStringExtra(MainActivity.EXTRA_TASK_TITLE)?.let {
            binding.etTitle.setText(it)
        }
        intent.getStringExtra(MainActivity.EXTRA_TASK_DESCRIPTION)?.let {
            binding.etDescription.setText(it)
        }
        intent.getLongExtra(MainActivity.EXTRA_TASK_TIMESTAMP, -1).let { millis ->
            if (millis > 0) {
                selectedCalendar.timeInMillis = millis
                updateDateDisplay()
                updateTimeDisplay()
            }
        }
    }

    // -----------------------------------------------------------------------
    //  Date picker
    // -----------------------------------------------------------------------

    private fun setupDatePicker() {
        binding.etDate.setOnClickListener { showDatePicker() }
        // Also listen to the TextInputLayout click.
        binding.tilDate.setOnClickListener { showDatePicker() }
    }

    private fun showDatePicker() {
        val now = Calendar.getInstance()

        // If editing, pre-populate with the existing date.
        val initialYear = selectedCalendar.get(Calendar.YEAR)
        val initialMonth = selectedCalendar.get(Calendar.MONTH)
        val initialDay = selectedCalendar.get(Calendar.DAY_OF_MONTH)

        val datePicker = DatePickerDialog(
            this,
            { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                selectedCalendar.set(Calendar.YEAR, year)
                selectedCalendar.set(Calendar.MONTH, month)
                selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateDisplay()
            },
            initialYear,
            initialMonth,
            initialDay
        )

        // Prevent selecting a past date.
        datePicker.datePicker.minDate = now.timeInMillis
        datePicker.show()
    }

    private fun updateDateDisplay() {
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
        binding.etDate.setText(sdf.format(selectedCalendar.time))
    }

    // -----------------------------------------------------------------------
    //  Time picker
    // -----------------------------------------------------------------------

    private fun setupTimePicker() {
        binding.etTime.setOnClickListener { showTimePicker() }
        binding.tilTime.setOnClickListener { showTimePicker() }
    }

    private fun showTimePicker() {
        val hour = selectedCalendar.get(Calendar.HOUR_OF_DAY)
        val minute = selectedCalendar.get(Calendar.MINUTE)

        val timePicker = TimePickerDialog(
            this,
            { _: TimePicker, hourOfDay: Int, minuteOfHour: Int ->
                selectedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                selectedCalendar.set(Calendar.MINUTE, minuteOfHour)
                selectedCalendar.set(Calendar.SECOND, 0)
                updateTimeDisplay()
            },
            hour,
            minute,
            false // Show 12-hour format
        )
        timePicker.show()
    }

    private fun updateTimeDisplay() {
        val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
        binding.etTime.setText(sdf.format(selectedCalendar.time))
    }

    // -----------------------------------------------------------------------
    //  Save / validation
    // -----------------------------------------------------------------------

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener { saveTask() }
    }

    private fun saveTask() {
        val title = binding.etTitle.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()

        // Validate title
        val titleError = ValidationUtils.validateTaskTitle(title)
        binding.tilTitle.error = titleError
        if (titleError != null) return

        // Validate description
        val descError = ValidationUtils.validateTaskDescription(description)
        binding.tilDescription.error = descError
        if (descError != null) return

        // Check date & time are selected
        val hasDate = binding.etDate.text.toString().isNotBlank()
        val hasTime = binding.etTime.text.toString().isNotBlank()

        if (!hasDate || !hasTime) {
            Snackbar.make(
                binding.root,
                getString(R.string.error_pick_date_time),
                Snackbar.LENGTH_LONG
            ).show()
            return
        }

        val reminderAt = selectedCalendar.time

        // Don't allow past times for new tasks (unless editing, allow same).
        if (!isEditMode && reminderAt.before(Date())) {
            Snackbar.make(
                binding.root,
                getString(R.string.error_past_time),
                Snackbar.LENGTH_LONG
            ).show()
            return
        }

        // Show loading
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSave.isEnabled = false

        if (isEditMode && editTaskId != null) {
            viewModel.updateTask(this, editTaskId!!, title, description, reminderAt)
        } else {
            viewModel.addTask(this, title, description, reminderAt)
        }

        // Observe the operation result.
        observeOperation()
    }

    private fun observeOperation() {
        viewModel.operationState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnSave.isEnabled = false
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSave.isEnabled = true
                    val message = if (isEditMode)
                        getString(R.string.task_updated)
                    else
                        getString(R.string.task_added)
                    Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
                    // Delay slightly so the user sees the snackbar.
                    binding.root.postDelayed({ finish() }, 500)
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSave.isEnabled = true
                    Snackbar.make(binding.root, resource.message, Snackbar.LENGTH_LONG).show()
                }
                null -> { /* No-op */ }
            }
        }
    }
}
