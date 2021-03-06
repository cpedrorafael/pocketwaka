package com.kondenko.pocketwaka.screens

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kondenko.pocketwaka.R
import com.kondenko.pocketwaka.ui.skeleton.Skeleton
import kotlinx.android.synthetic.main.activity_playground.*

class PlaygroundActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playground)
        val skeleton = Skeleton(this, textview_with_skeleton, getDrawable(R.drawable.all_skeleton)!!)
        textview_with_skeleton.setOnClickListener {
            if (skeleton.isShown) skeleton.hide()
            else skeleton.show()
        }
    }

}
