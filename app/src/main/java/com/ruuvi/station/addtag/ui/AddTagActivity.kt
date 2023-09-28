package com.ruuvi.station.addtag.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import android.widget.AdapterView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.ruuvi.station.util.extensions.viewModel
import com.ruuvi.station.R
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.tagsettings.ui.TagSettingsActivity
import com.ruuvi.station.util.BackgroundScanModes
import com.ruuvi.station.app.permissions.PermissionsInteractor
import com.ruuvi.station.databinding.ActivityAddTagBinding
import com.ruuvi.station.nfc.NfcScanReciever
import com.ruuvi.station.tagdetails.ui.SensorCardActivity
import com.ruuvi.station.util.base.NfcActivity
import com.ruuvi.station.util.extensions.openUrl
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import timber.log.Timber
import java.util.*

class AddTagActivity : NfcActivity(R.layout.activity_add_tag), KodeinAware {

    override val kodein: Kodein by closestKodein()

    private val viewModel: AddTagActivityViewModel by viewModel()
    private val preferencesRepository: PreferencesRepository by instance()
    private val tags: ArrayList<RuuviTagEntity> = arrayListOf()
    private val adapter: AddTagAdapter by lazy { AddTagAdapter(this, tags) }
    private lateinit var permissionsInteractor: PermissionsInteractor
    lateinit var binding: ActivityAddTagBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTagBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        permissionsInteractor = PermissionsInteractor(this)

        requestPermission()
        setupViewmodel()
        setupUI()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PermissionsInteractor.REQUEST_CODE_BLUETOOTH || requestCode == PermissionsInteractor.REQUEST_CODE_LOCATION) {
            requestPermission()
        } else {
            finish()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PermissionsInteractor.REQUEST_CODE_PERMISSIONS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestPermission()
                } else {
                    permissionsInteractor.showPermissionSnackbar()
                }
            }
        }
    }

    private fun setupViewmodel() {
        lifecycleScope.launchWhenStarted {
            viewModel.sensorFlow.collect { ruuviTags ->
                tags.clear()
                tags.addAll(ruuviTags)
                binding.content.noSensorsLayout.isVisible = tags.isEmpty()
                binding.content.buySensorsButton2.isVisible = tags.isNotEmpty()
                adapter.notifyDataSetChanged()
            }
        }
        lifecycleScope.launchWhenStarted {
            NfcScanReciever.nfcSensorScanned.collect{
                    scanInfo ->
                Timber.d("nfc scanned: $scanInfo")
                if (scanInfo != null) {
                    val response = viewModel.getNfcScanResponse(scanInfo)
                    Timber.d("nfc scanned response: $response")
                    if (response.existingSensor) {
                        SensorCardActivity.startWithDashboard(this@AddTagActivity, response.sensorId)
                    } else {
                        viewModel.addSensor(response.sensorId)
                        TagSettingsActivity.startAfterAddingNewSensor(this@AddTagActivity, response.sensorId)
                    }
                }
            }
        }
    }

    private fun setupUI() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.content.tagListView.adapter = adapter

        binding.content.tagListView.onItemClickListener = AdapterView.OnItemClickListener { _, _, i, _ ->
            val tag = binding.content.tagListView.getItemAtPosition(i) as RuuviTagEntity
            if (tag.id?.let { viewModel.getTagById(it)?.favorite } == true) {
                Toast.makeText(this, getString(R.string.sensor_already_added), Toast.LENGTH_SHORT)
                        .show()
                return@OnItemClickListener
            }
            viewModel.makeSensorFavorite(tag)
            TagSettingsActivity.startAfterAddingNewSensor(this, tag.id)
        }

        binding.content.buySensorsButton.setOnClickListener {
            openUrl(getString(R.string.buy_sensors_link))
        }

        binding.content.buySensorsButton2.setOnClickListener {
            openUrl(getString(R.string.buy_sensors_link))
        }

        binding.content.nfcHintTextView.isVisible = viewModel.nfcSupported
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        return true
    }

    private fun requestPermission() {
        permissionsInteractor.requestPermissions(
            needBackground = preferencesRepository.getBackgroundScanMode() == BackgroundScanModes.BACKGROUND,
            askForBluetooth = true
        )
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, AddTagActivity::class.java)
            context.startActivity(intent)
        }
    }
}