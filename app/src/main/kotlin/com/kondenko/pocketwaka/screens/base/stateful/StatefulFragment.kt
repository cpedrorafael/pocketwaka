package com.kondenko.pocketwaka.screens.base.stateful

import android.os.Parcelable
import android.support.v4.app.Fragment
import com.kondenko.pocketwaka.screens.base.stateful.states.FragmentEmptyState
import com.kondenko.pocketwaka.screens.base.stateful.states.FragmentErrorState
import com.kondenko.pocketwaka.screens.base.stateful.states.FragmentLoadingState
import com.kondenko.pocketwaka.utils.transaction


abstract class StatefulFragment<M : Parcelable> : Fragment(), StatefulView<M> {

    protected var containerId: Int = 0

    private val errorFragment = FragmentErrorState()

    private val emptyFragment by lazy {
        FragmentEmptyState()
    }

    private val loadingFragment by lazy {
        FragmentLoadingState()
    }

    protected var modelFragment: ModelFragment<M>? = null

    abstract fun initModelFragment(model: M): ModelFragment<M>

    override fun showModel(model: M) {
        modelFragment = initModelFragment(model).apply {
            this.show()
        }
    }

    override fun showError(throwable: Throwable?, messageStringRes: Int?) {
        messageStringRes?.let { errorFragment.setMessage(activity!!.getString(messageStringRes)) }
        errorFragment.show()
    }

    override fun showEmptyState() {
        emptyFragment.show()
    }

    override fun showLoading() {
        loadingFragment.show()
    }

    protected fun retryClicks() = errorFragment.retryClicks()

    private fun Fragment.show() {
        this@StatefulFragment.childFragmentManager.transaction {
            setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            replace(containerId, this@show)
        }
    }


}