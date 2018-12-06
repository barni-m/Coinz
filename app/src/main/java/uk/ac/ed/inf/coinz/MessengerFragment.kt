package uk.ac.ed.inf.coinz

import android.os.Bundle
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


class MessengerFragment: Fragment(){

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
    private  lateinit var extraInfo: ArrayList<ExtraInfo>

    // All currencies
    private val DOLR = "DOLR"
    private val SHIL = "SHIL"
    private val PENY = "PENY"
    private val QUID = "QUID"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_messenger, container,false)
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
        userDB.collection("bank").document("numberOfCoinsAddedTodayToBank").get().addOnCompleteListener {
            if (it.result!!.exists()){
                val counter = it.result!!.data as HashMap<String,Int>
                if (counter["n"] as Long >= 25.0){
                    createListOfMessagableCoins()
                }else{
                    noSpareChange.visibility = View.VISIBLE
                }
            }else{
                noSpareChange.visibility = View.VISIBLE
            }
        }
    }

    private fun setUpUser() {
        mAuth = FirebaseAuth.getInstance()
        currentUser = mAuth.currentUser
        email = currentUser?.email
        db = FirebaseFirestore.getInstance()
        if (email != null)
            userDB = db.collection("users").document(email!!)
    }


    private fun showPendingRepaymentsInRecycler() {
        mRecyclerViewItem = recycler_view_coins
        //mRecyclerViewItem.setHasFixedSize(true)
        mLayoutManager = LinearLayoutManager(activity)
        mAdapter = RepayRecyclerAdapter(cardViewItemList)
        mRecyclerViewItem.layoutManager = mLayoutManager
        mRecyclerViewItem.adapter = mAdapter
        setClickListenerOnRecyclerViewItemClick()
    }

    private fun setClickListenerOnRecyclerViewItemClick(){
        mAdapter.setOnItemClickListener { position ->
            val clickedCard = cardViewItemList.get(position)
            val currency = clickedCard.currency
            val repayTo = clickedCard.email
            val repayReference = db.collection("users").document(repayTo)
                    .collection("bank").document("currencies")
            repayReference.get().addOnCompleteListener {
                if(it.result!!.exists()){
                    val currenciesMap = it.result!!.data as HashMap<String,Double>
                    for ((bank_currency,balance) in currenciesMap){
                        if (currency == bank_currency){
                            val updatedBalance = balance + clickedCard.value
                            repayReference.update(bank_currency,updatedBalance)
                        }
                    }
                }
            }
            val currentUserBankRef = userDB.collection("bank").document("currencies")
            currentUserBankRef.get().addOnCompleteListener {
                if(it.result!!.exists()){
                    val currenciesMap = it.result!!.data as HashMap<String,Double>
                    for ((bank_currency,balance) in currenciesMap){
                        if (currency == bank_currency){
                            val updatedBalance = balance - clickedCard.value
                            currentUserBankRef.update(bank_currency,updatedBalance)
                        }
                    }
                }
            }

            val deleteLoanTaken = HashMap<String,Any>()
            deleteLoanTaken[clickedCard.id] = FieldValue.delete()
            userDB.collection("loans").document("loansTaken").update(deleteLoanTaken)
            removeItemFromPayees(position)
            if (cardViewItemList.isNullOrEmpty()){
                noLoans.visibility = View.VISIBLE
            }
        }
    }

    private fun createListOfPayees(){

        userDB.collection("loans").document("loansTaken").get()
                .addOnSuccessListener {documentSnapshot ->
                    if (documentSnapshot.exists()){
                        for ((id,detailMap) in documentSnapshot.data as HashMap<String,HashMap<String,Any>>){

                                var item :RepayCardItem
                                val currency = detailMap["currency"] as String
                                val dateTaken = Timestamp(detailMap["dateTaken"] as Date)
                                val email = detailMap["from"] as String
                                val interest = detailMap["interest"] as Double
                                val repayPeriod = (detailMap["repayPeriod"] as Long).toInt()
                                val value = detailMap["value"] as Double
                                val coinImg = getCoinImg(currency)
                                item = RepayCardItem(currency,dateTaken,email,interest,repayPeriod,value,coinImg,id)
                                cardViewItemList.add(item)

                        }
                    }
                    if (cardViewItemList.isNullOrEmpty()){
                        noLoans.visibility = View.VISIBLE
                    }else{
                        showPendingRepaymentsInRecycler()
                    }


                }

    }

    private fun getCoinImg(currency: String): Int = when (currency){
        DOLR -> R.drawable.coin_dolr
        PENY -> R.drawable.coin_peny
        QUID -> R.drawable.coin_quid
        else -> R.drawable.coin_shil
    }

    private fun removeItemFromPayees(position: Int){
        cardViewItemList.removeAt(position)
        mAdapter.notifyItemRemoved(position)
    }


    private fun showMessagableCoinsInRecycler() {
        mRecyclerViewItem = recycler_view_coins_for_messaging
        //mRecyclerViewItem.setHasFixedSize(true)
        mLayoutManager = LinearLayoutManager(activity)
        mAdapterMessaging = MessageRecyclerAdapter(cardViewCoinsForMessaging)
        mRecyclerViewItem.layoutManager = mLayoutManager
        mRecyclerViewItem.adapter = mAdapterMessaging
        setClickListenerOnSendButton()
    }

    private fun createListOfMessagableCoins(){

        userDB.collection("wallet").document("todaysCollectedCoins").get()
                .addOnSuccessListener {
                    extraInfo = arrayListOf()
                    if(it.exists()){
                        val hashMapOfCoins = it.data as HashMap<String,HashMap<String,Any>>
                        for ((id,coinData) in hashMapOfCoins){
                            val extras = ExtraInfo
                            extras.id = id
                            extras.date = Timestamp(coinData["date"] as Date)
                            extraInfo.add(extras)
                            val keys = coinData.keys
                            var currency: String = DOLR
                            var value: Double = 0.0
                            for (key in keys){
                                if (key in listOf<String>(DOLR,PENY,QUID,SHIL)){
                                    currency = key
                                    value = coinData[key] as Double
                                }
                            }
                            val coinImg = getCoinImg(currency)
                            var from = email!!
                            if("from" in coinData.keys){
                                from = coinData["from"] as String
                            }
                            val cardViewItem =CardViewItem(coinImg,currency,"%.2f".format(value), from)
                            cardViewCoinsForMessaging.add(cardViewItem)
                        }
                        showMessagableCoinsInRecycler()
                    }

                }
    }

    companion object ExtraInfo{
        lateinit var id :String
        lateinit var date: Timestamp
    }

    private fun setClickListenerOnSendButton() {
        mAdapterMessaging.setOnItemClickListener { position ->
            val clickedCard = cardViewCoinsForMessaging.get(position)
            val extras = extraInfo?.get(position)
            val currency = clickedCard.text1
            val value = clickedCard.text2.toDouble()
            var id = extras!!.id
            val now = Timestamp(Date())

            var coinInfo= HashMap<String,Any>()

                val id_with_email = id + "_" + email
                val coinToSend = hashMapOf<String,Any>(id_with_email to coinInfo)
                coinInfo[currency] = value
                coinInfo["date"] = Timestamp(Date())
                if (email != null){
                    coinInfo["from"] = email!!


                val toEmail = emailTo.text
                if (toEmail.isEmpty()){
                    val alert = AlertDialog.Builder(this.requireActivity())
                    alert.apply {
                        setPositiveButton("OK",null)
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
                            toRef.set(coinToSend, SetOptions.merge())
                            val deleteMyCoin = hashMapOf<String,Any>(id to FieldValue.delete())
                            userDB.collection("wallet").document("todaysCollectedCoins")
                                    .update(deleteMyCoin)
                            removeItemFromSendableCoins(position)
                        } else {
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

    private fun removeItemFromSendableCoins(position: Int){
        cardViewCoinsForMessaging.removeAt(position)
        mAdapterMessaging.notifyItemRemoved(position)
    }

}