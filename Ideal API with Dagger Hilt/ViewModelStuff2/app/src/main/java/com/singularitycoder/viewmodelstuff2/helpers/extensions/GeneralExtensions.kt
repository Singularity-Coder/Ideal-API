package com.singularitycoder.viewmodelstuff2.helpers.extensions

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.annotation.RawRes
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import com.google.gson.Gson
import com.singularitycoder.viewmodelstuff2.R
import com.singularitycoder.viewmodelstuff2.helpers.constants.DateType
import com.singularitycoder.viewmodelstuff2.helpers.utils.timeNow
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * https://stackoverflow.com/questions/14389349/android-get-current-locale-not-default
 * ISO 639 is not a stable standard — some languages' codes have changed. Locale's constructor recognizes both
 * the new and the old codes for the languages whose codes have changed, but this function always returns the old code.
 * If you want to check for a specific language whose code has changed,
 * don't do if (locale.getLanguage().equals("he")) // BAD! Instead, do if (locale.getLanguage().equals(new Locale("he").getLanguage()))
 *
 * Sample impl - context.getCurrentLocale()?.language == Locale("en").language
 * */
fun Context?.getCurrentLocale(): Locale? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
    this?.resources?.configuration?.locales?.get(0)
} else {
    this?.resources?.configuration?.locale
}

fun String?.isNullOrBlankOrNaOrNullString(): Boolean = this.isNullOrBlank() || "null" == this.toLowCase().trim() || "na" == this.toLowCase().trim()

fun String.toLowCase(): String = this.lowercase(Locale.getDefault())

fun String.toUpCase(): String = this.uppercase(Locale.getDefault())

fun String.capFirstChar(): String = this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

fun String.trimNewLines(): String = this.replace(oldValue = System.getProperty("line.separator") ?: "\n", newValue = " ")

// Works on Windows, Linux and Mac
// https://stackoverflow.com/questions/11048973/replace-new-line-return-with-space-using-regex
// https://javarevisited.blogspot.com/2014/04/how-to-replace-line-breaks-new-lines-windows-mac-linux.html
fun String.trimNewLinesUniversally(): String = this.replace(regex = Regex(pattern = "[\\t\\n\\r]+"), replacement = " ")

fun String.trimIndentsAndNewLines(): String = this.trimIndent().trimNewLinesUniversally()

fun Gson.toJSONObject(obj: Any): JSONObject? = try {
    JSONObject().get(this.toJson(obj)) as? JSONObject
} catch (e: JSONException) {
    Timber.e(e)
    null
} catch (e: Exception) {
    Timber.e(e)
    null
}

infix fun Long.toTimeOfType(type: DateType): String {
    val date = Date(this)
    val dateFormat = SimpleDateFormat(type.value, Locale.getDefault())
    return dateFormat.format(date)
}

// https://stackoverflow.com/questions/22741202/how-to-use-goasync-for-broadcastreceiver
/**  Run work asynchronously from a [BroadcastReceiver] */
fun BroadcastReceiver.goAsync(
    coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    work: suspend () -> Unit
) {
    val pendingResult = goAsync()
    coroutineScope.launch(dispatcher) {
        work()
        pendingResult.finish()
    }
}

fun String.trimJunk() = this
    .replace("<br>", "")
    .replace("</br>", "")
    .replace("<i>", "")
    .replace("</i>", "")
    .trimIndentsAndNewLines()

fun Context.showPermissionSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", this@showPermissionSettings.packageName, null)
    }
    startActivity(intent)
}

fun Context.hasContactPermission(): Boolean = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED

// https://stackoverflow.com/questions/1109022/how-do-you-close-hide-the-android-soft-keyboard-programmatically
fun EditText?.hideKeyboard() {
    if (this?.hasFocus() == true) {
        val imm = this.context?.getSystemService(Activity.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(this.windowToken, 0)
    }
}

fun Activity.hideKeyboard() {
    val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    // Find the currently focused view, so we can grab the correct window token from it.
    var view = currentFocus
    // If no view currently has focus, create a new one, just so we can grab a window token from it
    if (view == null) {
        view = View(this)
    }
    imm.hideSoftInputFromWindow(view.windowToken, 0)
}

// https://stackoverflow.com/questions/5105354/how-to-show-soft-keyboard-when-edittext-is-focused
fun EditText?.showKeyboard() {
    if (this?.hasFocus() == true) {
        val imm = this.context?.getSystemService(Activity.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }
}

fun Context.getRawPathOf(@RawRes video: Int) = "android.resource://$packageName/$video"

fun Context.getHtmlFormattedQuote(quote: String, author: String): String {
    val html = """
                <font color=${color(R.color.white)}>
                <body>${quote}</body>
                <br />
                <br />
                <small>${author}</small>
                """.trimIndentsAndNewLines()
    return HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
}

fun Long.toIntuitiveDateTime(): String {
    val postedTime = this
    val elapsedTimeMillis = timeNow - postedTime
    val elapsedTimeInSeconds = TimeUnit.MILLISECONDS.toSeconds(elapsedTimeMillis)
    val elapsedTimeInMinutes = TimeUnit.MILLISECONDS.toMinutes(elapsedTimeMillis)
    val elapsedTimeInHours = TimeUnit.MILLISECONDS.toHours(elapsedTimeMillis)
    val elapsedTimeInDays = TimeUnit.MILLISECONDS.toDays(elapsedTimeMillis)
    val elapsedTimeInMonths = elapsedTimeInDays / 30
    return when {
        elapsedTimeInSeconds < 60 -> "Now"
        elapsedTimeInMinutes == 1L -> "$elapsedTimeInMinutes Minute ago"
        elapsedTimeInMinutes < 60 -> "$elapsedTimeInMinutes Minutes ago"
        elapsedTimeInHours == 1L -> "$elapsedTimeInHours Hour ago"
        elapsedTimeInHours < 24 -> "$elapsedTimeInHours Hours ago"
        elapsedTimeInDays == 1L -> "$elapsedTimeInDays Day ago"
        elapsedTimeInDays < 30 -> "$elapsedTimeInDays Days ago"
        elapsedTimeInMonths == 1L -> "$elapsedTimeInMonths Month ago"
        elapsedTimeInMonths < 12 -> "$elapsedTimeInMonths Months ago"
        else -> postedTime toTimeOfType DateType.dd_MMM_yyyy_hh_mm_a
    }
}

fun Timer.doEvery(
    duration: Long,
    withInitialDelay: Long = 2.seconds(),
    task: suspend () -> Unit
) = scheduleAtFixedRate(
    object : TimerTask() {
        override fun run() {
            CoroutineScope(IO).launch { task.invoke() }
        }
    },
    withInitialDelay,
    duration
)

fun Int.seconds(): Long = TimeUnit.SECONDS.toMillis(this.toLong())

fun Int.minutes(): Long = TimeUnit.MINUTES.toMillis(this.toLong())

fun Int.hours(): Long = TimeUnit.HOURS.toMillis(this.toLong())
