package uk.ac.ed.inf.coinz

import android.view.LayoutInflater
import android.view.ViewGroup
import java.util.ArrayList

// extending my RecyclerViewAdapter for the coin messaging recycler view
class MessageRecyclerAdapter(itemList: ArrayList<CardViewItem>?) : RecyclerViewAdapter(itemList) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder {
        setUpUser()
        val v = LayoutInflater.from(parent.context).inflate(R.layout.card_view_coins_message, parent, false)
        return RecyclerViewHolder(v, mListener)
    }
}