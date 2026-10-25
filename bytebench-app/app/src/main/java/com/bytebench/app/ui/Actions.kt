package com.bytebench.app.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.bytebench.app.data.Owner

/** Opening WhatsApp, email, links and the share sheet, plus copy-to-clipboard. */
object Actions {

    fun open(context: Context, url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            toast(context, "No app can open this link.")
        }
    }

    fun whatsapp(context: Context, owner: Owner, message: String = owner.whatsappMessage) {
        open(context, "https://wa.me/${owner.whatsapp}?text=" + Uri.encode(message))
    }

    fun email(context: Context, to: String, subject: String = "", body: String = "") {
        val uri = Uri.parse("mailto:$to?subject=" + Uri.encode(subject) + "&body=" + Uri.encode(body))
        try {
            context.startActivity(Intent(Intent.ACTION_SENDTO, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            toast(context, "No email app is set up on this phone.")
        }
    }

    fun shareText(context: Context, text: String, subject: String? = null) {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        if (subject != null) send.putExtra(Intent.EXTRA_SUBJECT, subject)
        context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun copy(context: Context, text: String, what: String = "Copied") {
        if (text.isEmpty()) return
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("Bytebench", text))
        toast(context, what)
    }

    fun toast(context: Context, text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
}
