package com.chimeragaming.gamepulse.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chimeragaming.gamepulse.R
import com.chimeragaming.gamepulse.model.GameInfo

/**
 * Adapter for game collection list
 * v0.3: Displays games with battery/RAM stats and medals
 */
class GameCollectionAdapter(
    private val games: List<GameInfo>
) : RecyclerView.Adapter<GameCollectionAdapter.GameViewHolder>() {

    class GameViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val gameNameText: TextView = view.findViewById(R.id.gameNameText)
        val systemText: TextView = view.findViewById(R.id.systemText)
        val batteryText: TextView = view.findViewById(R.id.batteryText)
        val ramText: TextView = view.findViewById(R.id.ramText)
        val playtimeText: TextView = view.findViewById(R.id.playtimeText)
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
        holder.batteryText.text = "⚡ ${game.getBatteryDrainFormatted()}"
        holder.ramText.text = "💾 ${game.getRamUsageFormatted()}"
        holder.playtimeText.text = "⏱️ ${game.getPlaytimeFormatted()}"

        // Show medals for top 3
        if (position < 3) {
            holder.batteryMedalText.visibility = View.VISIBLE
            holder.batteryMedalText.text = game.getBatteryMedal(position + 1)
        } else {
            holder.batteryMedalText.visibility = View.GONE
        }

        // RAM medal (currently hidden, can be shown based on sort type)
        holder.ramMedalText.visibility = View.GONE
    }

    override fun getItemCount() = games.size
}