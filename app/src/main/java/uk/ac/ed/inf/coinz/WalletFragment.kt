package uk.ac.ed.inf.coinz

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
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
import android.widget.Adapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.coins_in_wallet_layout.*
import kotlinx.android.synthetic.main.fragment_wallet.*


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

    // Placeholder for selected coin
    private lateinit var placeholder: Placeholder

    // RecyclerView for collected coins
    private lateinit var mRecyclerViewItem: RecyclerView
    private lateinit var mAdapter: RecyclerViewAdapter
    private lateinit var mLayoutManager: RecyclerView.LayoutManager


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_wallet, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setUpUser()

        val collectedCoinsRef = userDB.collection("wallet").document("todaysCollectedCoins")
        collectedCoinsRef.get().addOnCompleteListener { coins ->
            separateValuesToCurrenies(coins.result?.data as HashMap<String, HashMap<String, Any>>)

            val currency = "DOLR"
            showCoinsInRecycler(currency)


        }


    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        placeholder = palceholder_coin
        placeholder.setContentId(coin_dolr.id)
        coin_currency_name.text = "DOLR"
        val scaleNewY = PropertyValuesHolder.ofFloat(View.SCALE_Y,1f,1.3f)
        val scaleNewX = PropertyValuesHolder.ofFloat(View.SCALE_X,1f,1.3f)

        ObjectAnimator.ofPropertyValuesHolder(coin_dolr,scaleNewX,scaleNewY).apply {
            interpolator = AnticipateInterpolator()
        }.setDuration(0).start()


        coin_shil.setOnClickListener { swapView(it)
        showCoinsInRecycler("SHIL")}
        coin_dolr.setOnClickListener { swapView(it)
        showCoinsInRecycler("DOLR")}
        coin_quid.setOnClickListener { swapView(it)
        showCoinsInRecycler("QUID")}
        coin_peny.setOnClickListener { swapView(it)
        showCoinsInRecycler("PENY")}



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
        for ((id, coin) in coins) {
            for ((key, value) in coin) {
                when (key) {
                    "DOLR" -> coinsDOLR?.add(value)

                    "PENY" -> coinsPENY?.add(value)

                    "QUID" -> coinsQUID?.add(value)

                    "SHIL" -> coinsSHIL?.add(value)
                }
            }
        }
    }




    private fun swapView(v: View){

        val oldCoin=placeholder.content

        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y,1.3f,1f)
        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X,1.3f,1f)

        ObjectAnimator.ofPropertyValuesHolder(oldCoin,scaleX,scaleY).apply {
            interpolator = AnticipateInterpolator()
        }.start()



        TransitionManager.beginDelayedTransition(constraint_layout_wallet)
        placeholder.setContentId(v.id)

        val scaleNewY = PropertyValuesHolder.ofFloat(View.SCALE_Y,0.7f,1.3f)
        val scaleNewX = PropertyValuesHolder.ofFloat(View.SCALE_X,0.7f,1.3f)

        ObjectAnimator.ofPropertyValuesHolder(v,scaleNewX,scaleNewY).apply {
            interpolator = AnticipateInterpolator()
        }.start()


        when (v) {
            coin_dolr -> coin_currency_name.text = "DOLR"
            coin_shil -> coin_currency_name.text = "SHIL"
            coin_quid -> coin_currency_name.text = "QUID"
            coin_peny -> coin_currency_name.text = "PENY"

        }

        val alpha = PropertyValuesHolder.ofFloat(View.ALPHA,0f,1f)

        ObjectAnimator.ofPropertyValuesHolder(coin_currency_name,alpha).apply {
            interpolator = AnticipateInterpolator()
        }.start()

    }


    private fun showCoinsInRecycler(currency: String) {
        var cardViewItemList = arrayListOf<CardViewItem>()
        when (currency) {
            "DOLR" -> cardViewItemList = CreateCardList(coinsDOLR,R.drawable.coin_dolr, currency)
            "SHIL" -> cardViewItemList = CreateCardList(coinsSHIL,R.drawable.coin_shil, currency)
            "QUID" -> cardViewItemList = CreateCardList(coinsQUID,R.drawable.coin_quid, currency)
            "PENY" -> cardViewItemList = CreateCardList(coinsPENY,R.drawable.coin_peny, currency)

        }
        mRecyclerViewItem = recycler_view_coins
        //mRecyclerViewItem.setHasFixedSize(true)
        mLayoutManager = LinearLayoutManager(activity)
        mAdapter = RecyclerViewAdapter(cardViewItemList)
        mRecyclerViewItem.layoutManager = mLayoutManager
        mRecyclerViewItem.adapter = mAdapter
    }

    private fun CreateCardList(coinsSelected: MutableList<Any>,imageCoin: Int, currency: String): ArrayList<CardViewItem> {
        val cardViewItemList: ArrayList<CardViewItem> = arrayListOf<CardViewItem>()
        for (coin in coinsSelected)
            cardViewItemList.add(CardViewItem(imageCoin, currency, "%.2f".format(coin)))
        return cardViewItemList
    }
}
