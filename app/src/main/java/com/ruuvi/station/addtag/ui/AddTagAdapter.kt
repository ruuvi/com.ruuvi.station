package com.ruuvi.station.addtag.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.ruuvi.station.R
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.databinding.RowItemAddBinding
import com.ruuvi.station.util.extensions.diffGreaterThan

class AddTagAdapter(
    context: Context,
    items: List<RuuviTagEntity>
) : ArrayAdapter<RuuviTagEntity>(context, 0, items) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = getItem(position)

        val binding = if (convertView != null) {
            RowItemAddBinding.bind(convertView)
        } else {
            RowItemAddBinding.inflate(LayoutInflater.from(context), parent, false)
        }

        with(binding) {
            address.text = item?.displayName()
            rssi.text = context.getString(
                R.string.signal_reading,
                item?.rssi,
                context.getString(R.string.signal_unit)
            )

            when {
                item?.rssi?.compareTo(LOW_SIGNAL) == -1 -> signalIcon.setImageResource(R.drawable.icon_connection_1)
                item?.rssi?.compareTo(MEDIUM_SIGNAL) == -1 -> signalIcon.setImageResource(R.drawable.icon_connection_2)
                else -> signalIcon.setImageResource(R.drawable.icon_connection_3)
            }

            address.isEnabled = item?.updateAt?.diffGreaterThan(10000) != true

            return root
        }
    }

    companion object {
        private const val LOW_SIGNAL = -80
        private const val MEDIUM_SIGNAL = -50
    }
}