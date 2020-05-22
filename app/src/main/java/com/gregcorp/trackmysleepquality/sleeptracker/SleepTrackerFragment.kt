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

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.gregcorp.trackmysleepquality.R
import com.gregcorp.trackmysleepquality.database.SleepDatabase
import com.gregcorp.trackmysleepquality.databinding.FragmentSleepTrackerBinding

/**
 * A fragment with buttons to record start and end times for sleep, which are saved in
 * a database. Cumulative data is displayed in a simple scrollable TextView.
 * (Because we have not learned about RecyclerView yet.)
 */
class SleepTrackerFragment : Fragment() {

    /**
     * Called when the Fragment is ready to display content to the screen.
     *
     * This function uses DataBindingUtil to inflate R.layout.fragment_sleep_tracker.
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        // Get a reference to the binding object and inflate the fragment views.
        val binding: FragmentSleepTrackerBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_sleep_tracker, container, false)

        // Get a reference to the application context
        val application = requireNotNull(this.activity).application

        // Get a reference to the DAO of the database
        val dataSource = SleepDatabase.getInstance(application).sleepDatabaseDao

        // Create an instance of the viewModelFactory
        val viewModelFactory = SleepTrackerViewModelFactory(dataSource, application)

        // Get a reference to the ViewModel associated with this fragment.
        val sleepTrackerViewModel = ViewModelProvider(this, viewModelFactory).get(SleepTrackerViewModel::class.java)

        // Link the data between fragment and layout
        binding.sleepTrackerViewModel = sleepTrackerViewModel

        // Define a grid layout for the recyclerView
        val manager = GridLayoutManager(activity, 3)
        binding.sleepList.layoutManager = manager

        // Instantiate the adapter
        val adapter = SleepNightAdapter(
            SleepNightListener {
                sleepTrackerViewModel.onSleepNightClicked(it)
            }
        )

        // sleepList is the id of the RecyclerView in fragment_sleep_tracker.xml
        binding.sleepList.adapter = adapter

        // Observer
        sleepTrackerViewModel.navigateToSleepDataQuality.observe(this,
            Observer {
                it?.let {
                    Log.i("Debug", "nightId = $it")
                    this.findNavController().navigate(
                        SleepTrackerFragmentDirections.actionSleepTrackerFragmentToSleepDetailFragment(it)
                    )
                    sleepTrackerViewModel.onSleepDataQualityNavigated()
                }
            }
        )

        // Observer
        sleepTrackerViewModel.nights.observe(viewLifecycleOwner,
            Observer {
                it?.let {
                    adapter.addHeaderAndSubmitList(it)
                }
            }
        )

        // Specify the current fragment as the lifecycle owner of the binding.
        // This is necessary so that the binding can observe LiveData updates.
        binding.lifecycleOwner = this

        // Observer for navigate to SleepQualityFragment when STOP button is pressed
        sleepTrackerViewModel.navigateToSleepQuality.observe(this,
            Observer {
                it?.let {
                    this.findNavController().navigate(
                        SleepTrackerFragmentDirections.actionSleepTrackerFragmentToSleepQualityFragment(it.nightId)
                    )
                    sleepTrackerViewModel.doneNavigating()
                }
            }
        )

        // Observer for the snackbar
        sleepTrackerViewModel.showSnackbarEvent.observe(this,
            Observer {
                if (it == true) {
                    Snackbar.make(
                        activity!!.findViewById(android.R.id.content),
                        getString(R.string.cleared_message),
                        Snackbar.LENGTH_SHORT // How long to display the message
                    ).show()
                    sleepTrackerViewModel.doneShowingSnackbar()
                }
            }
        )

        return binding.root
    }

}
