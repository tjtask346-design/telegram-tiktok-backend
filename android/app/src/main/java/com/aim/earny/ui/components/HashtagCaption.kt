package com.aim.earny.ui.components

import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.aim.earny.ui.theme.Gold

/**
 * Renders caption with clickable hashtags (gold, bold).
 * onHashtag receives the tag WITHOUT the leading '#'.
 */
@Composable
fun HashtagCaption(
    caption: String,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White,
    fontSize: Int = 14,
    onHashtag: (String) -> Unit
) {
    if (caption.isBlank()) return

    val annotated = buildAnnotatedString {
        val regex = Regex("#([A-Za-z0-9_]+)")
        var lastIndex = 0
        regex.findAll(caption).forEach { match ->
            // Text before hashtag
            if (match.range.first > lastIndex) {
                append(caption.substring(lastIndex, match.range.first))
            }
            val tag = match.groupValues[1]
            // Hashtag with annotation
            pushStringAnnotation(tag = "hashtag", annotation = tag)
            withStyle(SpanStyle(color = Gold, fontWeight = FontWeight.Bold)) {
                append(match.value)
            }
            pop()
            lastIndex = match.range.last + 1
        }
        // Trailing text
        if (lastIndex < caption.length) {
            append(caption.substring(lastIndex))
        }
    }

    ClickableText(
        text = annotated,
        style = TextStyle(color = textColor, fontSize = fontSize.sp),
        modifier = modifier,
        onClick = { offset ->
            annotated.getStringAnnotations(
                tag = "hashtag",
                start = offset,
                end = offset
            ).firstOrNull()?.let { ann -> onHashtag(ann.item) }
        }
    )
}
