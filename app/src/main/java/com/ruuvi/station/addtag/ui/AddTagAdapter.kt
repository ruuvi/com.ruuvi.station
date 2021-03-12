package com.ruuvi.station.addtag.ui

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.ruuvi.station.R
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.util.extensions.diffGreaterThan
import kotlinx.android.synthetic.main.row_item_add.view.address
import kotlinx.android.synthetic.main.row_item_add.view.rssi
import kotlinx.android.synthetic.main.row_item_add.view.signalIcon

class AddTagAdapter(
    context: Context,
    items: List<RuuviTagEntity>
) : ArrayAdapter<RuuviTagEntity>(context, 0, items) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = getItem(position)
        val view =
            convertView ?: LayoutInflater.from(context).inflate(R.layout.row_item_add, parent, false)
        view.address.text = item?.id.orEmpty()
        view.rssi.text = context.getString(R.string.signal_reading, item?.rssi, context.getString(R.string.signal_unit))

        when {
            item?.rssi?.compareTo(LOW_SIGNAL) == -1 -> view.signalIcon.setImageResource(R.drawable.icon_connection_1)
            item?.rssi?.compareTo(MEDIUM_SIGNAL) == -1 -> view.signalIcon.setImageResource(R.drawable.icon_connection_2)
            else -> view.signalIcon.setImageResource(R.drawable.icon_connection_3)
        }

        if (item?.updateAt?.diffGreaterThan(10000) == true) {
            view.address.setTextColor(Color.GRAY)
        } else {
            view.address.setTextColor(Color.BLACK)
        }

        return view
    }

    companion object {
        private const val LOW_SIGNAL = -80
        private const val MEDIUM_SIGNAL = -50
    }
}