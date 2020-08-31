package com.ruuvi.station.tagdetails.ui

import android.os.Bundle
import android.text.SpannableString
import android.text.style.SuperscriptSpan
import android.view.View
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.flexsentlabs.extensions.sharedViewModel
import com.flexsentlabs.extensions.viewModel
import com.ruuvi.station.R
import com.ruuvi.station.database.TagRepository
import com.ruuvi.station.graph.GraphView
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tagdetails.di.TagViewModelArgs
import com.ruuvi.station.util.Utils
import kotlinx.android.synthetic.main.view_tag_detail.tagContainer
import kotlinx.android.synthetic.main.view_tag_detail.tagHumidityTextView
import kotlinx.android.synthetic.main.view_tag_detail.tagPressureTextView
import kotlinx.android.synthetic.main.view_tag_detail.tagSignalTextView
import kotlinx.android.synthetic.main.view_tag_detail.tagTempUnitTextView
import kotlinx.android.synthetic.main.view_tag_detail.tagTemperatureTextView
import kotlinx.android.synthetic.main.view_tag_detail.tagUpdatedTextView
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.support.closestKodein
import org.kodein.di.generic.instance
import timber.log.Timber

@ExperimentalCoroutinesApi
class TagFragment : Fragment(R.layout.view_tag_detail), KodeinAware {

    override val kodein: Kodein by closestKodein()

    private val viewModel: TagViewModel by viewModel {
        arguments?.let {
            TagViewModelArgs(it.getString(TAG_ID, ""))
        }
    }

    private val activityViewModel: TagDetailsViewModel by sharedViewModel()

    private val repository: TagRepository by instance()

    private val graphView = GraphView()

    init {
        Timber.d("new TagFragment")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeShowGraph()
        observeTagEntry()
        observeTagReadings()
    }

    private fun observeShowGraph() {
        lifecycleScope.launchWhenResumed {
            activityViewModel.isShowGraphFlow.collect { isShowGraph ->
                view?.let {
                    setupViewVisibility(it, isShowGraph)
                    viewModel.isShowGraph(isShowGraph)
                }
            }
        }
    }

    private fun observeTagEntry() {
        lifecycleScope.launchWhenStarted {
            viewModel.tagEntryFlow.collect {
                it?.let { updateTagData(it) }
            }
        }
    }

    private fun observeTagReadings() {
        lifecycleScope.launchWhenResumed {
            viewModel.tagReadingsFlow.collect { readings ->
                readings?.let {
                    view?.let { view ->
                        graphView.drawChart(readings, view, requireContext(), repository)
                    }
                }
            }
        }
    }

    private fun updateTagData(tag: RuuviTag) {
        Timber.d("updateTagData for ${tag.id}")
        var temperature = viewModel.getTemperatureString(tag)
        val offset = if (temperature.endsWith("K")) 1 else 2
        val unit = temperature.substring(temperature.length - offset, temperature.length)
        temperature = temperature.substring(0, temperature.length - offset)
        tagTemperatureTextView.text = temperature

        tagHumidityTextView.text = viewModel.getHumidityString(tag)
        tagPressureTextView.text = getString(R.string.pressure_reading, tag.pressure / 100.0)
        tagSignalTextView.text = getString(R.string.signal_reading, tag.rssi)
        tagUpdatedTextView.text = getString(R.string.updated, Utils.strDescribingTimeSince(tag.updatedAt))

        val unitSpan = SpannableString(unit)
        unitSpan.setSpan(SuperscriptSpan(), 0, unit.length, 0)
        tagTempUnitTextView.text = unitSpan
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