package uk.ac.ed.inf.coinz

import android.content.Context
import android.support.v7.app.AlertDialog
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

class CoinMessageListener {

    // user (Firebase):
    private lateinit var db: FirebaseFirestore
    private lateinit var mAuth: FirebaseAuth
    private var currentUser: FirebaseUser? = null
    private lateinit var userDB: DocumentReference
    private var email: String? = null

    private var isFirst = true


    fun setUpUser() {
        mAuth = FirebaseAuth.getInstance()
        currentUser = mAuth.currentUser
        email = currentUser?.email
        db = FirebaseFirestore.getInstance()
        if (email != null)
            userDB = db.collection("users").document(email!!)
    }

    fun realTimeUpdateListener(context: Context){
        setUpUser()
        val walletReference = userDB.collection(COLLECTION_KEY).document(DOCUMENT_KEY)
        walletReference.addSnapshotListener { snapshot, exception ->
            if (exception != null){
                Log.w(TAG, "Listen failed.", exception)
            }
            if (snapshot != null && snapshot.exists()){
                if(isFirst){
                    isFirst = false
                }else{
                    val data = snapshot.data
                    val alert = AlertDialog.Builder(context)
                    alert.apply {
                        setPositiveButton("OK",null)
                        setCancelable(true)
                        setTitle("New coin received")
                        setMessage("")
                        create().show()
                    }
                }

            }else{

            }
        }
    }

    companion object {
        private const val COLLECTION_KEY = "wallet"
        private const val DOCUMENT_KEY = "todaysCollectedCoins"
        private const val TAG = "CoinsMessageListener"
    }


}