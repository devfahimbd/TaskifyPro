package com.taskify.pro.ui.task

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.taskify.pro.R
import com.taskify.pro.databinding.ActivityMainBinding
import com.taskify.pro.model.Task
import com.taskify.pro.ui.auth.LoginActivity
import com.taskify.pro.utils.Resource
import com.taskify.pro.viewmodel.AuthViewModel
import com.taskify.pro.viewmodel.TaskViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Main screen — displays the user's tasks split into Pending and Completed sections.
 *
 * Observes [TaskViewModel.tasksState] (StateFlow) for real-time Firestore updates
 * and delegates all CRUD operations to the ViewModel.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val taskViewModel: TaskViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    private lateinit var pendingAdapter: TaskAdapter
    private lateinit var completedAdapter: TaskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        initAdapters()
        initRecyclerViews()
        observeTasks()
        setupFab()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                showLogoutConfirmation()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // -----------------------------------------------------------------------
    //  RecyclerView setup
    // -----------------------------------------------------------------------

    private fun initAdapters() {
        pendingAdapter = TaskAdapter(
            onToggleComplete = { task ->
                taskViewModel.toggleTaskCompletion(this, task.documentId, !task.completed)
            },
            onEdit = { task -> openAddEditScreen(task) },
            onDelete = { task -> showDeleteConfirmation(task) }
        )

        completedAdapter = TaskAdapter(
            onToggleComplete = { task ->
                taskViewModel.toggleTaskCompletion(this, task.documentId, !task.completed)
            },
            onEdit = { task -> openAddEditScreen(task) },
            onDelete = { task -> showDeleteConfirmation(task) }
        )
    }

    private fun initRecyclerViews() {
        binding.rvPendingTasks.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = pendingAdapter
        }
        binding.rvCompletedTasks.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = completedAdapter
        }
    }

    // -----------------------------------------------------------------------
    //  State observation
    // -----------------------------------------------------------------------

    private fun observeTasks() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                taskViewModel.tasksState.collect { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.scrollView.visibility = View.GONE
                            binding.layoutEmptyState.visibility = View.GONE
                        }
                        is Resource.Success -> {
                            binding.progressBar.visibility = View.GONE

                            val tasks = resource.data
                            if (tasks.isNullOrEmpty()) {
                                // Show empty state
                                binding.scrollView.visibility = View.GONE
                                binding.layoutEmptyState.visibility = View.VISIBLE
                            } else {
                                binding.scrollView.visibility = View.VISIBLE
                                binding.layoutEmptyState.visibility = View.GONE

                                // Split into pending / completed
                                val pending = tasks.filter { !it.completed }
                                val completed = tasks.filter { it.completed }

                                pendingAdapter.submitList(pending)
                                completedAdapter.submitList(completed)

                                // Show / hide section headers
                                binding.tvPendingHeader.visibility =
                                    if (pending.isNotEmpty()) View.VISIBLE else View.GONE
                                binding.tvCompletedHeader.visibility =
                                    if (completed.isNotEmpty()) View.VISIBLE else View.GONE
                            }
                        }
                        is Resource.Error -> {
                            binding.progressBar.visibility = View.GONE
                            Snackbar.make(
                                binding.root,
                                resource.message,
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    //  FAB
    // -----------------------------------------------------------------------

    private fun setupFab() {
        binding.fabAddTask.setOnClickListener {
            openAddEditScreen(null)
        }
        // Also handle the empty-state button
        binding.btnEmptyAddTask.setOnClickListener {
            openAddEditScreen(null)
        }
    }

    private fun openAddEditScreen(task: Task?) {
        val intent = Intent(this, AddEditTaskActivity::class.java).apply {
            if (task != null) {
                putExtra(EXTRA_TASK_ID, task.documentId)
                putExtra(EXTRA_TASK_TITLE, task.title)
                putExtra(EXTRA_TASK_DESCRIPTION, task.description)
                task.timestamp?.let { putExtra(EXTRA_TASK_TIMESTAMP, it.time) }
                putExtra(EXTRA_TASK_COMPLETED, task.completed)
            }
        }
        startActivity(intent)
    }

    // -----------------------------------------------------------------------
    //  Delete confirmation dialog
    // -----------------------------------------------------------------------

    private fun showDeleteConfirmation(task: Task) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.delete_confirmation))
            .setMessage(getString(R.string.delete_confirmation_message))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                taskViewModel.deleteTask(this, task.documentId)
                Snackbar.make(
                    binding.root,
                    getString(R.string.task_deleted),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    // -----------------------------------------------------------------------
    //  Logout
    // -----------------------------------------------------------------------

    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Logout") { _, _ ->
                authViewModel.signOut()
                startActivity(
                    Intent(this, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                )
                finish()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // -----------------------------------------------------------------------
    //  Extras keys
    // -----------------------------------------------------------------------

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_TASK_DESCRIPTION = "extra_task_description"
        const val EXTRA_TASK_TIMESTAMP = "extra_task_timestamp"
        const val EXTRA_TASK_COMPLETED = "extra_task_completed"
    }
}
