package com.bytebench.app.tools

import androidx.compose.runtime.Composable
import com.bytebench.app.ui.Note

/** Shows the interactive part of a free tool. */
@Composable
fun ToolUi(slug: String) {
    Note("This tool is coming in the next build ($slug).")
}
