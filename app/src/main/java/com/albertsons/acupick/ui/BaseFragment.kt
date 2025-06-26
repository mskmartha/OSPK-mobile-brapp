package com.albertsons.acupick.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.core.view.children
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.albertsons.acupick.BR
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.ui.MainActivityViewModel.Companion.ERROR_DIALOG_TAG
import com.albertsons.acupick.ui.MainActivityViewModel.Companion.RETRY_ERROR_DIALOG_TAG
import com.albertsons.acupick.ui.bottomsheetdialog.BaseBottomSheetDialogFragment
import com.albertsons.acupick.ui.bottomsheetdialog.BottomSheetType
import com.albertsons.acupick.ui.dialog.BaseCustomDialogFragment
import com.albertsons.acupick.ui.dialog.CloseActionListenerProvider
import com.albertsons.acupick.ui.dialog.showWithFragment
import com.albertsons.acupick.ui.util.AcupickSnackbar
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import timber.log.Timber

/**
 * Manages DataBinding setup and navigation observer setup, allows per fragment logic/customizations via [setupBinding]
 */
abstract class BaseFragment<FRAGMENT_VIEW_MODEL : BaseViewModel, BINDING : ViewDataBinding> : Fragment(), CloseActionListenerProvider {

    /** Fragment scoped Android ViewModel. Primary ViewModel to interact with for the given Fragment. */
    protected abstract val fragmentViewModel: FRAGMENT_VIEW_MODEL

    /** Activity scoped Android ViewModel */
    protected val activityViewModel: MainActivityViewModel by sharedViewModel()

    // Use private nullable binding in order to set in onCreateView and onDestroyView
    private var _binding: BINDING? = null

    // Use this variable to cancel the existing open bottom sheet
    var currentBottomSheet: BaseBottomSheetDialogFragment? = null

    /**
     * Wraps private nullable property. Lifecycle valid from [onCreateView] to [onDestroyView].
     * Treat similar to [requireContext] which throws if context is null, meaning you are aware of when you can call it and should use it.
     */
    private val binding: BINDING
        get() = _binding!!

    /**
     * See [setupBinding] if you want to add logic post view creation.
     *
     * > It is recommended to **only** inflate the layout in this method and move logic that operates on the returned View to onViewCreated(View, Bundle).
     * > Source: https://developer.android.com/reference/androidx/fragment/app/Fragment#onCreateView(android.view.LayoutInflater,%20android.view.ViewGroup,%20android.os.Bundle)
     */
    @CallSuper
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Log screen view using fragment class name
        Timber.e("CurrentScreenName -> ${this@BaseFragment.javaClass.simpleName}")
        Firebase.analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, this@BaseFragment.javaClass.simpleName)
        }

        // Performance trace layout inflation
        perfTrace("Screen Load") {
            _binding = DataBindingUtil.inflate(inflater, getLayoutRes(), container, false)
            fragmentViewModel.updateUiLifecycle(true)
        }

        return binding.root
    }

    private fun perfTrace(name: String, block: Trace.() -> Unit) {
        FirebasePerformance.getInstance().newTrace(name).apply {
            putAttribute("ClassName", this@BaseFragment.javaClass.simpleName)
            start()
            block()
            stop()
        }
    }

    /**
     * Calls [setupNavigationObservers] and [setupBinding]
     */
    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupNavigationObservers()
        setupDialogObserver()
        setupBottomSheetObserver()
        setupSnackBarObserver()
        setupAcupickSnackObserver()
        setupToolbarObservers()
        setupLoadingStateObserver()
        setupErrorHandlers()
        setupKeyboardHandler()
        setupBinding(binding)
    }

    @CallSuper
    override fun onDestroyView() {
        super.onDestroyView()
        onDestroyView(binding)
    }

    /**
     * Override per fragment to add any further binding related setup, make ViewModel function calls, and add ViewModel LiveData Observer setup blocks.
     *
     * Sets up the [binding], calling [ViewDataBinding.setLifecycleOwner] and attaching the layout "viewModel" variable with [fragmentViewModel] (called in [onViewCreated]
     *
     * **NOTE: You must use "viewModel" as the layout variable name**
     */
    @CallSuper
    open fun setupBinding(binding: BINDING) {
        binding.lifecycleOwner = this.viewLifecycleOwner
        // In all layouts, the variable name should be "viewModel" for the given FRAGMENT_VIEW_MODEL
        binding.setVariable(BR.viewModel, fragmentViewModel)
    }

    /**
     * Override per fragment to add any further onDestroyView related cleanup.
     */
    @CallSuper
    open fun onDestroyView(binding: BINDING) {
        // Make LeakCanary happy by nulling out binding reference here: https://stackoverflow.com/questions/57647751/android-databinding-is-leaking-memory
        getAllRecyclerViews(binding.root.rootView as ViewGroup).forEach { it.adapter = null }
        getTablayoutsView(binding.root.rootView as ViewGroup)?.removeAllTabs()

        _binding = null
        fragmentViewModel.updateUiLifecycle(false)
        fragmentViewModel.clearSnackBarEvents()
        AcupickSnackbar.clearAll()
    }

    /**
     * Fragment layout id for the screen. (ex: R.layout.home_fragment)
     */
    @LayoutRes
    abstract fun getLayoutRes(): Int

    /**
     * Observes [BaseViewModel.navigationEvent] with [NavigationObserver][com.albertsons.acupick.navigation.NavigationObserver] and
     * [BaseViewModel.externalNavigationEvent] with [ExternalNavigationObserver][com.albertsons.acupick.navigation.ExternalNavigationObserver]
     * to prevent all subclasses from writing the same line of code.
     *
     * Called in [onViewCreated] */
    @CallSuper
    protected open fun setupNavigationObservers() {
        fragmentViewModel.observeNavigationEvents(this)
    }

    // /////////////////////////////////////////////////////////////////////////
    // Dialog Listener Lookup Framework
    // /////////////////////////////////////////////////////////////////////////
    /**
     * This function servers as a backup listener provider in cases where the child fragment doesn't explicitly handle the tag.
     */
    @CallSuper
    override fun provide(tag: String?) = fragmentViewModel.dialogTagCloseActionListenerMap[tag]

    /**
     * Called from onCreate so inlineDialogEvent is observed and CustomDialogFragment instantiated
     */
    private fun setupDialogObserver() {
        fragmentViewModel.inlineDialogEvent.observe(viewLifecycleOwner) { (argData, tag) ->
            BaseCustomDialogFragment.newInstance(argData).showWithFragment(this, tag)
        }
    }

    /**
     * Called from onCreate so inlineBottomSheetEvent is observed and BaseBottomSheetFragment instantiated
     */
    private fun setupBottomSheetObserver() {
        fragmentViewModel.inlineBottomSheetEvent.observe(viewLifecycleOwner) { (argData, tag) ->
            if (currentBottomSheet != null) {
                if (currentBottomSheet?.isArgDataInitialized() == true) {
                    // Dismiss already opened bottomsheet except Substitute Confirmation bottomsheet
                    if (currentBottomSheet?.argData?.dialogType !in listOf(BottomSheetType.SubstitutionConfirmation, BottomSheetType.MissingItemLocation, BottomSheetType.ManualEntryMfcDestaging)) {
                        currentBottomSheet?.dismiss()
                    }
                    currentBottomSheet = null
                }
            }
            currentBottomSheet = BaseBottomSheetDialogFragment.newInstance(argData)
            currentBottomSheet?.showWithFragment(this, tag)
        }
    }

    /**
     * Connects snack bar events from fragments to activity so it can anchor snack bar correctly
     */
    private fun setupSnackBarObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            fragmentViewModel.snackBarLiveEvent.observe(viewLifecycleOwner) { snackBarEvent ->
                snackBarEvent?.let {
                    it.callback = object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                        override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                            it.onDismissEventCallback?.invoke()
                            fragmentViewModel.snackBarEventList.remove(it)
                            fragmentViewModel.isDisplayingSnackbar.postValue(false)
                            super.onDismissed(transientBottomBar, event)
                        }
                    }
                    activityViewModel.snackBarEvent.postValue(it)
                    fragmentViewModel.isDisplayingSnackbar.postValue(true)
                    fragmentViewModel.snackBarEventList.add(it)
                }
            }
        }
    }

    /**
     * Connects snack bar events from fragments to activity
     */
    private fun setupAcupickSnackObserver() {
        fragmentViewModel.acupickSnackEvent.observe(viewLifecycleOwner) { snackEvent ->
            viewLifecycleOwner.lifecycleScope.launch {
                with(snackEvent) {
                    // wait for bottomsheets to go down (if open)
                    delay(500)
                    AcupickSnackbar.make(this@BaseFragment, this).show()
                    // snack event will not trigger, if any opened bottom sheet is down within 500 ms
                    activityViewModel.snackEvent.postValue(snackEvent)
                }
            }
        }

        fragmentViewModel.acupickSnackEventAnchored.observe(viewLifecycleOwner) { snackEvent ->
            if (isBottomSheetOpen()) {
                viewLifecycleOwner.lifecycleScope.launch {
                    delay(500)
                    // snack event will not trigger, if the bottom sheet is down within 500 ms
                    // applicable for success scenarios (e.g. Tote Scan)
                    activityViewModel.snackEvent.postValue(snackEvent)
                }
            }
        }
    }

    private fun isBottomSheetOpen() = childFragmentManager.fragments.lastOrNull() is BottomSheetDialogFragment

    /**
     * Connects BaseViewModel Toolbar Events to activityViewModel.
     */
    private fun setupToolbarObservers() {
        activityViewModel.let { avm ->
            with(fragmentViewModel) {
                // Events that update toolbar
                changeToolbarTitleEvent.observe(viewLifecycleOwner) { avm.setToolbarTitle(it) }
                changeToolbarTitleBackgroundImageEvent.observe(viewLifecycleOwner) { avm.setToolbarTitleBackground(it) }
                changeToolbarSmallTitleEvent.observe(viewLifecycleOwner) { avm.setToolbarSmallTitle(it) }
                changeToolbarLeftExtraEvent.observe(viewLifecycleOwner) { avm.setToolbarLeftExtra(it) }
                changeToolbarLeftExtraImageEvent.observe(viewLifecycleOwner) { avm.setToolbarLeftExtraImage(it) }
                changeToolbarRightExtraTopEvent.observe(viewLifecycleOwner) { avm.setToolbarRightExtraTop(it) }
                changeToolbarRightExtraBottomEvent.observe(viewLifecycleOwner) { avm.setToolbarRightExtraBottom(it) }
                // TODO - If DrawableIdHelper works well, use elsewhere
                changeToolbarRightSecondExtraImageEvent.observe(viewLifecycleOwner) { avm.setToolbarRightSecondExtraImage(it.get(requireContext())) }
                changeToolbarRightFirstExtraImageEvent.observe(viewLifecycleOwner) { avm.setToolbarRightFirstExtraImage(it.get(requireContext())) }
                changeToolbarExtraRightEvent.observe(viewLifecycleOwner) { avm.setToolbarRightExtra(it) }
                changeToolbarExtraRightCtaEvent.observe(viewLifecycleOwner) { it?.let { it1 -> avm.setToolbarRightExtraCta(it1.first, it.second) } }
                clearToolbarEvent.observe(viewLifecycleOwner) { avm.clearToolbar() }

                // Toolbar events notifying VM
                avm.navigationButtonIntercept.observe(viewLifecycleOwner) { navigationButtonEvent.postValue(Unit) }
                avm.triggerHomeClickIntercept.observe(viewLifecycleOwner) { triggerHomeButtonEvent.postValue(Unit) }
                avm.toolbarRightSecondImageClickEvent.observe(viewLifecycleOwner) { toolbarRightSecondImageEvent.postValue(Unit) }
                avm.toolbarRightFirstImageClickEvent.observe(viewLifecycleOwner) { toolbarRightFirstImageEvent.postValue(Unit) }
            }
        }
    }

    /**
     * Connect UI-blocking loading state to activityViewModel
     */
    private fun setupLoadingStateObserver() {
        fragmentViewModel.isBlockingUi.observe(viewLifecycleOwner) { activityViewModel.setLoadingState(it, true) }
    }

    /**
     * Connect error handling
     */
    private fun setupErrorHandlers() {
        /**
         * TODO move all instances of showErrorDialog and avm.handleApiErrors() to BaseViewModel
         * possibly it's own external class extended from BaseViewModel
         */
        activityViewModel.let { avm ->
            with(fragmentViewModel) {
                var retryEvent: (() -> Unit)? = null

                retryActionEvent.observe(viewLifecycleOwner) {
                    retryEvent = it
                }
                apiErrorEvent.observe(viewLifecycleOwner) { (tag, errorType) ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        avm.handleApiErrors(
                            tag = tag.ifEmpty {
                                if (errorType is ApiResult.Failure.GeneralFailure) RETRY_ERROR_DIALOG_TAG else ERROR_DIALOG_TAG
                            },
                            errorType = errorType,
                            retryActionEvent = retryEvent
                        )
                    }
                }
            }
        }
    }

    /**
     * Connect keyboard handler
     */
    private fun setupKeyboardHandler() {
        activityViewModel.let { avm ->
            with(fragmentViewModel) {
                keyboardActiveEvent.observe(viewLifecycleOwner) {
                    avm.keyboardActive.postValue(it)
                }
            }
        }
    }

    /** Recursive function to find TabLayout within a view */
    private fun getTablayoutsView(root: ViewGroup): TabLayout? {
        var view: TabLayout? = null
        for (child in root.children) {
            if (child is TabLayout) view = child
        }
        return view
    }

    /** Recursive function to find all RecyclerViews in a layout */
    private fun getAllRecyclerViews(root: ViewGroup): ArrayList<RecyclerView> {
        val views = arrayListOf<RecyclerView>()
        for (child in root.children) {
            if (child is ViewGroup) views.addAll(getAllRecyclerViews(child))
            if (child is RecyclerView) views.add(child)
        }
        return views
    }
}
