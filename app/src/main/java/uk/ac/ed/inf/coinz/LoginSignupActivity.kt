package uk.ac.ed.inf.coinz

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.View

import kotlinx.android.synthetic.main.activity_login_signup.*

class LoginSignupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_signup)
        //setSupportActionBar(toolbar)
        password_box.setOnClickListener {
            confirm_password_box.visibility = View.VISIBLE
        }

        cancel.setOnClickListener{
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
        }

    }

}
