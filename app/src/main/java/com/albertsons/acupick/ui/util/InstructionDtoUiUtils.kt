package com.albertsons.acupick.ui.util

import android.content.Context
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.response.InstructionDto

/**
 * Customer comment limited to [MAX_USER_COMMENT_CHARACTERS]. After that amount, there is a hard cut and an ellipsis character is appended.
 * If less than [MAX_USER_COMMENT_CHARACTERS], [InstructionDto.text] is passed through unaltered (except for null being converted to "")
 */
fun InstructionDto?.displayText(context: Context): String {
    val comment = this?.text.orEmpty().trim()
    val suffix = if (comment.length > MAX_USER_COMMENT_CHARACTERS) context.getString(R.string.ellipsis_character) else ""
    // Takes first 60 trimmed characters, trims the end (to remove any whitespace between end of string and ellipsis), and add an ellipsis character if the original trimmed text was > 60 characters
    return "${comment.take(MAX_USER_COMMENT_CHARACTERS).trimEnd()}$suffix"
}

private const val MAX_USER_COMMENT_CHARACTERS = 120
