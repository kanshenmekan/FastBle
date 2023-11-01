package com.hyh.blesample.adapter

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.AbsListView
import android.widget.BaseExpandableListAdapter
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import com.hyh.blesample.R
import com.hyh.blesample.databinding.ItemServiceBinding


class ServiceAdapter(val context: Context, private val services: List<BluetoothGattService>) :
    BaseExpandableListAdapter() {
    override fun getGroupCount(): Int {
        return services.count()
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        return services[groupPosition].characteristics.size
    }

    override fun getGroup(groupPosition: Int): Any {
        return services[groupPosition]
    }

    override fun getChild(groupPosition: Int, childPosition: Int): Any {
        return (services[groupPosition].characteristics[childPosition])
    }

    override fun getGroupId(groupPosition: Int): Long {
        return groupPosition.toLong()
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        return childPosition.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    override fun getGroupView(
        groupPosition: Int,
        isExpanded: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        val serviceHolder = if (convertView == null) {
            val binding =
                ItemServiceBinding.inflate(LayoutInflater.from(context), parent, false)
            ViewHolder(binding).apply {
                binding.root.tag = this
            }

        } else {
            convertView.tag as ViewHolder
        }
        serviceHolder.binding.tvTitle.text =
            "${context.getString(R.string.service)}($groupPosition)"
        serviceHolder.binding.tvUuid.text = services[groupPosition].uuid.toString()
        serviceHolder.binding.tvType.text =
            if (services[groupPosition].type == BluetoothGattService.SERVICE_TYPE_PRIMARY) {
                context.getString(R.string.primary_service)
            } else {
                context.getString(R.string.secondary_service)
            }
        if (isExpanded) {
            serviceHolder.binding.iv.setImageDrawable(
                ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_arrow_down
                )
            )
        } else {
            serviceHolder.binding.iv.setImageDrawable(
                ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_arrow_right
                )
            )
        }
        serviceHolder.binding.root.setBackgroundColor(context.getColor(R.color.gray))
        return serviceHolder.binding.root
    }

    override fun getChildView(
        groupPosition: Int,
        childPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        val characteristicHolder = if (convertView == null) {
            val binding =
                ItemServiceBinding.inflate(LayoutInflater.from(context), parent, false)
            ViewHolder(binding).apply {
                binding.root.tag = this
            }

        } else {
            convertView.tag as ViewHolder
        }
        val characteristic = services[groupPosition].characteristics[childPosition]
        characteristicHolder.binding.tvTitle.text =
            "${context.getString(R.string.characteristic)}($childPosition)"
        characteristicHolder.binding.tvUuid.text = characteristic.uuid.toString()
        val property = StringBuilder()
        val charaProp = characteristic.properties
        if (charaProp and BluetoothGattCharacteristic.PROPERTY_READ > 0) {
            property.append("Read")
            property.append(" , ")
        }
        if (charaProp and BluetoothGattCharacteristic.PROPERTY_WRITE > 0) {
            property.append("Write")
            property.append(" , ")
        }
        if (charaProp and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE > 0) {
            property.append("Write No Response")
            property.append(" , ")
        }
        if (charaProp and BluetoothGattCharacteristic.PROPERTY_NOTIFY > 0) {
            property.append("Notify")
            property.append(" , ")
        }
        if (charaProp and BluetoothGattCharacteristic.PROPERTY_INDICATE > 0) {
            property.append("Indicate")
            property.append(" , ")
        }
        if (property.length > 1) {
            property.delete(property.length - 3, property.length)
        }
        characteristicHolder.binding.tvType.text = "${context.getString(R.string.characteristic)}($property)"
        characteristicHolder.binding.iv.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.ic_arrow_right))
        return characteristicHolder.binding.root
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return true
    }

    class ViewHolder(val binding: ItemServiceBinding)

}