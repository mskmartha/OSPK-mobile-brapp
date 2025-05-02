package com.albertsons.acupick.ui.custom

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.BindingAdapter
import com.albertsons.acupick.databinding.PromptSnackBarBinding
import com.albertsons.acupick.ui.models.SnackBarEvent
import com.albertsons.acupick.ui.util.StringIdHelper
import com.albertsons.acupick.ui.util.orFalse

class AcuPickPromptSnackBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0,
) : ConstraintLayout(context, attrs, defStyle, defStyleRes) {

    val binding: PromptSnackBarBinding

    init {
        val inflater = LayoutInflater.from(context)
        binding = PromptSnackBarBinding.inflate(inflater, this, true)
        binding.root.visibility = INVISIBLE
    }

    fun setMessage(stringHelper: StringIdHelper?) {
        binding.root.visibility = if (stringHelper == null) GONE else VISIBLE
        binding.message.text = stringHelper?.getString(context)
    }

    fun setAction(actionVisible: Boolean, action: SnackBarEvent<String>?) {
        binding.root.visibility = if (action == null) GONE else VISIBLE
        binding.action.visibility = if (actionVisible) VISIBLE else GONE
        binding.action.setOnClickListener { action?.action?.invoke(null) }
    }
}

@BindingAdapter(value = ["app:actionVisible", "app:setSnackbarAction"], requireAll = true)
fun AcuPickPromptSnackBar.setSnackbarAction(actionVisible: Boolean, action: SnackBarEvent<String>?) {
    if (actionVisible.orFalse()) {
        setAction(actionVisible, action)
        action?.prompt?.let(::setMessage)
    }
}
