package com.bytebench.app.tools

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.bytebench.app.ui.Actions
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** A finished file from a tool: a converted image, a PDF page, an invoice… */
class OutFile(val name: String, val mime: String, val bytes: ByteArray)

private class CreateDocument : ActivityResultContract<Pair<String, String>, Uri?>() {
    override fun createIntent(context: Context, input: Pair<String, String>): Intent =
        Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(input.second)
            .putExtra(Intent.EXTRA_TITLE, input.first)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        if (resultCode == Activity.RESULT_OK) intent?.data else null
}

/** Saves files with the system "Save to…" picker, or shares them to other apps. */
class FileSaver(private val context: Context, private val launch: (OutFile) -> Unit) {
    fun save(file: OutFile) = launch(file)

    /** Several files are saved as one ZIP, like the website's "Download all". */
    fun saveAll(files: List<OutFile>, zipName: String) {
        if (files.size == 1) save(files[0]) else save(OutFile(zipName, "application/zip", zip(files)))
    }

    fun share(files: List<OutFile>) {
        if (files.isEmpty()) return
        val dir = File(context.cacheDir, "shared").apply { mkdirs(); listFiles()?.forEach { it.delete() } }
        val uris = ArrayList(files.map { f ->
            val out = File(dir, f.name).apply { writeBytes(f.bytes) }
            FileProvider.getUriForFile(context, context.packageName + ".files", out)
        })
        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).setType(files[0].mime).putExtra(Intent.EXTRA_STREAM, uris[0])
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).setType(if (files.all { it.mime.startsWith("image/") }) "image/*" else "*/*")
                .putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    companion object {
        fun zip(files: List<OutFile>): ByteArray {
            val bos = ByteArrayOutputStream()
            ZipOutputStream(bos).use { z ->
                val used = HashSet<String>()
                files.forEach { f ->
                    var name = f.name
                    var i = 2
                    while (!used.add(name)) name = f.name.substringBeforeLast('.') + "-${i++}." + f.name.substringAfterLast('.')
                    z.putNextEntry(ZipEntry(name)); z.write(f.bytes); z.closeEntry()
                }
            }
            return bos.toByteArray()
        }
    }
}

@Composable
fun rememberFileSaver(): FileSaver {
    val context = LocalContext.current
    val pending = remember { arrayOfNulls<OutFile>(1) }
    val launcher = rememberLauncherForActivityResult(CreateDocument()) { uri ->
        val f = pending[0]
        pending[0] = null
        if (uri != null && f != null) {
            runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(f.bytes) } }
                .onSuccess { Actions.toast(context, "Saved ${f.name}") }
                .onFailure { Actions.toast(context, "Couldn’t save the file.") }
        }
    }
    return remember(launcher) {
        FileSaver(context) { f ->
            pending[0] = f
            launcher.launch(f.name to f.mime)
        }
    }
}
