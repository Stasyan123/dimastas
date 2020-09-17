package com.sm.stasversion

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.content.Intent.ACTION_SEND
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.net.Uri
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.sm.stasversion.imagepicker.model.Image
import com.sm.stasversion.imagepicker.model.Video


class MenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        openIcons()

        val topClose = findViewById<ImageView>(R.id.topClose)
        topClose.setOnClickListener {
            finish()
        }

        val rate = findViewById<ConstraintLayout>(R.id.rate)
        rate.setOnClickListener {

            /*
             Uri uri = Uri.parse("market://details?id=" + context.getPackageName());
    Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
    // To count with Play market backstack, After pressing back button,
    // to taken back to our application, we need to add following flags to intent.
    goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
    try {
        startActivity(goToMarket);
    } catch (ActivityNotFoundException e) {
        startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("http://play.google.com/store/apps/details?id=" + context.getPackageName())));
    }
            * */
        }

        val contact = findViewById<ConstraintLayout>(R.id.contact)
        contact.setOnClickListener {
            val email = Intent(Intent.ACTION_SENDTO)
            email.data = Uri.parse("mailto:malienko.mpmr@gmail.com")
            startActivity(email)
        }

        val app_inst = findViewById<ConstraintLayout>(R.id.inst)
        app_inst.setOnClickListener {
            openInstagram("deadinvalentinesday")
        }

        val my = findViewById<ConstraintLayout>(R.id.my)
        my.setOnClickListener {
            openInstagram("deadinvalentinesday")
        }

        val boar = findViewById<ConstraintLayout>(R.id.boar)
        boar.setOnClickListener {
            openInstagram("dmitrygaziev")
        }
    }

    fun openIcons() {
        val stas = findViewById<ImageView>(R.id.stas)
        Glide.with(this)
            .load(getResources().getIdentifier("stas", "drawable", this.getPackageName()))
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(stas)

        val mood = findViewById<ImageView>(R.id.mood)
        Glide.with(this)
            .load(getResources().getIdentifier("mood", "drawable", this.getPackageName()))
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(mood)

        val dima_icon = findViewById<ImageView>(R.id.dima)
        Glide.with(this)
            .load(getResources().getIdentifier("dima", "drawable", this.getPackageName()))
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(dima_icon)
    }

    fun openInstagram(user: String) {
        val uri = Uri.parse("http://instagram.com/_u/" + user)
        val likeIng = Intent(Intent.ACTION_VIEW, uri)

        likeIng.setPackage("com.instagram.android")

        try {
            startActivity(likeIng)
        } catch (e: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("http://instagram.com/" + user)
                )
            )
        }
    }
}
