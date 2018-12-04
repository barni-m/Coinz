package uk.ac.ed.inf.coinz

import android.view.LayoutInflater
import android.view.ViewGroup
import java.util.ArrayList

class MessageRecyclerAdapter(itemList: ArrayList<CardViewItem>?) : RecyclerViewAdapter(itemList) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.card_view_coins_message, parent, false)
        return RecyclerViewHolder(v, mListener)
    }
}