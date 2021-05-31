package com.ruuvi.station.addtag.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.ruuvi.station.util.extensions.viewModel
import com.ruuvi.station.R
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.tagsettings.ui.TagSettingsActivity
import com.ruuvi.station.util.BackgroundScanModes
import com.ruuvi.station.bluetooth.domain.PermissionsInteractor
import kotlinx.android.synthetic.main.activity_add_tag.toolbar
import kotlinx.android.synthetic.main.content_add_tag.noTagsFoundTextView
import kotlinx.android.synthetic.main.content_add_tag.tagListView
import kotlinx.android.synthetic.main.content_tag_details.*
import kotlinx.coroutines.flow.collect
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate

class AddTagActivity : AppCompatActivity(), KodeinAware {

    override val kodein: Kodein by closestKodein()

    private val viewModel: AddTagActivityViewModel by viewModel()
    private val preferencesRepository: PreferencesRepository by instance()
    private val tags: ArrayList<RuuviTagEntity> = arrayListOf()
    private var adapter: AddTagAdapter? = null
    private lateinit var permissionsInteractor: PermissionsInteractor
    private var timer :Timer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_tag)
        setSupportActionBar(toolbar)
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
        lifecycleScope.launchWhenCreated {
            viewModel.tagsFlow.collect { ruuviTags ->

                tags.clear()
                tags.addAll(ruuviTags)

                noTagsFoundTextView.isVisible = tags.isEmpty()
                adapter?.notifyDataSetChanged()
            }
        }
    }

    private fun setupUI() {
        adapter = AddTagAdapter(this, tags)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        tagListView.adapter = adapter

        tagListView.onItemClickListener = AdapterView.OnItemClickListener { _, _, i, _ ->
            val tag = tagListView.getItemAtPosition(i) as RuuviTagEntity
            if (tag.id?.let { viewModel.getTagById(it)?.favorite } == true) {
                Toast.makeText(this, getString(R.string.sensor_already_added), Toast.LENGTH_SHORT)
                        .show()
                return@OnItemClickListener
            }
            viewModel.makeSensorFavorite(tag)
            TagSettingsActivity.startForResult(this, 1, tag.id)
        }

        adapter?.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()

        timer = Timer("AddTagActivityTimer", true)
        timer?.scheduleAtFixedRate(0, 3000) {
            viewModel.updateTags()
        }
    }

    override fun onPause() {
        super.onPause()
        timer?.cancel()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        return true
    }

    private fun requestPermission() {
        permissionsInteractor.requestPermissions(preferencesRepository.getBackgroundScanMode() == BackgroundScanModes.BACKGROUND)
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, AddTagActivity::class.java)
            context.startActivity(intent)
        }
    }
}