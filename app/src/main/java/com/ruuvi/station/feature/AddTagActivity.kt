package com.ruuvi.station.feature

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import com.ruuvi.station.R
import com.ruuvi.station.adapters.AddTagAdapter
import com.ruuvi.station.feature.main.MainActivity
import com.ruuvi.station.model.RuuviTag
import com.ruuvi.station.service.ScannerService
import com.ruuvi.station.util.Starter
import com.ruuvi.station.util.Utils

import kotlinx.android.synthetic.main.activity_add_tag.*
import kotlinx.android.synthetic.main.content_add_tag.*
import java.util.*

class AddTagActivity : AppCompatActivity() {
    private var adapter: AddTagAdapter? = null
    private var tags: MutableList<RuuviTag>? = null
    lateinit var starter: Starter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_tag)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        starter = Starter(this)

        tags = ArrayList()
        adapter = AddTagAdapter(this, tags)
        tag_listView.adapter = adapter

        tag_listView.onItemClickListener = AdapterView.OnItemClickListener { adapterView, view, i, l ->
            val tag = tag_listView.getItemAtPosition(i) as RuuviTag
            if (RuuviTag.get(tag.id).favorite) {
                Toast.makeText(this, getString(R.string.tag_already_added), Toast.LENGTH_SHORT)
                        .show()
                return@OnItemClickListener
            }
            tag.defaultBackground = getKindaRandomBackground()
            tag.update()
            ScannerService.logTag(tag, this, true)
            val settingsIntent = Intent(this, TagSettings::class.java)
            settingsIntent.putExtra(TagSettings.TAG_ID, tag.id)
            startActivityForResult(settingsIntent, 1)
        }

        adapter!!.notifyDataSetChanged()

        val handler = Handler()
        handler.post(object : Runnable {
            override fun run() {
                tags?.clear()
                tags?.addAll(RuuviTag.getAll(false))
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.SECOND, -5)
                var i = 0
                while (i < tags!!.size) {
                    if (tags!!.get(i).updateAt.time < calendar.time.time) {
                        tags!!.removeAt(i)
                        i--
                    }
                    i++
                }
                if (tags!!.size > 0) {
                    Utils.sortTagsByRssi(tags)
                    no_tags.visibility = View.INVISIBLE
                } else
                    no_tags.visibility = View.VISIBLE
                if (adapter != null) adapter?.notifyDataSetChanged()
                handler.postDelayed(this, 1000)
            }
        })

        starter.getThingsStarted()
    }

    override fun onResume() {
        super.onResume()
        checkBluetooth()
    }

    fun checkBluetooth(): Boolean {
        if (MainActivity.isBluetoothEnabled()) {
            return true
        }
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivity(enableBtIntent)
        return false
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
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                        starter.requestPermissions()
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

    private fun isBackgroundInUse(tags: List<RuuviTag>, background: Int): Boolean {
        for (tag in tags) {
            if (tag.defaultBackground == background) return true
        }
        return false
    }

    private fun getKindaRandomBackground(): Int {
        val tags = RuuviTag.getAll(true)
        var bg = (Math.random() * 9.0).toInt()
        for (i in 0..99) {
            if (!isBackgroundInUse(tags, bg)) {
                return bg
            }
            bg = (Math.random() * 9.0).toInt()
        }
        return bg
    }
}
