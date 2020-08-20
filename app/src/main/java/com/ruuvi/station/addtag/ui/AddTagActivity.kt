package com.ruuvi.station.addtag.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import com.google.android.material.snackbar.Snackbar
import androidx.core.app.ActivityCompat
import androidx.appcompat.app.AppCompatActivity
import android.view.MenuItem
import android.widget.AdapterView
import android.widget.Toast
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.flexsentlabs.extensions.viewModel
import com.ruuvi.station.R
import com.ruuvi.station.adapters.AddTagAdapter
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.tagsettings.ui.TagSettingsActivity
import com.ruuvi.station.util.PermissionsHelper
import com.ruuvi.station.util.Utils
import kotlinx.android.synthetic.main.activity_add_tag.toolbar
import kotlinx.android.synthetic.main.content_add_tag.noTagsFoundTextView
import kotlinx.android.synthetic.main.content_add_tag.tagListView
import kotlinx.android.synthetic.main.content_add_tag.tag_layout
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import java.util.ArrayList
import java.util.Calendar

@ExperimentalCoroutinesApi
class AddTagActivity : AppCompatActivity(), KodeinAware {

    override val kodein: Kodein by closestKodein()

    private val viewModel: AddTagActivityViewModel by viewModel()

    private lateinit var adapter: AddTagAdapter
    private var tags: MutableList<RuuviTagEntity>? = null
    private lateinit var permissionsHelper: PermissionsHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_tag)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        permissionsHelper = PermissionsHelper(this)

        tags = ArrayList()
        adapter = AddTagAdapter(this, tags)
        tagListView.adapter = adapter

        tagListView.onItemClickListener = AdapterView.OnItemClickListener { _, _, i, _ ->
            val tag = tagListView.getItemAtPosition(i) as RuuviTagEntity
            if (tag.id?.let { viewModel.getTagById(it)?.isFavorite } == true) {
                Toast.makeText(this, getString(R.string.tag_already_added), Toast.LENGTH_SHORT)
                    .show()
                return@OnItemClickListener
            }
            tag.defaultBackground = getKindaRandomBackground()
            tag.update()
            val settingsIntent = Intent(this, TagSettingsActivity::class.java)
            settingsIntent.putExtra(TagSettingsActivity.TAG_ID, tag.id)
            startActivityForResult(settingsIntent, 1)
        }

        adapter.notifyDataSetChanged()

        lifecycleScope.launchWhenCreated {
            viewModel.tagsFlow.collect {
                tags?.clear()
                it?.let {
                    tags?.addAll(it)
                }
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.SECOND, -5)
                var i = 0
                tags?.let { tags ->
                    while (i < tags.size) {
                        tags[i].updateAt?.time?.let { time ->
                            if (time < calendar.time.time) {
                                tags.removeAt(i)
                                i--
                            }
                        }
                        i++
                    }
                    if (tags.size > 0) {
                        Utils.sortTagsByRssi(tags)
                        noTagsFoundTextView.isInvisible = true
                    } else
                        noTagsFoundTextView.isVisible = true

                    adapter.notifyDataSetChanged()

                }
            }
        }

        //FIXME delete as repeated call?
        permissionsHelper.requestPermissions()
    }

    override fun onResume() {
        super.onResume()
        permissionsHelper.requestBluetoothPermissions()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            finish()
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            10 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // party
                } else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                        permissionsHelper.requestPermissions()
                    } else {
                        showPermissionSnackbar(this)
                    }
                    Toast.makeText(applicationContext, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showPermissionSnackbar(activity: Activity) {
        val snackbar = Snackbar.make(tag_layout, getString(R.string.location_permission_needed), Snackbar.LENGTH_LONG)
        snackbar.setAction(getString(R.string.settings)) {
            val intent = Intent()
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            val uri = Uri.fromParts("package", activity.packageName, null)
            intent.data = uri
            activity.startActivity(intent)
        }
        snackbar.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        finish()
    }

    private fun isBackgroundInUse(tags: List<RuuviTagEntity>, background: Int): Boolean {
        for (tag in tags) {
            if (tag.defaultBackground == background) return true
        }
        return false
    }

    private fun getKindaRandomBackground(): Int {
        val tags = viewModel.getAllTags(true)
        var background = (Math.random() * 9.0).toInt()
        for (i in 0..99) {
            if (!isBackgroundInUse(tags, background)) {
                return background
            }
            background = (Math.random() * 9.0).toInt()
        }
        return background
    }
}
