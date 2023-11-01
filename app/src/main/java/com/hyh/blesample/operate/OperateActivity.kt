package com.hyh.blesample.operate

import android.os.Build
import android.os.Bundle
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.hyh.ble.BleManager
import com.hyh.ble.data.BleDevice
import com.hyh.blesample.R
import com.hyh.blesample.databinding.ActivityOperateBinding

class OperateActivity : AppCompatActivity() {
    private var bleDevice: BleDevice? = null
    private lateinit var binding: ActivityOperateBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOperateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bleDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("device", BleDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("device")
        }
        if (bleDevice == null) {
            finish()
        }
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_operate_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val bundle = Bundle().apply {
            putParcelable("device", bleDevice)
        }
        navController.setGraph(R.navigation.operator_nav, bundle)

        val appBarConfiguration = AppBarConfiguration(topLevelDestinationIds = setOf()) {
            finish()
            false
        }
        binding.toolbar.setupWithNavController(navController, appBarConfiguration)

        onBackPressedDispatcher.addCallback(this, true) {
            if (!navController.popBackStack()) {
                remove()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        BleManager.clearCharacterCallback(bleDevice)
    }
}