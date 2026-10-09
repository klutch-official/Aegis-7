package com.aegis7.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.LinearLayout
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.EditText

class MainActivity : Activity() {

    private val bg = Color.rgb(7, 10, 19)
    private val panel = Color.rgb(14, 19, 32)
    private val blue = Color.rgb(88, 156, 255)
    private val white = Color.rgb(240, 244, 255)
    private val muted = Color.rgb(145, 157, 181)

    private lateinit var messages: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var input: EditText
    private lateinit var modelLabel: TextView

    private var codingMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = Color.BLACK
        window.decorView.systemUiVisibility = 0

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }

        // Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(14), dp(14), dp(14))
            setBackgroundColor(panel)
        }

        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }

        brand.addView(label("AEGIS-7", 21f, blue, true))
        brand.addView(label("OFFLINE AI ASSISTANT", 10f, muted, false))

        val newChat = TextView(this).apply {
            text = "+ New chat"
            textSize = 13f
            setTextColor(white)
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = rounded(Color.rgb(30, 42, 64), 12)
            setOnClickListener { resetChat() }
        }

        header.addView(brand)
        header.addView(newChat)
        root.addView(header)

        // Model selector
        val modelRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }

        modelLabel = label("●  General AI", 13f, white, true).apply {
            setPadding(dp(12), dp(9), dp(12), dp(9))
            background = rounded(Color.rgb(24, 44, 72), 18)
            setOnClickListener {
                codingMode = !codingMode
                modelLabel.text = if (codingMode) "‹  Coding AI" else "●  General AI"
                modelLabel.setTextColor(if (codingMode) Color.rgb(190, 155, 255) else white)
                modelLabel.background = rounded(
                    if (codingMode) Color.rgb(48, 33, 68) else Color.rgb(24, 44, 72), 18
                )
            }
        }

        modelRow.addView(modelLabel)
        modelRow.addView(
            label("Model selection is a preview", 11f, muted, false).apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(10), 0, 0, 0)
            }
        )
        root.addView(modelRow)

        // Messages area
        scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }

        messages = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(messages)

        val messageArea = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
        }
        messageArea.addView(scroll)
        root.addView(messageArea)

        showWelcome()

        // Input area
        val composer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(12))
            setBackgroundColor(panel)
        }

        input = EditText(this).apply {
            hint = "Message Aegis-7..."
            setHintTextColor(muted)
            setTextColor(white)
            textSize = 15f
            maxLines = 4
            minLines = 1
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = rounded(Color.rgb(25, 31, 47), 18)
            imeOptions = EditorInfo.IME_ACTION_SEND
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE

            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    sendMessage()
                    true
                } else false
            }

            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }

        val send = TextView(this).apply {
            text = "↑"
            textSize = 25f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = rounded(blue, 16)
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(48)).apply {
                leftMargin = dp(8)
            }
            setOnClickListener { sendMessage() }
        }

        composer.addView(input)
        composer.addView(send)
        root.addView(composer)

        setContentView(root)
    }

    private fun showWelcome() {
        messages.removeAllViews()

        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(1, dp(70))
        }
        messages.addView(spacer)

        val title = label("Your AI. Your device.", 25f, white, true).apply {
            gravity = Gravity.CENTER
        }
        messages.addView(title)

        messages.addView(label(
            "Aegis-7 is getting ready for offline intelligence.",
            14f, muted, false
        ).apply {
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(12), dp(12), dp(22))
        })

        val suggestion = TextView(this).apply {
            text = "What can you help me with?"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(blue)
            setPadding(dp(16), dp(13), dp(16), dp(13))
            background = rounded(Color.rgb(19, 29, 47), 16)
            setOnClickListener {
                input.setText("What can you help me with?")
                input.setSelection(input.text.length)
                input.requestFocus()
            }
        }
        messages.addView(suggestion)
    }

    private fun sendMessage() {
        val text = input.text.toString().trim()
        if (text.isEmpty()) return

        if (messages.childCount > 0 && messages.childCount <= 3) {
            messages.removeAllViews()
        }

        addBubble(text, true)
        input.text.clear()

        val manager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        manager.hideSoftInputFromWindow(input.windowToken, 0)

        addBubble(
            "The chat interface is working, but the offline AI model has not been connected yet.",
            false
        )
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun addBubble(text: String, fromUser: Boolean) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = if (fromUser) Gravity.END else Gravity.START
            setPadding(0, dp(5), 0, dp(5))
        }

        val bubble = TextView(this).apply {
            this.text = text
            textSize = 15f
            setTextColor(white)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(
                if (fromUser) Color.rgb(35, 75, 130) else panel,
                16
            )
            maxWidth = resources.displayMetrics.widthPixels * 3 / 4
        }

        row.addView(bubble)
        messages.addView(row)
    }

    private fun resetChat() {
        input.text.clear()
        showWelcome()
    }

    private fun label(
        text: String,
        size: Float,
        color: Int,
        bold: Boolean
    ): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
