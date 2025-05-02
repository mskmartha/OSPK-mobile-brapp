package com.albertsons.acupick.ui.bottomsheetdialog

import android.app.Application
import androidx.databinding.BindingAdapter
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.dialog.CloseAction
import com.hadilq.liveevent.LiveEvent
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Section

class ActionSheetBottomSheetViewModel(
    val app: Application,
) : BaseViewModel(app) {

    val options = mutableListOf<ActionSheetOptions>()
    val navigation: LiveData<Pair<CloseAction, Int?>> = LiveEvent()

    fun onOptionsClicked(optionsClicked: ActionSheetOptions) {
        val selectedIndex = options.indexOf(optionsClicked)
        navigation.postValue(Pair(CloseAction.Positive, selectedIndex))
    }

    fun onCancelClicked() {
        navigation.postValue(Pair(CloseAction.Negative, -1))
    }
}

@BindingAdapter(value = ["app:actionSettings", "app:viewModel"], requireAll = false)
fun RecyclerView.setSettings(data: HashMap<Int, Int>?, viewModel: ActionSheetBottomSheetViewModel?) {
    // Exit if not all information provided yet.
    if (viewModel == null) return

    @Suppress("UNCHECKED_CAST")
    layoutManager = LinearLayoutManager(context)
    adapter = GroupAdapter<GroupieViewHolder>().apply { add(generateSection(viewModel)) }
}

private fun generateSection(viewModel: ActionSheetBottomSheetViewModel): Section {
    val data = viewModel.options
    return Section().apply {
        update(
            data.map { settings ->
                ActionSheetDbViewModel(
                    settings,
                    viewModel::onOptionsClicked
                )
            }
        )
    }
}
