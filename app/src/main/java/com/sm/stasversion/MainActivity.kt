package com.sm.stasversion

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.constraintlayout.widget.ConstraintLayout


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val root = findViewById<ConstraintLayout>(R.id.rootView)

        android.os.Handler().postDelayed(
            { launchPicker() },
            1200
        )

        root.setOnClickListener{
            launchPicker();
        }
    }

    private fun showSingleImage(uri: Uri) {
        
    }

    private fun launchPicker() {
    }
}
