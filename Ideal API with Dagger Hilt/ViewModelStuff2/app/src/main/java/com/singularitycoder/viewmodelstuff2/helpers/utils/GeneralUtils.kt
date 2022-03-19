package com.singularitycoder.viewmodelstuff2.helpers.utils

import android.content.Context
import android.content.res.Resources
import android.graphics.Point
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.annotation.DrawableRes
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import com.singularitycoder.viewmodelstuff2.MainActivity
import com.singularitycoder.viewmodelstuff2.R
import com.singularitycoder.viewmodelstuff2.anime.model.AnimeErrorResponse
import com.singularitycoder.viewmodelstuff2.databinding.LayoutCustomToastBinding
import com.singularitycoder.viewmodelstuff2.helpers.extensions.dpToPx
import com.singularitycoder.viewmodelstuff2.helpers.extensions.isNullOrBlankOrNaOrNullString
import com.singularitycoder.viewmodelstuff2.more.model.AboutMeErrorResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Retrofit
import timber.log.Timber
import java.lang.reflect.Type
import javax.inject.Inject
import kotlin.reflect.KClass

// Easy Memory Leaks - Static context

// This Utils class should not be a singleton as it deals with context stuff. Move to Activity Module
class GeneralUtils @Inject constructor(
    val retrofit: Retrofit,
    val gson: Gson
) : Cloneable {

    @Throws(InstantiationException::class)
    private fun utils() {
        throw InstantiationException("This class is not for instantiation")
    }

    // https://www.geeksforgeeks.org/marker-interface-java/
    // https://stackoverflow.com/questions/49053432/how-to-clone-object-in-kotlin
    override fun clone(): Any = super.clone()

    fun getErrorMessage(context: Context, error: Any?): String {
        val somethingIsWrong = context.getString(R.string.something_is_wrong)
        try {
            val parentJsonObj = (error as? JSONObject) ?: return somethingIsWrong
            val childJsonObj = parentJsonObj.getJSONObject("error")
            val errorCode = childJsonObj.getString("code")
            val message = childJsonObj.getString("message") ?: ""
            return if (message.isNullOrBlankOrNaOrNullString()) somethingIsWrong else message
        } catch (e: JsonParseException) {
            Timber.e("getErrorMessage: ${e.localizedMessage}")
            return somethingIsWrong
        }
    }

    fun getErrorMessageWithGson(context: Context, error: Any?, gson: Gson): String {
        val somethingIsWrong = context.getString(R.string.something_is_wrong)
        try {
            val parentJsonObj = (error as? JSONObject) ?: return somethingIsWrong
            val errorResponse = gson.fromJson(parentJsonObj.toString(), AnimeErrorResponse::class.java) ?: return somethingIsWrong
            val errorCode = errorResponse.error.errorCode
            val message = errorResponse.error.message
            return if (message.isNullOrBlankOrNaOrNullString()) somethingIsWrong else message
        } catch (e: Exception) {
            Timber.e("getErrorMessageWithGson: ${e.localizedMessage}")
            return somethingIsWrong
        }
    }

    /**
     * To generate error, change the version of the API to a random number.
     * I am using inner functions here to avoid allocating permanent memory.
     * Theoretically this should improve perf as all ops happen in call stack and memory is released when ops are done.
     * This impacts breakpoint debugging a lot. It gets damn slow
     * TODO Should measure before and after
     * */
    fun <T> getErrorMessageWithRetrofit(context: Context, errorResponseBody: ResponseBody?): String {
        fun getDefaultMessage(): String = context.getString(R.string.something_is_wrong)
        try {
            errorResponseBody ?: return getDefaultMessage()
            fun getErrorResponse(): T? = retrofit.responseBodyConverter<T>(KClass::class.java, arrayOf()).convert(errorResponseBody)
            fun getMessage(): String = when (getErrorResponse()) {
                is AnimeErrorResponse -> (getErrorResponse() as? AnimeErrorResponse)?.error?.message ?: getDefaultMessage()
                is AboutMeErrorResponse -> (getErrorResponse() as? AboutMeErrorResponse)?.message ?: getDefaultMessage()
                else -> getDefaultMessage()
            }
            return if (getMessage().isNullOrBlankOrNaOrNullString()) getDefaultMessage() else getMessage()
        } catch (e: Exception) {
            Timber.e("getErrorMessageWithRetrofit: ${e.localizedMessage}")
            return getDefaultMessage()
        }
    }

    fun getDeviceSize(): Point = try {
        val deviceWidth = Resources.getSystem().displayMetrics.widthPixels
        val deviceHeight = Resources.getSystem().displayMetrics.heightPixels
        Point(deviceWidth, deviceHeight)
    } catch (e: Exception) {
        e.printStackTrace()
        Point(0, 0)
    }

    fun <T> asyncLog(message: String?, vararg objs: T) = CoroutineScope(Default).launch {
        message ?: return@launch
        try {
//            Timber.i(message, objs.map { it: T -> gson.toJson(it) })  // TODO Implement Timber
            println("$message: ${gson.toJson(objs[0])}")
        } catch (e: Exception) {
            Timber.e("Gson parse error: $e")
        }
    }

    fun error(): Nothing = throw IllegalStateException("Shouldn't be here") // Always throw an exception

    fun showSnackBar(
        view: View,
        message: String,
        anchorView: View? = null,
        duration: Int = Snackbar.LENGTH_SHORT,
        actionBtnText: String = "NA",
        action: () -> Unit = {}
    ) {
        Snackbar.make(view, message, duration).apply {
            this.animationMode = BaseTransientBottomBar.ANIMATION_MODE_SLIDE
            if (null != anchorView) this.anchorView = anchorView
            if ("NA" != actionBtnText) setAction(actionBtnText) { action.invoke() }
            this.show()
        }
    }

    inline fun <reified T : Any> listToString(list: ArrayList<T>?): String? {
        if (null == list) return null
        val type: Type = object : TypeToken<ArrayList<T>?>() {}.type
        return gson.toJson(list, type)
    }

    inline fun <reified T : Any> stringToList(string: String?): ArrayList<T>? {
        if (null == string) return null
        val type: Type = object : TypeToken<ArrayList<T>?>() {}.type
        return gson.fromJson<ArrayList<T>?>(string, type)
    }

    inline fun <reified T : Any> objectToString(obj: T?): String? {
        obj ?: return null
        return gson.toJson(obj)
    }

    inline fun <reified T : Any> stringToObject(string: String?): T? {
        string ?: return null
        return gson.fromJson(string, T::class.java)
    }

    private var toast: Toast? = null
    // https://stackoverflow.com/questions/32815407/is-it-normal-to-show-a-single-toast-multiple-times-on-android
    @ExperimentalCoroutinesApi
    fun showToast(
        message: String,
        context: Context,
        duration: Int = Toast.LENGTH_LONG,
        @DrawableRes textImage: Int = R.drawable.ic_android_black_24dp,
    ) {
        try {
            if ((context as? MainActivity)?.isFinishing == true) return
            if (message.isBlank()) return
            if (null != toast) toast?.cancel()

            val binding = LayoutCustomToastBinding.inflate(LayoutInflater.from(context)).apply {
                tvCustomToastText.apply {
                    text = message
                    setCompoundDrawablesWithIntrinsicBounds(textImage, 0, 0, 0)
                    compoundDrawablePadding = 12
                }
            }

            toast = Toast(context).apply {
                view = binding.root
                this.duration = duration
                setGravity(Gravity.BOTTOM, 0, 16.dpToPx())
            }
            toast?.show()
        } catch (e: Exception) {
            Timber.e("Something is wrong with the toast: $e")
        }
    }
}

// https://stackoverflow.com/questions/51299641/difference-of-setvalue-postvalue-in-mutablelivedata
// https://kinnrot.github.io/live-data-pitfall-you-should-be-aware-of/
// setValue(): Sets the value. If there are active observers, the value will be dispatched to them. This method must be called from the main thread.
// postValue(): Posts a task to a main thread to set the given value. If you called this method multiple times before a main thread executed a posted task, only the last value would be dispatched.
// Also calling get value too quickly just after postValue might get you old data
suspend fun wait(duration: Long = 500L) = delay(timeMillis = duration)

fun waitFor(duration: Long) = SystemClock.sleep(duration)

val timeNow: Long
    get() = System.currentTimeMillis()

// https://github.com/mslalith/focus_launcher
fun doAfter(duration: Long, task: () -> Unit) {
    Handler(Looper.getMainLooper()).postDelayed(task, duration)
}

