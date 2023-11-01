package com.hyh.blesample.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hyh.blesample.databinding.ScanFilterBinding

class ScanFilterAdapter : RecyclerView.Adapter<ScanFilterAdapter.ScanViewHolder>() {
    var scanFilterBinding: ScanFilterBinding? = null
        private set

    class ScanViewHolder(private val binding: ScanFilterBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScanViewHolder {
        return ScanViewHolder(
            ScanFilterBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ).apply {
                scanFilterBinding = this
            })
    }

    override fun getItemCount(): Int {
        return 1
    }

    override fun onBindViewHolder(holder: ScanViewHolder, position: Int) {

    }
}