package com.ntu.electricity.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

private const val TAG = "UserPrefs"

class UserPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ntu_ecocheck_config", Context.MODE_PRIVATE)

    var studentId: String
        get() = prefs.getString(KEY_STUDENT_ID, "") ?: ""
        set(value) { prefs.edit().putString(KEY_STUDENT_ID, value).commit() }

    var password: String
        get() = prefs.getString(KEY_PASSWORD, "") ?: ""
        set(value) { prefs.edit().putString(KEY_PASSWORD, value).commit() }

    var campus: String
        get() = prefs.getString(KEY_CAMPUS, "") ?: ""
        set(value) { prefs.edit().putString(KEY_CAMPUS, value).commit() }

    var building: String
        get() = prefs.getString(KEY_BUILDING, "") ?: ""
        set(value) { prefs.edit().putString(KEY_BUILDING, value).commit() }

    var room: String
        get() = prefs.getString(KEY_ROOM, "") ?: ""
        set(value) { prefs.edit().putString(KEY_ROOM, value).commit() }

    var rememberSelection: Boolean
        get() = prefs.getBoolean(KEY_REMEMBER, true)
        set(value) { prefs.edit().putBoolean(KEY_REMEMBER, value).commit() }

    var colorIndex: Int
        get() = prefs.getInt(KEY_COLOR_INDEX, 0)
        set(value) { prefs.edit().putInt(KEY_COLOR_INDEX, value).commit() }

    fun saveAll(
        studentId: String,
        password: String,
        campus: String,
        building: String,
        room: String,
        rememberSelection: Boolean
    ) {
        Log.d(TAG, "saveAll: sid=$studentId, campus=$campus, building=$building, room=$room, remember=$rememberSelection")
        prefs.edit()
            .putString(KEY_STUDENT_ID, studentId)
            .putString(KEY_PASSWORD, password)
            .putString(KEY_CAMPUS, campus)
            .putString(KEY_BUILDING, building)
            .putString(KEY_ROOM, room)
            .putBoolean(KEY_REMEMBER, rememberSelection)
            .commit()
        // Verify
        Log.d(TAG, "saveAll verify: sid=${this.studentId}, campus=${this.campus}, remember=${this.rememberSelection}")
    }

    fun logCurrentState() {
        Log.d(TAG, "Current state: sid=$studentId, pwd=${password.take(2)}**, campus=$campus, building=$building, room=$room, remember=$rememberSelection")
    }

    companion object {
        private const val KEY_STUDENT_ID = "student_id"
        private const val KEY_PASSWORD = "password"
        private const val KEY_CAMPUS = "campus"
        private const val KEY_BUILDING = "building"
        private const val KEY_ROOM = "room"
        private const val KEY_REMEMBER = "remember_selection"
        private const val KEY_COLOR_INDEX = "color_index"
    }
}
