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

    private val TAG = "WalletFragment"

    // user (Firebase):
    private lateinit var db: FirebaseFirestore
    private lateinit var mAuth: FirebaseAuth
    private var currentUser: FirebaseUser? = null
    private lateinit var userDB: DocumentReference
    private var email: String? = null

    // user coins in wallet
    private var coinsDOLR: ArrayList<HashMap<String,Any>> = arrayListOf<HashMap<String,Any>>()
    private var coinsPENY: ArrayList<HashMap<String,Any>> = arrayListOf<HashMap<String,Any>>()
    private var coinsSHIL: ArrayList<HashMap<String,Any>> = arrayListOf<HashMap<String,Any>>()
    private var coinsQUID: ArrayList<HashMap<String,Any>> = arrayListOf<HashMap<String,Any>>()




    // Currently selected coin currency
    private var currentCurrency :String? = "DOLR"
    private val preferencesFile = "WalletPrefsFile" // for storing preferences
    private var settings: SharedPreferences? = null


    // All currencies
    private val DOLR = "DOLR"
    private val SHIL = "SHIL"
    private val PENY = "PENY"
    private val QUID = "QUID"

    // RecyclerView for collected coins
    private lateinit var mRecyclerViewItem: RecyclerView
    private lateinit var mAdapter: RecyclerViewAdapter
    private lateinit var mLayoutManager: RecyclerView.LayoutManager
    private lateinit var cardViewItemList: ArrayList<CardViewItem>




    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        recycler_view_coins

        return inflater.inflate(R.layout.fragment_wallet, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setUpUser()
        recycler_view_coins // todo delete these lines
        settings= activity?.getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        currentCurrency = settings?.getString("currentCurrency", DOLR)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recycler_view_coins
        val collectedCoinsRef = userDB.collection("wallet").document("todaysCollectedCoins")
        collectedCoinsRef.get().addOnCompleteListener { coins ->
            if(coins.result!!.exists()) {
                separateValuesToCurrenies(coins.result?.data as HashMap<String, HashMap<String, Any>>)
                if (currentCurrency != null) {
                    showCoinsInRecycler(currentCurrency!!)
                }
            }
        }
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        recycler_view_coins
        val scaleNewY = PropertyValuesHolder.ofFloat(View.SCALE_Y,1f,1.3f)
        val scaleNewX = PropertyValuesHolder.ofFloat(View.SCALE_X,1f,1.3f)
        var currentCoinView: View? = null
        when (currentCurrency) {
            DOLR -> currentCoinView = coin_dolr
            SHIL -> currentCoinView = coin_shil
            QUID -> currentCoinView = coin_quid
            PENY -> currentCoinView = coin_peny
        }
        coin_currency_name.text = currentCurrency


        if (currentCoinView != null) {
            val a = currentCoinView.id
            placeholder_coin.setContentId(a)
        }


        ObjectAnimator.ofPropertyValuesHolder(currentCoinView,scaleNewX,scaleNewY).apply {
            interpolator = AnticipateInterpolator()
        }.setDuration(0).start()


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


    private fun setUpUser() {
        mAuth = FirebaseAuth.getInstance()
        currentUser = mAuth.currentUser
        email = currentUser?.email
        db = FirebaseFirestore.getInstance()
        if (email != null)
            userDB = db.collection("users").document(email!!)
    }


    private fun separateValuesToCurrenies(coins: HashMap<String, HashMap<String, Any>>) {
        coinsDOLR.clear()
        coinsPENY.clear()
        coinsQUID.clear()
        coinsSHIL.clear()

        for((id,coin)in coins){
            var currency: String = ""
            var coin_value: Double = 0.0
            var from_email: String = email!!
            val coin_detail_keys = coin.keys
            if ("from" in coin_detail_keys){
                from_email = coin["from"] as String
            }
            for (detail_key in coin_detail_keys){
                if (detail_key in listOf<String>(DOLR,PENY,SHIL,QUID)){
                    currency = detail_key
                    coin_value = coin[detail_key] as Double
                }
            }
            val valWithMail = hashMapOf<String,Any>("value" to coin_value, "from" to from_email)

            when (currency){
                DOLR -> coinsDOLR.add(valWithMail)
                SHIL -> coinsSHIL.add(valWithMail)
                QUID -> coinsQUID.add(valWithMail)
                PENY -> coinsPENY.add(valWithMail)
            }
        }
    }



    private fun swapView(v: View){

        val oldCoin=placeholder_coin.content

        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y,1.3f,1f)
        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X,1.3f,1f)


        ObjectAnimator.ofPropertyValuesHolder(oldCoin,scaleX,scaleY).apply {
            interpolator = AnticipateInterpolator()
        }.start()


        TransitionManager.beginDelayedTransition(constraint_layout_wallet)
        placeholder_coin.setContentId(v.id)


        val scaleNewY = PropertyValuesHolder.ofFloat(View.SCALE_Y,0.7f,1.3f)
        val scaleNewX = PropertyValuesHolder.ofFloat(View.SCALE_X,0.7f,1.3f)

        ObjectAnimator.ofPropertyValuesHolder(v,scaleNewX,scaleNewY).apply {
            interpolator = AnticipateInterpolator()
        }.start()


        when (v) {
            coin_dolr -> coin_currency_name.text = DOLR
            coin_shil -> coin_currency_name.text = SHIL
            coin_quid -> coin_currency_name.text = QUID
            coin_peny -> coin_currency_name.text = PENY

        }

        val alpha = PropertyValuesHolder.ofFloat(View.ALPHA,0f,1f)

        ObjectAnimator.ofPropertyValuesHolder(coin_currency_name,alpha).apply {
            interpolator = AnticipateInterpolator()
        }.start()

        val alphaShadow = PropertyValuesHolder.ofFloat(View.ALPHA,0f,1f)

        ObjectAnimator.ofPropertyValuesHolder(coin_currency_name,alpha).apply {
            interpolator = AnticipateInterpolator()
        }.start()

    }

    private fun swapViewNoAnimation(v: View){
        TransitionManager.beginDelayedTransition(constraint_layout_wallet)
        placeholder_coin.setContentId(v.id)
    }



    private fun showCoinsInRecycler(currency: String) {

        cardViewItemList = arrayListOf<CardViewItem>()
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
        mRecyclerViewItem = recycler_view_coins
        //mRecyclerViewItem.setHasFixedSize(true)
        mLayoutManager = LinearLayoutManager(activity)
        mAdapter = RecyclerViewAdapter(cardViewItemList)
        mRecyclerViewItem.layoutManager = mLayoutManager
        mRecyclerViewItem.adapter = mAdapter
        setClickListenerOnRecyclerViewItemClick()


    }

    private fun noCoinsTextToggle(hasNoCoins: Boolean){
        if (hasNoCoins){
            no_coin_of_selected_currency_text_view.visibility = View.VISIBLE
        }else{
            //TODO add to landscape
            no_coin_of_selected_currency_text_view.visibility = View.GONE
        }

    }

    private fun setClickListenerOnRecyclerViewItemClick() {
        mAdapter.setOnItemClickListener { position ->
            //cardViewItemList.get(index = position)
            val clickedcard = cardViewItemList.get(position)
            userDB.collection("bank").document("numberOfCoinsAddedTodayToBank").get()
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            if (it.result!!.exists()) {
                                val counterNHashMap = it.result?.data as java.util.HashMap<String, Any>
                                val counterValue = counterNHashMap["n"] as Long

                                if (currentCurrency != null && counterValue < 25) {
                                    addToBank(clickedcard.text2, currentCurrency!!,clickedcard.from)
                                    removeItem(position)
                                }else{
                                    val alert = AlertDialog.Builder(this.requireActivity())
                                    alert.apply {
                                        setPositiveButton("OK",null)
                                        setCancelable(true)
                                        setTitle("Transaction Limit")
                                        setMessage("Sorry, you cannot add more than 25 coins to the bank within a day.")
                                        create().show()
                                    }
                                }
                            } else {
                                if (currentCurrency != null) {
                                    addToBank(clickedcard.text2, currentCurrency!!, clickedcard.from)
                                    removeItem(position)
                                }
                            }
                        }
                    }
        }
    }

    private fun removeItem(position: Int){
        cardViewItemList.removeAt(position)
        mAdapter.notifyItemRemoved(position)
    }

    private fun addToBank(amount: String, currency: String, from: String){
        val collectedCoinsRef = userDB.collection("wallet").document("todaysCollectedCoins")
        collectedCoinsRef.get().addOnCompleteListener{
            val mapOfCollectedCoins = it.result?.data as HashMap<String, HashMap<String,Any>>
            loop@ for ((id, coin) in mapOfCollectedCoins) {
                if (currency in coin.keys){
                    val actualCoinValue = coin.get(currency)  as Double
                    val actualValRoundedString: String = "%.2f".format(actualCoinValue)
                    if (actualValRoundedString == amount){
                        val bankPath = userDB.collection("bank")
                        val bankCurrenciesPath = bankPath.document("currencies")

                        bankCurrenciesPath.get()
                                .addOnCompleteListener {
                                    if (it.isSuccessful){
                                        var balance: Double
                                        if (it.getResult()!!.exists()){
                                            val currencyValuesInBank = it.result?.data as HashMap
                                            if (currencyValuesInBank[currency] != null){
                                                balance = currencyValuesInBank[currency] as Double
                                                balance += actualCoinValue
                                                bankCurrenciesPath.update(currency,balance)
                                            }else{
                                                val newCurrencyValue=  HashMap<String,Any>()
                                                newCurrencyValue[currency] = actualCoinValue
                                                bankCurrenciesPath.set(newCurrencyValue, SetOptions.merge())
                                            }


                                            updateCounter(bankPath)
                                            addIdToDeletedCoins(id)
                                            deleteCoinFromWalletFragmentList(actualCoinValue, currency,from)
                                        }else{
                                            val newCurrencyValue=  HashMap<String,Any>()
                                            newCurrencyValue[currency]= actualCoinValue
                                            bankCurrenciesPath.set(newCurrencyValue)
                                            updateCounter(bankPath)
                                            addIdToDeletedCoins(id)
                                        }
                                    }else{
                                        Log.d(TAG, "No such document.")
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
            //TODO check why counter goes to 26

        }.addOnFailureListener {
            Toast.makeText(activity,"ERROR: Failed to delete coin.", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateCounter(bankPath: CollectionReference) {
        val counterPath = bankPath.document("numberOfCoinsAddedTodayToBank")
        counterPath.get().addOnCompleteListener {
            if (it.isSuccessful) {
                var n: Long
                val date = Timestamp(Date())
                if (it.result!!.exists()) {
                    val counterNHashMap = it.result?.data as HashMap<String, Any>
                    n = counterNHashMap["n"] as Long
                    n += 1
                    counterPath.update("n", n)
                    counterPath.update("date",date)
                } else {
                    val newCounter = HashMap<String, Any>()
                    newCounter["n"] = 1
                    newCounter["date"] = date
                    counterPath.set(newCounter)
                }
            }
        }
    }

    private fun addIdToDeletedCoins(id: String){
        val coinId = HashMap<String, Any>()
        coinId[id] = true
        userDB.collection("wallet").document("todaysCollectedAddedToBank").set(coinId, SetOptions.merge())
    }

    private fun deleteCoinFromWalletFragmentList(actualCoinValue: Double, currency: String,from: String){
        var valWithEmail = HashMap<String,Any>()
        valWithEmail["value"] = actualCoinValue
        valWithEmail["from"] = from
        when (currency) {
            DOLR -> coinsDOLR.remove(valWithEmail)
            SHIL -> coinsSHIL.remove(valWithEmail)
            QUID -> coinsQUID.remove(valWithEmail)
            PENY -> coinsPENY.remove(valWithEmail)

        }

    }


    private fun createCardList(coinsSelected: ArrayList<HashMap<String,Any>>, imageCoin: Int, currency: String): ArrayList<CardViewItem> {
        val cardViewItemList: ArrayList<CardViewItem> = arrayListOf<CardViewItem>()
        for (coin in coinsSelected)
            cardViewItemList.add(CardViewItem(imageCoin, currency, "%.2f".format(coin["value"] as Double),coin["from"] as String))
        return cardViewItemList
    }


    override fun onStop() {
        super.onStop()

        val settings = activity?.getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        val editor = settings?.edit()
        editor?.putString("currentCurrency", currentCurrency)
        editor?.apply()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)


    }
}

