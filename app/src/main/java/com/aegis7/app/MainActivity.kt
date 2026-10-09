package com.aegis7.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import com.aegis7.app.bridge.AiBridge

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.rgb(8, 12, 22))
        }

        val title = TextView(this).apply {
            text = "AEGIS-7"
            textSize = 30f
            setTextColor(Color.rgb(100, 170, 255))
            gravity = Gravity.CENTER
        }

        val status = TextView(this).apply {
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 0)
        }

        layout.addView(title)
        layout.addView(status)
        setContentView(layout)

        status.text = try {
            AiBridge.getStatus()
        } catch (error: UnsatisfiedLinkError) {
            "Native engine could not load."
        }
    }
}
