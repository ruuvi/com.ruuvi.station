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
import com.flexsentlabs.extensions.sharedViewModel
import com.flexsentlabs.extensions.viewModel
import com.ruuvi.station.R
import com.ruuvi.station.app.RuuviScannerApplication
import com.ruuvi.station.bluetooth.IRuuviGattListener
import com.ruuvi.station.bluetooth.LogReading
import com.ruuvi.station.graph.GraphView
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tagdetails.domain.TagViewModelArgs
import com.ruuvi.station.util.Utils
import kotlinx.android.synthetic.main.view_graphs.*
import kotlinx.android.synthetic.main.view_tag_detail.tagContainer
import kotlinx.android.synthetic.main.view_tag_detail.tagHumidityTextView
import kotlinx.android.synthetic.main.view_tag_detail.tagPressureTextView
import kotlinx.android.synthetic.main.view_tag_detail.tagSignalTextView
import kotlinx.android.synthetic.main.view_tag_detail.tagTempUnitTextView
import kotlinx.android.synthetic.main.view_tag_detail.tagTemperatureTextView
import kotlinx.android.synthetic.main.view_tag_detail.tagUpdatedTextView
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.support.closestKodein
import org.kodein.di.generic.instance
import timber.log.Timber

class TagFragment : Fragment(R.layout.view_tag_detail), KodeinAware {

    override val kodein: Kodein by closestKodein()

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

        syncButton.setOnClickListener {
            confirm(getString(R.string.sync_confirm), DialogInterface.OnClickListener { _, _ ->
                sync()
            })
        }
        clearDataButton.setOnClickListener {
            confirm(getString(R.string.clear_confirm), DialogInterface.OnClickListener { _, _ ->
                viewModel.removeTagData()
            })
        }
    }

    private fun confirm(message: String, positiveButtonClick: DialogInterface.OnClickListener){
        val builder = AlertDialog.Builder(requireContext())
        with(builder)
        {
            setMessage(message)
            setPositiveButton(getString(R.string.yes), positiveButtonClick)
            setNegativeButton(android.R.string.no, DialogInterface.OnClickListener { dialogInterface, i ->
                dialogInterface.dismiss()
            })
            show()
        }
    }

    private fun sync() {
        val builder = AlertDialog.Builder(requireContext())
        var text = "${getString(R.string.connecting)}.."
        builder.setMessage(text)
        builder.setPositiveButton(getText(R.string.ok)) { p0, _ -> p0.dismiss() }
        val ad = builder.show()
        ad.setCanceledOnTouchOutside(false)
        ad.getButton(Dialog.BUTTON_POSITIVE).isEnabled = false
        var completed = false
        viewModel.tagEntryObserve.value?.let { tag ->
            Timber.d("sync logs from: " +tag.lastSync)
            val found = (activity?.applicationContext as RuuviScannerApplication).readLogs(tag.id, tag.lastSync, object : IRuuviGattListener {
                override fun connected(state: Boolean) {
                    if (state) {
                        activity?.runOnUiThread {
                            text += "\n${getString(R.string.connected_reading_info)}.."
                            ad.setMessage(text)
                        }
                    } else {
                        if (completed) {
                            if (!text.contains(getString(R.string.sync_complete))) {
                                text += "\n${getString(R.string.sync_complete)}"
                            }
                        } else {
                            text += "\n${getString(R.string.disconnected)}"
                        }
                        activity?.runOnUiThread {
                            ad.getButton(Dialog.BUTTON_POSITIVE).isEnabled = true
                            ad.setMessage(text)
                        }
                    }
                }

                override fun deviceInfo(model: String, fw: String, canReadLogs: Boolean) {
                    activity?.runOnUiThread {
                        text += if (canReadLogs) {
                            "\n$model, $fw\n${getString(R.string.reading_history)}.."
                        } else {
                            "\n$model, $fw\n${getString(R.string.reading_history_not_supported)}"
                        }
                        ad.setMessage(text)
                    }
                }

                override fun dataReady(data: List<LogReading>) {
                    activity?.runOnUiThread {
                        text += if (data.isNotEmpty()) {
                            "\n${getString(R.string.data_points_read, data.size*3)}"
                        } else {
                            "\n${getString(R.string.no_new_data_points)}"
                        }
                        ad.setMessage(text)
                    }
                    viewModel.saveGattReadings(tag, data)
                    completed = true
                }

                override fun heartbeat(raw: String) {
                }
            })
            if (!found) {
                activity?.runOnUiThread {
                    ad.setMessage(getString(R.string.tag_not_in_range))
                    ad.getButton(Dialog.BUTTON_POSITIVE).isEnabled = true
                }
            }
        } ?: kotlin.run {
            activity?.runOnUiThread {
                ad.setMessage(getString(R.string.something_went_wrong))
                ad.getButton(Dialog.BUTTON_POSITIVE).isEnabled = true
            }
        }
    }

    private fun observeSelectedTag() {
        activityViewModel.selectedTagObserve.observe(viewLifecycleOwner, Observer {
            viewModel.tagSelected(it)
        })
    }


    private fun observeShowGraph() {
        activityViewModel.isShowGraphObserve.observe(viewLifecycleOwner, Observer {isShowGraph ->
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
        tagUpdatedTextView.text = getString(R.string.updated, Utils.strDescribingTimeSince(tag.updatedAt))

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