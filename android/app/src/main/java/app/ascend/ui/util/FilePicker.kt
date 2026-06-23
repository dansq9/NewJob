package app.ascend.ui.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** Allowed resume MIME types (PDF, DOCX, DOC). */
val RESUME_MIME = arrayOf(
    "application/pdf",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/msword",
)

data class PickedFile(val uri: Uri, val name: String, val sizeBytes: Long?, val mime: String?)

/** Returns a launcher that opens the SAF document picker for resume files. */
@Composable
fun rememberResumePicker(onPicked: (PickedFile) -> Unit): () -> Unit {
    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                ctx.contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            onPicked(queryFile(ctx, uri))
        }
    }
    return remember(launcher) { { runCatching { launcher.launch(RESUME_MIME) } } }
}

fun queryFile(ctx: Context, uri: Uri): PickedFile {
    var name = uri.lastPathSegment ?: "resume"
    var size: Long? = null
    runCatching {
        ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                c.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { name = c.getString(it) ?: name }
                c.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 }?.let { if (!c.isNull(it)) size = c.getLong(it) }
            }
        }
    }
    return PickedFile(uri, name, size, ctx.contentResolver.getType(uri))
}
