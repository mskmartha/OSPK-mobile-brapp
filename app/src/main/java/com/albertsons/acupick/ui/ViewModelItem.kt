package com.albertsons.acupick.ui

import android.annotation.SuppressLint
import android.view.View
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LifecycleOwner
import com.albertsons.acupick.ui.util.resourceIdName
import com.xwray.groupie.Item
import com.xwray.groupie.viewbinding.BindableItem
import timber.log.Timber
import kotlin.reflect.full.cast
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubtypeOf

class ViewModelItem<T : BaseBindableViewModel>(val viewModel: T, @LayoutRes private val _layout: Int, private val lifecycleOwner: LifecycleOwner? = null) : BindableItem<ViewDataBinding>() {

    override fun initializeViewBinding(view: View): ViewDataBinding {
        return DataBindingUtil.bind(view)!!
    }

    @SuppressLint("BinaryOperationInTimber")
    override fun bind(viewBinding: ViewDataBinding, position: Int) {
        lifecycleOwner?.let {
            viewBinding.lifecycleOwner = it
        }
        val members = viewBinding::class.members
        val setViewModels = members.filter {
            (it.name == "setViewModel" || it.name == "setDbViewModel") &&
                it.parameters.size == 2 &&
                it.parameters[0].type.isSubtypeOf(viewBinding::class.createType()) &&
                it.parameters[1].type.isSubtypeOf(viewModel::class.createType())
        }
        if (setViewModels.size == 1) {
            setViewModels.first().call(viewBinding, viewModel)
            viewBinding.executePendingBindings()
        } else {
            Timber.e(
                "Expected ${viewBinding::class.qualifiedName} to have exactly one member function setViewModel OR setDbViewModel - " +
                    "ensure that the ${_layout.resourceIdName(viewBinding.root.resources)} xml layout databinding data variable entry is named viewModel or dbViewModel"
            )
        }
    }

    override fun getLayout(): Int {
        return _layout
    }

    override fun isSameAs(other: Item<*>): Boolean {
        return when {
            super.isSameAs(other) -> true
            other::class == this::class -> (this::class.cast(other).viewModel == this.viewModel)
            else -> false
        }
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is Item<*> -> isSameAs(other)
            else -> super.equals(other)
        }
    }

    override fun hashCode(): Int {
        var result = viewModel.hashCode()
        result = 31 * result + _layout
        return result
    }
}
