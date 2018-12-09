package uk.ac.ed.inf.coinz

import android.content.Context
import android.support.v7.app.AlertDialog
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
// listener for changes in wallet (for coin receiving)
class CoinMessageListener {

    // user (Firebase):
    private lateinit var db: FirebaseFirestore
    private lateinit var mAuth: FirebaseAuth
    private var currentUser: FirebaseUser? = null
    private lateinit var userDB: DocumentReference
    private var email: String? = null
    // is first trigger
    private var isFirst = true

    @Suppress("UNCHECKED_CAST")
    fun realTimeUpdateListener(context: Context){
        // get user info from firebase
        mAuth = FirebaseAuth.getInstance()
        currentUser = mAuth.currentUser
        email = currentUser?.email
        db = FirebaseFirestore.getInstance()
        if (email != null)
            userDB = db.collection("users").document(email!!)

        //set notifier with a snapshot listener
        val walletReference = userDB.collection(COLLECTION_KEY).document(DOCUMENT_KEY)
        walletReference.addSnapshotListener { snapshot, exception ->
            if (exception != null){
                Log.w(TAG, "Listen failed.", exception)
            }
            if (snapshot != null && snapshot.exists() && snapshot.data!!.isNotEmpty()){
                // hide the first notification triggered as sson as the listener is created
                if(isFirst){
                    isFirst = false
                }else{
                    val coinsRetrieved = snapshot.data as HashMap<String,HashMap<String,Any>>
                    for ((coinid, coin) in coinsRetrieved){
                        val coinDecriptionKeys = coin.keys
                        // if the coin map contains a "from" key it means someone hes sent the user the given coin
                        // there shoud only be one alert hence after the alert we add the field alerted to the coin hash-map
                        if (coinDecriptionKeys.contains("from") && !coinDecriptionKeys.contains("alerted")) {
                            // alert user about new received coin
                            val fromEmail = coin["from"] as String
                            val alert = AlertDialog.Builder(context)
                            alert.apply {
                                setPositiveButton("OK", null)
                                setCancelable(true)
                                setTitle("New coin received")
                                setMessage("$fromEmail sent you a coin. You can find it in your wallet" +
                                        "\nThis coin can be placed in your bank even if you have reached" +
                                        " your transaction limit of 25 coins a day!" +
                                        " You may only place this coin in the bank once you've" +
                                        " added 25 collected coins to the bank today.")
                                create().show()
                            }
                            // add alerted to coin hash-map for future reference
                            coin["alerted"] = true
                            walletReference.update(coinid, coin)
                        }
                    }

                    }

                }

            }

    }

    companion object {
        private const val COLLECTION_KEY = "wallet"
        private const val DOCUMENT_KEY = "todaysCollectedCoins"
        private const val TAG = "CoinsMessageListener"
    }


}