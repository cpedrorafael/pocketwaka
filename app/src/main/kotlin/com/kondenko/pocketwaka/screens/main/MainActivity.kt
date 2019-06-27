package com.kondenko.pocketwaka.screens.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.kondenko.pocketwaka.R
import com.kondenko.pocketwaka.screens.base.MainState
import com.kondenko.pocketwaka.screens.login.LoginActivity
import com.kondenko.pocketwaka.screens.stats.FragmentStats
import com.kondenko.pocketwaka.utils.extensions.report
import com.kondenko.pocketwaka.utils.extensions.transaction
import io.reactivex.subjects.PublishSubject
import org.koin.androidx.viewmodel.ext.android.viewModel


class MainActivity : AppCompatActivity() {

    private val tagStats = "stats"

    private val vm: MainViewModel by viewModel()

    private val refreshEvents = PublishSubject.create<Any>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setTitle(R.string.screen_stats)
        val visibility = window.decorView.systemUiVisibility or SYSTEM_UI_FLAG_LAYOUT_STABLE or SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        window.decorView.systemUiVisibility = visibility
        vm.states().observe(this, Observer {
            when (it) {
                MainState.ShowStats -> showStats()
                MainState.ShowLoginScreen -> showLoginScreen()
                MainState.LogOut -> logout()
                is MainState.Error -> showError(it.cause)
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_activity_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_logout -> vm.logout()
            R.id.action_refresh -> refreshEvents.onNext(Any())
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showLoginScreen() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showStats() {
        val statsFragment = FragmentStats()
        statsFragment.subscribeToRefreshEvents(refreshEvents)
        setFragment(statsFragment, tagStats)
    }

    private fun showError(throwable: Throwable?) {
        throwable?.report()
        Toast.makeText(this, R.string.error_refreshing_token, Toast.LENGTH_LONG).show()
    }

    private fun logout() {
        finish()
        startActivity(Intent(this, LoginActivity::class.java))
    }

    private fun setFragment(fragment: androidx.fragment.app.Fragment, tag: String) {
        if (supportFragmentManager.findFragmentByTag(tag) == null) {
            supportFragmentManager.transaction {
                replace(R.id.container, fragment, tag)
            }
        }
    }

}
