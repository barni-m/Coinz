package uk.ac.ed.inf.coinz

import android.content.Intent
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_bottom_navigation.*


class BottomNavigationActivity : AppCompatActivity() {


    private lateinit  var fragmentSelected: Fragment
    private lateinit var mAuth: FirebaseAuth

    private lateinit var messengerFragment: MessengerFragment
    private lateinit var bankFragment: BankFragment
    private lateinit var walletFragment: WalletFragment



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bottom_navigation)
        // go to map button listener
        go_to_map.setOnClickListener{
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left)
        }
        // get instance of firebase user authentication
        mAuth = FirebaseAuth.getInstance()
        // add listener to log_out button which logs out and starts activity login/signup activity
        log_out.setOnClickListener {
            mAuth.signOut()
            val intent = Intent(this, LoginSignupActivity::class.java)
            startActivity(intent)
        }
        // initialise snapshot listener for coin receiving
        CoinMessageListener().realTimeUpdateListener(this)

        // create fragment insteances
        messengerFragment = MessengerFragment()
        bankFragment = BankFragment()
        walletFragment = WalletFragment()

        // set bottom navigation and add a listener for selection
        val bottomnav : BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomnav.setOnNavigationItemSelectedListener(navListener)
        // the first fragment to be loaded in the ragment container as activity starts
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container,
                WalletFragment()).commit()
    }


    // switch between fragments
    private val navListener : BottomNavigationView.OnNavigationItemSelectedListener =
            BottomNavigationView.OnNavigationItemSelectedListener {item ->

                when (item.itemId) {
                    R.id.nav_bottom_messenger -> {
                        fragmentSelected = messengerFragment
                    }
                    R.id.nav_bottom_bank -> {
                        fragmentSelected = bankFragment
                    }
                    R.id.nav_bottom_wallet -> {
                        fragmentSelected = walletFragment
                    }
                }


                supportFragmentManager.beginTransaction().replace(R.id.fragment_container,
                        fragmentSelected).commit()
                return@OnNavigationItemSelectedListener true
            }

    // create slide animation between activities
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left)
    }

}
