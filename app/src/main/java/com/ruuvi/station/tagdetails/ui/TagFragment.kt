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

        syncButton.setOnClickListener {
            viewModel.syncGatt()
        }
        clearDataButton.setOnClickListener {
            confirm(getString(R.string.clear_confirm), DialogInterface.OnClickListener { _, _ ->
                viewModel.removeTagData()
            })
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

    private var syncDialog: AlertDialog? = null
    var syncDialogText = ""

    private fun createSyncDialog(dismissButtonEnabled: Boolean) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setMessage(syncDialogText)
        builder.setPositiveButton(context?.getText(R.string.ok)) { p0, _ -> p0.dismiss() }
        syncDialog = builder.show()
        syncDialog?.setCanceledOnTouchOutside(false)
        syncDialog?.getButton(Dialog.BUTTON_POSITIVE)?.isEnabled = dismissButtonEnabled
    }

    private fun observeSync() {
        viewModel.syncStatusObserve.observe(viewLifecycleOwner, Observer {
            when (it.syncProgress) {
                TagViewModel.SyncProgress.STILL -> {
                    // nothing is happening
                }
                TagViewModel.SyncProgress.CONNECTING -> {
                    syncDialogText = "${context?.getString(R.string.connecting)}.."
                    createSyncDialog(false)
                }
                TagViewModel.SyncProgress.CONNECTED -> {
                    syncDialogText += "\n${context?.getString(R.string.connected_reading_info)}.."
                }
                TagViewModel.SyncProgress.DISCONNECTED -> {
                    syncDialogText += "\n${context?.getString(R.string.disconnected)}"
                    syncDialog?.getButton(Dialog.BUTTON_POSITIVE)?.isEnabled = true
                }
                TagViewModel.SyncProgress.READING_INFO -> {
                    syncDialogText += "\n${context?.getString(R.string.connected_reading_info)}.."
                }
                TagViewModel.SyncProgress.NOT_SUPPORTED -> {
                    syncDialogText += "\n${it.deviceInfoModel}, ${it.deviceInfoFw}\n${context?.getString(R.string.reading_history_not_supported)}.."
                }
                TagViewModel.SyncProgress.READING_DATA -> {
                    syncDialogText += "\n${it.deviceInfoModel}, ${it.deviceInfoFw}\n${context?.getString(R.string.reading_history)}.."
                }
                TagViewModel.SyncProgress.SAVING_DATA -> {
                    syncDialogText += if (it.readDataSize > 0) {
                        "\n${context?.getString(R.string.data_points_read, it.readDataSize)}"
                    } else {
                        "\n${context?.getString(R.string.no_new_data_points)}"
                    }
                }
                TagViewModel.SyncProgress.NOT_FOUND -> {
                    syncDialogText = "${context?.getString(R.string.tag_not_in_range)}"
                    createSyncDialog(true)
                }
                TagViewModel.SyncProgress.ERROR -> {
                    syncDialogText = context?.getString(R.string.something_went_wrong).toString()
                    syncDialog?.getButton(Dialog.BUTTON_POSITIVE)?.isEnabled = true
                }
                TagViewModel.SyncProgress.DONE -> {
                    syncDialogText += "\n${context?.getString(R.string.sync_complete)}"
                    syncDialog?.getButton(Dialog.BUTTON_POSITIVE)?.isEnabled = true
                }
            }
            syncDialog?.setMessage(syncDialogText)
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
                syncView.visibility = View.VISIBLE
            } else {
                syncView.visibility = View.GONE
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