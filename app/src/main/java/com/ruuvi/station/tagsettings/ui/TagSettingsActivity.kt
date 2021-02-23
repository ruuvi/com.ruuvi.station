package com.ruuvi.station.tagsettings.ui

import android.app.Activity
import android.app.Dialog
import android.content.*
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.InputFilter
import android.text.InputType
import android.util.DisplayMetrics
import android.view.*
import android.webkit.MimeTypeMap
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.lifecycleScope
import com.crystal.crystalrangeseekbar.widgets.CrystalRangeSeekbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.ruuvi.station.BuildConfig
import com.ruuvi.station.R
import com.ruuvi.station.database.TagRepository
import com.ruuvi.station.database.tables.Alarm
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.tagsettings.di.TagSettingsViewModelArgs
import com.ruuvi.station.tagsettings.domain.HumidityCalibrationInteractor
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.util.CsvExporter
import com.ruuvi.station.util.Utils
import com.ruuvi.station.util.extensions.makeWebLinks
import com.ruuvi.station.util.extensions.setDebouncedOnClickListener
import com.ruuvi.station.util.extensions.viewModel
import kotlinx.android.synthetic.main.activity_tag_settings.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.DateFormat
import java.text.DateFormat.getTimeInstance
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate
import kotlin.math.round

@ExperimentalCoroutinesApi
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
    private var timer: Timer? = null

    private var alarmCheckboxListener = CompoundButton.OnCheckedChangeListener { buttonView: CompoundButton, isChecked: Boolean ->
        val item = viewModel.alarmItems[buttonView.tag as Int]
        item.isChecked = isChecked
        if (!isChecked) item.mutedTill = null
        item.updateView()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_tag_settings)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        lifecycleScope.launchWhenCreated {
            viewModel.tagFlow.collect { tag ->
                tag?.let {
                    isTagFavorite(it)
                    setupTagName(it)
                    setupInputMac(it)
                    setupTagImage(it)
                    calibrateHumidity(it)
                    updateReadings(it)
                }
            }
        }

        viewModel.tagAlarms = Alarm.getForTag(viewModel.tagId)

        setupAlarmItems()

        removeTagButton.setDebouncedOnClickListener { delete() }
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
                val rotation = getCameraPhotoOrientation(viewModel.file)
                resize(viewModel.file, rotation)
                viewModel.tagFlow.value?.userBackground = viewModel.file.toString()
                viewModel.updateTagBackground(viewModel.file.toString(), null)
                val background = Utils.getBackground(applicationContext, viewModel.tagFlow.value)
                tagImageView.setImageBitmap(background)
            }
        } else if (requestCode == REQUEST_GALLERY_PHOTO && resultCode == Activity.RESULT_OK) {
            data?.let {
                try {
                    val path = data.data ?: return
                    if (!isImage(path)) {
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
                        val rotation = getCameraPhotoOrientation(uri)
                        resize(uri, rotation)
                        viewModel.tagFlow.value?.userBackground = uri.toString()
                        viewModel.updateTagBackground(uri.toString(), null)
                        val background = Utils.getBackground(applicationContext, viewModel.tagFlow.value)
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
            viewModel.tagFlow.value?.id?.let {
                exporter.toCsv(it)
            }
        } else {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        viewModel.tagFlow.value?.let {
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
                    tag.name = null
                } else {
                    tag.name = input.text.toString()
                }
                viewModel.updateTagName(tag.name)
                tagNameInputTextView.text = tag.name
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
        tagImageView.setImageBitmap(Utils.getBackground(this, tag))

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
    }

    private fun setupAlarmItems() {
        viewModel.alarmItems.clear()
        with(viewModel.alarmItems) {
            add(AlarmItem(
                getString(R.string.temperature, unitsConverter.getTemperatureUnitString()),
                Alarm.TEMPERATURE,
                false,
                -40,
                85
            ))
            add(AlarmItem(
                getString(R.string.humidity, unitsConverter.getHumidityUnitString()),
                Alarm.HUMIDITY,
                false,
                0,
                100
            ))
            add(AlarmItem(
                getString(R.string.pressure, unitsConverter.getPressureUnitString()),
                Alarm.PRESSURE,
                false,
                30000,
                110000
            ))
            add(AlarmItem(getString(R.string.rssi), Alarm.RSSI, false, -105, 0))
            add(AlarmItem(getString(R.string.alert_movement), Alarm.MOVEMENT, false, 0, 0))
        }

        for (alarm in viewModel.tagAlarms) {
            val item = viewModel.alarmItems[alarm.type]
            item.high = alarm.high
            item.low = alarm.low
            item.isChecked = alarm.enabled
            item.customDescription = alarm.customDescription ?: ""
            item.mutedTill = alarm.mutedTill
            item.alarm = alarm
            item.normalizeValues()
        }

        for (i in viewModel.alarmItems.indices) {
            val item = viewModel.alarmItems[i]

            item.view = layoutInflater.inflate(R.layout.view_alarm, alertsContainerLayout, false)

            item.view?.id = View.generateViewId()

            val switch = item.view?.findViewById<SwitchCompat>(R.id.alertSwitch)

            switch?.tag = i

            switch?.setOnCheckedChangeListener(alarmCheckboxListener)

            item.createView()

            alertsContainerLayout.addView(item.view)
        }
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
            for (alarm: AlarmItem in viewModel.alarmItems) {
                alarm.alarm?.let { viewModel.removeNotificationById(it.id) }
            }
            viewModel.tagFlow.value?.let { viewModel.deleteTag(it) }
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

    @Throws(IOException::class)
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

    private fun isImage(uri: Uri?): Boolean {
        val mime = uri?.let { getMimeType(it) }
        return mime == "jpeg" || mime == "jpg" || mime == "png"
    }

    private fun getMimeType(uri: Uri): String? {
        val contentResolver = applicationContext.contentResolver
        val mime = MimeTypeMap.getSingleton()
        return mime.getExtensionFromMimeType(contentResolver.getType(uri))
    }

    private fun getCameraPhotoOrientation(file: Uri?): Int {
        var rotate = 0
        file?.let {
            try {
                applicationContext.contentResolver.openInputStream(file).use { inputStream ->
                    val exif = ExifInterface(inputStream!!)

                    when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                        ExifInterface.ORIENTATION_ROTATE_270 -> rotate = 270
                        ExifInterface.ORIENTATION_ROTATE_180 -> rotate = 180
                        ExifInterface.ORIENTATION_ROTATE_90 -> rotate = 90
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Could not get orientation of image")
            }
        }

        return rotate
    }

    private fun resize(uri: Uri?, rotation: Int) {
        try {
            val displayMetrics = DisplayMetrics()

            windowManager.defaultDisplay.getMetrics(displayMetrics)

            val targetHeight = 1440

            val targetWidth = 960

            var bitmap = MediaStore.Images.Media.getBitmap(applicationContext.contentResolver, uri)

            bitmap = rotate(bitmap, rotation.toFloat())

            var out: Bitmap

            out = if ((targetHeight.toFloat() / bitmap.height.toFloat() * bitmap.width).toInt() > targetWidth) {
                Bitmap.createScaledBitmap(bitmap, (targetHeight.toFloat() / bitmap.height.toFloat() * bitmap.width).toInt(), targetHeight, false)
            } else {
                Bitmap.createScaledBitmap(bitmap, targetWidth, (targetWidth.toFloat() / bitmap.width.toFloat() * bitmap.height).toInt(), false)
            }

            var x = out.width / 2 - targetWidth / 2

            if (x < 0) x = 0

            out = Bitmap.createBitmap(out, x, 0, targetWidth, targetHeight)

            val file = currentPhotoPath?.let { File(it) }

            val outputStream = FileOutputStream(file)

            out.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)

            outputStream.flush()

            outputStream.close()

            bitmap.recycle()

            out.recycle()
        } catch (e: Exception) {
            Timber.e(e, "Could not resize background image")
        }
    }

    inner class AlarmItem(
        var name: String,
        var type: Int,
        var isChecked: Boolean,
        var min: Int,
        var max: Int,
        var customDescription: String = "",
        var mutedTill: Date? = null,
        val gap: Int = 1
    ) {
        private var subtitle: String? = null
        var low: Int
        var high: Int
        var view: View? = null
        var alarm: Alarm? = null

        fun createView() {
            view?.let { view ->
                val seekBar: CrystalRangeSeekbar = view.findViewById(R.id.alertSeekBar)

                seekBar.setMinValue(min.toFloat())

                seekBar.setMaxValue(max.toFloat())

                seekBar.setMinStartValue(low.toFloat())

                seekBar.setMaxStartValue(high.toFloat())

                seekBar.setGap(gap.toFloat())

                seekBar.apply()

                seekBar.setOnRangeSeekbarChangeListener { minValue: Number, maxValue: Number ->
                    low = minValue.toInt()
                    high = maxValue.toInt()
                    updateView()
                }

                val customDescriptionEditText = view.findViewById(R.id.customDescriptionEditText) as EditText
                customDescriptionEditText.setText(customDescription)
                customDescriptionEditText.addTextChangedListener {
                    customDescription = it.toString()
                }

                if (min == 0 && max == 0) {
                    seekBar.isVisible = false
                    view.findViewById<View>(R.id.alertMinValueTextView).visibility = View.GONE
                    view.findViewById<View>(R.id.alertMaxValueTextView).visibility = View.GONE
                }
            }

            updateView()
        }

        fun updateView() {
            view?.let { view ->
                val seekBar: CrystalRangeSeekbar = view.findViewById(R.id.alertSeekBar)
                val minTextView = (view.findViewById<View>(R.id.alertMinValueTextView) as TextView)
                val maxTextView = (view.findViewById<View>(R.id.alertMaxValueTextView) as TextView)
                val customDescriptionEditView = (view.findViewById<View>(R.id.customDescriptionEditText) as TextView)

                var lowDisplay = low
                var highDisplay = high

                val alertSwitch = view.findViewById<View>(R.id.alertSwitch) as SwitchCompat
                alertSwitch.isChecked = isChecked
                alertSwitch.text = name

                var setSeekbarColor = R.color.inactive
                when (type) {
                    Alarm.TEMPERATURE -> {
                        lowDisplay = round(unitsConverter.getTemperatureValue(low.toDouble())).toInt()
                        highDisplay = round(unitsConverter.getTemperatureValue(high.toDouble())).toInt()
                    }
                    Alarm.PRESSURE -> {
                        lowDisplay = round(unitsConverter.getPressureValue(low.toDouble())).toInt()
                        highDisplay = round(unitsConverter.getPressureValue(high.toDouble())).toInt()
                    }
                }

                if (isChecked) {
                    setSeekbarColor = R.color.main
                    subtitle = getString(R.string.alert_movement_description)
                    subtitle = when (type) {
                        Alarm.MOVEMENT -> getString(R.string.alert_movement_description)
                        else -> String.format(getString(R.string.alert_subtitle_on), lowDisplay, highDisplay)
                    }
                } else {
                    subtitle = getString(R.string.alert_subtitle_off)
                }

                seekBar.isGone = !isChecked || type == Alarm.MOVEMENT
                maxTextView.isGone = !isChecked || type == Alarm.MOVEMENT
                minTextView.isGone = !isChecked || type == Alarm.MOVEMENT
                customDescriptionEditView.isGone = !isChecked

                seekBar.setRightThumbColor(ContextCompat.getColor(this@TagSettingsActivity, setSeekbarColor))

                seekBar.setRightThumbHighlightColor(ContextCompat.getColor(this@TagSettingsActivity, setSeekbarColor))

                seekBar.setLeftThumbColor(ContextCompat.getColor(this@TagSettingsActivity, setSeekbarColor))

                seekBar.setLeftThumbHighlightColor(ContextCompat.getColor(this@TagSettingsActivity, setSeekbarColor))

                seekBar.setBarHighlightColor(ContextCompat.getColor(this@TagSettingsActivity, setSeekbarColor))

                seekBar.isEnabled = isChecked

                val mutedTextView = view.findViewById(R.id.mutedTextView) as TextView
                if (mutedTill ?: Date(0) > Date()) {
                    mutedTextView.text = getTimeInstance(DateFormat.SHORT).format(mutedTill)
                    mutedTextView.isGone = false
                } else {
                    mutedTextView.isGone = true
                }

                (view.findViewById<View>(R.id.alertSubtitleTextView) as TextView).text = subtitle

                minTextView.text = lowDisplay.toString()
                maxTextView.text = highDisplay.toString()
            }
        }

        fun normalizeValues() {
            if (low < min) low = min
            if (low >= max) low = max - gap
            if (high > max) high = max
            if (high < min) high = min + gap
            if (low > high) {
                low = high.also { high = low }
            }
        }

        init {
            low = min
            high = max
        }
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

        fun rotate(bitmap: Bitmap, degrees: Float): Bitmap {
            val matrix = Matrix()
            matrix.postRotate(degrees)
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
    }
}