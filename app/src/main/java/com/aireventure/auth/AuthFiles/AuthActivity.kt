package com.aireventure.auth.AuthFiles

import android.animation.ObjectAnimator
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnLayout
import androidx.fragment.app.commit
import com.aireventure.auth.R
import com.aireventure.auth.databinding.ActivityAuthBinding

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private var currentTab: Tab = Tab.LOGIN



    enum class Tab { LOGIN, SIGNUP }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(binding.authFragmentContainer.id, LoginFragment())
            }
        }

        // After layout, size indicator to half width minus padding effect
        binding.tabContainer.doOnLayout {
            val innerWidth = binding.tabContainer.width - binding.tabContainer.paddingLeft - binding.tabContainer.paddingRight
            binding.tabIndicator.layoutParams.width = innerWidth / 2
            binding.tabIndicator.requestLayout()
        }

        binding.tabLogin.setOnClickListener { switchTo(Tab.LOGIN) }
        binding.tabSignup.setOnClickListener { switchTo(Tab.SIGNUP) }
    }

    private fun switchTo(tab: Tab) {
        if (tab == currentTab) return
        currentTab = tab

        // Update text colors
        if (tab == Tab.LOGIN) {
            binding.tabLogin.setTextColor(getColor(R.color.tab_active_text))
            binding.tabSignup.setTextColor(getColor(R.color.tab_inactive_text))
        } else {
            binding.tabLogin.setTextColor(getColor(R.color.tab_inactive_text))
            binding.tabSignup.setTextColor(getColor(R.color.tab_active_text))
        }

        // Animate indicator
        val innerWidth = binding.tabContainer.width - binding.tabContainer.paddingLeft - binding.tabContainer.paddingRight
        val targetX = if (tab == Tab.LOGIN) 0f else (innerWidth / 2).toFloat()
        ObjectAnimator.ofFloat(binding.tabIndicator, "translationX", targetX).apply {
            duration = 220
            start()
        }

        // Swap fragment
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(
                binding.authFragmentContainer.id,
                if (tab == Tab.LOGIN) LoginFragment() else SignupFragment()
            )
        }
    }
}