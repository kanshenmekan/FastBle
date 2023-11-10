package com.hyh.blesample.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hyh.ble.BleManager
import com.hyh.ble.data.BleDevice
import com.hyh.blesample.R
import com.hyh.blesample.databinding.ItemBleDeviceBinding

/**
 */
class BleDeviceAdapter(
    private val values: List<BleDevice>
) : RecyclerView.Adapter<BleDeviceAdapter.ViewHolder>() {
    var onItemButtonClickListener: OnItemButtonClickListener? = null
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(
            ItemBleDeviceBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            ), onItemButtonClickListener
        )

    }

    @SuppressLint("MissingPermission")
    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            val map = payloads[0] as Map<*, *>
            val rssi = map["rssi"]
            holder.binding.tvRssi.text = rssi.toString()
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }

    }

    @SuppressLint("MissingPermission")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = values[position]
        holder.binding.tvName.text = item.name
        holder.binding.tvMac.text = item.mac
        holder.binding.tvRssi.text = item.rssi.toString()
        if (BleManager.isConnected(item)) {
            holder.binding.btnConnection.apply {
                text = context.getString(R.string.disconnect)
                setTextColor(context.getColor(R.color.colorPrimary))
            }
            holder.binding.btnDetail.visibility = View.VISIBLE
            holder.binding.tvRssi.visibility = View.GONE
            holder.binding.imgRssi.visibility = View.GONE
        } else {
            holder.binding.btnConnection.apply {
                text = context.getString(R.string.connect)
                setTextColor(context.getColor(R.color.black))
            }
            holder.binding.btnDetail.visibility = View.GONE
            holder.binding.tvRssi.visibility = View.VISIBLE
            holder.binding.imgRssi.visibility = View.VISIBLE
        }
    }

    override fun getItemCount(): Int = values.size


    class ViewHolder(
        val binding: ItemBleDeviceBinding,
        private val onItemButtonClickListener: OnItemButtonClickListener? = null
    ) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.btnConnection.setOnClickListener {
                onItemButtonClickListener?.onBtnConnectionClick(bindingAdapterPosition)
            }
            binding.btnDetail.setOnClickListener {
                onItemButtonClickListener?.onBtnDetailClick(bindingAdapterPosition)
            }
        }
    }

    interface OnItemButtonClickListener {
        fun onBtnConnectionClick(position: Int)
        fun onBtnDetailClick(position: Int)
    }
}