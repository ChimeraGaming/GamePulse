package com.chimeragaming.gamepulse.ui

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.chimeragaming.gamepulse.databinding.ActivityGameCollectionBinding
import com.chimeragaming.gamepulse.model.GameInfo
import com.chimeragaming.gamepulse.utils.GameCollectionRepository
import com.chimeragaming.gamepulse.utils.SharedPreferencesManager
import com.chimeragaming.gamepulse.utils.ThemeManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

/*
 * ╔═══════════════════════════════════════════════════════════════════════╗
 * ║                   GAME COLLECTION ACTIVITY                            ║
 * ║                   GamePulse Performance Tracker                       ║
 * ║              v0.3.2 - Theme Support + Crash Protection                ║
 * ╚═══════════════════════════════════════════════════════════════════════╝
 */
class GameCollectionActivity : AppCompatActivity() {

    private var _binding: ActivityGameCollectionBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: GameCollectionRepository
    private lateinit var prefsManager: SharedPreferencesManager
    private var gameAdapter: GameCollectionAdapter? = null
    private var allGames = mutableListOf<GameInfo>()
    private var filteredGames = mutableListOf<GameInfo>()
    private val pendingImageUris = mutableMapOf<String, String>()
    private var pendingImageGameId: String? = null

    private val sortOptions = arrayOf(
        "Battery (Best First)",
        "Battery (Worst First)",
        "RAM (Lowest First)",
        "RAM (Highest First)",
        "Temperature (Lowest First)",
        "Alphabetical"
    )

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val gameId = pendingImageGameId
        pendingImageGameId = null

        if (uri == null || gameId.isNullOrBlank()) {
            return@registerForActivityResult
        }

        try {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, takeFlags)
        } catch (_: Exception) {
        }

        pendingImageUris[gameId] = uri.toString()
        gameAdapter?.setPendingImage(gameId, uri.toString())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // CRITICAL: Apply theme BEFORE setContentView
        try {
            ThemeManager.applyTheme(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        super.onCreate(savedInstanceState)

        try {
            _binding = ActivityGameCollectionBinding.inflate(layoutInflater)
            setContentView(binding.root)
            repository = GameCollectionRepository(this)
            prefsManager = SharedPreferencesManager(this)

            setupUI()
            loadTrackedGames()
        } catch (e: Exception) {
            e.printStackTrace()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()

        try {
            if (_binding != null) {
                loadTrackedGames()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        try {
            if (_binding != null) {
                gameAdapter?.notifyDataSetChanged()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupUI() {
        if (_binding == null || isFinishing || isDestroyed) {
            return
        }

        try {
            // Back button
            binding.backButton.setOnClickListener {
                finish()
            }

            // Settings button
            binding.settingsButton.setOnClickListener {
                showSettingsDialog()
            }

            // Setup RecyclerView
            gameAdapter = GameCollectionAdapter(
                games = filteredGames,
                editEnabled = prefsManager.gameCollectionEditEnabled,
                deleteEnabled = prefsManager.gameCollectionDeleteEnabled,
                onEditImageClicked = { game ->
                    startImageEdit(game)
                },
                onSaveImageClicked = { game ->
                    saveGameImage(game)
                },
                onDeleteClicked = { game ->
                    confirmDelete(game)
                }
            )
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
                    try {
                        applySort(position)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadTrackedGames() {
        try {
            gameAdapter?.setEditEnabled(prefsManager.gameCollectionEditEnabled)
            gameAdapter?.setDeleteEnabled(prefsManager.gameCollectionDeleteEnabled)
            allGames.clear()
            filteredGames.clear()
            allGames.addAll(repository.getTrackedGames())
            filteredGames.addAll(allGames)
            val validIds = allGames.map { it.id }.toSet()
            pendingImageUris.keys.retainAll(validIds)
            gameAdapter?.setPendingImages(pendingImageUris)
            applySort(binding.sortSpinner.selectedItemPosition.coerceAtLeast(0))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun applySort(sortType: Int) {
        if (_binding == null || isFinishing || isDestroyed) {
            return
        }

        try {
            filteredGames.clear()
            filteredGames.addAll(allGames)

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
                4 -> { // Temperature Lowest First
                    filteredGames.sortBy { temperatureSortValue(it.averageTemperatureC) }
                }
                5 -> { // Alphabetical
                    filteredGames.sortBy { it.name }
                }
            }

            gameAdapter?.notifyDataSetChanged()
            updateCollectionState()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateCollectionState() {
        if (_binding == null || isFinishing || isDestroyed) {
            return
        }

        try {
            val hasGames = filteredGames.isNotEmpty()
            binding.gamesRecyclerView.visibility = if (hasGames) View.VISIBLE else View.GONE
            binding.emptyStateLayout.visibility = if (hasGames) View.GONE else View.VISIBLE
            binding.gameCountText.text = "${filteredGames.size} games"
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun temperatureSortValue(temperatureC: Float): Float {
        return if (temperatureC > 0f) {
            temperatureC
        } else {
            Float.MAX_VALUE
        }
    }

    private fun showSettingsDialog() {
        try {
            val dialog = GameCollectionSettingsDialog(this) {
                if (!prefsManager.gameCollectionEditEnabled) {
                    pendingImageGameId = null
                    pendingImageUris.clear()
                    gameAdapter?.setPendingImages(emptyMap())
                }
                gameAdapter?.setEditEnabled(prefsManager.gameCollectionEditEnabled)
                gameAdapter?.setDeleteEnabled(prefsManager.gameCollectionDeleteEnabled)
            }
            dialog.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startImageEdit(game: GameInfo) {
        try {
            pendingImageGameId = game.id
            imagePickerLauncher.launch(arrayOf("image/*"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveGameImage(game: GameInfo) {
        val imageUri = pendingImageUris[game.id] ?: return

        try {
            if (repository.updateTrackedGameImage(game.id, imageUri)) {
                pendingImageUris.remove(game.id)
                loadTrackedGames()
                Snackbar.make(binding.root, "Game image saved", Snackbar.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun confirmDelete(game: GameInfo) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Game")
            .setMessage("Delete ${game.name} from Game Collection? This removes the saved tracked stats for this game.")
            .setPositiveButton("Delete") { _, _ ->
                deleteGame(game)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteGame(game: GameInfo) {
        try {
            if (repository.deleteTrackedGame(game.id)) {
                loadTrackedGames()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            gameAdapter = null
            allGames.clear()
            filteredGames.clear()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        _binding = null
    }
}
