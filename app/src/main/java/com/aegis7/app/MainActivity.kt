package com.aegis7.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.aegis7.app.bridge.AiBridge
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors

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
    private lateinit var statusLabel: TextView
    private lateinit var sendButton: Button
    private lateinit var loadButton: Button

    private val executor = Executors.newSingleThreadExecutor()

    @Volatile
    private var modelReady = false

    @Volatile
    private var busy = false

    private var hasStartedChat = false
    private var modelName = "No model loaded"
    private var codingMode = false

    companion object {
        private const val REQUEST_PICK_MODEL = 7104
    }

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
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(8))
            setBackgroundColor(panel)
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val brandColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        brandColumn.addView(
            label("AEGIS-7", 22f, white, true)
        )

        brandColumn.addView(
            label("OFFLINE AI ASSISTANT", 10f, muted, true)
        )

        topRow.addView(
            brandColumn,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        val newChatButton = Button(this).apply {
            text = "+ New chat"
            textSize = 11f
            isAllCaps = false
            setTextColor(white)
            background = rounded(Color.rgb(30, 39, 57), 12)
            setOnClickListener { resetChat() }
        }

        topRow.addView(
            newChatButton,
            LinearLayout.LayoutParams(-2, dp(42))
        )

        header.addView(topRow)

        val secondRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(12), 0, dp(4))
        }

        modelLabel = label("● General AI", 13f, blue, true)
        modelLabel.setPadding(dp(10), dp(8), dp(10), dp(8))
        modelLabel.background = rounded(Color.rgb(19, 29, 47), 12)
        modelLabel.setOnClickListener {
            codingMode = !codingMode
            updateModelLabel()
        }

        secondRow.addView(
            modelLabel,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        loadButton = Button(this).apply {
            text = "Load model"
            textSize = 12f
            isAllCaps = false
            setTextColor(white)
            background = rounded(Color.rgb(35, 75, 130), 12)
            setOnClickListener { chooseModelFile() }
        }

        secondRow.addView(
            loadButton,
            LinearLayout.LayoutParams(-2, dp(42))
        )

        header.addView(secondRow)

        statusLabel = label(
            "No model loaded. Import a GGUF model to begin.",
            11f,
            muted,
            false
        )
        statusLabel.setPadding(dp(2), dp(3), dp(2), dp(4))
        header.addView(statusLabel)

        root.addView(header)

        // Conversation area
        scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            setPadding(dp(14), dp(10), dp(14), dp(10))
        }

        messages = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        scroll.addView(
            messages,
            ViewGroup.LayoutParams(-1, -2)
        )

        root.addView(
            scroll,
            LinearLayout.LayoutParams( -1, 0, 1f)
        )

        // Composer
        val composer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(12))
            setBackgroundColor(panel)
        }

        input = EditText(this).apply {
            hint = "Message Aegis-7..."
            textSize = 15f
            setTextColor(white)
            setHintTextColor(muted)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = rounded(Color.rgb(25, 31, 46), 16)
            minHeight = dp(48)
            maxLines = 4
            setSingleLine(false)
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_SEND
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                    sendMessage()
                    true
                } else {
                    false
                }
            }
        }

        composer.addView(
            input,
            LinearLayout.LayoutParams(0, -2, 1f).apply {
                marginEnd = dp(8)
            }
        )

        sendButton = Button(this).apply {
            text = "Send"
            textSize = 13f
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = rounded(Color.rgb(35, 95, 175), 14)
            setOnClickListener { sendMessage() }
        }

        composer.addView(
            sendButton,
            LinearLayout.LayoutParams(-2, dp(48))
        )

        root.addView(composer)

        setContentView(root)
        showWelcome()

        // Check that the native library can be reached.
        executor.execute {
            try {
                val nativeStatus = AiBridge.getStatus()
                runOnUiThread {
                    if (!isFinishing) {
                        statusLabel.text = nativeStatus
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    if (!isFinishing) {
                        statusLabel.text =
                            "Native AI unavailable: ${e.message ?: "unknown error"}"
                    }
                }
            } catch (e: UnsatisfiedLinkError) {
                runOnUiThread {
                    if (!isFinishing) {
                        statusLabel.text =
                            "Native library failed to load. Check the APK build."
                    }
                }
            }
        }
    }

    private fun chooseModelFile() {
        if (busy) {
            toast("Please wait for the current operation to finish.")
            return
        }

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }

        try {
            startActivityForResult(intent, REQUEST_PICK_MODEL)
        } catch (e: Exception) {
            toast("Could not open the file picker.")
        }
    }

    @Deprecated("Uses the Activity result API for broad Android compatibility")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != REQUEST_PICK_MODEL ||
            resultCode != RESULT_OK
        ) {
            return
        }

        val uri = data?.data ?: return
        importModel(uri)
    }

    private fun importModel(uri: Uri) {
        if (busy) return

        busy = true
        modelReady = false
        loadButton.isEnabled = false
        sendButton.isEnabled = false
        statusLabel.text = "Preparing model file..."
        statusLabel.setTextColor(blue)

        executor.execute {
            var copiedFile: File? = null

            try {
                val displayName = getDisplayName(uri)
                    ?: "model.gguf"

                if (!displayName.lowercase().endsWith(".gguf")) {
                    throw IllegalArgumentException(
                        "Please choose a .gguf model file."
                    )
                }

                val modelsDir = File(filesDir, "models")
                if (!modelsDir.exists() && !modelsDir.mkdirs()) {
                    throw IllegalStateException(
                        "Could not create the models folder."
                    )
                }

                val safeName = displayName
                    .replace(Regex("[^A-Za-z0-9._-]"), "_")
                    .let {
                        if (it.isBlank()) "model.gguf" else it
                    }

                val target = File(modelsDir, safeName)
                val temp = File(modelsDir, "$safeName.copying")
                copiedFile = temp

                contentResolver.openInputStream(uri).use { source ->
                    if (source == null) {
                        throw IllegalStateException(
                            "Could not read the selected file."
                        )
                    }

                    FileOutputStream(temp).use { output ->
                        val buffer = ByteArray(1024 * 1024)
                        while (true) {
                            val count = source.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                        }
                        output.fd.sync()
                    }
                }

                if (temp.length() <= 0L) {
                    throw IllegalStateException("The selected file is empty.")
                }

                if (target.exists() && !target.delete()) {
                    throw IllegalStateException(
                        "Could not replace the existing model file."
                    )
                }

                if (!temp.renameTo(target)) {
                    throw IllegalStateException(
                        "Could not finish copying the model."
                    )
                }

                copiedFile = target

                runOnUiThread {
                    if (!isFinishing) {
                        statusLabel.text =
                            "Loading $safeName — this may take a while."
                    }
                }

                val result = AiBridge.loadModel(target.absolutePath)

                if (result.startsWith("ERROR", ignoreCase = true) ||
                    result.contains("failed", ignoreCase = true)
                ) {
                    throw IllegalStateException(result)
                }

                modelReady = true
                modelName = safeName

                runOnUiThread {
                    if (!isFinishing) {
                        statusLabel.text = "Ready • $modelName"
                        statusLabel.setTextColor(
                            Color.rgb(112, 220, 160)
                        )
                        updateModelLabel()
                        toast("Model loaded successfully.")
                    }
                }
            } catch (e: Exception) {
                modelReady = false
                val message = e.message ?: "Unknown model loading error"

                runOnUiThread {
                    if (!isFinishing) {
                        statusLabel.text = "Model error: $message"
                        statusLabel.setTextColor(
                            Color.rgb(255, 130, 130)
                        )
                        toast("Could not load model: $message")
                    }
                }
            } catch (e: UnsatisfiedLinkError) {
                modelReady = false
                runOnUiThread {
                    if (!isFinishing) {
                        statusLabel.text =
                            "Native AI library is unavailable."
                        statusLabel.setTextColor(
                            Color.rgb(255, 130, 130)
                        )
                    }
                }
            } finally {
                runOnUiThread {
                    busy = false
                    if (!isFinishing) {
                        loadButton.isEnabled = true
                        sendButton.isEnabled = true
                    }
                }
            }
        }
    }

    private fun getDisplayName(uri: Uri): String? {
        var cursor: Cursor? = null

        return try {
            cursor = contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )

            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(
                    OpenableColumns.DISPLAY_NAME
                )

                if (index >= 0) cursor.getString(index) else null
            } else {
                null
            }
        } finally {
            cursor?.close()
        }
    }

    private fun updateModelLabel() {
        val mode = if (codingMode) "Coding mode" else "General AI"
        modelLabel.text = if (modelReady) {
            "● $mode"
        } else {
            "● $mode · No model"
        }
    }

    private fun sendMessage() {
        val prompt = input.text.toString().trim()
        if (prompt.isEmpty()) return

        if (busy) {
            toast("Please wait for the current operation to finish.")
            return
        }

        if (!modelReady) {
            toast("Load a GGUF model first.")
            statusLabel.text = "Import a compatible .gguf model before chatting."
            return
        }

        if (!hasStartedChat) {
            messages.removeAllViews()
            hasStartedChat = true
        }

        addBubble(prompt, true)
        input.text.clear()

        val manager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        manager.hideSoftInputFromWindow(input.windowToken, 0)

        busy = true
        sendButton.isEnabled = false
        loadButton.isEnabled = false
        statusLabel.text = "Aegis-7 is thinking..."
        statusLabel.setTextColor(blue)

        val modeInstructions = if (codingMode) {
            "You are Aegis-7, an offline coding assistant. " +
                "Explain code clearly, use correct syntax, and be honest " +
                "when you are unsure. Do not invent APIs."
        } else {
            "You are Aegis-7, a helpful offline assistant. " +
                "Answer clearly and honestly. If you do not know something, " +
                "say so rather than making it up."
        }

        val fullPrompt =
            "$modeInstructions\n\nUser: $prompt\nAssistant:"

        executor.execute {
            try {
                val answer = AiBridge.generate(fullPrompt)
                val cleanAnswer = answer.trim().ifEmpty {
                    "The model returned an empty response. Try another prompt."
                }

                runOnUiThread {
                    if (!isFinishing) {
                        addBubble(cleanAnswer, false)
                        statusLabel.text = "Ready • $modelName"
                        statusLabel.setTextColor(
                            Color.rgb(112, 220, 160)
                        )
                        scroll.post {
                            scroll.fullScroll(View.FOCUS_DOWN)
                        }
                    }
                }
            } catch (e: Exception) {
                val message = e.message ?: "Unknown inference error"

                runOnUiThread {
                    if (!isFinishing) {
                        addBubble("AI error: $message", false)
                        statusLabel.text = "Generation failed."
                        statusLabel.setTextColor(
                            Color.rgb(255, 130, 130)
                        )
                    }
                }
            } catch (e: UnsatisfiedLinkError) {
                runOnUiThread {
                    if (!isFinishing) {
                        addBubble(
                            "The native AI library could not be reached. " +
                                "Check the APK build.",
                            false
                        )
                        statusLabel.text = "Native AI unavailable."
                    }
                }
            } finally {
                runOnUiThread {
                    busy = false
                    if (!isFinishing) {
                        sendButton.isEnabled = true
                        loadButton.isEnabled = true
                        input.requestFocus()
                    }
                }
            }
        }

        scroll.post {
            scroll.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun showWelcome() {
        messages.removeAllViews()

        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(1, dp(70))
        }
        messages.addView(spacer)

        val title = label(
            "Your AI. Your device.",
            25f,
            white,
            true
        ).apply {
            gravity = Gravity.CENTER
        }
        messages.addView(title)

        val subtitle = label(
            "Aegis-7 runs AI locally when a compatible model is loaded.",
            14f,
            muted,
            false
        ).apply {
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(12), dp(12), dp(22))
        }
        messages.addView(subtitle)

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

        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER
        }
        row.addView(suggestion)
        messages.addView(row)
    }

    private fun resetChat() {
        if (busy) {
            toast("Wait for the current operation to finish.")
            return
        }

        input.text.clear()
        hasStartedChat = false
        showWelcome()
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

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}
