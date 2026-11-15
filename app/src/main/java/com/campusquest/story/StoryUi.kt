package com.campusquest.story

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.*
import com.google.android.material.button.MaterialButton

/** Small shared building blocks for the story's dynamic task screens. */
object StoryUi {
    fun column(context: Context): LinearLayout = LinearLayout(context).apply {
        orientation=LinearLayout.VERTICAL;setPadding(24,24,24,32)
        setBackgroundColor(Color.rgb(246,233,204))
    }
    fun text(column: LinearLayout, value: String, size: Float = 17f): TextView = TextView(column.context).apply {
        text=value;textSize=size;setTextColor(Color.rgb(29,49,36));setPadding(8,12,8,12)
        column.addView(this,LinearLayout.LayoutParams(-1,-2))
    }
    fun button(column: LinearLayout, label: String, action: () -> Unit): MaterialButton = MaterialButton(column.context).apply {
        text=label;minHeight=(52*resources.displayMetrics.density).toInt()
        column.addView(this,LinearLayout.LayoutParams(-1,-2));setOnClickListener { action() }
    }
    fun input(column: LinearLayout, label: String, value: String = ""): EditText {
        text(column,label,14f)
        return EditText(column.context).apply {setText(value);setTextColor(Color.rgb(29,49,36));setSingleLine();column.addView(this,LinearLayout.LayoutParams(-1,-2))}
    }
    fun pixels(context: Context, dp: Int) = (dp*context.resources.displayMetrics.density).toInt()
}
