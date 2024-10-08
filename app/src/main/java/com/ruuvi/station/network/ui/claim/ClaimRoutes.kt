package com.ruuvi.station.network.ui.claim

import android.content.Context
import com.ruuvi.station.R

object ClaimRoutes {
    const val CHECK_CLAIM_STATE = "check_claim_state"
    const val IN_PROGRESS = "in_progress"

    const val FREE_TO_CLAIM = "free_to_claim"
    const val CLAIM_IN_PROGRESS = "claim_in_progress"

    const val FORCE_CLAIM_INIT = "force_claim_init"
    const val FORCE_CLAIM_GETTING_ID = "force_claim_getting_id"
    const val FORCE_CLAIM_ERROR = "force_claim_error"

    const val NOT_SIGNED_IN = "not_signed_in"
    const val UNCLAIM = "unclaim"

    fun getTitleByRoute(context: Context, route: String): String {
        return when (route) {
            FREE_TO_CLAIM -> context.getString(R.string.claim_sensor)
            NOT_SIGNED_IN -> context.getString(R.string.claim_sensor)
            CLAIM_IN_PROGRESS -> context.getString(R.string.claim_sensor)
            FORCE_CLAIM_INIT -> context.getString(R.string.force_claim_sensor)
            FORCE_CLAIM_GETTING_ID -> context.getString(R.string.force_claim_sensor)
            UNCLAIM -> context.getString(R.string.unclaim_sensor)
            FORCE_CLAIM_ERROR -> context.getString(R.string.error)
            else -> context.getString(R.string.app_name)
        }
    }
}