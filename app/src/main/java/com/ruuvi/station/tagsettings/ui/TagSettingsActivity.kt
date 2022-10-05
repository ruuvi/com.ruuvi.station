package com.ruuvi.station.tagsettings.ui

import android.app.Activity
import android.content.*
import android.graphics.BitmapFactory
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.TypedValue
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.FileProvider
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.ruuvi.station.R
import com.ruuvi.station.alarm.ui.AlarmItemsViewModel
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.database.domain.SensorHistoryRepository
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.database.tables.SensorSettings
import com.ruuvi.station.databinding.ActivityTagSettingsBinding
import com.ruuvi.station.image.ImageInteractor
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tagsettings.di.TagSettingsViewModelArgs
import com.ruuvi.station.tagsettings.domain.CsvExporter
import com.ruuvi.station.units.domain.AccelerationConverter
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.util.Utils
import com.ruuvi.station.util.extensions.resolveColorAttr
import com.ruuvi.station.util.extensions.setDebouncedOnClickListener
import com.ruuvi.station.util.extensions.viewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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

    private val alarmsViewModel: AlarmItemsViewModel by viewModel {
        intent.getStringExtra(TAG_ID)
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
        binding.alertsCompose.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { 
                RuuviTheme {
                    SensorSettings(viewModel, alarmsViewModel)
                }
            }
        }
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        setupViewModel()

        scrollToAlarms()
    }

    private fun scrollToAlarms() {
        val scrollToAlarms = intent.getBooleanExtra(SCROLL_TO_ALARMS, false)
        Timber.d("SCROLL_TO_ALARMS = $scrollToAlarms")
        if (scrollToAlarms) {
            Handler(Looper.getMainLooper()).post {
                binding.scrollView.scrollTo(0, binding.alertsCompose.top-binding.toolbar.height)
            }
        }
    }

    private fun setupViewModel() {
        CoroutineScope(Dispatchers.Main).launch {
            viewModel.sensorState.collectLatest { sensorState ->
                setupSensorImage(sensorState)
            }
        }


        viewModel.operationStatusObserve.observe(this) {
            if (!it.isNullOrEmpty()) {
                Snackbar.make(binding.toolbarContainer, it, Snackbar.LENGTH_SHORT).show()
                viewModel.statusProcessed()
            }
        }
    }

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
            exporter.toCsv(viewModel.sensorId)
        } else {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_edit, menu)
        return true
    }

    private fun setupSensorImage(sensorState: RuuviTag) {
        if (sensorState.userBackground.isNullOrEmpty() == false) {
            val backgroundUri = Uri.parse(sensorState.userBackground)
            val background = imageInteractor.getImage(backgroundUri)
            binding.tagImageView.setImageBitmap(background)
        } else {
            binding.tagImageView.setImageBitmap(BitmapFactory.decodeResource(getResources(), Utils.getDefaultBackground(sensorState.defaultBackground)))
        }

        binding.tagImageCameraButton.setDebouncedOnClickListener { showImageSourceSheet() }

        binding.tagImageSelectButton.setDebouncedOnClickListener {
            val defaultBackground = if (sensorState.defaultBackground == 8) 0 else sensorState.defaultBackground + 1
            viewModel.updateTagBackground(null, defaultBackground)
            binding.tagImageView.setImageDrawable(Utils.getDefaultBackground(defaultBackground, applicationContext))
        }
    }

    private fun showImageSourceSheet() {
        val sheetDialog = BottomSheetDialog(this)
        val listView = ListView(this, null, R.style.AppTheme)
        val menu = arrayOf(
            resources.getString(R.string.camera),
            resources.getString(R.string.gallery)
        )
        val dividerColor = resolveColorAttr(R.attr.colorDivider)
        listView.divider = ColorDrawable(dividerColor).mutate()
        listView.dividerHeight = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            1f,
            resources.displayMetrics
        ).toInt()

        listView.adapter = ArrayAdapter(this, R.layout.bottom_sheet_select_image_source, menu)
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
        private const val SCROLL_TO_ALARMS = "SCROLL_TO_ALARMS"

        private const val REQUEST_TAKE_PHOTO = 1

        private const val REQUEST_GALLERY_PHOTO = 2

        fun start(context: Context, tagId: String?, scrollToAlarms: Boolean = false) {
            val intent = Intent(context, TagSettingsActivity::class.java)
            intent.putExtra(TAG_ID, tagId)
            intent.putExtra(SCROLL_TO_ALARMS, scrollToAlarms)
            context.startActivity(intent)
        }

        fun startForResult(context: Activity, requestCode: Int, tagId: String?) {
            val settingsIntent = Intent(context, TagSettingsActivity::class.java)
            settingsIntent.putExtra(TAG_ID, tagId)
            context.startActivityForResult(settingsIntent, requestCode)
        }
    }
}