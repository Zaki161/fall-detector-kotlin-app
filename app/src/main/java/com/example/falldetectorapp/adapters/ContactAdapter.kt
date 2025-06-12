package com.example.falldetectorapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.falldetectorapp.R
import com.example.falldetectorapp.models.ContactPerson

class ContactAdapter(private val contacts: List<ContactPerson>) :
    RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

    class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.contactNameText)
        val emailText: TextView = itemView.findViewById(R.id.contactEmailText)
        val phoneText: TextView = itemView.findViewById(R.id.contactPhoneText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contacts[position]
        holder.nameText.text = contact.name
        holder.emailText.text = contact.email
        holder.phoneText.text = contact.phoneNumber
    }

    override fun getItemCount() = contacts.size
}