package uk.ac.ed.inf.coinz

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnticipateInterpolator
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import kotlinx.android.synthetic.main.card_view_coins_loan.*
import kotlinx.android.synthetic.main.coins_in_wallet_layout.*
import kotlinx.android.synthetic.main.fragment_bank.*
import kotlinx.android.synthetic.main.fragment_wallet.*
import org.joda.time.DateTime
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.concurrent.schedule


class BankFragment: Fragment(), AdapterView.OnItemSelectedListener{
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

    // Spinner selected currency
    private var selectedCurrency = "DOLR"

    // RecyclerView for collected coins
    private lateinit var mRecyclerViewItem: RecyclerView
    private lateinit var mAdapter: LoanRecyclerViewAdapter
    private lateinit var mLayoutManager: RecyclerView.LayoutManager
    //private lateinit var cardViewItemList: ArrayList<LoanCardItem>

    // Loan add cards
    private lateinit var  listOfLoanCardItems :ArrayList<LoanCardItem>

    // Hidden bonus feature magnet <- click counter:
    private  var magnetActivateClickCounter: Int = 0



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {



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
        val createLoansSubtree = HashMap<String,Any>()
        createLoansSubtree["exists"] = true
        userDB.collection("loans").document("loanAds").set(createLoansSubtree, SetOptions.merge()).addOnSuccessListener { _->
            showBalances()
            userDB.collection("loans").document("loanAds").get().addOnSuccessListener{
                showCoinsInRecycler(it)

            }
        }

        total_gold_img.setOnClickListener {
            if (magnetActivateClickCounter == 25){


            }
            magnetActivateClickCounter += 1
        }


        val currencySpinner = lend_currency
        val adapter : ArrayAdapter<CharSequence> = ArrayAdapter
                .createFromResource(activity,R.array.currencies, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        currencySpinner.adapter = adapter
        currencySpinner.onItemSelectedListener = this

        loan_ad_submit.setOnClickListener{if(editText_loanValue.text.toString() != "")advertiseLoan()}

    }


    private fun setUpUser() {
        mAuth = FirebaseAuth.getInstance()
        currentUser = mAuth.currentUser
        email = currentUser?.email
        db = FirebaseFirestore.getInstance()
        if (email != null)
            userDB = db.collection("users").document(email!!)
    }

    private fun  showBalances(){
        userDB.collection("bank").document("currencies").get().addOnCompleteListener {
            if(it.result!!.exists()){
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
            }

            total_value_gold.text = "%.0f".format(totalValueInGold())
        }

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

    private fun advertiseLoan() {
        val valString = editText_loanValue.text.toString()
        val value = valString.toDouble()
        userDB.collection("bank").document("currencies").get().addOnSuccessListener {
            if (it.exists()) {
                val currenciesData = it.data as HashMap<String, Double>
                val balance = currenciesData[selectedCurrency]
                if (balance != null && balance >= value && email != null) {

                    val loanAdDataPath = db.collection("loanAds")
                    val loanAdHashMap = HashMap<String, Any>()
                    loanAdHashMap["currency"] = selectedCurrency
                    loanAdHashMap["value"] = value
                    loanAdHashMap["userEmail"] = email!!
                    loanAdDataPath.add(loanAdHashMap).addOnSuccessListener {
                        val loanId = it.id
                        val id = HashMap<String, Any?>()
                        id[loanId] = null
                        userDB.collection("loans").document("loanAds").set(id, SetOptions.merge())
                        userDB.collection("bank").document("currencies").update(selectedCurrency,balance-value)
                        loan_ad_submit.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check_mark_8,0,0,0)
                        loan_ad_submit.text = "Submitted"
                        val scaleNewY = PropertyValuesHolder.ofFloat(View.SCALE_Y,0.7f,1f)
                        val scaleNewX = PropertyValuesHolder.ofFloat(View.SCALE_X,0.7f,1f)

                        ObjectAnimator.ofPropertyValuesHolder(loan_ad_submit,scaleNewX,scaleNewY).apply {
                            interpolator = AnticipateInterpolator()
                        }.start()
                        Timer("main",false).schedule(1600) {
                            activity?.runOnUiThread(java.lang.Runnable {
                                loan_ad_submit.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_invest_lend,0,0,0)
                                loan_ad_submit.text = "Create Loan Ad"
                                val alphaNew = PropertyValuesHolder.ofFloat(View.ALPHA,0f,1f)


                                ObjectAnimator.ofPropertyValuesHolder(loan_ad_submit, alphaNew).apply {
                                    interpolator = AnticipateInterpolator()
                                }.start()
                            })
                        }
                        showBalances()
                    }
                }else{
                    val alert = AlertDialog.Builder(this.requireActivity())
                    alert.apply {
                        setPositiveButton("OK",null)
                        setCancelable(true)
                        setTitle("Not Enough Balance to Proceed")
                        setMessage("You don't have enough ${selectedCurrency}s to lend that amount.")
                        create().show()
                    }
                }
            }else{
                val alert = AlertDialog.Builder(this.requireActivity())
                alert.apply {
                    setPositiveButton("OK",null)
                    setCancelable(true)
                    setTitle("Not Enough Balance to Proceed")
                    setMessage("You don't have enough ${selectedCurrency}s to lend that amount.")
                    create().show()
                }
            }

            editText_loanValue.text = null
        }
    }


    private fun showCoinsInRecycler(documentSnapshot: DocumentSnapshot) {
        listOfLoanCardItems = ArrayList<LoanCardItem>()
        db.collection("loanAds").get().addOnSuccessListener {querySnap ->
            querySnap.forEach {loanAd ->

                    if(documentSnapshot.exists()){
                        val currentUserAdvertisedLoans = documentSnapshot.data as HashMap<String,Any?>
                        if (loanAd.id !in currentUserAdvertisedLoans.keys){
                            val adHashMap = loanAd.data as HashMap<String,Any>
                            val loanId = loanAd.id
                            val loan_currency = adHashMap["currency"] as String
                            val loan_value = adHashMap["value"] as Double
                            val fromEmail = adHashMap["userEmail"] as String
                            val interest_rate = 0.15
                            val repayPeriod = 5
                            when (loan_currency) {
                                DOLR -> {
                                    listOfLoanCardItems.add(LoanCardItem(R.drawable.coin_dolr, loan_currency,loan_value,interest_rate,repayPeriod
                                            ,loanId, fromEmail))
                                }
                                SHIL -> {
                                    listOfLoanCardItems.add(LoanCardItem(R.drawable.coin_shil, loan_currency,loan_value,interest_rate,repayPeriod
                                            ,loanId,fromEmail))
                                }

                                QUID -> {
                                    listOfLoanCardItems.add(LoanCardItem(R.drawable.coin_quid, loan_currency,loan_value,interest_rate,repayPeriod,
                                            loanId,fromEmail))
                                }

                                PENY -> {
                                    listOfLoanCardItems.add(LoanCardItem(R.drawable.coin_peny, loan_currency,loan_value,interest_rate,repayPeriod
                                            ,loanId,fromEmail))
                                }


                            }
                            noCoinsTextToggle(listOfLoanCardItems.isEmpty())
                        }
                    }




            }

            mRecyclerViewItem = recycler_view_coins
            //mRecyclerViewItem.setHasFixedSize(true)
            mLayoutManager = LinearLayoutManager(activity)
            mAdapter = LoanRecyclerViewAdapter(listOfLoanCardItems)
            mRecyclerViewItem.layoutManager = mLayoutManager
            mRecyclerViewItem.adapter = mAdapter

            setClickListenerOnRecyclerViewItemClick()
        }





    }

    private fun noCoinsTextToggle(hasNoCoins: Boolean){
        if (hasNoCoins){
            no_ads.visibility = View.VISIBLE
        }else{
            no_ads.visibility = View.GONE
        }

    }



    private fun setClickListenerOnRecyclerViewItemClick() {
        mAdapter.setOnItemClickListener { position ->
            //cardViewItemList.get(index = position)
            val clickedcard = listOfLoanCardItems.get(position)
            val takenLoansPath = userDB.collection("loans").document("loansTaken")
            val loanCurrency =  clickedcard.currency
            val loanAmount = clickedcard.value

            val loanDetails = HashMap<String,Any>()
            loanDetails["value"] = loanAmount
            loanDetails["currency"] = loanCurrency
            loanDetails["interest"] = clickedcard.interestRate
            loanDetails["repayPeriod"] = clickedcard.repayPeriod
            loanDetails["from"] = clickedcard.email
            loanDetails["dateTaken"] = Timestamp(Date())

            val loanWrapperId = HashMap<String,Any>()
            loanWrapperId[clickedcard.id] = loanDetails

            takenLoansPath.set(loanWrapperId, SetOptions.merge()).addOnSuccessListener {
                val bankCurrenciesPath = userDB.collection("bank").document("currencies")
                bankCurrenciesPath.get().addOnCompleteListener{
                    var balance : Double = 0.0
                    if(it.result!!.exists()){
                        val bankCurrencies = it.result?.data as HashMap<String,Double>
                        if (bankCurrencies[loanCurrency] != null) {
                            balance = bankCurrencies[loanCurrency]!!
                        }
                    }
                    val updatedBalance = hashMapOf<String,Any>(loanCurrency to (balance + loanAmount))
                    bankCurrenciesPath.set(updatedBalance, SetOptions.merge())
                            .addOnSuccessListener {showBalances()}
                }

            }
            removeItem(position)
            db.collection("loanAds").document(clickedcard.id).delete()
        }
    }

    private fun removeItem(position: Int){
        listOfLoanCardItems.removeAt(position)
        mAdapter.notifyItemRemoved(position)
    }





    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, p3: Long) {
        val currency : String = parent?.getItemAtPosition(position) as String
        selectedCurrency = currency
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {

    }
}