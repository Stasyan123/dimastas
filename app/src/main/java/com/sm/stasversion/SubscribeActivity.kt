package com.sm.stasversion

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout

class SubscribeActivity : AppCompatActivity() {

    private var type: String = "p_month"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscribe)

        val close = findViewById<ImageView>(R.id.topClose)
        close.setOnClickListener {
            finish()
        }

        val restore = findViewById<TextView>(R.id.restore)
        restore.setOnClickListener {

        }

        val p_month = findViewById<ConstraintLayout>(R.id.p_month)
        val pv_month = findViewById<ConstraintLayout>(R.id.pv_month)
        val p_year = findViewById<ConstraintLayout>(R.id.p_year)
        val pv_year = findViewById<ConstraintLayout>(R.id.pv_year)

        val p_month_active = findViewById<ConstraintLayout>(R.id.p_month_active)
        val pv_month_active = findViewById<ConstraintLayout>(R.id.pv_month_active)
        val p_year_active = findViewById<ConstraintLayout>(R.id.p_year_active)
        val pv_year_active = findViewById<ConstraintLayout>(R.id.pv_year_active)

        fun hideAll() {
            p_month_active.visibility = GONE
            pv_month_active.visibility = GONE
            p_year_active.visibility = GONE
            pv_year_active.visibility = GONE
        }

        fun showAll() {
            p_month.visibility = VISIBLE
            pv_month.visibility = VISIBLE
            p_year.visibility = VISIBLE
            pv_year.visibility = VISIBLE
        }

        p_month.setOnClickListener {
            hideAll()
            showAll()

            p_month.visibility = GONE
            p_month_active.visibility = VISIBLE


            type = "p_month"
        }

        pv_month.setOnClickListener {
            hideAll()
            showAll()

            pv_month.visibility = GONE
            pv_month_active.visibility = VISIBLE

            type = "pv_month"
        }

        p_year.setOnClickListener {
            hideAll()
            showAll()

            p_year.visibility = GONE
            p_year_active.visibility = VISIBLE

            type = "p_year"
        }

        pv_year.setOnClickListener {
            hideAll()
            showAll()

            pv_year.visibility = GONE
            pv_year_active.visibility = VISIBLE

            type = "pv_year"
        }
    }
}
