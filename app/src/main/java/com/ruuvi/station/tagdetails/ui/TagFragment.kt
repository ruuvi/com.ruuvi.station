package com.ruuvi.station.tagdetails.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.SpannableString
import android.text.style.SuperscriptSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ruuvi.station.R
import com.ruuvi.station.database.RuuviTagRepository
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.graph.GraphView
import com.ruuvi.station.util.Utils
import kotlinx.android.synthetic.main.view_tag_detail.*
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.support.closestKodein
import org.kodein.di.generic.instance
import timber.log.Timber

class TagFragment : Fragment() , KodeinAware {
    override val kodein: Kodein by closestKodein()
    private val viewModeFactory: ViewModelProvider.Factory by instance()
    private val viewModel: TagViewModel by lazy {
        ViewModelProviders.of(this, viewModeFactory).get(TagViewModel::class.java)
    }
    private val activityViewModel: TagDetailsViewModel? by lazy {
        activity?.let {
            return@lazy ViewModelProviders.of(it, viewModeFactory).get(TagDetailsViewModel::class.java)
        }
    }

    lateinit var tagId: String
    private val graphView = GraphView()

    init {
        Timber.d("new TagFragment")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            tagId = it.getString(TAG_ID, "")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        var view = inflater.inflate(R.layout.view_tag_detail, container, false)
        val showGraph = activityViewModel?.getCurrentShowGraph() ?: false
        setupViewVisibility(view, showGraph)
        if (showGraph) viewModel.startShowGraph() else viewModel.stopShowGraph()
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        observeShowGraph()
        observeTagEntry()
        observeTagReadings()
        viewModel.getTagInfo(tagId)
    }

    fun observeTagEntry() {
        viewModel.observeTagEntry().observe(this, Observer { tag ->
            tag?.let {
                updateTagData(it)
            }
        })
    }

    fun updateTagData(tag: RuuviTagEntity) {
        Timber.d("updateTagData for ${tag.id}")
        var temperature = RuuviTagRepository.getTemperatureString(context, tag)
        val offset = if (temperature.endsWith("K")) 1 else 2
        val unit = temperature.substring(temperature.length - offset, temperature.length)
        temperature = temperature.substring(0, temperature.length - offset)
        tag_temp.text = temperature

        tag_humidity.text = RuuviTagRepository.getHumidityString(context, tag)
        tag_pressure.text = String.format(getString(R.string.pressure_reading), tag.pressure / 100.0)
        tag_signal.text = String.format(getString(R.string.signal_reading), tag.rssi)
        tag_updated.text = getString(R.string.updated) + " " + Utils.strDescribingTimeSince(tag.updateAt)

        val unitSpan = SpannableString(unit)
        unitSpan.setSpan(SuperscriptSpan(), 0, unit.length, 0)
        tag_temp_unit.text = unitSpan
    }

    fun observeShowGraph() {
        activityViewModel?.observeShowGraph()?.observe(this, Observer { showGraph ->
            view?.let {
                setupViewVisibility(it, showGraph ?: false)
                if (showGraph == true) viewModel.startShowGraph() else viewModel.stopShowGraph()
            }
        })
    }

    private fun setupViewVisibility(view: View, showGraph: Boolean) {
        val graph = view.findViewById<View>(R.id.tag_graphs)
        val container = view.findViewById<View>(R.id.tag_container)
        if (showGraph) {
            if (graph.visibility == View.INVISIBLE) {
                graph.visibility = View.VISIBLE
                container.visibility = View.INVISIBLE
            }
        } else {
            if (graph.visibility == View.VISIBLE) {
                graph.visibility = View.INVISIBLE
                container.visibility = View.VISIBLE
            }
        }
    }

    fun observeTagReadings() {
        viewModel.observeTagReadings().observe(this, Observer { readings ->
            readings?.let {
                context?.let { context ->
                    view?.let { view ->
                        graphView.drawChart(readings, view, context)
                    }
                }
            }
        })
    }

    companion object {
        const val TAG_ID = "TAG_ID"

        fun newInstance(tagEntity: RuuviTagEntity) : TagFragment {
            val tagFragment = TagFragment()
            val args = Bundle()
            args.putString(TAG_ID, tagEntity.id ?: "")
            tagFragment.arguments = args
            return tagFragment
        }
    }
}