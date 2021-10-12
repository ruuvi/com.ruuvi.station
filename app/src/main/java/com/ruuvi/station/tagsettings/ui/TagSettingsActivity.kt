package com.ruuvi.station.tagsettings.ui

import android.app.Activity
import android.content.*
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.ruuvi.station.BuildConfig
import com.ruuvi.station.R
import com.ruuvi.station.calibration.model.CalibrationType
import com.ruuvi.station.calibration.ui.CalibrationActivity
import com.ruuvi.station.database.domain.SensorHistoryRepository
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.database.tables.SensorSettings
import com.ruuvi.station.databinding.ActivityTagSettingsBinding
import com.ruuvi.station.dfu.ui.DfuUpdateActivity
import com.ruuvi.station.image.ImageInteractor
import com.ruuvi.station.network.ui.ClaimSensorActivity
import com.ruuvi.station.network.ui.ShareSensorActivity
import com.ruuvi.station.tagsettings.di.TagSettingsViewModelArgs
import com.ruuvi.station.tagsettings.domain.CsvExporter
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.units.model.HumidityUnit
import com.ruuvi.station.util.Utils
import com.ruuvi.station.util.extensions.setDebouncedOnClickListener
import com.ruuvi.station.util.extensions.viewModel
import no.nordicsemi.android.dfu.DfuServiceInitiator
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate

class TagSettingsActivity : AppCompatActivity(R.layout.activity_tag_settings), KodeinAware {

    override val kodein: Kodein by closestKodein()

    private lateinit var binding: ActivityTagSettingsBinding

    private val viewModel: TagSettingsViewModel by viewModel {
        intent.getStringExtra(TAG_ID)?.let {
            TagSettingsViewModelArgs(it)
        }
    }

    private val repository: TagRepository by instance()
    private val unitsConverter: UnitsConverter by instance()
    private val imageInteractor: ImageInteractor by instance()
    private val sensorHistoryRepository: SensorHistoryRepository by instance()
    private val sensorSettingsRepository: SensorSettingsRepository by instance()
    private var timer: Timer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTagSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        setupViewModel()

        setupUI()
    }

    private fun setupViewModel() {
        viewModel.setupAlarmElements()

        viewModel.tagObserve.observe(this) { tag ->
            tag?.let {
                setupCalibration(it)
                updateReadings(it)
            }
        }

        viewModel.sensorSettingsObserve.observe(this) {sensorSettings ->
            sensorSettings?.let {
                setupInputMac(sensorSettings)
                setupSensorImage(sensorSettings)
                setupSensorName(sensorSettings)
            }
            binding.calibrateTemperature.setItemValue(
                unitsConverter.getTemperatureOffsetString(sensorSettings?.temperatureOffset ?: 0.0))
            binding.calibratePressure.setItemValue(
                unitsConverter.getPressureString(sensorSettings?.pressureOffset ?: 0.0))
            binding.calibrateHumidity.setItemValue(
                unitsConverter.getHumidityString(sensorSettings?.humidityOffset ?: 0.0, 0.0, HumidityUnit.PERCENT)
            )
            binding.ownerValueTextView.text = sensorSettings?.owner ?: "None"

            if (sensorSettings?.networkSensor != true) {
                deleteString = getString(R.string.remove_local_sensor)
                binding.ownerLayout.isEnabled = true
                binding.ownerValueTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, getDrawable(R.drawable.ic_baseline_arrow_forward_ios_24), null)
            } else {
                binding.ownerLayout.isEnabled = false
                binding.ownerValueTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
                if (viewModel.sensorOwnedByUserObserve.value == true) {
                    deleteString = getString(R.string.remove_claimed_sensor)
                } else {
                    deleteString = getString(R.string.remove_shared_sensor)
                }
            }
        }

        viewModel.userLoggedInObserve.observe(this) {
            if (it == true) {
                binding.ownerLayout.visibility = View.VISIBLE
            } else {
                binding.ownerLayout.visibility = View.GONE
            }
        }

        viewModel.sensorOwnedByUserObserve.observe(this) {
            if (it) {
                binding.shareLayout.visibility = View.VISIBLE
            } else {
                binding.shareLayout.visibility = View.GONE
            }
        }

        viewModel.operationStatusObserve.observe(this) {
            if (!it.isNullOrEmpty()) {
                Snackbar.make(binding.toolbarContainer, it, Snackbar.LENGTH_SHORT).show()
                viewModel.statusProcessed()
            }
        }

        viewModel.canCalibrate.observe(this) {
            binding.calibrationHeaderTextView.isVisible = it
            binding.calibrationLayout.isVisible = it
        }

        viewModel.sensorSharedObserve.observe(this) { isShared ->
            binding.shareValueTextView.text = if (isShared) {
                getText(R.string.sensor_shared)
            } else {
                getText(R.string.sensor_not_shared)
            }
        }
    }

    private fun setupUI() {
        setupAlarmItems()

        binding.removeSensorTitleTextView.setDebouncedOnClickListener { delete() }

        binding.ownerValueTextView.setDebouncedOnClickListener {
            if (binding.ownerLayout.isEnabled) {
                ClaimSensorActivity.start(this, viewModel.sensorId)
            }
        }

        binding.shareLayout.setDebouncedOnClickListener {
            ShareSensorActivity.start(this, viewModel.sensorId)
        }

        binding.calibrateHumidity.setDebouncedOnClickListener {
            CalibrationActivity.start(this, viewModel.sensorId, CalibrationType.HUMIDITY)
        }

        binding.calibrateTemperature.setDebouncedOnClickListener {
            CalibrationActivity.start(this, viewModel.sensorId, CalibrationType.TEMPERATURE)
        }

        binding.calibratePressure.setDebouncedOnClickListener {
            CalibrationActivity.start(this, viewModel.sensorId, CalibrationType.PRESSURE)
        }

        binding.firmwareUpdateTitleTextView.setDebouncedOnClickListener {
            DfuUpdateActivity.start(this, viewModel.sensorId)
        }

        binding.generalHeader.setDebouncedOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                DfuServiceInitiator.createDfuNotificationChannel(this)
            }


//            val sensorUpdateMac = incrementMacAddress(viewModel.sensorId)
//            Timber.d("incrementMacAddress $sensorUpdateMac")
//            val initiator = DfuServiceInitiator(sensorUpdateMac)
//                 .setKeepBond(true)
//            val file = File("//storage//emulated//0//DFU//ruuvitag3_30_4_dfu.zip")
//            Timber.d("file exists ${file.exists()}")
//            initiator.setZip("//storage//emulated//0//DFU//ruuvitag3_30_4_dfu.zip")
//            initiator.start(this, DfuService::class.java)
        }
    }

    private var deleteString: String = ""

    override fun onResume() {
        super.onResume()
        timer = Timer("TagSettingsActivityTimer", true)
        timer?.scheduleAtFixedRate(0, 1000) {
            viewModel.getTagInfo()
        }
        viewModel.checkIfSensorShared()
    }

    override fun onPause() {
        super.onPause()
        timer?.cancel()
        binding.alarmTemperature.saveAlarm()
        binding.alarmHumidity.saveAlarm()
        binding.alarmPressure.saveAlarm()
        binding.alarmRssi.saveAlarm()
        binding.alarmMovement.saveAlarm()

    }

    @Suppress("NAME_SHADOWING")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == Activity.RESULT_OK) {
            if (viewModel.file != null) {
                val rotation = imageInteractor.getCameraPhotoOrientation(viewModel.file)
                imageInteractor.resize(currentPhotoPath, viewModel.file, rotation)
                viewModel.updateTagBackground(viewModel.file.toString(), null)
                val backgroundUri = Uri.parse(viewModel.file.toString())
                val background = imageInteractor.getImage(backgroundUri)
                binding.tagImageView.setImageBitmap(background)
            }
        } else if (requestCode == REQUEST_GALLERY_PHOTO && resultCode == Activity.RESULT_OK) {
            data?.let {
                try {
                    val path = data.data ?: return
                    if (!imageInteractor.isImage(path)) {
                        Toast.makeText(this, getString(R.string.file_not_supported), Toast.LENGTH_SHORT).show()
                        return
                    }
                    val inputStream = applicationContext.contentResolver.openInputStream(path)
                    var photoFile: File? = null
                    try {
                        photoFile = createImageFile()
                    } catch (ex: IOException) {
                        // This is fine :)
                    }
                    if (photoFile != null) {
                        try {
                            photoFile.createNewFile()
                        } catch (ioEx: IOException) {
                            // :(
                            return
                        }
                        val output: OutputStream = FileOutputStream(photoFile)
                        output.use { output ->
                            val buffer = ByteArray(4 * 1024) // or other buffer size
                            var read: Int
                            while (inputStream!!.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                            }
                            output.flush()
                        }
                        val uri = Uri.fromFile(photoFile)
                        val rotation = imageInteractor.getCameraPhotoOrientation(uri)
                        imageInteractor.resize(currentPhotoPath, uri, rotation)
                        viewModel.updateTagBackground(uri.toString(), null)
                        val backgroundUri = Uri.parse(uri.toString())
                        val background = imageInteractor.getImage(backgroundUri)
                        binding.tagImageView.setImageBitmap(background)
                    }
                } catch (e: Exception) {
                    Timber.e("Could not load photo: $e")
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_export) {
            val exporter = CsvExporter(this, repository, sensorHistoryRepository, sensorSettingsRepository, unitsConverter)
            viewModel.tagObserve.value?.id?.let {
                exporter.toCsv(it)
            }
        } else {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_edit, menu)
        return true
    }

    private fun setupSensorName(sensorSettings: SensorSettings) {
        binding.tagNameInputTextView.text = sensorSettings.displayName

        binding.tagNameInputTextView.setDebouncedOnClickListener {
            val sensorNameEditDialog = SensorNameEditDialog.new_instance(sensorSettings.name, object: SensorNameEditListener {
                override fun onDialogPositiveClick(dialog: DialogFragment, value: String?) {
                    viewModel.setName(value)
                }
                override fun onDialogNegativeClick(dialog: DialogFragment) { }
            })

            sensorNameEditDialog.show(this.supportFragmentManager, "sensorName")
        }
    }

    private fun setupInputMac(sensorSettings: SensorSettings) {
        binding.inputMacTextView.text = sensorSettings.id

        binding.inputMacTextView.setOnLongClickListener {
            val clipboard: ClipboardManager? = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

            val clip = ClipData.newPlainText(getString(R.string.mac_address), sensorSettings.id)

            try {
                if (BuildConfig.DEBUG && clipboard == null) {
                    error("Assertion failed")
                }

                clipboard?.setPrimaryClip(clip)

                Toast.makeText(this@TagSettingsActivity, getString(R.string.mac_copied), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Timber.e(e, "Could not copy mac to clipboard")
            }
            false
        }
    }

    private fun setupSensorImage(sensorSettings: SensorSettings) {
        if (sensorSettings.userBackground.isNullOrEmpty() == false) {
            val backgroundUri = Uri.parse(sensorSettings.userBackground)
            val background = imageInteractor.getImage(backgroundUri)
            binding.tagImageView.setImageBitmap(background)
        } else {
            binding.tagImageView.setImageBitmap(BitmapFactory.decodeResource(getResources(), Utils.getDefaultBackground(sensorSettings.defaultBackground)))
        }

        binding.tagImageCameraButton.setDebouncedOnClickListener { showImageSourceSheet() }

        binding.tagImageSelectButton.setDebouncedOnClickListener {
            val defaultBackground = if (sensorSettings.defaultBackground == 8) 0 else sensorSettings.defaultBackground + 1
            viewModel.updateTagBackground(null, defaultBackground)
            sensorSettings.defaultBackground = defaultBackground
            binding.tagImageView.setImageDrawable(Utils.getDefaultBackground(defaultBackground, applicationContext))
        }
    }

    private fun setupCalibration(tag: RuuviTagEntity) {
        binding.calibrateHumidity.isGone = tag.humidity == null
        binding.calibratePressure.isGone = tag.pressure == null
        binding.calibrateHumidityDivider.isGone = tag.humidity == null
        binding.calibratePressureDivider.isGone = tag.pressure == null
    }

    private fun setupAlarmItems() {
        binding.alarmTemperature.restoreState(viewModel.alarmElements[0])
        binding.alarmHumidity.restoreState(viewModel.alarmElements[1])
        binding.alarmPressure.restoreState(viewModel.alarmElements[2])
        binding.alarmRssi.restoreState(viewModel.alarmElements[3])
        binding.alarmMovement.restoreState(viewModel.alarmElements[4])
    }

    private fun updateReadings(tag: RuuviTagEntity) {
        if (tag.dataFormat == 3 || tag.dataFormat == 5) {
            binding.rawValuesLayout.isVisible = true
            binding.inputVoltageTextView.text = this.getString(R.string.voltage_reading, tag.voltage.toString(), getString(R.string.voltage_unit))
            binding.xInputTextView.text = getString(R.string.acceleration_reading, tag.accelX)
            binding.yInputTextView.text = getString(R.string.acceleration_reading, tag.accelY)
            binding.zInputTextView.text = getString(R.string.acceleration_reading, tag.accelZ)
            binding.dataFormatTextView.text = tag.dataFormat.toString()
            binding.txPowerTextView.text = getString(R.string.tx_power_reading, tag.txPower)
            binding.rssiTextView.text = unitsConverter.getSignalString(tag.rssi)
            binding.sequenceNumberTextView.text = tag.measurementSequenceNumber.toString()
        } else {
            binding.rawValuesLayout.isVisible = false
        }
    }

    private fun delete() {
        val builder = AlertDialog.Builder(this)

        builder.setTitle(this.getString(R.string.tagsettings_sensor_remove))

        builder.setMessage(deleteString)

        builder.setPositiveButton(android.R.string.ok) { _, _ ->
            for (alarm in viewModel.alarmElements) {
                alarm.alarm?.let { viewModel.removeNotificationById(it.id) }
            }
            viewModel.tagObserve.value?.let { viewModel.deleteTag(it) }
            finish()
        }

        builder.setNegativeButton(android.R.string.cancel, null)

        builder.show()
    }

    private fun showImageSourceSheet() {
        val sheetDialog = BottomSheetDialog(this)
        val listView = ListView(this)
        val menu = arrayOf(
            resources.getString(R.string.camera),
            resources.getString(R.string.gallery)
        )
        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, menu)
        listView.onItemClickListener = AdapterView.OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
            when (position) {
                0 -> dispatchTakePictureIntent()
                1 -> imageFromGallery
            }
            sheetDialog.dismiss()
        }
        sheetDialog.setContentView(listView)
        sheetDialog.show()
    }

    private var currentPhotoPath: String? = null

    private fun createImageFile(): File {
        val imageFileName = "background_" + viewModel.sensorId
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(imageFileName, ".jpg", storageDir)
        currentPhotoPath = image.absolutePath
        return image
    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            // Create the File where the photo should go
            var photoFile: File? = null
            try {
                photoFile = createImageFile()
            } catch (ex: IOException) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                try {
                    photoFile.createNewFile()
                } catch (ioEx: IOException) {
                    Toast.makeText(this, getString(R.string.camera_fail), Toast.LENGTH_SHORT).show()
                    return
                }
                viewModel.file = FileProvider.getUriForFile(
                    this,
                    "com.ruuvi.station.fileprovider",
                    photoFile
                )
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, viewModel.file)
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO)
            }
        }
    }

    private val imageFromGallery: Unit
        get() {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent, getString(R.string.select_picture)), REQUEST_GALLERY_PHOTO)
        }

    companion object {
        private const val TAG_ID = "TAG_ID"

        private const val REQUEST_TAKE_PHOTO = 1

        private const val REQUEST_GALLERY_PHOTO = 2

        fun start(context: Context, tagId: String?) {
            val intent = Intent(context, TagSettingsActivity::class.java)
            intent.putExtra(TAG_ID, tagId)
            context.startActivity(intent)
        }

        fun startForResult(context: Activity, requestCode: Int, tagId: String?) {
            val settingsIntent = Intent(context, TagSettingsActivity::class.java)
            settingsIntent.putExtra(TAG_ID, tagId)
            context.startActivityForResult(settingsIntent, requestCode)
        }
    }
}