/*
 * Copyright 2018, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gregcorp.trackmysleepquality.sleeptracker

import android.app.Application
//import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.gregcorp.trackmysleepquality.database.SleepDatabaseDao
import com.gregcorp.trackmysleepquality.database.SleepNight
//import com.gregcorp.trackmysleepquality.formatNights
import kotlinx.coroutines.*

/**
 * ViewModel for SleepTrackerFragment.
 */
class SleepTrackerViewModel(val database: SleepDatabaseDao,
                            application: Application) : AndroidViewModel(application) {

    /**
     * viewModelJob allows us to cancel all coroutines started by this ViewModel.
     */
    private var viewModelJob = Job()

    /**
     * A [CoroutineScope] keeps track of all coroutines started by this ViewModel.
     *
     * Because we pass it [viewModelJob], any coroutine started in this uiScope can be cancelled
     * by calling `viewModelJob.cancel()`
     *
     * By default, all coroutines started in uiScope will launch in [Dispatchers.Main] which is
     * the main thread on Android. This is a sensible default because most coroutines started by
     * a ViewModel update the UI after performing some processing.
     */
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    private var tonight = MutableLiveData<SleepNight?>()

    val nights = database.getAllNights()

    // if (tonight == null) => startButtonVisible = true
    val startButtonVisible = Transformations.map(tonight) {
        null == it
    }

    // if (tonight != null) => stopButtonVisible = true
    val stopButtonVisible = Transformations.map(tonight) {
        null != it
    }

    // if (nights.isNotEmpty()) => clearButtonVisible = true
    val clearButtonVisible = Transformations.map(nights) {
        it?.isNotEmpty()
    }

    private var _showSnackbarEvent = MutableLiveData<Boolean>()
    val showSnackbarEvent: LiveData<Boolean>
        get() = _showSnackbarEvent
    fun doneShowingSnackbar() {
        _showSnackbarEvent.value = false
    }

    private val _navigateToSleepQuality = MutableLiveData<SleepNight>()
    val navigateToSleepQuality: LiveData<SleepNight>
        get() = _navigateToSleepQuality
    fun onStopTracking() {
        uiScope.launch {
            val oldNight = tonight.value ?: return@launch
            oldNight.endTimeMilli = System.currentTimeMillis()
            updateTheDatabase(oldNight)
            _navigateToSleepQuality.value = oldNight
        }
    }
    fun doneNavigating() {
        _navigateToSleepQuality.value = null
    }

    private val _navigateToSleepDataQuality = MutableLiveData<Int>()
    val navigateToSleepDataQuality
        get() = _navigateToSleepDataQuality
    fun onSleepNightClicked(id: Int) {
        _navigateToSleepDataQuality.value = id
    }
    fun onSleepDataQualityNavigated() {
        _navigateToSleepDataQuality.value = null
    }

    init {
//        Log.i("SleepTrackerViewModel", "viewModel init")
        initializeTonight()
    }

    private fun initializeTonight() {
        uiScope.launch {
            tonight.value = getTonightFromDatabase()
        }
    }

    private suspend fun getTonightFromDatabase(): SleepNight? {
        return withContext(Dispatchers.IO) {
            var night = database.getTonight()
            if (night?.endTimeMilli != night?.startTimeMilli) {
//                Log.i("SleepTrackerViewModel", "night = null")
                night = null
            }
            night
        }
    }

    fun onStartTracking() {
        uiScope.launch {
            val newNight = SleepNight()
            insertIntoDatabase(newNight)
            tonight.value = getTonightFromDatabase()
        }
    }

    private suspend fun insertIntoDatabase(night: SleepNight) {
        withContext(Dispatchers.IO) {
            database.insert(night)
        }
    }


    private suspend fun updateTheDatabase(night: SleepNight) {
        withContext(Dispatchers.IO) {
            database.update(night)
        }
    }

    /**
     * Executes when the CLEAR button is clicked.
     */
    fun onClear() {
        uiScope.launch {
            clearTheDatabase() // Clear the database
            tonight.value = null // Clear tonight since it's no longer in the database
            _showSnackbarEvent.value = true
        }
    }

    private suspend fun clearTheDatabase() {
        withContext(Dispatchers.IO) {
            database.clear()
        }
    }


    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}
