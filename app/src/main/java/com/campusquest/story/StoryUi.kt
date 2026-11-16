package com.campusquest.story

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import com.campusquest.R
import com.google.android.material.button.MaterialButton

/**
 * Shared building blocks for the story's task screens. The look follows the rest of the app:
 * parchment page, serif headings, a plain readable body, leather buttons with outlined secondary ones.
 */
object StoryUi {
    private val ink = Color.rgb(32, 44, 36)
    private val muted = Color.rgb(86, 86, 64)
    private val brown = Color.rgb(124, 79, 42)
    private val gold = Color.rgb(217, 164, 65)

    fun column(context: Context): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        val side = pixels(context, 20)
        setPadding(side, pixels(context, 12), side, pixels(context, 32))
        setBackgroundResource(R.drawable.bg_parchment)
    }

    /**
     * Text sized by role: 26sp and up is the screen title, 20sp and up a serif heading,
     * 14.5sp and below a quiet note, anything else body text. An empty string takes no space.
     */
    fun text(column: LinearLayout, value: String, size: Float = 17f): TextView = TextView(column.context).apply {
        text = value
        when {
            size >= 26f -> {
                typeface = Typeface.create("serif", Typeface.BOLD); textSize = 30f; setTextColor(ink)
            }
            size >= 20f -> {
                typeface = Typeface.create("serif", Typeface.BOLD); textSize = 22f; setTextColor(ink)
            }
            size <= 14.5f -> {
                textSize = 14f; setTextColor(muted)
            }
            else -> {
                textSize = 16f; setTextColor(ink); setLineSpacing(0f, 1.15f)
            }
        }
        val top = pixels(context, if (size >= 20f) 16 else 8)
        setPadding(0, top, 0, pixels(context, 4))
        // empty placeholders (status lines) stay invisible until something is written to them
        visibility = if (value.isEmpty()) View.GONE else View.VISIBLE
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (tag != "keep") visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
            }
        })
        column.addView(this, LinearLayout.LayoutParams(-1, -2))
    }

    /** The main action on a screen is a leather button; [secondary] makes it an outlined one. */
    fun button(column: LinearLayout, label: String, secondary: Boolean = false, action: () -> Unit): MaterialButton {
        val ctx = column.context
        return (if (secondary) MaterialButton(ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle)
        else MaterialButton(ctx)).apply {
            text = label
            minHeight = pixels(ctx, 52)
            if (secondary) {
                setTextColor(brown); strokeColor = android.content.res.ColorStateList.valueOf(gold)
                backgroundTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
            }
            val lp = LinearLayout.LayoutParams(-1, -2).apply { topMargin = pixels(ctx, 12) }
            column.addView(this, lp)
            setOnClickListener { action() }
        }
    }

    /** A quiet "back" link with an arrow, for the top of a screen. */
    fun back(column: LinearLayout, label: String, action: () -> Unit): MaterialButton {
        val ctx = column.context
        return MaterialButton(ctx, null, com.google.android.material.R.attr.borderlessButtonStyle).apply {
            text = label
            setTextColor(brown)
            icon = ContextCompat.getDrawable(ctx, R.drawable.ic_arrow_back)
            iconTint = android.content.res.ColorStateList.valueOf(brown)
            iconPadding = pixels(ctx, 6)
            minHeight = pixels(ctx, 48)
            column.addView(this, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = -pixels(ctx, 12) })
            setOnClickListener { action() }
        }
    }

    /** A card of ready / needs-attention rows, instead of a dump of technical text. */
    fun checklist(column: LinearLayout, rows: List<Triple<String, Boolean, String>>) {
        val ctx = column.context
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_stat_tile)
            setPadding(pixels(ctx, 16), pixels(ctx, 10), pixels(ctx, 16), pixels(ctx, 10))
        }
        rows.forEach { (label, ok, detail) ->
            val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            row.addView(ImageView(ctx).apply {
                setImageResource(if (ok) R.drawable.ic_check else R.drawable.ic_info)
                setColorFilter(if (ok) Color.rgb(46, 125, 91) else gold)
            }, LinearLayout.LayoutParams(pixels(ctx, 22), pixels(ctx, 22)))
            row.addView(TextView(ctx).apply {
                text = label; textSize = 15f; setTextColor(ink); setTypeface(typeface, Typeface.BOLD)
                setPadding(pixels(ctx, 10), pixels(ctx, 8), 0, pixels(ctx, 8))
            }, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(TextView(ctx).apply { text = detail; textSize = 14f; setTextColor(muted); gravity = Gravity.END },
                LinearLayout.LayoutParams(-2, -2))
            card.addView(row)
        }
        column.addView(card, LinearLayout.LayoutParams(-1, -2).apply { topMargin = pixels(ctx, 12) })
    }

    /** Small monospace readout for technical details (sensor readings). Starts however the caller sets it. */
    fun note(column: LinearLayout, value: String): TextView = TextView(column.context).apply {
        val ctx = column.context
        text = value; textSize = 12f; typeface = Typeface.MONOSPACE; setTextColor(muted)
        tag = "keep"
        setBackgroundResource(R.drawable.bg_stat_tile)
        setPadding(pixels(ctx, 14), pixels(ctx, 12), pixels(ctx, 14), pixels(ctx, 12))
        column.addView(this, LinearLayout.LayoutParams(-1, -2).apply { topMargin = pixels(ctx, 12) })
    }

    fun input(column: LinearLayout, label: String, value: String = ""): EditText {
        text(column, label, 14f)
        val ctx = column.context
        return EditText(ctx).apply {
            setText(value); setTextColor(ink); setSingleLine(); textSize = 16f
            setBackgroundResource(R.drawable.bg_input)
            setPadding(pixels(ctx, 14), pixels(ctx, 12), pixels(ctx, 14), pixels(ctx, 12))
            column.addView(this, LinearLayout.LayoutParams(-1, -2))
        }
    }

    /** Metres below a kilometre, otherwise kilometres: "About 120 m" or "About 213 km". */
    fun distance(metres: Float): String =
        if (metres >= 1000f) "${(metres / 1000f).toInt()} km" else "${metres.toInt()} m"

    fun pixels(context: Context, dp: Int) = (dp * context.resources.displayMetrics.density).toInt()
}
