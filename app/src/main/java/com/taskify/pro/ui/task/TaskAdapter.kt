package com.taskify.pro.ui.task

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.taskify.pro.R
import com.taskify.pro.databinding.ItemTaskBinding
import com.taskify.pro.model.Task
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * RecyclerView adapter for the task list.
 *
 * Features:
 *  - DiffUtil for efficient, animated list updates.
 *  - Separate callback interfaces for checkbox, edit, and delete actions.
 *  - Strikethrough styling and muted colours for completed tasks.
 */
class TaskAdapter(
    private val onToggleComplete: (Task) -> Unit,
    private val onEdit: (Task) -> Unit,
    private val onDelete: (Task) -> Unit
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    inner class TaskViewHolder(val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = getItem(position)
        bindTask(holder.binding, task)
    }

    private fun bindTask(binding: ItemTaskBinding, task: Task) {
        // Title
        binding.tvTitle.text = task.title
        if (task.completed) {
            binding.tvTitle.paintFlags =
                binding.tvTitle.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            binding.tvTitle.alpha = 0.5f
        } else {
            binding.tvTitle.paintFlags =
                binding.tvTitle.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
            binding.tvTitle.alpha = 1.0f
        }

        // Description
        if (task.description.isNotBlank()) {
            binding.tvDescription.text = task.description
            binding.tvDescription.visibility = android.view.View.VISIBLE
            binding.tvDescription.alpha = if (task.completed) 0.4f else 1.0f
        } else {
            binding.tvDescription.visibility = android.view.View.GONE
        }

        // Timestamp
        val timestamp = task.timestamp
        if (timestamp != null) {
            val dateStr = dateFormat.format(timestamp)
            val timeStr = timeFormat.format(timestamp)
            binding.tvTimestamp.text = "Due: $dateStr  $timeStr"
            binding.tvTimestamp.visibility = android.view.View.VISIBLE
        } else {
            binding.tvTimestamp.visibility = android.view.View.GONE
        }

        // Status badge
        when {
            task.completed -> {
                binding.tvStatus.text = "Completed"
                binding.tvStatus.setTextColor(
                    binding.root.context.getColor(R.color.green_500)
                )
                binding.tvStatus.visibility = android.view.View.VISIBLE
            }
            task.isOverdue -> {
                binding.tvStatus.text = "Overdue"
                binding.tvStatus.setTextColor(
                    binding.root.context.getColor(R.color.red_500)
                )
                binding.tvStatus.visibility = android.view.View.VISIBLE
            }
            else -> {
                binding.tvStatus.visibility = android.view.View.GONE
            }
        }

        // Checkbox
        binding.cbCompleted.setOnCheckedChangeListener(null) // Avoid recursive calls
        binding.cbCompleted.isChecked = task.completed
        binding.cbCompleted.setOnCheckedChangeListener { _, _ ->
            onToggleComplete(task)
        }

        // Edit button
        binding.btnEdit.setOnClickListener { onEdit(task) }

        // Delete button
        binding.btnDelete.setOnClickListener { onDelete(task) }

        // Card elevation tint based on completion
        if (task.completed) {
            binding.cardTask.alpha = 0.7f
        } else {
            binding.cardTask.alpha = 1.0f
        }
    }

    // -----------------------------------------------------------------------
    //  DiffUtil
    // -----------------------------------------------------------------------

    class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean =
            oldItem.documentId == newItem.documentId

        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean =
            oldItem == newItem
    }
}
