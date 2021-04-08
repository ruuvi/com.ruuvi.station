package com.ruuvi.station.tagsettings.ui

import android.app.Activity
import android.app.Dialog
import android.content.*
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.InputFilter
import android.text.InputType
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.FileProvider
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.ruuvi.station.BuildConfig
import com.ruuvi.station.R
import com.ruuvi.station.calibration.model.CalibrationType
import com.ruuvi.station.calibration.ui.CalibrationActivity
import com.ruuvi.station.database.TagRepository
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.image.ImageInteractor
import com.ruuvi.station.network.ui.ShareSensorActivity
import com.ruuvi.station.tagsettings.di.TagSettingsViewModelArgs
import com.ruuvi.station.tagsettings.domain.HumidityCalibrationInteractor
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.util.CsvExporter
import com.ruuvi.station.util.Utils
import com.ruuvi.station.util.extensions.makeWebLinks
import com.ruuvi.station.util.extensions.setDebouncedOnClickListener
import com.ruuvi.station.util.extensions.viewModel
import kotlinx.android.synthetic.main.activity_tag_settings.*
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

class TagSettingsActivity : AppCompatActivity(), KodeinAware {

    override val kodein: Kodein by closestKodein()

    private val viewModel: TagSettingsViewModel by viewModel {
        intent.getStringExtra(TAG_ID)?.let {
            TagSettingsViewModelArgs(it)
        }
    }

    private val repository: TagRepository by instance()
    private val humidityCalibrationInteractor: HumidityCalibrationInteractor by instance()
    private val unitsConverter: UnitsConverter by instance()
    private val imageInteractor: ImageInteractor by instance()
    private var timer: Timer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_tag_settings)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        setupViewModel()

        setupUI()
    }

    private fun setupUI() {
        setupAlarmItems()

        removeTagButton.setDebouncedOnClickListener { delete() }

        claimTagButton.setDebouncedOnClickListener {
            viewModel.claimSensor()
        }

        shareTagButton.setDebouncedOnClickListener {
            ShareSensorActivity.start(this, viewModel.tagId )
        }

        calibrateHumidity.setDebouncedOnClickListener {
            CalibrationActivity.start(this, viewModel.tagId, CalibrationType.HUMIDITY)
        }

        calibrateTemperature.setDebouncedOnClickListener {
            CalibrationActivity.start(this, viewModel.tagId, CalibrationType.TEMPERATURE)
        }

        calibratePressure.setDebouncedOnClickListener {
            CalibrationActivity.start(this, viewModel.tagId, CalibrationType.PRESSURE)
        }
    }

    private fun setupViewModel() {
        viewModel.setupAlarmElements()

        viewModel.tagObserve.observe(this, Observer {  tag ->
            tag?.let {
                isTagFavorite(it)
                setupTagName(it)
                setupInputMac(it)
                setupTagImage(it)
                calibrateHumidity(it)
                updateReadings(it)
            }
        })

        viewModel.sensorSettingsObserve.observe(this, Observer {
            calibrateTemperature.setItemValue(
                unitsConverter.getTemperatureOffsetString(it?.temperatureOffset ?: 0.0))
        })

        viewModel.userLoggedInObserve.observe(this, Observer {
            if (it == true) {
                claimTagButton.visibility = View.VISIBLE
                shareTagButton.visibility = View.VISIBLE
                networkLayout.visibility = View.VISIBLE
            } else {
                claimTagButton.visibility = View.GONE
                shareTagButton.visibility = View.GONE
                networkLayout.visibility = View.GONE
            }
        })

        viewModel.sensorOwnedByUserObserve.observe(this, Observer {
            shareTagButton.isEnabled = it
        })

        viewModel.isNetworkTagObserve.observe(this, Observer {
            claimTagButton.isEnabled = !it
        })

        viewModel.operationStatusObserve.observe(this, Observer {
            if (!it.isNullOrEmpty()) {
                Snackbar.make(toolbarContainer, it, Snackbar.LENGTH_SHORT).show()
                viewModel.statusProcessed()
            }
        })

        viewModel.sensorOwnerObserve.observe(this, Observer {
            ownerValueTextView.text = it ?: "None"
        })
    }

    override fun onResume() {
        super.onResume()
        timer = Timer("TagSettingsActivityTimer", true)
        timer?.scheduleAtFixedRate(0, 1000) {
            viewModel.getTagInfo()
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.saveOrUpdateAlarmItems()
        timer?.cancel()
    }

    @Suppress("NAME_SHADOWING")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == Activity.RESULT_OK) {
            if (viewModel.file != null) {
                val rotation = imageInteractor.getCameraPhotoOrientation(viewModel.file)
                imageInteractor.resize(currentPhotoPath, viewModel.file, rotation)
                viewModel.tagObserve.value?.userBackground = viewModel.file.toString()
                viewModel.updateTagBackground(viewModel.file.toString(), null)
                val backgroundUri = Uri.parse(viewModel.tagObserve.value?.userBackground)
                val background = imageInteractor.getImage(backgroundUri)
                tagImageView.setImageBitmap(background)
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
                        viewModel.tagObserve.value?.userBackground = uri.toString()
                        viewModel.updateTagBackground(uri.toString(), null)
                        val backgroundUri = Uri.parse(viewModel.tagObserve.value?.userBackground)
                        val background = imageInteractor.getImage(backgroundUri)
                        tagImageView.setImageBitmap(background)
                    }
                } catch (e: Exception) {
                    // ... O.o
                    Timber.e("Could not load photo: $e")
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_export) {
            val exporter = CsvExporter(this, repository, unitsConverter)
            viewModel.tagObserve.value?.id?.let {
                exporter.toCsv(it)
            }
        } else {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        viewModel.tagObserve.value?.let {
            if (it.isFavorite) {
                menuInflater.inflate(R.menu.menu_edit, menu)
            }
        }
        return true
    }

    private fun isTagFavorite(tag: RuuviTagEntity) {
        if (!tag.isFavorite) {
            tag.isFavorite = true
            tag.createDate = Date()
            tag.update()
        }
    }

    private fun setupTagName(tag: RuuviTagEntity) {
        tagNameInputTextView.text = tag.displayName

        // TODO: 25/10/17 make this less ugly
        tagNameInputTextView.setDebouncedOnClickListener {
            val builder = AlertDialog.Builder(ContextThemeWrapper(this@TagSettingsActivity, R.style.AppTheme))

            builder.setTitle(getString(R.string.tag_name))

            val input = EditText(this@TagSettingsActivity)

            input.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(32))

            input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

            input.setText(tag.name)

            val container = FrameLayout(applicationContext)

            val params =
                FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

            params.leftMargin = resources.getDimensionPixelSize(R.dimen.dialog_margin)

            params.rightMargin = resources.getDimensionPixelSize(R.dimen.dialog_margin)

            input.layoutParams = params

            container.addView(input)

            builder.setView(container)

            builder.setPositiveButton(getString(R.string.ok)) { _: DialogInterface?, _: Int ->
                val newValue = input.text.toString()
                if (newValue.isNullOrEmpty()) {
                    viewModel.setName(null)
                } else {
                    viewModel.setName(input.text.toString())
                }
            }

            builder.setNegativeButton(getString(R.string.cancel), null)

            val dialog: Dialog = builder.create()

            try {
                dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
            } catch (e: Exception) {
                Timber.e(e, "Could not open keyboard")
            }
            dialog.show()

            input.requestFocus()
        }
    }

    private fun setupInputMac(tag: RuuviTagEntity) {
        inputMacTextView.text = tag.id

        inputMacTextView.setOnLongClickListener {
            val clipboard: ClipboardManager? = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

            val clip = ClipData.newPlainText(getString(R.string.mac_address), tag.id)

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

    private fun setupTagImage(tag: RuuviTagEntity) {
        if (viewModel.tagObserve.value?.userBackground.isNullOrEmpty() == false) {
            val backgroundUri = Uri.parse(viewModel.tagObserve.value?.userBackground)
            val background = imageInteractor.getImage(backgroundUri)
            tagImageView.setImageBitmap(background)
        } else {
            tagImageView.setImageBitmap(BitmapFactory.decodeResource(getResources(), Utils.getDefaultBackground(tag.defaultBackground)))
        }

        tagImageCameraButton.setDebouncedOnClickListener { showImageSourceSheet() }

        tagImageSelectButton.setDebouncedOnClickListener {
            val defaultBackground = if (tag.defaultBackground == 8) 0 else tag.defaultBackground + 1
            viewModel.updateTagBackground(null, defaultBackground)
            tag.defaultBackground = defaultBackground
            tagImageView.setImageDrawable(Utils.getDefaultBackground(defaultBackground, applicationContext))
        }
    }

    private fun calibrateHumidity(tag: RuuviTagEntity) {
        calibrateHumidityButton.isGone = tag.humidity == null
        calibrateHumidityButton.setDebouncedOnClickListener {
            calibrateOld(tag)
        }
    }

    private fun calibrateOld(tag: RuuviTagEntity) {
        val builder = AlertDialog.Builder(ContextThemeWrapper(this@TagSettingsActivity, R.style.AppTheme))

        val content = View.inflate(this, R.layout.dialog_humidity_calibration, null)

        val container = FrameLayout(applicationContext)

        val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        params.leftMargin = resources.getDimensionPixelSize(R.dimen.dialog_margin)

        params.rightMargin = resources.getDimensionPixelSize(R.dimen.dialog_margin)

        content.layoutParams = params

        val infoTextView = content.findViewById<View>(R.id.info) as TextView
        infoTextView?.let {
            it.makeWebLinks(this, Pair(getString(R.string.calibration_humidity_link), getString(R.string.calibration_humidity_link_url)))
        }

        (content.findViewById<View>(R.id.calibration) as TextView).text = this.getString(R.string.calibration_hint, Math.round(tag.humidity ?: 0.0))

        builder.setPositiveButton(getString(R.string.calibrate)) { _, _ ->
            tag.id?.let {
                humidityCalibrationInteractor.calibrate(it)
                viewModel.getTagInfo()
            }
        }
        if (tag.humidityOffset != 0.0) {
            builder.setNegativeButton(getString(R.string.clear)) { _, _ ->
                tag.id?.let {
                    humidityCalibrationInteractor.clear(it)
                    viewModel.getTagInfo()
                }
            }
            (content.findViewById<View>(R.id.timestamp) as TextView).text = this.getString(R.string.calibrated, tag.humidityOffsetDate.toString())
        }

        builder.setNeutralButton(getString(R.string.close), null)

        container.addView(content)

        builder.setView(container)

        builder.create().show()
    }

    private fun setupAlarmItems() {
        alarmTemperature.restoreState(viewModel.alarmElements[0])
        alarmHumidity.restoreState(viewModel.alarmElements[1])
        alarmPressure.restoreState(viewModel.alarmElements[2])
        alarmRssi.restoreState(viewModel.alarmElements[3])
        alarmMovement.restoreState(viewModel.alarmElements[4])
    }

    private fun updateReadings(tag: RuuviTagEntity) {
        if (tag.dataFormat == 3 || tag.dataFormat == 5) {
            rawValuesLayout.isVisible = true
            inputVoltageTextView.text = this.getString(R.string.voltage_reading, tag.voltage.toString(), getString(R.string.voltage_unit))
            xInputTextView.text = getString(R.string.acceleration_reading, tag.accelX)
            yInputTextView.text = getString(R.string.acceleration_reading, tag.accelY)
            zInputTextView.text = getString(R.string.acceleration_reading, tag.accelZ)
            dataFormatTextView.text = tag.dataFormat.toString()
            txPowerTextView.text = getString(R.string.tx_power_reading, tag.txPower)
            movementCounterTextView.text = tag.movementCounter.toString()
            sequenceNumberTextView.text = tag.measurementSequenceNumber.toString()
        } else {
            rawValuesLayout.isVisible = false
        }
    }

    private fun delete() {
        val builder = AlertDialog.Builder(this)

        builder.setTitle(this.getString(R.string.tagsettings_sensor_remove))

        builder.setMessage(this.getString(R.string.tagsettings_sensor_remove_confirm))

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
        val imageFileName = "background_" + viewModel.tagId
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