package com.albertsons.acupick.ui.custom

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.BindingAdapter
import com.albertsons.acupick.databinding.PromptBarBinding
import com.albertsons.acupick.ui.models.SnackBarEvent
import com.albertsons.acupick.ui.util.StringIdHelper
import com.albertsons.acupick.ui.util.orFalse

class AcuPickPromptBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0
) : ConstraintLayout(context, attrs, defStyle, defStyleRes) {

    val binding: PromptBarBinding

    init {
        val inflater = LayoutInflater.from(context)
        binding = PromptBarBinding.inflate(inflater, this, true)
        binding.root.visibility = INVISIBLE
    }

    fun setMessage(stringHelper: StringIdHelper) {
        binding.root.visibility = VISIBLE
        binding.action.visibility = GONE
        binding.message.text = stringHelper.getString(context)
    }

    fun setAction(action: SnackBarEvent<String>) {
        binding.action.visibility = VISIBLE
        binding.action.setOnClickListener { action.action?.invoke(null) }
    }
}

@BindingAdapter("app:setPrompt")
fun AcuPickPromptBar.setPrompt(prompt: StringIdHelper?) {
    prompt?.let { setMessage(it) }
}

@BindingAdapter(value = ["app:actionVisible", "app:setSnackbarAction"], requireAll = true)
fun AcuPickPromptBar.setSnackbarAction(actionVisible: Boolean, action: SnackBarEvent<String>) {
    if (actionVisible.orFalse()) setAction(action)
}
