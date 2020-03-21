package com.companyname.weather.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.companyname.weather.databinding.ForecastFragmentBinding
import com.companyname.weather.viewModels.ConditionsViewModel
import com.companyname.weather.viewModels.LocationViewModel

class ForecastFragment: Fragment() {

    lateinit var locationViewModel: LocationViewModel
    lateinit var conditionsViewModel: ConditionsViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        locationViewModel = ViewModelProvider(this)[LocationViewModel::class.java]
        conditionsViewModel = ViewModelProvider(this)[ConditionsViewModel::class.java]

        val binding = ForecastFragmentBinding.inflate(layoutInflater)
        binding.lifecycleOwner = this
        binding.locationViewModel = locationViewModel
        binding.conditionsViewModel = conditionsViewModel
        locationViewModel.location.observe(viewLifecycleOwner, androidx.lifecycle.Observer {location ->
            (activity as AppCompatActivity).supportActionBar?.title = location
        })
        conditionsViewModel.details.observe(viewLifecycleOwner, androidx.lifecycle.Observer { binding.timestamp.invalidate() })

        return binding.root
    }
}