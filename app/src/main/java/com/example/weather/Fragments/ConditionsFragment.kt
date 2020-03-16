package com.example.weather

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.weather.databinding.ConditionsCardBinding
import com.example.weather.databinding.ConditionsCardBinding.inflate
import com.example.weather.databinding.ConditionsFragmentBinding
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.conditions_fragment.*
import kotlinx.android.synthetic.main.conditions_fragment.view.*
import kotlinx.android.synthetic.main.main_activity.*

class ConditionsFragment: Fragment() {
    private val logTag = javaClass.kotlin.simpleName

    private lateinit var locationViewModel: LocationViewModel
    private lateinit var conditionsViewModel: ConditionsViewModel
    private lateinit var forecastViewModel: ForecastViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        locationViewModel = ViewModelProvider(this)[LocationViewModel::class.java]
        conditionsViewModel = ViewModelProvider(this)[ConditionsViewModel::class.java]
        forecastViewModel = ViewModelProvider(this)[ForecastViewModel::class.java]
        val binding = ConditionsFragmentBinding.inflate(layoutInflater)
        binding.lifecycleOwner = this
        binding.locationViewModel = locationViewModel
        binding.conditionsViewModel = conditionsViewModel

        val recyclerView = binding.root.recyclerView as RecyclerView
        val itemDecor = DividerItemDecoration(context, LinearLayoutManager.VERTICAL)
        recyclerView.addItemDecoration(itemDecor)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        val adapter = ConditionsAdapter()
        recyclerView.adapter = adapter

        locationViewModel.location.observe(viewLifecycleOwner, Observer { location.invalidate() })
        conditionsViewModel.details.observe(viewLifecycleOwner, Observer { adapter.notifyDataSetChanged() })
        forecastViewModel.forecasts.observe(viewLifecycleOwner, Observer { adapter.notifyDataSetChanged() })

        return binding.root
    }

    inner class ConditionsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val binding = inflate(inflater)
            val viewHolder = ViewHolder(binding)
            val lp = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            viewHolder.itemView.layoutParams = lp
            return viewHolder
        }

        override fun getItemCount(): Int {
            var count = 0
            if (conditionsViewModel.details.value != null) count++
            if (forecastViewModel.forecasts.value != null) count += forecastViewModel.forecasts.value!!.count()
            return count
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, index: Int) {
            val viewHolder = holder as ViewHolder
            viewHolder.bind(index)
        }

        inner class ViewHolder(private val binding: ConditionsCardBinding) : RecyclerView.ViewHolder(binding.root) {

            private var index: Int = -1
            lateinit var details: DetailedConditionsViewModel.Details

            fun bind(index: Int) {
                this.index = index

                details = when {
                    conditionsViewModel.details.value != null && index == 0 -> conditionsViewModel.details.value!!
                    conditionsViewModel.details.value == null -> forecastViewModel.forecasts.value!![index]
                    forecastViewModel.forecasts.value != null -> forecastViewModel.forecasts.value!![index - 1]
                    else -> DetailedConditionsViewModel.Details()
                }

                val viewModel = DetailedConditionsViewModel()
                viewModel.details.value = details
                binding.viewModel = viewModel
                binding.viewHolder = this

                if (details.icon != null)
                    Picasso.get().load(details.icon!!).into(binding.icon)
            }

            fun onClick(view:View) {
                val activity = this@ConditionsFragment.activity
                activity ?: return

                var parentView = this@ConditionsFragment.view
                while(parentView != null && parentView.tag != "PortraitConditionsFragment")
                    parentView = parentView.parent as? View

                val landscapeFragment = activity.supportFragmentManager.findFragmentByTag("LandscapeDetailedConditionsFragment")
                val portraitFragment = activity.supportFragmentManager.findFragmentByTag("PortraitDetailedConditionsFragment")

                val fragment = if (parentView == null) landscapeFragment
                    else portraitFragment ?: DetailedConditionsFragment().let {
                        val manager = activity.supportFragmentManager
                        val transaction = manager.beginTransaction()
                        transaction.add(R.id.mainPortraitFragment, it, "PortraitDetailedConditionsFragment")
                        transaction.addToBackStack(null)
                        transaction.commit()
                        manager.executePendingTransactions()
                        it
                    }

                val detailedViewModel = ViewModelProvider(fragment!!)[DetailedConditionsViewModel::class.java]
                detailedViewModel.details.postValue(details)
            }
        }
    }
}