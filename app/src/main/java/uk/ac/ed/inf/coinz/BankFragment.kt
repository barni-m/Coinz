package uk.ac.ed.inf.coinz

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
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
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import kotlinx.android.synthetic.main.coins_in_wallet_layout.*
import kotlinx.android.synthetic.main.fragment_bank.*
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.concurrent.schedule


class BankFragment : Fragment(), AdapterView.OnItemSelectedListener {
    // user (Firebase):
    private lateinit var db: FirebaseFirestore
    private lateinit var mAuth: FirebaseAuth
    private var currentUser: FirebaseUser? = null
    private lateinit var userDB: DocumentReference
    private var email: String? = null


    companion object {
        // All currencies
        private const val DOLR = "DOLR"
        private const val SHIL = "SHIL"
        private const val PENY = "PENY"
        private const val QUID = "QUID"
    }

    // Currency bank values
    private var valDOLR: Double = .0
    private var valPENY: Double = .0
    private var valQUID: Double = .0
    private var valSHIL: Double = .0
    private var gold: Double = .0

    // Shared Prefs
    private val preferencesFile = "PrefsFile" // for storing preferences
    private var settings: SharedPreferences? = null


    // Rates
    private lateinit var rates: JSONObject

    // Spinner selected currency
    private var selectedCurrency = "DOLR"

    // RecyclerView for collected coins
    private lateinit var mRecyclerViewItem: RecyclerView
    private lateinit var mAdapter: LoanRecyclerViewAdapter
    private lateinit var mLayoutManager: RecyclerView.LayoutManager
    //private lateinit var cardViewItemList: ArrayList<LoanCardItem>

    // Loan add cards
    private lateinit var listOfLoanCardItems: ArrayList<LoanCardItem>


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {


        return inflater.inflate(R.layout.fragment_bank, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setting up user and Firebase
        setUpUser()
        // getting the rates from the shared prefs file
        settings = activity?.getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        rates = JSONObject(settings?.getString("ratesJSONAsString", ""))


    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val createLoansSubtree = HashMap<String, Any>()
        createLoansSubtree["exists"] = true
        userDB.collection("loans").document("loanAds").set(createLoansSubtree, SetOptions.merge()).addOnSuccessListener {
            showBalances()
            // setting click listener for the gold coin hidden level up
            total_gold_img.setOnClickListener {
                if (gold >= 100000) {
                    val settings = activity?.getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
                    val editor = settings?.edit()
                    editor?.putBoolean("levelUp", true)
                    editor?.apply()
                    val intent = Intent(activity, MapActivity::class.java)
                    startActivity(intent)

                }
            }
            // filling the recycler view with loan ads
            userDB.collection("loans").document("loanAds").get().addOnSuccessListener { ad ->
                showCoinsInRecycler(ad)
            }
        }


        // create spinner for selecting currencies (loan ad creation)
        val currencySpinner = lend_currency
        val adapter: ArrayAdapter<CharSequence> = ArrayAdapter
                .createFromResource(activity, R.array.currencies, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        currencySpinner.adapter = adapter
        currencySpinner.onItemSelectedListener = this
        // advertise loan button click listener
        loan_ad_submit.setOnClickListener {
            if (editText_loanValue.text.toString() != "")
                advertiseLoan()
        }

    }


    private fun setUpUser() {
        mAuth = FirebaseAuth.getInstance()
        currentUser = mAuth.currentUser
        email = currentUser?.email
        db = FirebaseFirestore.getInstance()
        if (email != null)
        // getting the email of the user
            userDB = db.collection("users").document(email!!)
    }

    @SuppressLint("SetTextI18n")
    @Suppress("UNCHECKED_CAST")
    // show balances of the currencies in the bank
    private fun showBalances() {
        userDB.collection("bank").document("currencies").get().addOnCompleteListener {
            if (it.result!!.exists()) {
                val currenciesHashMap = it.result?.data as HashMap<String, Double>

                for ((currency, value) in currenciesHashMap) {
                    when (currency) {
                        DOLR -> {
                            bank_dolr_amount.text = "%.2f".format(value)
                            valDOLR = value
                        }
                        SHIL -> {
                            bank_shil_amount.text = "%.2f".format(value)
                            valSHIL = value
                        }
                        PENY -> {
                            bank_peny_amount.text = "%.2f".format(value)
                            valPENY = value
                        }
                        QUID -> {
                            bank_quid_amount.text = "%.2f".format(value)
                            valQUID = value
                        }
                    }
                }
            }
            // computing the value of all the money in the bank in gold
            gold = totalValueInGold()
            total_value_gold.text = "%.0f".format(gold)
        }

    }

    // converting to gold
    private fun totalValueInGold(): Double {
        val shilRate = rates[SHIL] as Double
        val dolrRate = rates[DOLR] as Double
        val quidRate = rates[QUID] as Double
        val penyRate = rates[PENY] as Double

        return (shilRate * valSHIL) +
                (dolrRate * valDOLR) +
                (quidRate * valQUID) +
                (penyRate * valPENY)
    }

    @Suppress("UNCHECKED_CAST")
    @SuppressLint("SetTextI18n")
    private fun advertiseLoan() {
        // the value the user wants to loan:
        val value = editText_loanValue.text.toString().toDouble()

        userDB.collection("bank").document("currencies").get().addOnSuccessListener {
            if (it.exists()) {
                val currenciesData = it.data as HashMap<String, Double>
                // current bank balance of the selected currency (selected in the spinner)
                val balance = currenciesData[selectedCurrency]
                // add to loanAds if ballance is sufficient
                if (balance != null && balance >= value && email != null) {
                    // add loan add to ads document in the database
                    val loanAdDataPath = db.collection("loanAds")
                    val loanAdHashMap = HashMap<String, Any>()
                    loanAdHashMap["currency"] = selectedCurrency
                    loanAdHashMap["value"] = value
                    loanAdHashMap["userEmail"] = email!!
                    loanAdDataPath.add(loanAdHashMap).addOnSuccessListener { loanAdRef ->
                        // if successful put the loand's id in the users database as reference
                        // & deduct amount from bank balance
                        val loanId = loanAdRef.id
                        val id = HashMap<String, Any?>()
                        id[loanId] = null
                        userDB.collection("loans").document("loanAds").set(id, SetOptions.merge())
                        userDB.collection("bank").document("currencies").update(selectedCurrency, balance - value)
                        // programmatically create SUBMITTED button
                        loan_ad_submit.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check_mark_8, 0, 0, 0)
                        loan_ad_submit.text = "Submitted"
                        val scaleNewY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.7f, 1f)
                        val scaleNewX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.7f, 1f)
                        // animate button on submission
                        ObjectAnimator.ofPropertyValuesHolder(loan_ad_submit, scaleNewX, scaleNewY).apply {
                            interpolator = AnticipateInterpolator()
                        }.start()
                        Timer("main", false).schedule(1600) {
                            activity?.runOnUiThread {
                                // after 1.6 seconds change button back to original (with fade in animation)
                                loan_ad_submit.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_invest_lend, 0, 0, 0)
                                loan_ad_submit.text = "Create Loan Ad"
                                val alphaNew = PropertyValuesHolder.ofFloat(View.ALPHA, 0f, 1f)
                                ObjectAnimator.ofPropertyValuesHolder(loan_ad_submit, alphaNew).apply {
                                    interpolator = AnticipateInterpolator()
                                }.start()
                            }
                        }
                        showBalances()
                    }
                } else {
                    // if balance is not sufficient to create loan alert the user
                    alertNonSufficientBalance()
                }
            } else {
                // if bank hasn't even been created alert user
                alertNonSufficientBalance()
            }
            editText_loanValue.text = null
        }
    }

    private fun alertNonSufficientBalance() {
        val alert = AlertDialog.Builder(this.requireActivity())
        alert.apply {
            setPositiveButton("OK", null)
            setCancelable(true)
            setTitle("Not Enough Balance to Proceed")
            setMessage("You don't have enough ${selectedCurrency}s to lend that amount.")
            create().show()
        }
    }

    // shows coin ads in recycler view for users so they can borrow the amount
    // self advertised loans are not shown
    private fun showCoinsInRecycler(documentSnapshot: DocumentSnapshot) {
        listOfLoanCardItems = ArrayList()
        db.collection("loanAds").get().addOnSuccessListener { querySnap ->
            querySnap.forEach { loanAd ->
                //for each loan ad a a card is created and added to the recycler
                if (documentSnapshot.exists()) {
                    val currentUserAdvertisedLoans = documentSnapshot.data as HashMap<String, Any?>
                    if (loanAd.id !in currentUserAdvertisedLoans.keys) {
                        val adHashMap = loanAd.data as HashMap<String, Any>
                        val loanId = loanAd.id
                        val loanCurrency = adHashMap["currency"] as String
                        val loanValue = adHashMap["value"] as Double
                        val fromEmail = adHashMap["userEmail"] as String
                        val interestRate = 0.15
                        val repayPeriod = 5
                        when (loanCurrency) {
                            DOLR -> {
                                listOfLoanCardItems.add(LoanCardItem(R.drawable.coin_dolr, loanCurrency, loanValue, interestRate, repayPeriod
                                        , loanId, fromEmail))
                            }
                            SHIL -> {
                                listOfLoanCardItems.add(LoanCardItem(R.drawable.coin_shil, loanCurrency, loanValue, interestRate, repayPeriod
                                        , loanId, fromEmail))
                            }

                            QUID -> {
                                listOfLoanCardItems.add(LoanCardItem(R.drawable.coin_quid, loanCurrency, loanValue, interestRate, repayPeriod,
                                        loanId, fromEmail))
                            }

                            PENY -> {
                                listOfLoanCardItems.add(LoanCardItem(R.drawable.coin_peny, loanCurrency, loanValue, interestRate, repayPeriod
                                        , loanId, fromEmail))
                            }


                        }
                        // show or hide text stating that no adds are available
                        noCoinsTextToggle(listOfLoanCardItems.isEmpty())
                    }
                }


            }
            // create recycler
            mRecyclerViewItem = recycler_view_coins
            mLayoutManager = LinearLayoutManager(activity)
            mAdapter = LoanRecyclerViewAdapter(listOfLoanCardItems)
            mRecyclerViewItem.layoutManager = mLayoutManager
            mRecyclerViewItem.adapter = mAdapter
            setClickListenerOnRecyclerViewItemClick()
        }


    }

    // show or hide text stating that no adds are available
    private fun noCoinsTextToggle(hasNoCoins: Boolean) {
        if (hasNoCoins) {
            no_ads.visibility = View.VISIBLE
        } else {
            no_ads.visibility = View.GONE
        }
    }

    // click listener for loan button on loan add items
    @Suppress("UNCHECKED_CAST")
    private fun setClickListenerOnRecyclerViewItemClick() {
        mAdapter.setOnItemClickListener { position ->
            // put loan details in a hash map and send to users loansTaken document
            val clickedcard = listOfLoanCardItems[position]
            val takenLoansPath = userDB.collection("loans").document("loansTaken")
            val loanCurrency = clickedcard.currency
            val loanAmount = clickedcard.value

            val loanDetails = HashMap<String, Any>()
            loanDetails["value"] = loanAmount
            loanDetails["currency"] = loanCurrency
            loanDetails["interest"] = clickedcard.interestRate
            loanDetails["repayPeriod"] = clickedcard.repayPeriod
            loanDetails["from"] = clickedcard.email
            loanDetails["dateTaken"] = Timestamp(Date())

            // loan detail hashmap is the value of the loan id (which is the key) to prevent overwriting
            val loanWrapperId = HashMap<String, Any>()
            loanWrapperId[clickedcard.id] = loanDetails

            takenLoansPath.set(loanWrapperId, SetOptions.merge()).addOnSuccessListener {
                val bankCurrenciesPath = userDB.collection("bank").document("currencies")
                bankCurrenciesPath.get().addOnCompleteListener { bankBalances ->
                    var balance = 0.0
                    // get current balance if it exists and update balance variable
                    if (bankBalances.result!!.exists()) {
                        val bankCurrencies = bankBalances.result?.data as HashMap<String, Double>
                        if (bankCurrencies[loanCurrency] != null) {
                            balance = bankCurrencies[loanCurrency]!!
                        }
                    }
                    // add loan amount to balance variable and update the bank
                    val updatedBalance = hashMapOf<String, Any>(loanCurrency to (balance + loanAmount))
                    bankCurrenciesPath.set(updatedBalance, SetOptions.merge())
                            .addOnSuccessListener { showBalances() }
                }

            }
            // remove card from recycler and the ad from the database
            removeItem(position)
            db.collection("loanAds").document(clickedcard.id).delete()
        }
    }

    // method for removing recycler card at given position
    private fun removeItem(position: Int) {
        listOfLoanCardItems.removeAt(position)
        mAdapter.notifyItemRemoved(position)
    }

    // implementation of OnItemSelectedListener for spinner
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, p3: Long) {
        val currency: String = parent?.getItemAtPosition(position) as String
        selectedCurrency = currency
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
    }
}