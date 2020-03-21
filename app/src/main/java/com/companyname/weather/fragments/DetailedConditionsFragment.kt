package com.companyname.weather.fragments

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.companyname.weather.viewModels.ConditionsViewModel
import com.companyname.weather.viewModels.DetailedConditionsViewModel
import com.companyname.weather.R
import com.companyname.weather.services.FileCachingService
import com.companyname.weather.databinding.DetailedConditionsFragmentBinding

class DetailedConditionsFragment : Fragment() {
    private lateinit var viewModel: DetailedConditionsViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = ViewModelProvider(this)[DetailedConditionsViewModel::class.java]

        val binding = DetailedConditionsFragmentBinding.inflate(layoutInflater)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.fragment = this

        // if the view model isn't set, start with current conditions
        if (tag == "LandscapeDetailedConditionsFragment") {
            val conditionsViewModel = ViewModelProvider(this)[ConditionsViewModel::class.java]
            conditionsViewModel.details.observe(viewLifecycleOwner, Observer {
                viewModel.details.postValue(conditionsViewModel.details.value)
                conditionsViewModel.details.removeObservers(viewLifecycleOwner)
            })
        }

        viewModel.details.observe(viewLifecycleOwner, Observer {
            binding.invalidateAll()

            val iconUrl = viewModel.details.value?.icon?.replace("medium", resources.getString(R.string.smallIconSize))
            iconUrl?.let {
                FileCachingService.instance.getCachedFile(it, context).observe(viewLifecycleOwner, Observer { path ->
                    val bitmap = BitmapFactory.decodeFile(path)
                    binding.icon.setImageBitmap(bitmap)
                })
            }
        })

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val constraintLayout = view.parent as? ConstraintLayout
        constraintLayout?.let {
            ConstraintSet().apply {
                clone(it)
                if (tag == "PortraitDetailedConditionsFragment") {
                    connect(R.id.conditionsCard, ConstraintLayout.LayoutParams.START, R.id.startGuideline, ConstraintLayout.LayoutParams.END)
                    connect(R.id.conditionsCard, ConstraintLayout.LayoutParams.END, R.id.endGuideline, ConstraintLayout.LayoutParams.START)
                    constrainWidth(R.id.conditionsCard,  ConstraintSet.MATCH_CONSTRAINT)
                }
                else {
                    connect(R.id.conditionsCard, ConstraintLayout.LayoutParams.START, ConstraintLayout.LayoutParams.PARENT_ID, ConstraintLayout.LayoutParams.START)
                    connect(R.id.conditionsCard, ConstraintLayout.LayoutParams.END, ConstraintLayout.LayoutParams.PARENT_ID, ConstraintLayout.LayoutParams.END)
                }
                connect(R.id.conditionsCard, ConstraintLayout.LayoutParams.TOP, ConstraintLayout.LayoutParams.PARENT_ID, ConstraintLayout.LayoutParams.TOP)
                connect(R.id.conditionsCard, ConstraintLayout.LayoutParams.BOTTOM, ConstraintLayout.LayoutParams.PARENT_ID, ConstraintLayout.LayoutParams.BOTTOM)
                applyTo(it)
            }
        }
    }

    fun onClick(view:View) {
        if (tag == "PortraitDetailedConditionsFragment") {
            parentFragmentManager.beginTransaction()
                .remove(this)
                .commit()
        }
    }
}
