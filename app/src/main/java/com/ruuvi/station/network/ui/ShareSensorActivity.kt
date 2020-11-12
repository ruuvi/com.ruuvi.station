package com.ruuvi.station.network.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.ruuvi.station.R
import com.ruuvi.station.util.extensions.hideKeyboard
import com.ruuvi.station.util.extensions.viewModel
import kotlinx.android.synthetic.main.activity_share_sensor.*
import kotlinx.android.synthetic.main.item_shared_to_email.view.*
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import timber.log.Timber

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

        sharingSwitch.setOnCheckedChangeListener{ _, isChecked ->
            Timber.d("setOnCheckedChangeListener $isChecked")
            viewModel.sharingEnabled(isChecked)
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

        viewModel.sharingEnabledObserve.observe(this, Observer {
            sharingLayout.isVisible = it
            sharingSwitch.isChecked = it
        })

        viewModel.unshareAllConfirmObserve.observe(this, Observer {
            if (it) {
                viewModel.unshareAllConfirmDismiss()
                confirmUnshareAll()
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

    private fun confirmUnshareAll() {
        val simpleAlert = androidx.appcompat.app.AlertDialog.Builder(this).create()
        simpleAlert.setTitle("Unshare for all?")
        simpleAlert.setButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE, resources.getText(R.string.yes)) { _, _ ->
            viewModel.unshareAll()
        }
        simpleAlert.setButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE, resources.getText(R.string.no)) { _, _ ->
            sharingSwitch.isChecked = true
        }
        simpleAlert.setOnDismissListener {
            sharingSwitch.isChecked = true
        }
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

class SharedSensorRecipientsAdapter(private val clickListener: SharedSensorRecipientsListener): ListAdapter<String, SharedSensorRecipientsAdapter.SharedSensorRecipientsViewHolder>(SharedSensorRecipientsDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SharedSensorRecipientsViewHolder {
        return SharedSensorRecipientsViewHolder.inflate(parent)
    }

    override fun onBindViewHolder(holder: SharedSensorRecipientsViewHolder, position: Int) {
        holder.bind(clickListener, getItem(position))
    }

    class SharedSensorRecipientsViewHolder private constructor(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val userEmailTextView: TextView = itemView.findViewById(R.id.userEmailTextView)
        private val unshareButton:ImageView = itemView.findViewById(R.id.unshareButton)

        fun bind(clickListener: SharedSensorRecipientsListener, item: String) {
            userEmailTextView.text = item


            unshareButton.setOnClickListener {
                clickListener.onClick(item)
            }
        }

        companion object {
            fun inflate(parent: ViewGroup): SharedSensorRecipientsViewHolder {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_shared_to_email, parent, false)
                return SharedSensorRecipientsViewHolder(view)
            }
        }
    }

    class SharedSensorRecipientsDiffCallback :
        DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return false
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return false
        }
    }

    class SharedSensorRecipientsListener(val clickListener: (recipientEmail: String) -> Unit) {
        fun onClick(recipientEmail: String) = clickListener(recipientEmail)
    }
}