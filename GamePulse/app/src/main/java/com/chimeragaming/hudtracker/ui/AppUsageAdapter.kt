package com.chimeragaming.gamepulse.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.chimeragaming.gamepulse.databinding.ItemAppUsageBinding
import com.chimeragaming.gamepulse.model.AppEnergyUsage

/**
 * RecyclerView adapter for displaying app energy usage
 */
class AppUsageAdapter : ListAdapter<AppEnergyUsage, AppUsageAdapter.ViewHolder>(DiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAppUsageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class ViewHolder(private val binding: ItemAppUsageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(appUsage: AppEnergyUsage) {
            binding.appNameText.text = appUsage.appName
            binding.cpuUsageText.text = String.format("CPU: %.1f%%", appUsage.cpuUsagePercent)
            binding.screenTimeText.text = String.format("Screen: %d min", appUsage.screenOnTimeMinutes)
            binding.batteryDrainText.text = String.format("Battery: %.1f%%", appUsage.batteryDrainPercent)
            binding.energyScoreText.text = String.format("Score: %.1f", appUsage.getTotalEnergyScore())
        }
    }
    
    private class DiffCallback : DiffUtil.ItemCallback<AppEnergyUsage>() {
        override fun areItemsTheSame(oldItem: AppEnergyUsage, newItem: AppEnergyUsage): Boolean {
            return oldItem.packageName == newItem.packageName
        }
        
        override fun areContentsTheSame(oldItem: AppEnergyUsage, newItem: AppEnergyUsage): Boolean {
            return oldItem == newItem
        }
    }
}
