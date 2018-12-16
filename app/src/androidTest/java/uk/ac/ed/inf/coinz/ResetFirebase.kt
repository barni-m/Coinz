package uk.ac.ed.inf.coinz

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

class ResetFirebase {
    // user (Firebase):
    private lateinit var db: FirebaseFirestore
    private lateinit var mAuth: FirebaseAuth
    private var currentUser: FirebaseUser? = null
    private lateinit var userDB: DocumentReference
    private var email: String? = null

    fun userSetup(){

        mAuth = FirebaseAuth.getInstance()
        currentUser = mAuth.currentUser
        email = currentUser?.email
        db = FirebaseFirestore.getInstance()
        if (email != null)
            userDB = db.collection("users").document(email!!)
    }

    private fun deleteTestUser(){
        currentUser?.delete()
    }



    fun resetFirebase(){
        userSetup()
        deleteTestUser()
    }



}