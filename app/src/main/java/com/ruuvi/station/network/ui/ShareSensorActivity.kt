package com.ruuvi.station.network.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import com.ruuvi.station.R
import com.ruuvi.station.util.extensions.hideKeyboard
import com.ruuvi.station.util.extensions.viewModel
import kotlinx.android.synthetic.main.activity_share_sensor.*
import kotlinx.android.synthetic.main.item_shared_to_email.view.*
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein

class ShareSensorActivity : AppCompatActivity() , KodeinAware {

    override val kodein: Kodein by closestKodein()

    private val viewModel: ShareSensorViewModel by viewModel {
        intent.getStringExtra(TAG_ID)?.let {
            it
        }
    }

    val emailsList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share_sensor)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        setupUI()
        setupViewModel()
    }

    private fun setupUI() {
        shareButton.setOnClickListener {
            viewModel.shareTag(friendEmailEditText.text.toString())
            friendEmailEditText.setText("")
            hideKeyboard()
        }
    }

    private fun setupViewModel() {
        val adapter = TestListAdapter(this, emailsList) { email ->
            confirmUnshareSensor(email)
        }

        sensorRecipientsListView.adapter = adapter

        viewModel.emailsObserve.observe(this, Observer {
            emailsList.clear()
            emailsList.addAll(it)
            adapter.notifyDataSetChanged()
        })

        viewModel.operationStatusObserve.observe(this, Observer {
            if (!it.isNullOrEmpty()) {
                Snackbar.make(sensorRecipientsListView, it, Snackbar.LENGTH_SHORT).show()
                viewModel.statusProcessed()
            }
        })
    }

    private fun confirmUnshareSensor(email: String) {
        val simpleAlert = androidx.appcompat.app.AlertDialog.Builder(this).create()
        simpleAlert.setTitle("Unshare sensor for $email?")
        simpleAlert.setButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE, resources.getText(R.string.yes)) { _, _ ->
            viewModel.unshareTag(email)
        }
        simpleAlert.setButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE, resources.getText(R.string.no)) { _, _ -> }
        simpleAlert.show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        finish()
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val TAG_ID = "TAG_ID"

        fun start(context: Context, tagId: String?) {
            val intent = Intent(context, ShareSensorActivity::class.java)
            intent.putExtra(TAG_ID, tagId)
            context.startActivity(intent)
        }
    }
}

class TestListAdapter (
    context: Context,
    items: List<String>,
    val clickListener: (recipientEmail: String) -> Unit
): ArrayAdapter<String>(context, 0, items) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = getItem(position)
        val view =
            convertView
                ?: LayoutInflater.from(context).inflate(R.layout.item_shared_to_email, parent, false)

        view.userEmailTextView.text = item
        item?.let {
            view.unshareButton.setOnClickListener { clickListener(item) }
        }
        return view
    }
}