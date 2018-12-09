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
import android.transition.TransitionManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnticipateInterpolator
import android.widget.Toast
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import kotlinx.android.synthetic.main.coins_in_wallet_layout.*
import kotlinx.android.synthetic.main.fragment_wallet.*

import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class WalletFragment : Fragment() {

    // user (Firebase):
    private lateinit var db: FirebaseFirestore
    private lateinit var mAuth: FirebaseAuth
    private var currentUser: FirebaseUser? = null
    private lateinit var userDB: DocumentReference
    private var email: String? = null

    // user coins in wallet
    private var coinsDOLR: ArrayList<HashMap<String,Any>> = arrayListOf()
    private var coinsPENY: ArrayList<HashMap<String,Any>> = arrayListOf()
    private var coinsSHIL: ArrayList<HashMap<String,Any>> = arrayListOf()
    private var coinsQUID: ArrayList<HashMap<String,Any>> = arrayListOf()




    // Currently selected coin currency
    private var currentCurrency :String? = "DOLR"
    private val preferencesFile = "WalletPrefsFile" // for storing preferences
    private var settings: SharedPreferences? = null


    // RecyclerView for collected coins
    private lateinit var mRecyclerViewItem: RecyclerView
    private lateinit var mAdapter: RecyclerViewAdapter
    private lateinit var mLayoutManager: RecyclerView.LayoutManager
    private lateinit var cardViewItemList: ArrayList<CardViewItem>




    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_wallet, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // initialise Firebase user
        setUpUser()
        // get the last selected currency from shared preferences and put it in local variable
        settings= activity?.getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        currentCurrency = settings?.getString("currentCurrency", DOLR)

    }

    @Suppress("UNCHECKED_CAST")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // group coins to currencies and show the selected one in the recycler after getting the contents of the wallet
        val collectedCoinsRef = userDB.collection("wallet").document("todaysCollectedCoins")
        collectedCoinsRef.get().addOnCompleteListener { coins ->
            if(coins.result!!.exists()) {
                separateValuesToCurrenies(coins.result?.data  as HashMap<String, HashMap<String, Any>>)
                if (currentCurrency != null) {
                    showCoinsInRecycler(currentCurrency!!)
                }
            }
        }
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // set up initially selecte currency
        var currentCoinView: View? = null
        when (currentCurrency) {
            DOLR -> currentCoinView = coin_dolr
            SHIL -> currentCoinView = coin_shil
            QUID -> currentCoinView = coin_quid
            PENY -> currentCoinView = coin_peny
        }
        coin_currency_name.text = currentCurrency


        // display initially selected coins
        if (currentCoinView != null) {
            val a = currentCoinView.id
            placeholder_coin.setContentId(a)
        }
        // make initiyally selected coin grow in size
        val scaleNewY = PropertyValuesHolder.ofFloat(View.SCALE_Y,1f,1.3f)
        val scaleNewX = PropertyValuesHolder.ofFloat(View.SCALE_X,1f,1.3f)
        ObjectAnimator.ofPropertyValuesHolder(currentCoinView,scaleNewX,scaleNewY).apply {
            interpolator = AnticipateInterpolator()
        }.setDuration(0).start()


        /* set click listener for each coin:
            swap selected currency in UI
            change currently selected  currency variable*/
        coin_shil.setOnClickListener {
            swapView(it)
            currentCurrency = SHIL
            showCoinsInRecycler(SHIL)
        }
        coin_dolr.setOnClickListener {
            swapView(it)
            currentCurrency = DOLR
            showCoinsInRecycler(DOLR)
        }
        coin_quid.setOnClickListener {
            swapView(it)
            currentCurrency = QUID
            showCoinsInRecycler(QUID)
        }
        coin_peny.setOnClickListener {
            swapView(it)
            currentCurrency = PENY
            showCoinsInRecycler(PENY)
        }

    }

    // initialise Firebase and get user's personal database (userDB) as well as their email (email)
    private fun setUpUser() {
        mAuth = FirebaseAuth.getInstance()
        currentUser = mAuth.currentUser
        email = currentUser?.email
        db = FirebaseFirestore.getInstance()
        if (email != null)
            userDB = db.collection("users").document(email!!)
    }

    // create lists of the available coin values
    private fun separateValuesToCurrenies(coins: HashMap<String, HashMap<String, Any>>) {
        coinsDOLR.clear()
        coinsPENY.clear()
        coinsQUID.clear()
        coinsSHIL.clear()

        for((_,coin)in coins){
            var currency = ""
            var coinValue = 0.0
            var fromEmail: String = email!!
            val coinDetailKeys = coin.keys
            if ("from" in coinDetailKeys){
                fromEmail = coin["from"] as String
            }
            for (detail_key in coinDetailKeys){
                if (detail_key in listOf(DOLR,PENY,SHIL,QUID)){
                    currency = detail_key
                    coinValue = coin[detail_key] as Double
                }
            }

            val valWithMail = hashMapOf("value" to coinValue, "from" to fromEmail)

            when (currency){
                DOLR -> coinsDOLR.add(valWithMail)
                SHIL -> coinsSHIL.add(valWithMail)
                QUID -> coinsQUID.add(valWithMail)
                PENY -> coinsPENY.add(valWithMail)
            }
        }
    }



    private fun swapView(v: View){
        // previously selected currency
        val oldCoin=placeholder_coin.content
        // animation of the shrinkage of the no longer selected currency's coin
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y,1.3f,1f)
        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X,1.3f,1f)
        ObjectAnimator.ofPropertyValuesHolder(oldCoin,scaleX,scaleY).apply {
            interpolator = AnticipateInterpolator()
        }.start()

        // replace currently selected coin
        TransitionManager.beginDelayedTransition(constraint_layout_wallet)
        placeholder_coin.setContentId(v.id)

        // animate coin growh after click
        val scaleNewY = PropertyValuesHolder.ofFloat(View.SCALE_Y,0.7f,1.3f)
        val scaleNewX = PropertyValuesHolder.ofFloat(View.SCALE_X,0.7f,1.3f)
        ObjectAnimator.ofPropertyValuesHolder(v,scaleNewX,scaleNewY).apply {
            interpolator = AnticipateInterpolator()
        }.start()

        // show the selected currency in the Textview coin_currency_name
        when (v) {
            coin_dolr -> coin_currency_name.text = DOLR
            coin_shil -> coin_currency_name.text = SHIL
            coin_quid -> coin_currency_name.text = QUID
            coin_peny -> coin_currency_name.text = PENY

        }

        // animate the fade in of the currency's name
        val alpha = PropertyValuesHolder.ofFloat(View.ALPHA,0f,1f)
        ObjectAnimator.ofPropertyValuesHolder(coin_currency_name,alpha).apply {
            interpolator = AnticipateInterpolator()
        }.start()
    }


    // show the selected currency's collected/received coins in recycler
    private fun showCoinsInRecycler(currency: String) {
        cardViewItemList = arrayListOf()
        when (currency) {
            DOLR -> {
                cardViewItemList = createCardList(coinsDOLR, R.drawable.coin_dolr, currency)
                noCoinsTextToggle(coinsDOLR.isEmpty())
            }
            SHIL -> {
                cardViewItemList = createCardList(coinsSHIL, R.drawable.coin_shil, currency)
                noCoinsTextToggle(coinsSHIL.isEmpty())
            }

            QUID -> {
                cardViewItemList = createCardList(coinsQUID, R.drawable.coin_quid, currency)
                noCoinsTextToggle(coinsQUID.isEmpty())
            }

            PENY -> {
                cardViewItemList = createCardList(coinsPENY, R.drawable.coin_peny, currency)
                noCoinsTextToggle(coinsPENY.isEmpty())
            }


        }
        // put selected coins in the recycler
        mRecyclerViewItem = recycler_view_coins
        mLayoutManager = LinearLayoutManager(activity)
        mAdapter = RecyclerViewAdapter(cardViewItemList)
        mRecyclerViewItem.layoutManager = mLayoutManager
        mRecyclerViewItem.adapter = mAdapter
        setClickListenerOnRecyclerViewItemClick()
    }

    // Show or hide the text stating no coins are available
    private fun noCoinsTextToggle(hasNoCoins: Boolean){
        if (hasNoCoins){
            no_coin_of_selected_currency_text_view.visibility = View.VISIBLE
        }else{
            no_coin_of_selected_currency_text_view.visibility = View.GONE
        }
    }

    //Click listener for bankcard icon. Adds the clicked  card's coin to bank if allowed.
    private fun setClickListenerOnRecyclerViewItemClick() {
        mAdapter.setOnItemClickListener { position ->
            // get the selected coin from the card view objects containing the coins
            val clickedcard = cardViewItemList[position]
            // retrieve the counter (counts the number of coins added to the bank on the current day)
            userDB.collection("bank").document("numberOfCoinsAddedTodayToBank").get()
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            // check if counter exists
                            if (it.result!!.exists()) {
                                val counterNHashMap = it.result?.data as java.util.HashMap<String, Any>
                                val counterValue = counterNHashMap["n"] as Long
                                if (currentCurrency != null
                                        //  check if counter is less than the limit if the card was collected by current user
                                        && ((counterValue < 25 && clickedcard.from == email)
                                                // check if counter is greater tha or equal to 25 if it was received from another user
                                                || (counterValue >= 25 && clickedcard.from != email))) {
                                    // add to bank
                                    addToBank(clickedcard.text2, currentCurrency!!, clickedcard.from)
                                    removeItem(position)
                                } else {
                                    // alert user: Denied Transaction
                                    val alert = AlertDialog.Builder(this.requireActivity())
                                    alert.apply {
                                        setPositiveButton("OK", null)
                                        setCancelable(true)
                                        setTitle("Transaction Denied")
                                        setMessage("Sorry, you cannot add more than 25 collected coins to the bank within a day," +
                                                " and you may only add coins received from others to the bank once you've" +
                                                " deposited 25 collected coins")
                                        create().show()
                                    }
                                }
                            } else {
                                // if it doesn't exist it means that no coins have been collected this day
                                if (currentCurrency != null) {
                                    // thus we add coin to bank and remove the card from the recycler view
                                    addToBank(clickedcard.text2, currentCurrency!!, clickedcard.from)
                                    removeItem(position)
                                }
                            }
                        }
                    }
        }
    }

    // remove card associated with coin
    private fun removeItem(position: Int){
        cardViewItemList.removeAt(position)
        mAdapter.notifyItemRemoved(position)
    }
    // add coin to bank
    @Suppress("UNCHECKED_CAST")
    private fun addToBank(amount: String, currency: String, from: String){
        val collectedCoinsRef = userDB.collection("wallet").document("todaysCollectedCoins")
        collectedCoinsRef.get().addOnCompleteListener{ todayCollected ->
            // retrieve the coins collected today so they can be checked and removed
            val mapOfCollectedCoins = todayCollected.result?.data as HashMap<String, HashMap<String,Any>>
            loop@ for ((id, coin) in mapOfCollectedCoins) {
                if (currency in coin.keys){
                    // the value of the coin without rounding
                    val actualCoinValue = coin[currency] as Double
                    // recreate rounding for comparison purposes
                    val actualValRoundedString: String = "%.2f".format(actualCoinValue)
                    // remove coin from wallet and add to bank then update counter
                    if (actualValRoundedString == amount){
                        val bankPath = userDB.collection("bank")
                        val bankCurrenciesPath = bankPath.document("currencies")

                        bankCurrenciesPath.get()
                                .addOnCompleteListener {
                                    if (it.isSuccessful){
                                        var balance: Double
                                        if (it.result!!.exists()){
                                            val currencyValuesInBank = it.result?.data as HashMap
                                            if (currencyValuesInBank[currency] != null){
                                                balance = currencyValuesInBank[currency] as Double
                                                balance += actualCoinValue
                                                // update the bank balance if the wanted currency exists
                                                bankCurrenciesPath.update(currency,balance)
                                            }else{
                                                // create new instance of currency in the bank with the
                                                // current coin's value as the balance
                                                val newCurrencyValue=  HashMap<String,Any>()
                                                newCurrencyValue[currency] = actualCoinValue
                                                bankCurrenciesPath.set(newCurrencyValue, SetOptions.merge())
                                            }
                                            // update the counter, add coin to the list of coins added to bank
                                            // today so they can be retrieved so they won't who up
                                            // on map
                                            updateCounter(bankPath)
                                            addIdToDeletedCoins(id)
                                            deleteCoinFromWalletFragmentList(actualCoinValue, currency,from)
                                        }else{
                                            // if bank doesn't yet exist create it and add the new balace
                                            val newCurrencyValue=  HashMap<String,Any>()
                                            newCurrencyValue[currency]= actualCoinValue
                                            bankCurrenciesPath.set(newCurrencyValue)
                                            updateCounter(bankPath)
                                            addIdToDeletedCoins(id)
                                        }
                                    }else{
                                        // if Firestore couldn't be reached
                                        Log.d(TAG, "Couldn't reach firebase.")
                                    }

                                }
                        // delete coin from wallet/todaysCollectedCoins
                        val deleteCoin =  HashMap<String,Any>()
                        deleteCoin[id] = FieldValue.delete()
                        collectedCoinsRef.update(deleteCoin)
                        break@loop
                    }

                }
            }
        }.addOnFailureListener {
            Toast.makeText(activity,"ERROR: Failed to add coin to bank.", Toast.LENGTH_LONG).show()
        }
    }

    // update the counter counting the number of coins added to the bank today
    private fun updateCounter(bankPath: CollectionReference) {
        val counterPath = bankPath.document("numberOfCoinsAddedTodayToBank")
        counterPath.get().addOnCompleteListener {
            if (it.isSuccessful) {
                var n: Long
                val date = Timestamp(Date())
                if (it.result!!.exists()) {
                    // add 1 to counter value
                    val counterNHashMap = it.result?.data as HashMap<String, Any>
                    n = counterNHashMap["n"] as Long
                    n += 1
                    counterPath.update("n", n)
                    // record the date of the counter update
                    counterPath.update("date",date)
                } else {
                    // if no coins were added to bank today then create counter and set it to 1
                    val newCounter = HashMap<String, Any>()
                    newCounter["n"] = 1
                    newCounter["date"] = date
                    counterPath.set(newCounter)
                }
            }
        }
    }

    // add the currently moved coin's id to list of coins added to bank for reference
    private fun addIdToDeletedCoins(id: String){
        val coinId = HashMap<String, Any>()
        coinId[id] = true
        userDB.collection("wallet").document("todaysCollectedAddedToBank").set(coinId, SetOptions.merge())
    }

    // delete the coin value from the list that's storing currency-wise coin values
    private fun deleteCoinFromWalletFragmentList(actualCoinValue: Double, currency: String,from: String){
        val valWithEmail = HashMap<String,Any>()
        valWithEmail["value"] = actualCoinValue
        valWithEmail["from"] = from
        when (currency) {
            DOLR -> coinsDOLR.remove(valWithEmail)
            SHIL -> coinsSHIL.remove(valWithEmail)
            QUID -> coinsQUID.remove(valWithEmail)
            PENY -> coinsPENY.remove(valWithEmail)

        }
    }

    //  create the list of card objects with coins
    private fun createCardList(coinsSelected: ArrayList<HashMap<String,Any>>, imageCoin: Int, currency: String): ArrayList<CardViewItem> {
        val cardViewItemList: ArrayList<CardViewItem> = arrayListOf()
        for (coin in coinsSelected)
            cardViewItemList.add(CardViewItem(imageCoin, currency, "%.2f".format(coin["value"] as Double),coin["from"] as String))
        return cardViewItemList
    }

    override fun onStop() {
        super.onStop()
        // save currently selected coin to shared preferences so on next start
        // it's still the one selected
        val settings = activity?.getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        val editor = settings?.edit()
        editor?.putString("currentCurrency", currentCurrency)
        editor?.apply()
    }

    companion object {
        private const val TAG = "WalletFragment"
        // All currencies
        private const val DOLR = "DOLR"
        private const val SHIL = "SHIL"
        private const val PENY = "PENY"
        private const val QUID = "QUID"
    }
}

