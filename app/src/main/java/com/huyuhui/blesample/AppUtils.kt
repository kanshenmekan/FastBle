package com.huyuhui.blesample

import android.annotation.SuppressLint
import android.app.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions


object AppUtils {
    @SuppressLint("MissingPermission")
    fun enableBluetooth(
        activity: ComponentActivity,
        launcher: ActivityResultLauncher<Intent>
    ) {
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        if (XXPermissions.isGranted(activity, *Permission.Group.BLUETOOTH)) {
            launcher.launch(intent)
        }
    }

    fun isSupportBle(context: Context?): Boolean {
        if (context == null || !context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            return false
        }
        context.getSystemService(Context.BLUETOOTH_SERVICE)?.let {
            return (it as BluetoothManager).adapter != null
        }
        return false
    }

    fun isBleEnable(context: Context): Boolean {
        if (!isSupportBle(context)) {
            return false
        }
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return manager.adapter.isEnabled
    }

    fun isSupportGPS(context: Context): Boolean {
        return context.getSystemService(Context.LOCATION_SERVICE) != null
    }

    /**
     * 判断GPS是否开启，GPS或者AGPS开启一个就认为是开启的
     *
     * @param context
     * @return true 表示开启
     */
    fun isOPenGPS(context: Context): Boolean {
        if (!isSupportGPS(context)) return false
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        // 通过GPS卫星定位，定位级别可以精确到街（通过24颗卫星定位，在室外和空旷的地方定位准确、速度快）
        val gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        // 通过WLAN或移动网络(3G/2G)确定的位置（也称作AGPS，辅助GPS定位。主要用于在室内或遮盖物（建筑群或茂密的深林等）密集的地方定位）
        val network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        return gps || network
    }

    fun openGPS(activity: Activity) {
        val intent =
            Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        activity.startActivity(intent)
    }

}