package uk.ac.ed.inf.coinz

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.support.constraint.Placeholder
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnticipateInterpolator
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.coins_in_wallet_layout.*
import kotlinx.android.synthetic.main.fragment_wallet.*
import java.text.FieldPosition


class WalletFragment : Fragment() {

    // user (Firebase):
    private lateinit var db: FirebaseFirestore
    private lateinit var mAuth: FirebaseAuth
    private var currentUser: FirebaseUser? = null
    private lateinit var userDB: DocumentReference
    private var email: String? = null

    // user coins in wallet
    private var coinsDOLR: MutableList<Any> = mutableListOf<Any>()
    private var coinsPENY: MutableList<Any> = mutableListOf<Any>()
    private var coinsSHIL: MutableList<Any> = mutableListOf<Any>()
    private var coinsQUID: MutableList<Any> = mutableListOf<Any>()



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
        recycler_view_coins
        settings= activity?.getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        currentCurrency = settings?.getString("currentCurrency", DOLR)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recycler_view_coins
        val collectedCoinsRef = userDB.collection("wallet").document("todaysCollectedCoins")
        collectedCoinsRef.get().addOnCompleteListener { coins ->
            separateValuesToCurrenies(coins.result?.data as HashMap<String, HashMap<String, Any>>)
            if (currentCurrency != null){
                showCoinsInRecycler(currentCurrency!!)
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
        for ((_, coin) in coins) {
            for ((key, value) in coin) {
                when (key) {
                    DOLR -> coinsDOLR.add(value)

                    PENY -> coinsPENY.add(value)

                    QUID -> coinsQUID.add(value)

                    SHIL -> coinsSHIL.add(value)
                }
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

    }

    private fun swapViewNoAnimation(v: View){
        TransitionManager.beginDelayedTransition(constraint_layout_wallet)
        placeholder_coin.setContentId(v.id)
    }



    private fun showCoinsInRecycler(currency: String) {
        cardViewItemList = arrayListOf<CardViewItem>()
        when (currency) {
            DOLR -> cardViewItemList = CreateCardList(coinsDOLR,R.drawable.coin_dolr, currency)
            SHIL -> cardViewItemList = CreateCardList(coinsSHIL,R.drawable.coin_shil, currency)
            QUID -> cardViewItemList = CreateCardList(coinsQUID,R.drawable.coin_quid, currency)
            PENY -> cardViewItemList = CreateCardList(coinsPENY,R.drawable.coin_peny, currency)

        }
        mRecyclerViewItem = recycler_view_coins
        //mRecyclerViewItem.setHasFixedSize(true)
        mLayoutManager = LinearLayoutManager(activity)
        mAdapter = RecyclerViewAdapter(cardViewItemList)
        mRecyclerViewItem.layoutManager = mLayoutManager
        mRecyclerViewItem.adapter = mAdapter
        setClickListenerOnRecyclerViewItemClick()
    }

    private fun setClickListenerOnRecyclerViewItemClick() {
        mAdapter.setOnItemClickListener { position ->
            //cardViewItemList.get(index = position)
            val clickedcard =cardViewItemList.get(position)
            if (currentCurrency != null){
                addToBank(clickedcard.text2, currentCurrency!!)
                removeItem(position)
            }


        }

    }

    private fun removeItem(position: Int){
        cardViewItemList.removeAt(position)
        mAdapter.notifyItemRemoved(position)
    }

    private fun addToBank(amount: String, currency: String){
        val collectedCoinsRef = userDB.collection("wallet").document("todaysCollectedCoins")
        collectedCoinsRef.get().addOnCompleteListener{
            val mapOfCollectedCoins = it.result?.data as HashMap<String, HashMap<String,Any>>
            loop@ for ((id, coin) in mapOfCollectedCoins) {
                if (currency in coin.keys){
                    val actualCoinValue = coin.get(currency)  as Double
                    val actualValRoundedString: String = "%.2f".format(actualCoinValue)
                    if (actualValRoundedString == amount){

                        //userDB.collection("bank").document("currencies").set()

                        val deleteCoin =  HashMap<String,Any>()
                        deleteCoin[id] = FieldValue.delete()
                        collectedCoinsRef.update(deleteCoin)
                        break@loop
                    }

                }
            }
            //TODO add to bank

        }.addOnFailureListener {
            Toast.makeText(activity,"ERROR: Failed to delete coin.", Toast.LENGTH_LONG).show()
        }
    }


    private fun CreateCardList(coinsSelected: MutableList<Any>,imageCoin: Int, currency: String): ArrayList<CardViewItem> {
        val cardViewItemList: ArrayList<CardViewItem> = arrayListOf<CardViewItem>()
        for (coin in coinsSelected)
            cardViewItemList.add(CardViewItem(imageCoin, currency, "%.2f".format(coin)))
        return cardViewItemList
    }


    override fun onStop() {
        super.onStop()

        val settings = activity?.getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        val editor = settings?.edit()
        editor?.putString("currentCurrency", currentCurrency)
        editor?.apply()
    }
}
