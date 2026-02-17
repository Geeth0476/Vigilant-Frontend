package com.simats.vigilant

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ReplacementSpan
import androidx.core.content.ContextCompat
import android.text.style.ForegroundColorSpan

object VigilantBrand {
    
    /**
     * Returns "Vigilant" with the 'i's colored to match the arrow logo.
     * Simpler implementation per user request: Color the entire 'i', not just the dot.
     */
    fun getStyledLogo(context: Context): SpannableString {
        val text = "Vigilant"
        val spannable = SpannableString(text)
        // Use the blue from the arrow (vigilant_logo_blue)
        val arrowBlue = ContextCompat.getColor(context, R.color.vigilant_logo_blue)

        // Color first 'i' (index 1)
        spannable.setSpan(
            ForegroundColorSpan(arrowBlue),
            1, 2,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // Color second 'i' (index 3)
        spannable.setSpan(
            ForegroundColorSpan(arrowBlue),
            3, 4,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        return spannable
    }

    /**
     * Returns "Powered by SIMATS Engineering" with "SIMATS Engineering" in Bold and Blue
     */
    fun getPoweredByText(context: Context): SpannableString {
        val prefix = "Powered by "
        val company = "SIMATS Engineering"
        val text = prefix + company
        val spannable = SpannableString(text)

        val blueColor = ContextCompat.getColor(context, R.color.vigilant_blue)
        val start = prefix.length
        val end = text.length

        // Bold
        spannable.setSpan(
            android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
            start, end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // Blue Color
        spannable.setSpan(
            ForegroundColorSpan(blueColor),
            start, end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        return spannable
    }
}
