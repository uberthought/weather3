package com.example.weather

import android.app.ActionBar
import android.content.res.TypedArray
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Dimension
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.weather.databinding.DetailedConditionsFragmentBinding
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.detailed_conditions_fragment.view.*

class DetailedConditionsFragment : Fragment() {
    private val logTag = javaClass.kotlin.simpleName

    private lateinit var viewModel: DetailedConditionsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

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
            if (viewModel.details.value?.icon != null)
                Picasso.get().load(viewModel.details.value?.icon).into(binding.icon)
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
                applyTo(it);
            }
        }
    }

    fun onClick(view:View) {
        if (tag == "PortraitDetailedConditionsFragment")
            activity?.onBackPressed()
    }
}
