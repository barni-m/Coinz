package uk.ac.ed.inf.coinz

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

import kotlinx.android.synthetic.main.activity_login_signup.*
import kotlin.collections.HashMap


class LoginSignupActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private var isLogin = true

    private lateinit var db: FirebaseFirestore


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_signup)

        // Setting up User Authentication
        mAuth = FirebaseAuth.getInstance()

        // Loging in or signing in
        login_signup_button.setOnClickListener {
            val email = email_input_field.text.toString()
            val password= password_input_field.text.toString()
            val confirmEmail = emai_confirm_input_field.text.toString()
            val confirmPassword = password_confirm_input_field.text.toString()
            if (email != "" || password != ""){
                if (isLogin == false){
                    if (email == confirmEmail && password == confirmPassword){
                        // Sign up if emails and passwords match
                        mAuth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener(this, OnCompleteListener<AuthResult> {  task ->
                                    if (task.isSuccessful) {
                                        // Sign in success, update UI with the signed-in user's information
                                        // Add new user to database "users" (in Firestore)
                                        registerNewUser(email)
                                        val intent = Intent(this, MapActivity::class.java)
                                        startActivity(intent)
                                    }else{
                                        // If sign in fails, display a message to the user.
                                        Toast.makeText(this, "Authentication failed.",
                                                Toast.LENGTH_SHORT).show()
                                    }
                                })
                    }else{
                        if(email != confirmEmail){
                            // If emails don't match Toast
                            Toast.makeText(this, "The e-mails don't match.",
                                    Toast.LENGTH_SHORT).show()
                        }else{
                            // If passwords don't match Toast
                            Toast.makeText(this, "The passwords don't match.",
                                    Toast.LENGTH_SHORT).show()
                        }

                    }


                }else{
                    // Login
                    mAuth.signInWithEmailAndPassword(email,password)
                            .addOnCompleteListener(this, OnCompleteListener<AuthResult> { task ->
                                if(task.isSuccessful){
                                    // Sign in success, update UI with the signed-in user's information
                                    val intent = Intent(this, MapActivity::class.java)
                                    startActivity(intent)
                                }else{
                                    // If sign in fails, display a message to the user.
                                    Toast.makeText(this, "Authentication failed.",
                                            Toast.LENGTH_SHORT).show()
                                }
                            })
                }
            }else{
                Toast.makeText(this, "Email or password empty.",
                        Toast.LENGTH_SHORT).show()
            }

        }


        sign_up_link.setOnClickListener{view -> toggleFormLayout(view)}

        //db = FirebaseFirestore.getInstance()

        //mapOf<String, Objects>(email)


    }


    private fun toggleFormLayout(view: View){
        if (sign_up_link.text.toString() == "Sign up here!"){
            sign_up_link.text = getString(R.string.login_layout_link)
            no_account_question.visibility = View.GONE
            emai_confirm_input_field.visibility = View.VISIBLE
            password_confirm_input_field.visibility = View.VISIBLE
            login_signup_title_view.text = getString(R.string.sign_up_title)
            login_signup_button.text = getString(R.string.sign_up_button)
            isLogin = false
        }else{
            sign_up_link.text = getString(R.string.sign_up)
            no_account_question.visibility = View.VISIBLE
            emai_confirm_input_field.visibility = View.GONE
            password_confirm_input_field.visibility = View.GONE
            login_signup_title_view.text = getString(R.string.login_form_title)
            login_signup_button.text = getString(R.string.login_signup_button_text)
            isLogin = true
        }

    }



    private fun registerNewUser(email: String) {
        val registered = HashMap<String,Any>()
        registered["registered"] = true
        db = FirebaseFirestore.getInstance()
        val userReference = db.collection("users").document(email)
        userReference.set(registered)
    }





}
