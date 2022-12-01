package com.ruuvi.station.network.ui.claim

import android.content.Context
import androidx.annotation.StringRes
import com.ruuvi.station.R

object ClaimRoutes {
    const val CHECK_CLAIM_STATE = "check_claim_state"
    const val IN_PROGRESS = "in_progress"
    const val FREE_TO_CLAIM = "free_to_claim"
    const val FORCE_CLAIM_INIT = "force_claim_init"
    const val FORCE_CLAIM_GETTING_ID = "force_claim_getting_id"

    fun getTitleByRoute(context: Context, route: String): String {
        return when (route) {
            FREE_TO_CLAIM -> context.getString(R.string.claim_sensor)
            FORCE_CLAIM_INIT -> context.getString(R.string.force_claim_sensor)
            FORCE_CLAIM_GETTING_ID -> context.getString(R.string.force_claim_sensor)
            else -> context.getString(R.string.app_name)
        }
    }
}

sealed class ClaimRouteDestination(
    @StringRes val titleRes: Int,
    route: String
) {
    object CheckClaimState: ClaimRouteDestination(
        R.string.claim_sensor,
        ClaimRoutes.CHECK_CLAIM_STATE
    )

    object FreeToClaim: ClaimRouteDestination(
        R.string.claim_sensor,
        ClaimRoutes.FREE_TO_CLAIM
    )
}