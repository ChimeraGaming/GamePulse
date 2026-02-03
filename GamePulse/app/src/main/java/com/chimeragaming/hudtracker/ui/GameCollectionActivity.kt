package com.chimeragaming.gamepulse.ui

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.chimeragaming.gamepulse.databinding.ActivityGameCollectionBinding
import com.chimeragaming.gamepulse.model.GameInfo

/**
 * Activity for displaying game collection with battery and RAM stats
 * v0.3: Initial implementation with test data
 */
class GameCollectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGameCollectionBinding
    private lateinit var gameAdapter: GameCollectionAdapter
    private var allGames = mutableListOf<GameInfo>()
    private var filteredGames = mutableListOf<GameInfo>()

    private val sortOptions = arrayOf(
        "Battery (Best First)",
        "Battery (Worst First)",
        "RAM (Lowest First)",
        "RAM (Highest First)",
        "Alphabetical"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameCollectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        loadTestData()
        applySort(0) // Default sort by battery best first
    }

    private fun setupUI() {
        // Back button
        binding.backButton.setOnClickListener {
            finish()
        }

        // Settings button
        binding.settingsButton.setOnClickListener {
            showSettingsDialog()
        }

        // Setup RecyclerView
        gameAdapter = GameCollectionAdapter(filteredGames)
        binding.gamesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@GameCollectionActivity)
            adapter = gameAdapter
        }

        // Setup sort spinner
        val sortAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sortOptions)
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.sortSpinner.adapter = sortAdapter

        binding.sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                applySort(position)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadTestData() {
        // Test data - will be replaced with real data later
        allGames.addAll(listOf(
            GameInfo(
                id = "1",
                name = "Genshin Impact",
                system = "Android",
                batteryDrainPerHour = 25.5f,
                ramUsageGB = 4.2f,
                averageSessionMinutes = 45,
                totalPlaytimeMinutes = 1320,
                lastPlayedTimestamp = System.currentTimeMillis() - 3600000
            ),
            GameInfo(
                id = "2",
                name = "Zelda: Breath of the Wild",
                system = "Switch Emulator",
                batteryDrainPerHour = 32.8f,
                ramUsageGB = 6.5f,
                averageSessionMinutes = 90,
                totalPlaytimeMinutes = 4500,
                lastPlayedTimestamp = System.currentTimeMillis() - 7200000
            ),
            GameInfo(
                id = "3",
                name = "Stardew Valley",
                system = "Android",
                batteryDrainPerHour = 15.2f,
                ramUsageGB = 2.1f,
                averageSessionMinutes = 60,
                totalPlaytimeMinutes = 2400,
                lastPlayedTimestamp = System.currentTimeMillis() - 86400000
            ),
            GameInfo(
                id = "4",
                name = "Call of Duty Mobile",
                system = "Android",
                batteryDrainPerHour = 28.9f,
                ramUsageGB = 5.8f,
                averageSessionMinutes = 30,
                totalPlaytimeMinutes = 900,
                lastPlayedTimestamp = System.currentTimeMillis() - 172800000
            ),
            GameInfo(
                id = "5",
                name = "Pokemon Emerald",
                system = "GBA Emulator",
                batteryDrainPerHour = 12.3f,
                ramUsageGB = 1.5f,
                averageSessionMinutes = 75,
                totalPlaytimeMinutes = 3600,
                lastPlayedTimestamp = System.currentTimeMillis() - 259200000
            ),
            GameInfo(
                id = "6",
                name = "God of War",
                system = "PS2 Emulator",
                batteryDrainPerHour = 35.6f,
                ramUsageGB = 7.2f,
                averageSessionMinutes = 60,
                totalPlaytimeMinutes = 1800,
                lastPlayedTimestamp = System.currentTimeMillis() - 432000000
            ),
            GameInfo(
                id = "7",
                name = "Minecraft",
                system = "Android",
                batteryDrainPerHour = 18.7f,
                ramUsageGB = 3.4f,
                averageSessionMinutes = 120,
                totalPlaytimeMinutes = 6000,
                lastPlayedTimestamp = System.currentTimeMillis() - 600000
            ),
            GameInfo(
                id = "8",
                name = "Among Us",
                system = "Android",
                batteryDrainPerHour = 14.5f,
                ramUsageGB = 1.8f,
                averageSessionMinutes = 25,
                totalPlaytimeMinutes = 750,
                lastPlayedTimestamp = System.currentTimeMillis() - 1209600000
            )
        ))

        filteredGames.addAll(allGames)
    }

    private fun applySort(sortType: Int) {
        when (sortType) {
            0 -> { // Battery Best First (lowest drain)
                filteredGames.sortBy { it.batteryDrainPerHour }
            }
            1 -> { // Battery Worst First (highest drain)
                filteredGames.sortByDescending { it.batteryDrainPerHour }
            }
            2 -> { // RAM Lowest First
                filteredGames.sortBy { it.ramUsageGB }
            }
            3 -> { // RAM Highest First
                filteredGames.sortByDescending { it.ramUsageGB }
            }
            4 -> { // Alphabetical
                filteredGames.sortBy { it.name }
            }
        }

        gameAdapter.notifyDataSetChanged()
        updateGameCount()
    }

    private fun updateGameCount() {
        binding.gameCountText.text = "${filteredGames.size} games"
    }

    private fun showSettingsDialog() {
        val dialog = GameCollectionSettingsDialog(this) {
            // Handle settings changes in future
        }
        dialog.show()
    }
}