package com.hyh.blesample

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.hyh.ble.BleManager
import com.hyh.ble.callback.BleGattCallback
import com.hyh.ble.callback.BleScanCallback
import com.hyh.ble.data.BleDevice
import com.hyh.ble.exception.BleException
import com.hyh.ble.scan.BleScanRuleConfig
import com.hyh.blesample.adapter.BleDeviceAdapter
import com.hyh.blesample.adapter.ScanFilterAdapter
import com.hyh.blesample.databinding.ActivityMainBinding
import com.hyh.blesample.databinding.ScanFilterBinding
import com.hyh.blesample.operate.OperateActivity
import com.lxj.xpopup.XPopup
import java.util.UUID


class MainActivity : AppCompatActivity() {
    private val defaultUUID = "0000xxxx-0000-1000-8000-00805F9B34FB"
    private lateinit var binding: ActivityMainBinding
    private var macs: List<String>? = null
    private val scanFilterBinding: ScanFilterBinding
        get() = ((binding.rv.adapter as ConcatAdapter).adapters[0] as ScanFilterAdapter).scanFilterBinding!!
    private var isScanning = false
    private val bleDevices = mutableListOf<BleDevice>()
    private lateinit var bleDeviceAdapter: BleDeviceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        bleDeviceAdapter = BleDeviceAdapter(bleDevices).apply {
            onItemButtonClickListener = object : BleDeviceAdapter.OnItemButtonClickListener {
                @SuppressLint("MissingPermission")
                override fun onBtnConnectionClick(position: Int) {
                    val bleDevice = bleDevices[position]
                    if (BleManager.isConnected(bleDevice)) {
                        BleManager.disconnect(bleDevice)
                    } else {
                        BleManager.cancelScan()
                        connect(bleDevice)
                    }
                }

                @SuppressLint("MissingPermission")
                override fun onBtnDetailClick(position: Int) {
                    val bleDevice = bleDevices[position]
                    Intent(this@MainActivity, OperateActivity::class.java).apply {
                        putExtra("device", bleDevice)
                        startActivity(this)
                    }

                }

            }
        }
        val adapter =
            ConcatAdapter(ScanFilterAdapter(), bleDeviceAdapter)
        binding.rv.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            this.adapter = adapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
        launcher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            activityResultCallback
        )
        requestPermissions()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_app_bar, menu)
        menu?.findItem(R.id.men_scan)?.isVisible = !isScanning
        menu?.findItem(R.id.men_stop)?.isVisible = isScanning
        if (isScanning) {
            binding.indicator.visibility = View.VISIBLE
        } else {
            binding.indicator.visibility = View.GONE
        }
        return super.onCreateOptionsMenu(menu)
    }

    @SuppressLint("MissingPermission")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.men_scan -> {
                scan()
            }

            R.id.men_stop -> {
                BleManager.cancelScan()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun requestPermissions() {
        if (!XXPermissions.isGranted(this, Permission.Group.BLUETOOTH)) {
            XXPermissions.with(this).permission(Permission.Group.BLUETOOTH)
                .request(object : OnPermissionCallback {
                    override fun onGranted(permissions: MutableList<String>, allGranted: Boolean) {
                        openBle()
                    }

                    override fun onDenied(
                        permissions: MutableList<String>,
                        doNotAskAgain: Boolean
                    ) {
                        super.onDenied(permissions, doNotAskAgain)
                        finish()
                    }
                })
        } else {
            openBle()
        }
    }

    private lateinit var launcher: ActivityResultLauncher<Intent>
    private val activityResultCallback = ActivityResultCallback<ActivityResult> {
        if (it.resultCode == RESULT_OK) {
            openGPS()
        }
    }

    private fun openBle() {
        if (AppUtils.isSupportBle(this@MainActivity) && !AppUtils.isBleEnable(this@MainActivity)) {
            AppUtils.enableBluetooth(this@MainActivity, launcher)
        }
    }

    private fun openGPS() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            scan()
        } else {
            if (AppUtils.isOPenGPS(this)) {
                scan()
            } else {
                AppUtils.openGPS(this)
            }
        }
    }

    private fun initScanConfig() {
        val strUuid: String = scanFilterBinding.etUuid.text.toString()
        val uuids = if (TextUtils.isEmpty(strUuid)) {
            null
        } else {
            strUuid.split(",".toRegex()).toList()
        }
        val serviceUUID = uuids?.takeUnless { it.isEmpty() }?.filter {
            if (it.length == 36) {
                val components = it.split("-".toRegex()).toList()
                components.forEach { component ->
                    if (component.length != 5) {
                        return@filter false
                    }
                }
                return@filter true
            } else if (it.length == 4) {
                return@filter true
            }
            false
        }?.mapNotNull {
            val s = if (it.length == 4) {
                defaultUUID.replaceRange(4, 8, it)
            } else it
            try {
                UUID.fromString(s)
            } catch (_: IllegalArgumentException) {
                null
            }
        }?.toList()

        val strName = scanFilterBinding.etName.text.toString()
        val names = if (strName.isEmpty()) {
            null
        } else {
            strName.split(",".toRegex()).toList()
        }
        val macStr = scanFilterBinding.etMac.text
        macs = if (macStr.isNullOrEmpty()) {
            null
        } else {
            macStr.split(",".toRegex()).toList()
        }
        val autoConnect = scanFilterBinding.cb.isChecked

        val scanRuleConfig = BleScanRuleConfig.Builder()
            .setServiceUuids(serviceUUID) // 只扫描指定的服务的设备，可选
            .setDeviceName(names) // 只扫描指定广播名的设备，可选
            .setAutoConnect(autoConnect) // 连接时的autoConnect参数，可选，默认false
            .setScanTimeOut(10000) // 扫描超时时间，可选，默认10秒
            .build()
        BleManager.bleScanRuleConfig = scanRuleConfig
    }

    @SuppressLint("MissingPermission")
    private fun scan() {
        if (!XXPermissions.isGranted(this, Permission.Group.BLUETOOTH)) {
            Toast.makeText(this, R.string.permission_onDenied, Toast.LENGTH_LONG).show()
            return
        }
        if (AppUtils.isSupportBle(this@MainActivity) && !AppUtils.isBleEnable(this@MainActivity)) {
            AppUtils.enableBluetooth(this@MainActivity, launcher)
            return
        }
        initScanConfig()
        if (XXPermissions.isGranted(this, Permission.Group.BLUETOOTH)) {
            BleManager.scan(object : BleScanCallback {
                @SuppressLint("NotifyDataSetChanged")
                override fun onScanStarted(success: Boolean) {
                    isScanning = success
                    invalidateOptionsMenu()
                    bleDevices.clear()
                    bleDevices.addAll(BleManager.getAllConnectedDevice())
                    bleDeviceAdapter.notifyDataSetChanged()
                }

                override fun onLeScan(
                    oldDevice: BleDevice?,
                    newDevice: BleDevice?,
                    scannedBefore: Boolean
                ) {
                    BleManager.getAllConnectedDevice().forEach {
                        if (it.mac == oldDevice?.mac)
                            return
                    }
                    if (!scannedBefore) {
                        oldDevice?.let {
                            bleDevices.add(it)
                            bleDeviceAdapter.notifyItemInserted(bleDevices.lastIndex)
                        }
                    } else {
                        bleDevices.forEachIndexed { index, bleDevice ->
                            if (bleDevice.mac == newDevice?.mac) {
                                bleDevices[index] = bleDevice
                                val payloads = mutableMapOf<String, String>().apply {
                                    put("rssi", "${newDevice?.rssi}")
                                }
                                bleDeviceAdapter.notifyItemChanged(index, payloads)
                                return@forEachIndexed
                            }
                        }
                    }
                }

                override fun onScanFinished(scanResultList: List<BleDevice>) {
                    isScanning = false
                    invalidateOptionsMenu()
                }

                override fun onFilter(bleDevice: BleDevice): Boolean {
                    return macs?.let {
                        for (mac in it) {
                            if (bleDevice.mac.equals(mac)) {
                                return@let true
                            }
                        }
                        return@let false
                    } ?: true
                }
            })
        }
    }

    private val progressLoading by lazy {
        XPopup.Builder(this).dismissOnTouchOutside(false).asLoading()
    }

    @SuppressLint("MissingPermission")
    private fun connect(bleDevice: BleDevice) {
        BleManager.connect(bleDevice,
            object : BleGattCallback() {
                override fun onStartConnect(bleDevice: BleDevice) {
                    progressLoading.show()
                }

                override fun onConnectFail(bleDevice: BleDevice?, exception: BleException?) {
                    progressLoading.dismiss()
                    Toast.makeText(this@MainActivity, R.string.connect_fail, Toast.LENGTH_LONG)
                        .show()
                }

                override fun onConnectSuccess(
                    bleDevice: BleDevice?,
                    gatt: BluetoothGatt?,
                    status: Int
                ) {
                    progressLoading.dismiss()
                    val index = bleDevices.indexOf(bleDevice)
                    if (index != -1) {
                        bleDeviceAdapter.notifyItemChanged(index)
                    }
                }

                override fun onDisConnected(
                    isActiveDisConnected: Boolean,
                    device: BleDevice?,
                    gatt: BluetoothGatt?,
                    status: Int
                ) {
                    bleDevices.forEachIndexed { index, bleDevice ->
                        if (bleDevice.mac == device?.mac) {
                            bleDeviceAdapter.notifyItemChanged(index)
                        }
                    }
                    if (isActiveDisConnected) {

                        Toast.makeText(
                            this@MainActivity,
                            R.string.active_disconnected,
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(this@MainActivity, R.string.disconnected, Toast.LENGTH_LONG)
                            .show()
                    }
                }

            })
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        BleManager.destroy()
    }
}


