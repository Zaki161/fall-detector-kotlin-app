package com.example.falldetectorapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.falldetectorapp.R
import com.example.falldetectorapp.models.User

/**
 * Adapter dla RecyclerView wyświetlający listę kontaktów użytkowników ([User]).
 *
 * Wykorzystuje układ `item_contact.xml` do prezentacji danych kontaktowych (imię, e-mail, telefon).
 *
 * @param contacts Lista obiektów [User], które mają zostać wyświetlone.
 */
class ContactAdapter(private val contacts: List<User>) :
    RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

//    Pojedynczy element
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
        holder.nameText.text = contact.nick
        holder.emailText.text = contact.mail
        holder.phoneText.text = contact.phone
    }

    override fun getItemCount() = contacts.size
}