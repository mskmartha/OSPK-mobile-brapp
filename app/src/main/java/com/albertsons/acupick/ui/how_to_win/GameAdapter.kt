package com.albertsons.acupick.ui.my_score

import android.view.View
import androidx.databinding.BindingAdapter
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.response.OthRule
import com.albertsons.acupick.databinding.AdapterGameInfoBinding
import com.albertsons.acupick.ui.models.ToteUI
import com.albertsons.acupick.ui.swapsubstitution.generateSwapSubItems
import com.xwray.groupie.GroupieAdapter
import com.xwray.groupie.viewbinding.BindableItem
import timber.log.Timber

class BasePointsViewHolder(val data: OthRule?) : BindableItem<AdapterGameInfoBinding>() {
    override fun initializeViewBinding(view: View) = AdapterGameInfoBinding.bind(view)
    override fun getLayout() = R.layout.adapter_game_info
    override fun bind(viewBinding: AdapterGameInfoBinding, position: Int) {
        viewBinding.labelName = data?.ruleName
        viewBinding.gamePoints = viewBinding.textView.context.getString(R.string.lbl_base_pts,(data?.score ?: 0).toString())
    }
}

class OTHViewHolder(val data: OthRule?) : BindableItem<AdapterGameInfoBinding>() {
    override fun initializeViewBinding(view: View) = AdapterGameInfoBinding.bind(view)
    override fun getLayout() = R.layout.adapter_game_info
    override fun bind(viewBinding: AdapterGameInfoBinding, position: Int) {
        viewBinding.labelName = data?.ruleName
        viewBinding.gamePoints = viewBinding.textView.context.getString(R.string.lbl_other_pts,(data?.score ?: 0).toString())
    }
}

@BindingAdapter(value = ["setItems","app:itemViewType","app:setViewModel", "app:fragmentViewLifecycleOwner"], requireAll = false)
fun RecyclerView.setGameAdapter(items: List<OthRule>?, type:String?, vm:MyScoreViewModel?,
                                fragmentViewLifecycleOwner: LifecycleOwner? = null){
    if (vm == null || type == null) return


    layoutManager = LinearLayoutManager(context)

   /* val list = when (type) {
        vm.BASE_POINTS -> {
            vm.basePoints.value ?: emptyList()
        }
        vm.OTH_STORE -> {
            vm.othStore.value?.sortedBy { it.displayOrder } ?: emptyList()
        }
        else -> {
            vm.othRule5.value?.sortedBy { it.displayOrder } ?: emptyList()
        }
    }

    adapter =
        GroupieAdapter().apply {
            list.forEach {
                when (type) {
                    vm.BASE_POINTS -> {
                        add(BasePointsViewHolder(it))

                    }
                    else -> {
                        add(OTHViewHolder(it))
                    }
                }
            }
        }*/
}