package com.ruuvi.station.dashboard.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import com.ruuvi.station.R
import com.ruuvi.station.alarm.domain.AlarmStatus
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.util.Utils
import kotlinx.android.synthetic.main.item_dashboard.view.bell
import kotlinx.android.synthetic.main.item_dashboard.view.dashboardContainer
import kotlinx.android.synthetic.main.item_dashboard.view.deviceId
import kotlinx.android.synthetic.main.item_dashboard.view.humidity
import kotlinx.android.synthetic.main.item_dashboard.view.lastSeenTextView
import kotlinx.android.synthetic.main.item_dashboard.view.letterImage
import kotlinx.android.synthetic.main.item_dashboard.view.pressure
import kotlinx.android.synthetic.main.item_dashboard.view.signal
import kotlinx.android.synthetic.main.item_dashboard.view.temperature

class RuuviTagAdapter(
    context: Context,
    items: List<RuuviTag>
) : ArrayAdapter<RuuviTag>(context, 0, items) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = getItem(position)
        val view =
            convertView ?: LayoutInflater.from(context).inflate(R.layout.item_dashboard, parent, false)

        view.dashboardContainer.tag = item
        view.deviceId.text = item?.displayName
        view.temperature.text = item?.temperatureString
        view.humidity.text = item?.humidityString
        view.pressure.text = context.getString(R.string.pressure_reading, item?.pressure?.div(100))
        view.signal.text = context.getString(R.string.signal_reading, item?.rssi)

        val ballColorRes = if (position % 2 == 0) R.color.main else R.color.mainLight
        val ballRadius = context.resources.getDimension(R.dimen.letter_ball_radius).toInt()
        val ballColor = ContextCompat.getColor(context, ballColorRes)
        val firsLetter = view.deviceId.text[0].toString() + ""
        val ballBitmap = Utils.createBall(ballRadius, ballColor, Color.WHITE, firsLetter)
        view.letterImage.setImageBitmap(ballBitmap)

        val updatedAt =
            context.getString(R.string.updated, Utils.strDescribingTimeSince(item?.updatedAt))
        view.lastSeenTextView.text = updatedAt

        val status = item?.status ?: AlarmStatus.NO_ALARM
        val (isVisible, iconResource) =
            when (status) {
                AlarmStatus.NO_ALARM -> true to R.drawable.ic_notifications_off_24px
                AlarmStatus.NO_TRIGGERED -> true to R.drawable.ic_notifications_on_24px
                AlarmStatus.TRIGGERED -> !view.bell.isVisible to R.drawable.ic_notifications_active_24px
            }
        view.bell.isVisible = isVisible
        view.bell.setImageResource(iconResource)

        val colorStateList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.main))
        ImageViewCompat.setImageTintList(view.bell, colorStateList)

        return view
    }
}