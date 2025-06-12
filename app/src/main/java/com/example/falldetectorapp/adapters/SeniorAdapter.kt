package com.example.falldetectorapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.falldetectorapp.databinding.ItemSeniorBinding
import com.example.falldetectorapp.models.User

class SeniorAdapter(private val seniors: List<User>) : RecyclerView.Adapter<SeniorAdapter.SeniorViewHolder>() {

    class SeniorViewHolder(val binding: ItemSeniorBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeniorViewHolder {
        val binding = ItemSeniorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SeniorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SeniorViewHolder, position: Int) {
        val senior = seniors[position]
        holder.binding.seniorNameText.text = senior.nick
        holder.binding.seniorEmailText.text = senior.mail
        holder.binding.seniorPhoneText.text = senior.phone
    }

    override fun getItemCount(): Int = seniors.size
}