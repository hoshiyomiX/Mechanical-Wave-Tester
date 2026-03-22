package com.transsion.rgbtester.ui.main

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.GridView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import com.transsion.rgbtester.R
import com.transsion.rgbtester.data.model.LEDEffect
import com.transsion.rgbtester.data.model.RGBColor
import com.transsion.rgbtester.data.model.RGBDeviceGroup
import com.transsion.rgbtester.data.model.TestResult
import com.transsion.rgbtester.databinding.ActivityMainBinding
import com.transsion.rgbtester.viewmodel.RGBViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: RGBViewModel by viewModels()

    private val logBuilder = StringBuilder()
    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    // Preset colors
    private val presetColors = listOf(
        RGBColor.RED to "Red",
        RGBColor.GREEN to "Green",
        RGBColor.BLUE to "Blue",
        RGBColor.WHITE to "White",
        RGBColor.YELLOW to "Yellow",
        RGBColor.CYAN to "Cyan",
        RGBColor.MAGENTA to "Magenta",
        RGBColor.ORANGE to "Orange",
        RGBColor.PURPLE to "Purple",
        RGBColor.PINK to "Pink"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupObservers()
        setupColorPreview()
        setupBrightnessControl()
        setupEffectChips()
        setupPresetColors()
        setupButtons()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)
    }

    private fun setupObservers() {
        // Root status
        viewModel.rootAvailable.observe(this) { hasRoot ->
            val statusText = if (hasRoot) "Available" else "Not Available"
            val color = if (hasRoot) "#4CAF50" else "#F44336"
            binding.rootStatusText.text = statusText
            binding.rootStatusText.setTextColor(Color.parseColor(color))

            if (!hasRoot) {
                showRootRequiredDialog()
            }
        }

        // Device count
        viewModel.deviceGroups.observe(this) { groups ->
            binding.deviceCountText.text = groups.size.toString()
            setupDeviceSpinner(groups)
        }

        // Current color
        viewModel.currentRGBColor.observe(this) { color ->
            updateColorPreview(color)
            binding.colorHexText.text = color.hexColor
        }

        // Brightness
        viewModel.brightness.observe(this) { brightness ->
            binding.brightnessValue.text = "$brightness%"
        }

        // LED on/off state
        viewModel.isLedOn.observe(this) { isOn ->
            updatePowerButton(isOn)
        }

        // Effect
        viewModel.currentEffect.observe(this) { effect ->
            updateEffectSelection(effect)
        }

        // Operation result
        viewModel.operationResult.observe(this) { result ->
            addLog(result.message)
        }

        // Test progress
        viewModel.testProgress.observe(this) { progress ->
            if (progress.isNotEmpty()) {
                binding.testProgressText.visibility = View.VISIBLE
                binding.testProgressText.text = progress
            }
        }

        // Loading state
        viewModel.isLoading.observe(this) { isLoading ->
            binding.loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Error messages
        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                addLog("ERROR: $it")
                viewModel.clearError()
            }
        }
    }

    private fun setupDeviceSpinner(groups: List<RGBDeviceGroup>) {
        if (groups.isEmpty()) {
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOf("No devices found"))
            binding.deviceSpinner.adapter = adapter
            return
        }

        val deviceNames = groups.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, deviceNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.deviceSpinner.adapter = adapter

        binding.deviceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                viewModel.selectDeviceGroup(groups[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupColorPreview() {
        binding.colorPreview.setOnClickListener {
            showColorPickerDialog()
        }
    }

    private fun showColorPickerDialog() {
        val currentColor = viewModel.currentRGBColor.value?.androidColor ?: Color.WHITE

        ColorPickerDialog.Builder(this)
            .setTitle("Select Color")
            .setPreferenceName("rgb_color_picker")
            .setPositiveButton("Apply", ColorEnvelopeListener { envelope, fromUser ->
                val color = RGBColor.fromColor(envelope.color)
                viewModel.setColor(color)
            })
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .attachAlphaSlideBar(false)
            .attachBrightnessSlideBar(true)
            .setBottomSpace(12)
            .show()
    }

    private fun updateColorPreview(color: RGBColor) {
        val adjusted = color.getAdjustedColor()
        binding.colorPreview.setBackgroundColor(adjusted.androidColor)
    }

    private fun setupBrightnessControl() {
        binding.brightnessSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                viewModel.setBrightness(value.toInt())
            }
        }
    }

    private fun setupEffectChips() {
        binding.effectChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener

            val checkedId = checkedIds.first()
            val effect = when (checkedId) {
                R.id.chipOff -> LEDEffect.OFF
                R.id.chipStatic -> LEDEffect.STATIC
                R.id.chipBreathing -> LEDEffect.BREATHING
                R.id.chipFlash -> LEDEffect.FLASH
                R.id.chipRainbow -> LEDEffect.RAINBOW
                else -> LEDEffect.STATIC
            }
            viewModel.setEffect(effect)
        }
    }

    private fun updateEffectSelection(effect: LEDEffect) {
        val chipId = when (effect) {
            LEDEffect.OFF -> R.id.chipOff
            LEDEffect.STATIC -> R.id.chipStatic
            LEDEffect.BREATHING -> R.id.chipBreathing
            LEDEffect.FLASH -> R.id.chipFlash
            LEDEffect.RAINBOW -> R.id.chipRainbow
            else -> R.id.chipStatic
        }
        binding.effectChipGroup.check(chipId)
    }

    private fun setupPresetColors() {
        val gridLayout = binding.presetGrid
        gridLayout.removeAllViews()

        presetColors.forEach { (color, name) ->
            val button = Button(this).apply {
                layoutParams = android.widget.GridLayout.LayoutParams().apply {
                    width = 0
                    height = 120
                    columnSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f)
                    setMargins(4, 4, 4, 4)
                }

                val drawable = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color.androidColor)
                    setStroke(2, Color.WHITE)
                }
                background = drawable

                setOnClickListener {
                    viewModel.applyPreset(color)
                }

                setOnLongClickListener {
                    Toast.makeText(context, name, Toast.LENGTH_SHORT).show()
                    true
                }
            }
            gridLayout.addView(button)
        }
    }

    private fun setupButtons() {
        // Power button
        binding.btnPower.setOnClickListener {
            viewModel.togglePower()
        }

        // Test button
        binding.btnTest.setOnClickListener {
            showTestOptionsDialog()
        }
    }

    private fun updatePowerButton(isOn: Boolean) {
        binding.btnPower.text = if (isOn) "Power Off" else "Power On"
        val color = if (isOn) "#F44336" else "#4CAF50"
        binding.btnPower.setBackgroundColor(Color.parseColor(color))
    }

    private fun showTestOptionsDialog() {
        val options = arrayOf(
            "Full RGB Test",
            "Breathing Effect Test",
            "Flash Effect Test",
            "Rainbow Effect Test",
            "Channel Test"
        )

        MaterialAlertDialogBuilder(this)
            .setTitle("Select Test")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> viewModel.runRGBTest()
                    1 -> viewModel.setEffect(LEDEffect.BREATHING)
                    2 -> viewModel.setEffect(LEDEffect.FLASH)
                    3 -> viewModel.setEffect(LEDEffect.RAINBOW)
                    4 -> showChannelTestDialog()
                }
            }
            .show()
    }

    private fun showChannelTestDialog() {
        val groups = viewModel.deviceGroups.value ?: return
        if (groups.isEmpty()) {
            Toast.makeText(this, "No devices available", Toast.LENGTH_SHORT).show()
            return
        }

        val groupNames = groups.map { it.name }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle("Select Device to Test")
            .setItems(groupNames) { _, which ->
                groups[which].let { group ->
                    lifecycleScope.launch {
                        // Test each channel
                        listOfNotNull(group.redPath, group.greenPath, group.bluePath, group.singlePath).forEach { device ->
                            viewModel.testChannel(device)
                        }
                    }
                }
            }
            .show()
    }

    private fun showRootRequiredDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Root Required")
            .setMessage("This app requires root access to control RGB LEDs via sysfs.\n\n" +
                    "Please ensure:\n" +
                    "1. Your device is rooted\n" +
                    "2. You grant root permission when prompted\n" +
                    "3. SELinux is set to Permissive if needed")
            .setPositiveButton("Retry") { _, _ ->
                viewModel.initialize()
            }
            .setNegativeButton("Exit") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun addLog(message: String) {
        val timestamp = dateFormat.format(Date())
        logBuilder.append("[$timestamp] $message\n")
        binding.logText.text = logBuilder.toString()
        binding.logCard.visibility = View.VISIBLE
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                viewModel.detectDevices()
                true
            }
            R.id.action_settings -> {
                // Open settings
                true
            }
            R.id.action_clear_log -> {
                logBuilder.clear()
                binding.logText.text = ""
                binding.logCard.visibility = View.GONE
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
