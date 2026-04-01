package com.example.ecogeoguard.utils

import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import com.example.ecogeoguard.R

object AppAnimationUtils {

    fun playLogoAnimation(view: View) {
        val anim = AnimationUtils.loadAnimation(
            view.context,
            R.anim.splash_logo_anim
        )
        view.startAnimation(anim)
    }

    fun playTextAnimation(view: View) {
        val anim = AnimationUtils.loadAnimation(
            view.context,
            R.anim.splash_text_anim
        )
        view.startAnimation(anim)
    }
}
