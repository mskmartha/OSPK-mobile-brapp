package com.albertsons.acupick.ui.dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.InterjectionForAllUsersBinding

class InterjectionForAllUsersDialogFragment : BaseCustomDialogFragment() {

    override val shouldFillScreen
        get() = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        // vibrateAndShake(dialog?.window?.decorView, context)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun getViewDataBinding(inflater: LayoutInflater, container: ViewGroup?): ViewDataBinding =
        DataBindingUtil.inflate<InterjectionForAllUsersBinding>(inflater, R.layout.interjection_for_all_users, container, false).apply {
            viewData = argData.toViewData(requireContext())
            viewModel = fragmentViewModel
            customerArrivalTime = argData.cutomerArrivalTime
        }

  /*
  TODO: For Future Reference
  private fun vibrateAndShake(view: View?, context: Context?) {
        AnimatorSet().apply {
            play(
                ObjectAnimator.ofFloat(view, ROTATION_PROPERTY, 0f, -10f, 0f, 10f, 0f).apply {
                    duration = ANIMATION_AND_VIBRATION_DURATION
                    repeatMode = ObjectAnimator.REVERSE
                }
            )
        }.start()

        // Vibration
        (context?.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.let { vibrator ->
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(ANIMATION_AND_VIBRATION_DURATION, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vibrator.vibrate(ANIMATION_AND_VIBRATION_DURATION)
            }
        }
    }

    companion object {
        const val ROTATION_PROPERTY = "rotation"
        const val ANIMATION_AND_VIBRATION_DURATION = 1000L
    }

   */
}
