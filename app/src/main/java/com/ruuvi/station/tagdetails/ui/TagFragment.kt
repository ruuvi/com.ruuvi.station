package com.ruuvi.station.tagdetails.ui

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.SpannableString
import android.text.style.SuperscriptSpan
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.ruuvi.station.R
import com.ruuvi.station.graph.GraphView
import com.ruuvi.station.network.ui.SignInActivity
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tagdetails.domain.TagViewModelArgs
import com.ruuvi.station.util.extensions.describingTimeSince
import com.ruuvi.station.util.extensions.sharedViewModel
import com.ruuvi.station.util.extensions.viewModel
import kotlinx.android.synthetic.main.view_graphs.*
import kotlinx.android.synthetic.main.view_tag_detail.*
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.support.closestKodein
import org.kodein.di.generic.instance
import timber.log.Timber
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate


class TagFragment : Fragment(R.layout.view_tag_detail), KodeinAware {

    override val kodein: Kodein by closestKodein()
    private var timer: Timer? = null

    private val viewModel: TagViewModel by viewModel {
        arguments?.let {
            TagViewModelArgs(it.getString(TAG_ID, ""))
        }
    }

    private val activityViewModel: TagDetailsViewModel by sharedViewModel()

    private val graphView: GraphView by instance()

    init {
        Timber.d("new TagFragment")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeShowGraph()
        observeTagEntry()
        observeTagReadings()
        observeSelectedTag()
        observeSync()

        gattSyncButton.setOnClickListener {
            viewModel.syncGatt()
        }
        clearDataButton.setOnClickListener {
            confirm(getString(R.string.clear_confirm), DialogInterface.OnClickListener { _, _ ->
                viewModel.removeTagData()
            })
        }
        gattSyncCancel.setOnClickListener {
            viewModel.disconnectGatt()
        }
    }

    override fun onResume() {
        super.onResume()
        timer = Timer("TagFragmentTimer", true)
        timer?.scheduleAtFixedRate(0, 1000) {
            viewModel.getTagInfo()
        }
    }

    override fun onPause() {
        super.onPause()
        timer?.cancel()
    }

    private fun observeSelectedTag() {
        activityViewModel.selectedTagObserve.observe(viewLifecycleOwner, Observer {
            viewModel.tagSelected(it)
        })
    }

    private fun confirm(message: String, positiveButtonClick: DialogInterface.OnClickListener) {
        val builder = AlertDialog.Builder(requireContext())
        with(builder)
        {
            setMessage(message)
            setPositiveButton(getString(R.string.yes), positiveButtonClick)
            setNegativeButton(getString(R.string.no), DialogInterface.OnClickListener { dialogInterface, i ->
                dialogInterface.dismiss()
            })
            show()
        }
    }

    private fun gattAlertDialog(message: String) {
        val alertDialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog).create()
        alertDialog.setMessage(message)
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.ok)
        ) { dialog, _ -> dialog.dismiss() }
        alertDialog.setOnDismissListener {
            gattSyncViewButtons.visibility = View.VISIBLE
            gattSyncViewProgress.visibility = View.GONE
        }
        alertDialog.show()
    }

    private fun observeShowGraph() {
        activityViewModel.isShowGraphObserve.observe(viewLifecycleOwner, Observer { isShowGraph ->
            view?.let {
                setupViewVisibility(it, isShowGraph)
                viewModel.isShowGraph(isShowGraph)
            }
        })
    }

    private fun observeTagEntry() {
        viewModel.tagEntryObserve.observe(viewLifecycleOwner, Observer {
            it?.let { updateTagData(it) }
        })
    }

    private fun observeSync() {
        viewModel.syncStatusObserve.observe(viewLifecycleOwner, Observer {
            when (it.syncProgress) {
                TagViewModel.SyncProgress.STILL -> {
                    // nothing is happening
                }
                TagViewModel.SyncProgress.CONNECTING -> {
                    gattSyncStatusTextView.text = "${context?.getString(R.string.connecting)}"
                    gattSyncViewButtons.visibility = View.GONE
                    gattSyncViewProgress.visibility = View.VISIBLE
                    gattSyncCancel.visibility = View.GONE
                }
                TagViewModel.SyncProgress.CONNECTED -> {
                    gattSyncStatusTextView.text = "${context?.getString(R.string.connected_reading_info)}"
                }
                TagViewModel.SyncProgress.DISCONNECTED -> {
                    gattAlertDialog(requireContext().getString(R.string.disconnected))
                }
                TagViewModel.SyncProgress.READING_INFO -> {
                    gattSyncStatusTextView.text = "${context?.getString(R.string.connected_reading_info)}"
                }
                TagViewModel.SyncProgress.NOT_SUPPORTED -> {
                    gattAlertDialog("${it.deviceInfoModel}, ${it.deviceInfoFw}\n${context?.getString(R.string.reading_history_not_supported)}")
                }
                TagViewModel.SyncProgress.READING_DATA -> {
                    gattSyncStatusTextView.text = "${context?.getString(R.string.reading_history)}"+"..."
                    gattSyncCancel.visibility = View.VISIBLE
                }
                TagViewModel.SyncProgress.SAVING_DATA -> {
                    gattSyncStatusTextView.text = if (it.readDataSize > 0) {
                        "${context?.getString(R.string.data_points_read, it.readDataSize)}"
                    } else {
                        "${context?.getString(R.string.no_new_data_points)}"
                    }
                }
                TagViewModel.SyncProgress.NOT_FOUND -> {
                    gattAlertDialog(requireContext().getString(R.string.tag_not_in_range))
                }
                TagViewModel.SyncProgress.ERROR -> {
                    gattAlertDialog(requireContext().getString(R.string.something_went_wrong))
                }
                TagViewModel.SyncProgress.DONE -> {
                    //gattAlertDialog(requireContext().getString(R.string.sync_complete))
                    gattSyncViewButtons.visibility = View.VISIBLE
                    gattSyncViewProgress.visibility = View.GONE
                }
            }
        })
    }

    private fun observeTagReadings() {
        viewModel.tagReadingsObserve.observe(viewLifecycleOwner, Observer { readings ->
            readings?.let {
                view?.let { view ->
                    graphView.drawChart(readings, view)
                }
            }
        })
    }

    private fun updateTagData(tag: RuuviTag) {
        Timber.d("updateTagData for ${tag.id}")
        tagTemperatureTextView.text = viewModel.getTemperatureStringWithoutUnit(tag)
        tagHumidityTextView.text = viewModel.getHumidityString(tag)
        tagPressureTextView.text = viewModel.getPressureString(tag)
        tagSignalTextView.text = viewModel.getSignalString(tag)
        tagUpdatedTextView.text = getString(R.string.updated, tag.updatedAt?.describingTimeSince(requireContext()))

        val unit = viewModel.getTemperatureUnitString()
        val unitSpan = SpannableString(unit)
        unitSpan.setSpan(SuperscriptSpan(), 0, unit.length, 0)
        tagTempUnitTextView.text = unitSpan
        tag.connectable?.let {
            if (it) {
                gattSyncView.visibility = View.VISIBLE
            } else {
                gattSyncView.visibility = View.GONE
            }
        }
    }

    private fun setupViewVisibility(view: View, showGraph: Boolean) {
        val graph = view.findViewById<View>(R.id.tag_graphs)
        graph.isVisible = showGraph
        tagContainer.isVisible = !showGraph
    }

    companion object {
        private const val TAG_ID = "TAG_ID"

        fun newInstance(tagEntity: RuuviTag): TagFragment {
            val tagFragment = TagFragment()
            val arguments = Bundle()
            arguments.putString(TAG_ID, tagEntity.id)
            tagFragment.arguments = arguments
            return tagFragment
        }
    }
}