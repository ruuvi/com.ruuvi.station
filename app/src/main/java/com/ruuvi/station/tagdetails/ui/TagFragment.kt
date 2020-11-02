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
import com.ruuvi.station.util.extensions.sharedViewModel
import com.ruuvi.station.util.extensions.viewModel
import com.ruuvi.station.R
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


        syncButton.setOnClickListener {
            confirm(getString(R.string.sync_confirm), DialogInterface.OnClickListener { _, _ ->
                context?.let {
                    viewModel.syncGatt(it)
                }
            })
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