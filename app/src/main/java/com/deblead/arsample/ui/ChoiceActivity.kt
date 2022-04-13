package com.deblead.arsample.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.widget.AppCompatButton
import com.xplora.arsample.R

class ChoiceActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choice)
        initListener()
    }

    private fun initListener() {
        findViewById<AppCompatButton>(R.id.button).setOnClickListener {
            finish()
            startActivity(Intent(this, ArActivityTest::class.java))
        }
        findViewById<AppCompatButton>(R.id.button2).setOnClickListener {
            finish()
            startActivity(Intent(this, ChromeVideoActivity::class.java))
        }
    }
}