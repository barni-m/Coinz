package uk.ac.ed.inf.coinz

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.coins_in_wallet_layout.*
import java.util.*
import kotlin.collections.ArrayList


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
        createListOfPayees()
    }

    private fun setUpUser() {
        mAuth = FirebaseAuth.getInstance()
        currentUser = mAuth.currentUser
        email = currentUser?.email
        db = FirebaseFirestore.getInstance()
        if (email != null)
            userDB = db.collection("users").document(email!!)
    }


    private fun showPendingRepaymentsInRecycler(){
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
            val clickedcard = cardViewItemList.get(position)

        }
    }

    private fun createListOfPayees(){

        userDB.collection("loans").document("loansTaken").get()
                .addOnSuccessListener {documentSnapshot ->
                    if (documentSnapshot.exists()){
                        for ((_,detailMap) in documentSnapshot.data as HashMap<String,HashMap<String,Any>>){
                            //for((_,) in loanTaken){
                                var item :RepayCardItem
                                val currency = detailMap["currency"] as String
                                val dateTaken = Timestamp(detailMap["dateTaken"] as Date)
                                val email = detailMap["from"] as String
                                val interest = detailMap["interest"] as Double
                                val repayPeriod = (detailMap["repayPeriod"] as Long).toInt()
                                val value = detailMap["value"] as Double
                                item = RepayCardItem(currency,dateTaken,email,interest,repayPeriod,value)
                                cardViewItemList.add(item)
                            //}
                        }
                    }
                    showPendingRepaymentsInRecycler()
                }

    }

}