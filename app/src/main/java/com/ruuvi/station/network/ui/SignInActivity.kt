package com.ruuvi.station.network.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.ruuvi.station.R
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.tagdetails.ui.TagDetailsActivity
import kotlinx.android.synthetic.main.activity_sign_in.*
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance

class SignInActivity() : AppCompatActivity(), KodeinAware{

    override val kodein: Kodein by closestKodein()
    val networkInteractor: RuuviNetworkInteractor by instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    fun updateTitle(title: String) {
        toolbar.title = title
    }

    fun goBackEnabled(enabled: Boolean) {
        supportActionBar?.setDisplayHomeAsUpEnabled(enabled)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            super.onBackPressed()
        }
        return true
    }

    override fun onBackPressed() {
        if (networkInteractor.signedIn) {
            val parentIntent = Intent(this, TagDetailsActivity::class.java)
            parentIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(parentIntent)
            finish()
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, SignInActivity::class.java)
            context.startActivity(intent)
        }
    }
}