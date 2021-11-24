package com.ruuvi.station.tagdetails.ui

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
import com.ruuvi.station.bluetooth.model.SyncProgress
import com.ruuvi.station.databinding.ViewTagDetailBinding
import com.ruuvi.station.graph.GraphView
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tagdetails.domain.TagViewModelArgs
import com.ruuvi.station.util.extensions.describingTimeSince
import com.ruuvi.station.util.extensions.sharedViewModel
import com.ruuvi.station.util.extensions.viewModel
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
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

    private lateinit var binding: ViewTagDetailBinding

    private val graphView: GraphView by instance()

    init {
        Timber.d("new TagFragment")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = ViewTagDetailBinding.bind(view)
        observeShowGraph()
        observeTagEntry()
        observeTagReadings()
        observeSelectedTag()
        observeSync()
        observeSyncStatus()

        with(binding.graphsContent) {
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
    }

    private fun observeSyncStatus() {
        var i = 0
        viewModel.syncStatus.observe(viewLifecycleOwner, Observer {
            binding.tagSynchronizingTextView.isVisible = it
            if (it) {
                var syncText = getText(R.string.synchronizing).toString()
                for (j in 1 .. i) {
                    syncText = syncText + "."
                }
                i++
                if (i > 3) i = 0
                binding.tagSynchronizingTextView.text = syncText
            }
        })
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
            binding.graphsContent.gattSyncViewButtons?.visibility = View.VISIBLE
            binding.graphsContent.gattSyncViewProgress?.visibility = View.GONE
        }
        viewModel.resetGattStatus()
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
            with(binding.graphsContent) {
                when (it.syncProgress) {
                    SyncProgress.STILL -> {
                        // nothing is happening
                    }
                    SyncProgress.CONNECTING -> {
                        gattSyncStatusTextView.text = "${context?.getString(R.string.connecting)}"
                    }
                    SyncProgress.CONNECTED -> {
                        gattSyncStatusTextView.text =
                            "${context?.getString(R.string.connected_reading_info)}"
                    }
                    SyncProgress.DISCONNECTED -> {
                        gattAlertDialog(requireContext().getString(R.string.disconnected))
                    }
                    SyncProgress.READING_INFO -> {
                        gattSyncStatusTextView.text =
                            "${context?.getString(R.string.connected_reading_info)}"
                    }
                    SyncProgress.NOT_SUPPORTED -> {
                        gattAlertDialog(
                            "${it.deviceInfoModel}, ${it.deviceInfoFw}\n${
                                context?.getString(
                                    R.string.reading_history_not_supported
                                )
                            }"
                        )
                    }
                    SyncProgress.READING_DATA -> {
                        var status = "${context?.getString(R.string.reading_history)}.. "
                        if (it.syncedDataPoints > 0) status += it.syncedDataPoints
                        gattSyncStatusTextView.text = status
                    }
                    SyncProgress.SAVING_DATA -> {
                        gattSyncStatusTextView.text =
                            if (it.readDataSize > 0) {
                                "${context?.getString(R.string.data_points_read, it.readDataSize)}"
                            } else {
                                "${context?.getString(R.string.no_new_data_points)}"
                            }
                    }
                    SyncProgress.NOT_FOUND -> {
                        gattAlertDialog(requireContext().getString(R.string.tag_not_in_range))
                    }
                    SyncProgress.ERROR -> {
                        gattAlertDialog(requireContext().getString(R.string.something_went_wrong))
                    }
                    SyncProgress.DONE -> {
                        //gattAlertDialog(requireContext().getString(R.string.sync_complete))
                    }
                }

                gattSyncViewButtons.isVisible =
                    it.syncProgress == SyncProgress.STILL || it.syncProgress == SyncProgress.DONE
                gattSyncViewProgress.isVisible = !gattSyncViewButtons.isVisible
                gattSyncCancel.isVisible = it.syncProgress == SyncProgress.READING_DATA
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
        with(binding) {
            Timber.d("updateTagData for ${tag.id}")
            tagTemperatureTextView.text = viewModel.getTemperatureStringWithoutUnit(tag)
            tagHumidityTextView.text = tag.humidityString
            tagPressureTextView.text = tag.pressureString
            tagMovementTextView.text = tag.movementCounter.toString()
            tagUpdatedTextView.text = tag.updatedAt?.describingTimeSince(requireContext())

            val unit = viewModel.getTemperatureUnitString()
            val unitSpan = SpannableString(unit)
            unitSpan.setSpan(SuperscriptSpan(), 0, unit.length, 0)
            tagTempUnitTextView.text = unitSpan
            tag.connectable?.let {
                if (it) {
                    graphsContent.gattSyncView.visibility = View.VISIBLE
                } else {
                    graphsContent.gattSyncView.visibility = View.GONE
                }
            }

            if (tag.updatedAt == tag.networkLastSync) {
                sourceTypeImageView.setImageResource(R.drawable.ic_icon_gateway)
            } else {
                sourceTypeImageView.setImageResource(R.drawable.ic_icon_bluetooth)
            }
        }
    }

    private fun setupViewVisibility(view: View, showGraph: Boolean) {
        val graph = view.findViewById<View>(R.id.graphsContent)
        graph.isVisible = showGraph
        binding.tagContainer.isVisible = !showGraph
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