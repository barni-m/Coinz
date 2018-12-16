package uk.ac.ed.inf.coinz

import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.android.synthetic.main.coins_in_wallet_layout.*
import kotlinx.android.synthetic.main.coins_up_for_messaging.*
import kotlinx.android.synthetic.main.fragment_messenger.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


@Suppress("UNCHECKED_CAST")
class MessengerFragment : Fragment() {

    // user (Firebase):
    private lateinit var db: FirebaseFirestore
    private lateinit var mAuth: FirebaseAuth
    private var currentUser: FirebaseUser? = null
    private lateinit var userDB: DocumentReference
    private var email: String? = null

    // RecyclerView for collected coins
    private lateinit var mRecyclerViewItem: RecyclerView
    private lateinit var mAdapter: RepayRecyclerAdapter
    private lateinit var mLayoutManager: RecyclerView.LayoutManager
    private lateinit var cardViewItemList: ArrayList<RepayCardItem>

    private lateinit var mAdapterMessaging: MessageRecyclerAdapter
    private lateinit var cardViewCoinsForMessaging: ArrayList<CardViewItem>
    private lateinit var extraInfo: ArrayList<HashMap<String,Any>>

    companion object {
        // All currencies
        private const val DOLR = "DOLR"
        private const val SHIL = "SHIL"
        private const val PENY = "PENY"
        private const val QUID = "QUID"
    }



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_messenger, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setUpUser()


    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cardViewItemList = arrayListOf()
        cardViewCoinsForMessaging = arrayListOf()
        createListOfPayees()
        // check for network conncetion before accessing the network
        val connectivityManager = activity?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        if (activeNetworkInfo != null && activeNetworkInfo.isConnected) {
            // check if user has spare change and depending on that update UI
            userDB.collection("bank").document("numberOfCoinsAddedTodayToBank").get().addOnCompleteListener {

                if (it.result!!.exists() && it.result != null) {
                    val counter = it.result!!.data as HashMap<String, Int>
                    if (counter["n"]!! >= 25) {
                        // create spare changer recycler with spare change
                        createListOfMessagableCoins()
                    } else {
                        noSpareChange.visibility = View.VISIBLE
                    }
                } else {
                    noSpareChange.visibility = View.VISIBLE
                }
            }
        }else Snackbar.make(messenger_layout, "Please connect to a network!", Snackbar.LENGTH_LONG).show()
    }

    // set up user (FirebaseAuth & Firestore)
    private fun setUpUser() {
        mAuth = FirebaseAuth.getInstance()
        currentUser = mAuth.currentUser
        email = currentUser?.email
        db = FirebaseFirestore.getInstance()
        if (email != null)
            userDB = db.collection("users").document(email!!)
    }

    // show pending repayments in the top recycler dedicated for loans taken
    private fun showPendingRepaymentsInRecycler() {
        mRecyclerViewItem = recycler_view_coins
        mLayoutManager = LinearLayoutManager(activity)
        mAdapter = RepayRecyclerAdapter(cardViewItemList)
        mRecyclerViewItem.layoutManager = mLayoutManager
        mRecyclerViewItem.adapter = mAdapter
        setClickListenerOnRecyclerViewItemClick()
    }

    // click listener for repay button on loan card item
    private fun setClickListenerOnRecyclerViewItemClick() {
        mAdapter.setOnItemClickListener { position ->
            val clickedCard = cardViewItemList[position]
            val currency = clickedCard.currency
            val repayTo = clickedCard.email
            // adds repayed value to investors bank balance
            val repayReference = db.collection("users").document(repayTo)
                    .collection("bank").document("currencies")
            repayReference.get().addOnCompleteListener {
                if (it.result!!.exists()) {
                    val currenciesMap = it.result!!.data as HashMap<String, Double>
                    for ((bank_currency, balance) in currenciesMap) {
                        if (currency == bank_currency) {
                            val updatedBalance = balance + clickedCard.value
                            repayReference.update(bank_currency, updatedBalance)
                        }
                    }
                }
            }
            // deducts value from this users bank balance
            val currentUserBankRef = userDB.collection("bank").document("currencies")
            currentUserBankRef.get().addOnCompleteListener {
                if (it.result!!.exists()) {
                    val currenciesMap = it.result!!.data as HashMap<String, Double>
                    for ((bank_currency, balance) in currenciesMap) {
                        if (currency == bank_currency) {
                            val updatedBalance = balance - clickedCard.value
                            currentUserBankRef.update(bank_currency, updatedBalance)
                        }
                    }
                }
            }

            val deleteLoanTaken = HashMap<String, Any>()
            deleteLoanTaken[clickedCard.id] = FieldValue.delete()
            userDB.collection("loans").document("loansTaken").update(deleteLoanTaken)
            removeItemFromPayees(position)
            if (cardViewItemList.isNullOrEmpty()) {
                noLoans.visibility = View.VISIBLE
            }
        }
    }

    private fun createListOfPayees() {
        // create list of card items that are showing the loans taken that are pending repayment
        userDB.collection("loans").document("loansTaken").get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        for ((id, detailMap) in documentSnapshot.data as HashMap<String, HashMap<String, Any>>) {
                            var item: RepayCardItem
                            val currency = detailMap["currency"] as String
                            val dateTaken = Timestamp(detailMap["dateTaken"] as Date)
                            val email = detailMap["from"] as String
                            val interest = detailMap["interest"] as Double
                            val repayPeriod = (detailMap["repayPeriod"] as Long).toInt()
                            val value = detailMap["value"] as Double
                            val coinImg = getCoinImg(currency)
                            item = RepayCardItem(currency, dateTaken, email, interest, repayPeriod, value, coinImg, id)
                            cardViewItemList.add(item)
                        }
                    }
                    // toggle text depending on whther there are any non-repayed loans take by the user
                    if (cardViewItemList.isNullOrEmpty()) {
                        noLoans.visibility = View.VISIBLE
                    } else {
                        showPendingRepaymentsInRecycler()
                    }


                }

    }

    // function returns the coin image of specific currency based on its name
    private fun getCoinImg(currency: String): Int = when (currency) {
        DOLR -> R.drawable.coin_dolr
        PENY -> R.drawable.coin_peny
        QUID -> R.drawable.coin_quid
        else -> R.drawable.coin_shil
    }

    // remove recycler card from loan repayment recycler view
    private fun removeItemFromPayees(position: Int) {
        cardViewItemList.removeAt(position)
        mAdapter.notifyItemRemoved(position)
    }


    // showing spare change coins in recycler ready for messaging
    private fun showMessagableCoinsInRecycler() {
        mRecyclerViewItem = recycler_view_coins_for_messaging
        mLayoutManager = LinearLayoutManager(activity)
        mAdapterMessaging = MessageRecyclerAdapter(cardViewCoinsForMessaging)
        mRecyclerViewItem.layoutManager = mLayoutManager
        mRecyclerViewItem.adapter = mAdapterMessaging
        setClickListenerOnSendButton()
    }

    private fun createListOfMessagableCoins() {
        // creating list of card items that correspond to the spare change in the wallet
        userDB.collection("wallet").document("todaysCollectedCoins").get()
                .addOnSuccessListener {
                    extraInfo = arrayListOf()
                    if (it.exists()) {
                        val hashMapOfCoins = it.data as HashMap<String, HashMap<String, Any>>
                        for ((id, coinData) in hashMapOfCoins) {
                            val extras = HashMap<String,Any>()
                            extras["id"] = id
                            extras["date"] = Timestamp(coinData["date"] as Date)
                            extraInfo.add(extras)
                            val keys = coinData.keys
                            var currency: String = DOLR
                            var value = 0.0
                            for (key in keys) {
                                if (key in listOf(DOLR, PENY, QUID, SHIL)) {
                                    currency = key
                                    value = coinData[key] as Double
                                }
                            }
                            val coinImg = getCoinImg(currency)
                            var from = email!!
                            if ("from" in coinData.keys) {
                                from = coinData["from"] as String
                            }
                            val cardViewItem = CardViewItem(coinImg, currency, "%.2f".format(value), from)
                            cardViewCoinsForMessaging.add(cardViewItem)
                        }
                        showMessagableCoinsInRecycler()
                    }

                }
    }


    private fun setClickListenerOnSendButton() {
        mAdapterMessaging.setOnItemClickListener { position ->
            val clickedCard = cardViewCoinsForMessaging[position]
            val extras = extraInfo[position]
            extraInfo.removeAt(position)
            val currency = clickedCard.text1
            val value = clickedCard.text2.toDouble()
            val id = extras["id"] as String


            val coinInfo = HashMap<String, Any>()
            // adding email to id so no two coins have tha same ids
            val idWithEmail = id + "_" + email
            // a hashmap of the coin to be sent
            val coinToSend = hashMapOf<String, Any>(idWithEmail to coinInfo)
            coinInfo[currency] = value
            coinInfo["date"] = Timestamp(Date())
            // setting including email in sent coin
            if (email != null) {
                coinInfo["from"] = email!!

                // getting the email from the input field
                val toEmail = emailTo.text
                // alert user that no address was input if that's the case
                if (toEmail.isEmpty()) {
                    val alert = AlertDialog.Builder(this.requireActivity())
                    alert.apply {
                        setPositiveButton("OK", null)
                        setCancelable(true)
                        setTitle("No email address")
                        setMessage("Please fill out the \"To\" field with another palyers email address.")
                        create().show()
                    }
                } else {
                    val toRef = db.collection("users").document(toEmail.toString())
                            .collection("wallet").document("todaysCollectedCoins")
                    toRef.get().addOnCompleteListener {
                        if (it.result!!.exists()) {
                            // send coin and delete sender's coin instance
                            toRef.set(coinToSend, SetOptions.merge())
                            val deleteMyCoin = hashMapOf<String, Any>(id to FieldValue.delete())
                            val myWallet = userDB.collection("wallet").document("todaysCollectedCoins")
                            myWallet.update(deleteMyCoin)
                            // add to database document that exempts the coin from showing up on the map again
                            myWallet.parent.document("todaysCollectedAddedToBank").set(hashMapOf<String,Any>(id to true), SetOptions.merge())
                            removeItemFromSendableCoins(position)
                        } else {
                            db.collection("users").document(toEmail.toString()).get().addOnCompleteListener { recipient ->
                                if (recipient.result!!.exists()){
                                    toRef.set(coinToSend, SetOptions.merge())
                                }
                            }
                            // alert that user email is not registered on Coinz
                            val alert = AlertDialog.Builder(this.requireActivity())
                            alert.apply {
                                setPositiveButton("OK", null)
                                setCancelable(true)
                                setTitle("User doesn't exist")
                                setMessage("Please enter an email registered on Coinz.")
                                create().show()
                            }
                        }
                    }
                }

            }

        }
    }

    // remove coin from recycler view
    private fun removeItemFromSendableCoins(position: Int) {
        cardViewCoinsForMessaging.removeAt(position)
        mAdapterMessaging.notifyItemRemoved(position)
    }

}