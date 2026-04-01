package com.chimeragaming.gamepulse.ui

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chimeragaming.gamepulse.R
import com.chimeragaming.gamepulse.model.GameInfo

class GameCollectionAdapter(
    private val games: List<GameInfo>,
    private var editEnabled: Boolean,
    private var deleteEnabled: Boolean,
    private val onEditImageClicked: (GameInfo) -> Unit,
    private val onSaveImageClicked: (GameInfo) -> Unit,
    private val onDeleteClicked: (GameInfo) -> Unit
) : RecyclerView.Adapter<GameCollectionAdapter.GameViewHolder>() {

    private val pendingImageUris = mutableMapOf<String, String>()

    class GameViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val gameImageView: ImageView = view.findViewById(R.id.gameImageView)
        val gameImageFallbackText: TextView = view.findViewById(R.id.gameImageFallbackText)
        val gameNameText: TextView = view.findViewById(R.id.gameNameText)
        val systemText: TextView = view.findViewById(R.id.systemText)
        val batteryText: TextView = view.findViewById(R.id.batteryText)
        val ramText: TextView = view.findViewById(R.id.ramText)
        val tempText: TextView = view.findViewById(R.id.tempText)
        val playtimeText: TextView = view.findViewById(R.id.playtimeText)
        val analysisDurationText: TextView = view.findViewById(R.id.analysisDurationText)
        val editActionsRow: View = view.findViewById(R.id.editActionsRow)
        val editImageButton: View = view.findViewById(R.id.editImageButton)
        val saveImageButton: View = view.findViewById(R.id.saveImageButton)
        val deleteButton: View = view.findViewById(R.id.deleteButton)
        val batteryMedalText: TextView = view.findViewById(R.id.batteryMedalText)
        val ramMedalText: TextView = view.findViewById(R.id.ramMedalText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_game_collection, parent, false)
        return GameViewHolder(view)
    }

    override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
        val game = games[position]

        holder.gameNameText.text = game.name
        holder.systemText.text = game.system
        holder.batteryText.text = game.getBatteryDrainFormatted()
        holder.ramText.text = game.getRamUsageFormatted()
        holder.tempText.text = game.getAverageTemperatureFormatted()
        holder.playtimeText.text = game.getPlaytimeFormatted()
        holder.analysisDurationText.text = "Last Analysis: ${game.getLastTestDurationFormatted()}"
        bindGameImage(holder, game, pendingImageUris[game.id] ?: game.iconUrl)

        val hasPendingImage = pendingImageUris.containsKey(game.id)
        holder.editActionsRow.visibility = if (editEnabled) View.VISIBLE else View.GONE
        holder.editImageButton.visibility = if (editEnabled) View.VISIBLE else View.GONE
        holder.saveImageButton.visibility = if (editEnabled && hasPendingImage) View.VISIBLE else View.GONE

        holder.editImageButton.setOnClickListener {
            onEditImageClicked(game)
        }
        holder.saveImageButton.setOnClickListener {
            onSaveImageClicked(game)
        }

        holder.deleteButton.visibility = if (deleteEnabled) View.VISIBLE else View.GONE
        holder.deleteButton.setOnClickListener {
            onDeleteClicked(game)
        }

        if (position < 3) {
            holder.batteryMedalText.visibility = View.VISIBLE
            holder.batteryMedalText.text = game.getBatteryMedal(position + 1)
        } else {
            holder.batteryMedalText.visibility = View.GONE
        }

        holder.ramMedalText.visibility = View.GONE
    }

    override fun getItemCount() = games.size

    fun setEditEnabled(enabled: Boolean) {
        if (editEnabled == enabled) {
            return
        }

        editEnabled = enabled
        notifyDataSetChanged()
    }

    fun setDeleteEnabled(enabled: Boolean) {
        if (deleteEnabled == enabled) {
            return
        }

        deleteEnabled = enabled
        notifyDataSetChanged()
    }

    fun setPendingImage(gameId: String, imageUri: String?) {
        if (imageUri.isNullOrBlank()) {
            pendingImageUris.remove(gameId)
        } else {
            pendingImageUris[gameId] = imageUri
        }
        notifyDataSetChanged()
    }

    fun setPendingImages(values: Map<String, String>) {
        pendingImageUris.clear()
        pendingImageUris.putAll(values)
        notifyDataSetChanged()
    }

    private fun bindGameImage(holder: GameViewHolder, game: GameInfo, imageUri: String?) {
        val fallbackText = buildInitials(game.name)
        holder.gameImageFallbackText.text = fallbackText
        holder.gameImageView.setImageDrawable(null)
        holder.gameImageView.visibility = View.GONE
        holder.gameImageFallbackText.visibility = View.VISIBLE

        if (imageUri.isNullOrBlank()) {
            return
        }

        try {
            holder.gameImageView.setImageURI(Uri.parse(imageUri))
            if (holder.gameImageView.drawable != null) {
                holder.gameImageView.visibility = View.VISIBLE
                holder.gameImageFallbackText.visibility = View.GONE
            }
        } catch (_: Exception) {
            holder.gameImageView.setImageDrawable(null)
            holder.gameImageView.visibility = View.GONE
            holder.gameImageFallbackText.visibility = View.VISIBLE
        }
    }

    private fun buildInitials(name: String): String {
        val words = name.trim()
            .split("\\s+".toRegex())
            .filter { it.isNotBlank() }

        val initials = words.take(2).joinToString("") { it.take(1) }
        return initials.ifBlank { "GP" }.uppercase()
    }
}
