package com.hyh.blesample.operate

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.hyh.ble.BleManager
import com.hyh.ble.data.BleDevice
import com.hyh.blesample.R
import com.hyh.blesample.adapter.ServiceAdapter
import com.hyh.blesample.databinding.FragmentServiceBinding


class ServiceFragment : Fragment() {
    private var _binding: FragmentServiceBinding? = null
    private val binding
        get() = _binding!!
    private var bleDevice: BleDevice? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bleDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable("device", BleDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable("device")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentServiceBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvName.text = bleDevice?.name
        binding.tvMac.text = bleDevice?.mac
        BleManager.getBluetoothGattServices(bleDevice)?.let {
            val adapter = ServiceAdapter(requireContext(), it)
            binding.lv.setAdapter(adapter)
            binding.lv.setOnChildClickListener { _, _, groupPosition, childPosition, _ ->
                val bundle = Bundle().apply {
                    putParcelable("device", bleDevice)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        putParcelable(
                            "characteristic",
                            it[groupPosition].characteristics[childPosition]
                        )
                    } else {
                        putString(
                            "serviceUUID",
                            it[groupPosition].uuid.toString()
                        )
                        putString(
                            "characteristicUUID",
                            it[groupPosition].characteristics[childPosition].uuid.toString()
                        )
                    }
                }
                findNavController().navigate(R.id.action_serviceFragment_to_operateFragment, bundle)
                true
            }
        } ?: Toast.makeText(requireContext(), R.string.connection_broken, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}