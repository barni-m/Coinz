package uk.ac.ed.inf.coinz

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_bank.*
import org.json.JSONObject



class BankFragment: Fragment(){
    // user (Firebase):
    private lateinit var db: FirebaseFirestore
    private lateinit var mAuth: FirebaseAuth
    private var currentUser: FirebaseUser? = null
    private lateinit var userDB: DocumentReference
    private var email: String? = null

    // All currencies
    private val DOLR = "DOLR"
    private val SHIL = "SHIL"
    private val PENY = "PENY"
    private val QUID = "QUID"

    // Currency bank values
    private  var val_DOLR: Double = .0
    private var val_PENY: Double = .0
    private var val_QUID: Double = .0
    private var val_SHIL: Double = .0

    // Shared Prefs
    private val preferencesFile = "RatesPrefsFile" // for storing preferences
    private var settings: SharedPreferences? = null

    // Rates
    private  lateinit var  rates : JSONObject



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        userDB.collection("bank").document("currencies").get().addOnCompleteListener {
            val currenciesHashMap = it.result?.data as HashMap<String,Double>

            for ((currency,value) in currenciesHashMap){

                when (currency) {
                    DOLR -> {
                        bank_dolr_amount.text = "%.2f".format(value)
                        val_DOLR = value
                    }
                    SHIL -> {
                        bank_shil_amount.text = "%.2f".format(value)
                        val_SHIL = value
                    }
                    PENY -> {
                        bank_peny_amount.text = "%.2f".format(value)
                        val_PENY = value
                    }
                    QUID -> {
                        bank_quid_amount.text = "%.2f".format(value)
                        val_QUID = value
                    }
                }
            }


            total_value_gold.text = "%.0f".format(totalValueInGold())
        }

        return inflater.inflate(R.layout.fragment_bank, container,false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setUpUser()

        settings= activity?.getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        rates = JSONObject(settings?.getString("ratesJSONAsString", ""))

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }


    private fun setUpUser() {
        mAuth = FirebaseAuth.getInstance()
        currentUser = mAuth.currentUser
        email = currentUser?.email
        db = FirebaseFirestore.getInstance()
        if (email != null)
            userDB = db.collection("users").document(email!!)
    }

    private fun totalValueInGold(): Double{
        val shil_rate =  rates[SHIL] as Double
        val dolr_rate =  rates[DOLR] as Double
        val quid_rate =  rates[QUID] as Double
        val peny_rate =  rates[PENY] as Double

       val totalInGold = (shil_rate * val_SHIL) +
                (dolr_rate * val_DOLR) +
                (quid_rate * val_QUID) +
                (peny_rate * val_PENY)

        return totalInGold
    }
}