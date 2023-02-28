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
import androidx.core.view.isVisible
import com.google.android.material.snackbar.Snackbar
import com.ruuvi.station.R
import com.ruuvi.station.databinding.ActivityShareSensorBinding
import com.ruuvi.station.databinding.ItemSharedToEmailBinding
import com.ruuvi.station.network.ui.model.ShareOperationType
import com.ruuvi.station.util.extensions.hideKeyboard
import com.ruuvi.station.util.extensions.viewModel
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein

class ShareSensorActivity : AppCompatActivity(R.layout.activity_share_sensor) , KodeinAware {

    override val kodein: Kodein by closestKodein()

    private val viewModel: ShareSensorViewModel by viewModel {
        intent.getStringExtra(TAG_ID)?.let {
            it
        }
    }

    private lateinit var binding: ActivityShareSensorBinding

    val emailsList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityShareSensorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        setupUI()
        setupViewModel()
    }

    private fun setupUI() {
        binding.shareButton.setOnClickListener {
            viewModel.shareTag(binding.friendEmailEditText.text.toString())
            binding.friendEmailEditText.setText("")
            hideKeyboard()
        }
    }

    private fun setupViewModel() {
        val adapter = TestListAdapter(this, emailsList) { email ->
            confirmUnshareSensor(email)
        }

        binding.sensorRecipientsListView.adapter = adapter

        viewModel.emailsObserve.observe(this){
            emailsList.clear()
            emailsList.addAll(it)
            binding.sharedTextView.isVisible = emailsList.isNotEmpty()
            binding.sharedTextView.text = getString(R.string.share_sensor_already_shared, emailsList.size, 10)
            adapter.notifyDataSetChanged()
        }

        viewModel.operationStatusObserve.observe(this) {
            it?.let { status ->
                when (status.type) {
                    ShareOperationType.SHARING_SUCCESS ->
                        Snackbar.make(
                            binding.sensorRecipientsListView,
                            getString(R.string.successfully_shared),
                            Snackbar.LENGTH_SHORT
                        ).show()
                    ShareOperationType.SHARING_ERROR ->
                        if (!it.message.isNullOrEmpty()) {
                            Snackbar.make(
                                binding.sensorRecipientsListView,
                                it.message ?: "",
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }
                }
                viewModel.statusProcessed()
            }
        }

        viewModel.canShareObserve.observe(this) {
            binding.shareSensorDisabledTitleTextView.isVisible = !it
            binding.sharingLayout.isVisible = it
            binding.shareSeparator.isVisible = it
        }
    }

    private fun confirmUnshareSensor(email: String) {
        val simpleAlert = androidx.appcompat.app.AlertDialog.Builder(this, R.style.CustomAlertDialog).create()
        simpleAlert.setTitle(getString(R.string.confirm))
        simpleAlert.setMessage(getString(R.string.share_sensor_unshare_confirm, email))
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
        val binding = if (convertView != null) {
            ItemSharedToEmailBinding.bind(convertView)
        } else {
            ItemSharedToEmailBinding.inflate(LayoutInflater.from(context), parent, false)
        }

        binding.userEmailTextView.text = item
        item?.let {
            binding.unshareButton.setOnClickListener { clickListener(item) }
        }
        return binding.root
    }
}