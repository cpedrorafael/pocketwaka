package com.kondenko.pocketwaka.screens.stats


import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.browser.customtabs.CustomTabsIntent
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.kondenko.pocketwaka.Const
import com.kondenko.pocketwaka.R
import com.kondenko.pocketwaka.domain.stats.model.StatsItem
import com.kondenko.pocketwaka.domain.stats.model.StatsModel
import com.kondenko.pocketwaka.screens.base.State
import com.kondenko.pocketwaka.ui.CardStats
import com.kondenko.pocketwaka.ui.ObservableScrollView
import com.kondenko.pocketwaka.utils.attachToLifecycle
import com.kondenko.pocketwaka.utils.extensions.elevation
import com.kondenko.pocketwaka.utils.extensions.observe
import com.kondenko.pocketwaka.utils.extensions.rxClicks
import com.kondenko.pocketwaka.utils.extensions.showFirstView
import com.kondenko.pocketwaka.utils.report
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.fragment_stats.*
import kotlinx.android.synthetic.main.layout_stats_data.*
import kotlinx.android.synthetic.main.layout_stats_empty.*
import kotlinx.android.synthetic.main.layout_stats_error.view.*
import kotlinx.android.synthetic.main.layout_stats_info.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.roundToInt


class FragmentStatsTab : Fragment() {

    companion object {
        const val ARG_RANGE = "range"
    }

    private val vm: StatsViewModel by viewModel {
        parametersOf(arguments?.getString(ARG_RANGE))
    }

    private var shadowAnimationNeeded = true

    private val scrollDirection = PublishSubject.create<ScrollDirection>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_stats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUi(view)
        vm.state().observe(viewLifecycleOwner) { state ->
            when (state) {
                is State.Success<StatsModel> -> onSuccess(state.data)
                is State.Failure -> onError(state.error)
                State.Empty -> onEmpty()
                State.Loading -> onLoading()
            }
        }
    }

    fun scrollDirection(): Observable<ScrollDirection> = scrollDirection

    private fun setupUi(view: View) {
        stats_group_bestday.elevation(resources.getDimension(R.dimen.elevation_stats_bestday))

        view.button_errorstate_retry.rxClicks().subscribe {
            vm.update()
        }.attachToLifecycle(viewLifecycleOwner)

        (layout_data as ObservableScrollView).scrolls().subscribe {
            shadowAnimationNeeded = if (it.y >= 10) {
                if (shadowAnimationNeeded) {
                    scrollDirection.onNext(ScrollDirection.Down)
                }
                false
            } else {
                scrollDirection.onNext(ScrollDirection.Up)
                true
            }
        }.attachToLifecycle(viewLifecycleOwner)

        button_emptystate_plugins.rxClicks().subscribe {
            val uri = Const.URL_PLUGINS
            val builder = CustomTabsIntent.Builder()
            builder.setToolbarColor(ContextCompat.getColor(context!!, R.color.color_primary))
            val customTabsIntent = builder.build()
            customTabsIntent.launchUrl(context, Uri.parse(uri))
        }.attachToLifecycle(viewLifecycleOwner)

    }

    private fun onSuccess(model: StatsModel) {
        showFirstView(layout_data, layout_empty, layout_loading, layout_error)
        stats_textview_time_total.text = model.humanReadableTotal
        stats_textview_daily_average.text = model.humanReadableDailyAverage
        if (model.bestDay != null) {
            bestday_textview_date.text = model.bestDay.date
            bestday_textview_time.text = model.bestDay.time
        } else {
            stats_group_bestday.visibility = View.GONE
        }
        addStatsCards(model)
    }

    private fun onEmpty() {
        showFirstView(layout_empty, layout_loading, layout_data, layout_error)
    }

    private fun onError(throwable: Throwable?) {
        showFirstView(layout_error, layout_loading, layout_empty, layout_data)
        throwable?.report()
    }

    private fun onLoading() {
        showFirstView(layout_loading, layout_empty, layout_data, layout_error)
    }

    private fun addStatsCards(stats: StatsModel) {
        val cards = getAvailableCards(stats)
        var prevViewId = R.id.stats_cardview_bestday
        cards.forEachIndexed { index, card ->
            if (index == 0) {
                val cs = ConstraintSet()
                with(cs) {
                    clone(stats_constraintlayout_content)
                    connect(R.id.stats_cardview_bestday, ConstraintSet.BOTTOM, card.view.id, ConstraintSet.TOP)
                    applyTo(stats_constraintlayout_content)
                }
            }
            val nextViewId = if (index < cards.size - 1) cards[index + 1].view.id else ConstraintSet.PARENT_ID
            val nextViewSide = if (nextViewId == ConstraintSet.PARENT_ID) ConstraintSet.BOTTOM else ConstraintSet.TOP
            addCardView(prevViewId, nextViewId, card.view, nextViewSide)
            prevViewId = card.view.id
        }
    }

    private fun addCardView(prevViewId: Int, nextViewId: Int, view: View, nextViewSide: Int) {
        if (stats_constraintlayout_content.findViewById<View>(view.id) == null) {
            view.layoutParams = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_CONSTRAINT, ConstraintLayout.LayoutParams.WRAP_CONTENT)
            stats_constraintlayout_content.addView(view)
            val constraintSet = ConstraintSet()
            with(constraintSet) {
                val marginVertical = resources.getDimension(R.dimen.margin_all_card_outer_vertical).roundToInt()
                val marginHorizontal = resources.getDimension(R.dimen.margin_all_card_outer_horizontal).roundToInt()
                clone(stats_constraintlayout_content)
                connect(view.id, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT, marginHorizontal)
                connect(view.id, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT, marginHorizontal)
                connect(view.id, ConstraintSet.TOP, prevViewId, ConstraintSet.BOTTOM, marginVertical)
                connect(view.id, ConstraintSet.BOTTOM, nextViewId, nextViewSide, marginVertical)
                applyTo(stats_constraintlayout_content)
            }
        }
    }

    private fun getAvailableCards(stats: StatsModel): ArrayList<CardStats> {
        val cards = ArrayList<CardStats>()
        cards.addIfNotEmpty(stats.projects, getString(R.string.stats_card_header_projects))
        cards.addIfNotEmpty(stats.editors, getString(R.string.stats_card_header_editors))
        cards.addIfNotEmpty(stats.languages, getString(R.string.stats_card_header_languages))
        cards.addIfNotEmpty(stats.operatingSystems, getString(R.string.stats_card_header_operating_systems))
        return cards
    }

    private fun ArrayList<CardStats>.addIfNotEmpty(dataArray: List<StatsItem>?, title: String) {
        if (dataArray != null && dataArray.isNotEmpty()) {
            val card = CardStats(context!!, title, dataArray)
            card.view.id = card.hashCode()
            this.add(card)
        }
    }

    fun isScrollviewOnTop() = layout_data?.scrollY ?: 0 == 0

    fun subscribeToRefreshEvents(refreshEvents: Observable<Any>): Disposable {
        return refreshEvents.subscribe {
            vm.update()
        }
    }

}
