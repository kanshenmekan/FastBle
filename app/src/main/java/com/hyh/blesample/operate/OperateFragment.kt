package com.hyh.blesample.operate

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGattCharacteristic
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import com.hyh.ble.BleManager
import com.hyh.ble.callback.BleIndicateCallback
import com.hyh.ble.callback.BleNotifyCallback
import com.hyh.ble.callback.BleReadCallback
import com.hyh.ble.callback.BleWriteCallback
import com.hyh.ble.data.BleDevice
import com.hyh.ble.exception.BleException
import com.hyh.ble.queue.operate.SequenceWriteOperator
import com.hyh.ble.utils.HexUtil
import com.hyh.blesample.adapter.SharedOperateAdapter
import com.hyh.blesample.databinding.FragmentOperateBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class OperateFragment : Fragment() {
    private val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val readData = mutableListOf<String>()
    private val writeData = mutableListOf<String>()
    private val notifyData = mutableListOf<String>()
    private val indicateData = mutableListOf<String>()
    private var bleDevice: BleDevice? = null
    private var characteristic: BluetoothGattCharacteristic? = null
    private var _binding: FragmentOperateBinding? = null
    private val binding
        get() = _binding!!

    private val readAdapter = SharedOperateAdapter(readData)
    private val writeAdapter = SharedOperateAdapter(writeData)
    private val notifyAdapter = SharedOperateAdapter(notifyData)
    private val indicateAdapter = SharedOperateAdapter(indicateData)

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bleDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable("device", BleDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable("device")
        }
        characteristic = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable("characteristic", BluetoothGattCharacteristic::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable("characteristic")
        }
        if (characteristic == null) {
            val characteristicUUID =
                arguments?.getString("characteristicUUID").run { UUID.fromString(this) }
            val serviceUUID = arguments?.getString("serviceUUID").run { UUID.fromString(this) }
            characteristic = BleManager.getBluetoothGatt(bleDevice)?.getService(serviceUUID)
                ?.getCharacteristic(characteristicUUID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOperateBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        BleManager.clearCharacterCallback(bleDevice)
        BleManager.clearAllQueue(bleDevice)
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvUuid.text = characteristic?.uuid.toString()
        setVisibility()
        val recyclerPool = RecycledViewPool()
        binding.rvWrite.setRecycledViewPool(recyclerPool)
        binding.rvNotify.setRecycledViewPool(recyclerPool)
        binding.rvRead.setRecycledViewPool(recyclerPool)
        binding.rvIndicate.setRecycledViewPool(recyclerPool)

        binding.btnRead.setOnClickListener {
            read()
        }
        val writeListener = OnClickListener {
            val writeType = when (it) {
                binding.btnWrite -> BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                binding.btnWriteNoResponse -> BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                else -> {
                    BluetoothGattCharacteristic.WRITE_TYPE_SIGNED
                }
            }
            HexUtil.hexStringToBytes(binding.etData.text.toString())?.let { data ->
//                write(data, writeType)
                writeByQueue(data, writeType)
                binding.etData.text = null
            } ?: binding.etData.setError("input error")
        }
        binding.btnWrite.setOnClickListener(writeListener)
        binding.btnWriteNoResponse.setOnClickListener(writeListener)
        binding.btnWriteSigned.setOnClickListener(writeListener)

        binding.swNotify.setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView.isPressed) {
                notify(isChecked)
            }
        }
        binding.swIndicate.setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView.isPressed) {
                indicate(isChecked)
            }
        }
        binding.rvRead.adapter = readAdapter
        binding.rvWrite.adapter = writeAdapter
        binding.rvNotify.adapter = notifyAdapter
        binding.rvIndicate.adapter = indicateAdapter


    }

    private fun setVisibility() {
        characteristic?.let {
            binding.readView.visibility =
                if (it.properties and BluetoothGattCharacteristic.PROPERTY_READ > 0) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            if (it.properties and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE or BluetoothGattCharacteristic.WRITE_TYPE_SIGNED) == 0) {
                binding.writeView.visibility = View.GONE
            } else {
                binding.writeView.visibility = View.VISIBLE
                binding.btnWrite.visibility =
                    if (it.properties and BluetoothGattCharacteristic.PROPERTY_WRITE > 0) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }

                binding.btnWriteNoResponse.visibility =
                    if (it.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE > 0) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                binding.btnWriteSigned.visibility =
                    if (it.properties and BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE > 0) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
            }
            binding.notifyView.visibility =
                if (it.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY > 0) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            binding.indicateView.visibility =
                if (it.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE > 0) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
        }
    }

    @SuppressLint("MissingPermission")
    private fun read() {
        characteristic?.let {
            BleManager.read(bleDevice, it.service.uuid.toString(), it.uuid.toString(),
                object : BleReadCallback() {
                    override fun onReadSuccess(
                        bleDevice: BleDevice,
                        characteristic: BluetoothGattCharacteristic,
                        data: ByteArray?
                    ) {
                        readData.add("${sdf.format(Date())}: ${HexUtil.encodeHexStr(data)}")
                        readAdapter.notifyItemInserted(readData.lastIndex)
                        binding.rvRead.scrollToPosition(readData.lastIndex)
                    }

                    override fun onReadFailure(
                        bleDevice: BleDevice?,
                        characteristic: BluetoothGattCharacteristic?,
                        exception: BleException?
                    ) {

                    }

                })
        }
    }

    private val bleWriteCallback = object : BleWriteCallback() {
        override fun onWriteSuccess(
            bleDevice: BleDevice,
            characteristic: BluetoothGattCharacteristic,
            current: Int,
            total: Int,
            justWrite: ByteArray?,
            data: ByteArray?
        ) {
            if (current == total) {
                writeData.add(
                    "${sdf.format(Date())}: success ${
                        HexUtil.encodeHexStr(
                            data
                        )
                    }"
                )
                writeAdapter.notifyItemInserted(writeData.lastIndex)
                binding.rvWrite.scrollToPosition(writeData.lastIndex)
            }
        }

        override fun onWriteFailure(
            bleDevice: BleDevice?,
            characteristic: BluetoothGattCharacteristic?,
            exception: BleException?,
            current: Int,
            total: Int,
            justWrite: ByteArray?,
            data: ByteArray?,
            isTotalFail: Boolean
        ) {
            if (isTotalFail) {
                writeData.add("${sdf.format(Date())}: fail ${HexUtil.encodeHexStr(data)}")
                writeAdapter.notifyItemInserted(writeData.lastIndex)
                binding.rvWrite.scrollToPosition(writeData.lastIndex)
            }
        }

    }

    private fun writeByQueue(data: ByteArray, writeType: Int) {
        characteristic?.let {
            val sequenceWriteOperator =
                SequenceWriteOperator.Builder().serviceUUID(it.service.uuid.toString())
                    .characteristicUUID(it.uuid.toString()).data(data).writeType(writeType)
                    .bleWriteCallback(bleWriteCallback)
                    .build()
            BleManager.addOperatorToQueue(bleDevice, sequenceBleOperator = sequenceWriteOperator)
        }
    }

    @SuppressLint("MissingPermission")
    private fun write(data: ByteArray, writeType: Int) {
        characteristic?.let {
            BleManager.write(
                bleDevice,
                it.service.uuid.toString(),
                it.uuid.toString(),
                data,
                writeType = writeType,
                callback = bleWriteCallback
            )
        }
    }

    private val bleNotifyCallback = object : BleNotifyCallback() {
        override fun onNotifySuccess(
            bleDevice: BleDevice,
            characteristic: BluetoothGattCharacteristic
        ) {
            binding.swNotify.takeUnless { it.isChecked }?.isChecked = true
        }

        override fun onNotifyFailure(
            bleDevice: BleDevice?,
            characteristic: BluetoothGattCharacteristic?,
            exception: BleException?
        ) {
            Toast.makeText(
                requireContext(),
                "onNotifyFailure ${exception?.description}",
                Toast.LENGTH_LONG
            ).show()
            binding.swNotify.takeIf { it.isChecked }?.isChecked = false
        }

        override fun onNotifyCancel(
            bleDevice: BleDevice?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            binding.swNotify.takeIf { it.isChecked }?.isChecked = false
        }

        override fun onCharacteristicChanged(
            bleDevice: BleDevice,
            characteristic: BluetoothGattCharacteristic,
            data: ByteArray?
        ) {
            notifyData.add("${sdf.format(Date())}: ${HexUtil.encodeHexStr(data)}")
            notifyAdapter.notifyItemInserted(notifyData.lastIndex)
            binding.rvNotify.scrollToPosition(notifyData.lastIndex)
        }

    }

    @SuppressLint("MissingPermission")
    private fun notify(enable: Boolean) {
        characteristic?.let {
            if (enable) {
                BleManager.notify(
                    bleDevice,
                    it.service.uuid.toString(),
                    it.uuid.toString(),
                    bleNotifyCallback
                )
            } else {
                BleManager.stopNotify(
                    bleDevice,
                    it.service.uuid.toString(),
                    it.uuid.toString(),
                )
            }
        }
    }

    private val bleIndicateCallback = object : BleIndicateCallback() {
        override fun onIndicateSuccess(
            bleDevice: BleDevice,
            characteristic: BluetoothGattCharacteristic
        ) {
            binding.swIndicate.takeUnless { it.isChecked }?.isChecked = true
        }

        override fun onIndicateFailure(
            bleDevice: BleDevice?,
            characteristic: BluetoothGattCharacteristic?,
            exception: BleException?
        ) {
            Toast.makeText(
                requireContext(),
                "onIndicateFailure ${exception?.description}",
                Toast.LENGTH_LONG
            ).show()
            binding.swIndicate.takeIf { it.isChecked }?.isChecked = false
        }

        override fun onIndicateCancel(
            bleDevice: BleDevice?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            binding.swIndicate.takeIf { it.isChecked }?.isChecked = false
        }

        override fun onCharacteristicChanged(
            bleDevice: BleDevice,
            characteristic: BluetoothGattCharacteristic,
            data: ByteArray?
        ) {
            indicateData.add("${sdf.format(Date())}: ${HexUtil.encodeHexStr(data)}")
            indicateAdapter.notifyItemInserted(indicateData.lastIndex)
            binding.rvIndicate.scrollToPosition(indicateData.lastIndex)
        }

    }

    @SuppressLint("MissingPermission")
    private fun indicate(enable: Boolean) {
        characteristic?.let {
            if (enable) {
                BleManager.indicate(
                    bleDevice,
                    it.service.uuid.toString(),
                    it.uuid.toString(),
                    bleIndicateCallback
                )
            } else {
                BleManager.stopIndicate(
                    bleDevice,
                    it.service.uuid.toString(),
                    it.uuid.toString(),
                )
            }
        }
    }
}