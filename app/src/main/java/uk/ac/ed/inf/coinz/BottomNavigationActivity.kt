package uk.ac.ed.inf.coinz

import android.content.Intent
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import kotlinx.android.synthetic.main.activity_bottom_navigation.*


class BottomNavigationActivity : AppCompatActivity() {


    private lateinit  var fragmentSelected: Fragment


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bottom_navigation)

        go_to_map.setOnClickListener{
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left)
        }

        var bottomnav : BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomnav.setOnNavigationItemSelectedListener(navListener)

        supportFragmentManager.beginTransaction().replace(R.id.fragment_container,
                WalletFragment()).commit()
    }

    private val navListener : BottomNavigationView.OnNavigationItemSelectedListener =
            BottomNavigationView.OnNavigationItemSelectedListener {item ->

                when (item.itemId) {
                    R.id.nav_bottom_store -> {
                        fragmentSelected = StoreFragment()
                    }
                    R.id.nav_bottom_bank -> {
                        fragmentSelected = BankFragment()
                    }
                    R.id.nav_bottom_wallet -> {
                        fragmentSelected = WalletFragment()
                    }
                }

                supportFragmentManager.beginTransaction().replace(R.id.fragment_container,
                        fragmentSelected).commit()

                return@OnNavigationItemSelectedListener true
            }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left)
    }

}
