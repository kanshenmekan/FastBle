# FastBle

支持与外围BLE设备进行扫描、连接、读、写、通知订阅与取消等基本操作<br>
支持获取信号强度、设置最大传输单元<br>
支持自定义扫描规则<br>
支持多设备连接<br>
支持重连机制<br>
支持配置超时机制

## 接入文档
### 1. Add it in your root build.gradle at the end of repositories
```
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```
### 2. Add the dependency
```
implementation 'com.github.kanshenmekan:FastBle:latestVersion'
```
### android12 权限适配
```
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
```
### 初始化和全局配置
```
BleManager.apply {
    enableLog(true)
    maxConnectCount = 5
    operateTimeout = 2000
    splitWriteNum = 20
    bleConnectStrategy = BleConnectStrategy().apply {
        connectOverTime = 10000
        connectBackpressureStrategy = BleConnectStrategy.CONNECT_BACKPRESSURE_DROP
        setReConnectCount(1,2000)
    }
}.init(this)
```
### 默认打开库中的运行日志，如果不喜欢可以关闭
```
BleManager enableLog(boolean enable)
```
### 配置全局连接策略 BleManager bleConnectStrategy
设置连接超时<br>
```
BleConnectStrategy connectOverTime
```

设置连接时重连次数和重连间隔（毫秒），默认为0次不重连<br>
```
BleConnectStrategy setReConnectCount(count: Int, interval: Long)
```

设置连接背压策略<br>
```
BleConnectStrategy connectBackpressureStrategy

1.BleConnectStrategy.CONNECT_BACKPRESSURE_DROP
  当存在mac相同的设备已经在连接的时候，忽略掉后面发起的连接，直至这次连接失败或者成功,已经存在连接成功，不会发起连接

2.BleConnectStrategy.CONNECT_BACKPRESSURE_LAST
  当存在mac相同的设备已经在连接的时候，取消之前的链接，直接用最新发起的，已经存在连接成功，不会发起连接
```
### 配置分包发送
设置分包发送的时候，每一包的数据长度，默认20个字节<br>
```
BleManager.splitWriteNum
```
### 配置操作超时
设置readRssi、setMtu、write、read、notify、indicate的超时时间（毫秒），默认5秒<br>
```
BleManager.operateTimeout
```


## 扫描及连接
### 配置扫描规则
```
val scanRuleConfig = BleScanRuleConfig.Builder()
    .setServiceUuids(serviceUUID) // 只扫描指定的服务的设备，可选
    .setDeviceName(names, true) // 只扫描指定广播名的设备，可选
    .setAutoConnect(autoConnect) // 连接时的autoConnect参数，可选，默认false
    .setScanTimeOut(10000) // 扫描超时时间，可选，默认10秒
    .setDeviceMac()
//            .setScanSettings(scanSettings: ScanSettings)
    .build()
BleManager.bleScanRuleConfig = scanRuleConfig


Tips:
可以通过设置ScanSettings，设置搜索模式，或者回调模式，默认是SCAN_MODE_BALANCED搜索模式，回调是CALLBACK_TYPE_ALL_MATCHE，还要匹配模式等等，具体看库代码
默认serviceUuid和macs只要满足list，其中一个就会被认定符合条件（serviceUuid中包含，或者macs包含），当names的模糊模式关闭的时候，和macs效果一样，
当names的模糊关闭的时候，会在所有通过serviceUuid和macs的设备，之后再判断是否包含names中的一个

更多自定义过滤规则在搜索回调中实现
```

### 扫描
```
BleManager.scan(object : BleScanCallback {
    override fun onScanStarted(success: Boolean) {
           //开始扫描 success 表示是否成功开始扫描
          
    }

    override fun onLeScan(
        oldDevice: BleDevice,
        newDevice: BleDevice,
        scannedBefore: Boolean) {
         // 扫描到一个符合规则的设备
         // oldDevice 上一次扫描到的 newDevice新扫描到的
         // scannedBefore之前是否扫描到过，当scannedBefore为false的时候 oldDevice和newDevice相同        
    }

    override fun onScanFinished(scanResultList: List<BleDevice>) {
              // 扫描结束，列出所有扫描到的符合扫描规则的BLE设备
    }

    override fun onFilter(bleDevice: BleDevice): Boolean {
               // 通过配置扫描规则之后，自定义过滤规则    
    }
})
```

### 停止扫描
```
BleManager.cancelScan()
Tips:
- 调用该方法后，如果当前还处在扫描状态，会立即结束，并回调`onScanFinished`方法。
```

### 通过设备对象连接
```
BleManager.connect(bleDevice,
    object : BleGattCallback() {
        override fun onStartConnect(bleDevice: BleDevice) {
                    // 开始连接
        }

        override fun onConnectFail(bleDevice: BleDevice?, exception: BleException) {
                   // 连接失败 
        }

        override fun onConnectCancel(bleDevice: BleDevice, skip: Boolean) {
                   // 连接取消 skip true表示发起了新连接取代这个链接，或者这个设备已经连接成功了 false表示主动取消了这次连接
        }

       override fun onConnectSuccess(
            bleDevice: BleDevice,
            gatt: BluetoothGatt?,
            status: Int) {
                 // 连接成功，BleDevice即为所连接的BLE设备
       }

        override fun onDisConnected(
            isActiveDisConnected: Boolean,
            device: BleDevice,
            gatt: BluetoothGatt?,
            status: Int) {
                   // 连接中断，isActiveDisConnected表示是否是主动调用了断开连接方法
        }
},strategy: BleConnectStrategy = bleConnectStrategy)
Tips:
- 在某些型号手机上，connectGatt必须在主线程才能有效。非常建议把连接过程放在主线程。
- 连接失败后重连：框架中包含连接失败后的重连机制，可以配置重连次数和时间间隔。当然也可以自行在`onConnectFail`回调方法中延时调用`connect`方法。
- 连接断开后重连：可以在`onDisConnected`回调方法中再次调用`connect`方法。
- 为保证重连成功率，建议断开后间隔一段时间之后进行重连。
- 某些机型上连接失败后会短暂地无法扫描到设备，可以通过设备对象或设备mac直连，而不经过扫描。
- 可以单独配置某次连接的连接策略，不设置默认为全局的策略
```
### 通过mac连接设备
通过已知设备Mac直接<br>
```
fun connect(
    mac: String,
    bleGattCallback: BleGattCallback?,
    strategy: BleConnectStrategy = bleConnectStrategy,
): BluetoothGatt?
```

## 蓝牙操作

### 订阅通知notify
```
fun notify(
    bleDevice: BleDevice,
    uuid_service: String,
    uuid_notify: String,
    callback: BleNotifyCallback?,
)

BleNotifyCallback() {
    override fun onNotifySuccess(
        bleDevice: BleDevice,
        characteristic: BluetoothGattCharacteristic) {
            // 打开通知操作成功
    }
    override fun onNotifyFailure(
        bleDevice: BleDevice,
        characteristic: BluetoothGattCharacteristic?,
        exception: BleException) {
           // 打开通知操作失败
    }

    override fun onNotifyCancel(
        bleDevice: BleDevice,
        characteristic: BluetoothGattCharacteristic) {
            // 打开之后，再取消
    }

    override fun onCharacteristicChanged(
        bleDevice: BleDevice,
        characteristic: BluetoothGattCharacteristic,
        data: ByteArray) {
          // 打开通知后，设备发过来的数据将在这里出现
    }
}
```

### 取消订阅通知notify，并移除数据接收的回调监听
```
fun stopNotify(
    bleDevice: BleDevice,
    uuid_service: String,
    uuid_notify: String,
): Boolean
```

### 订阅通知indicate
```
fun indicate(
    bleDevice: BleDevice,
    uuid_service: String,
    uuid_indicate: String,
    callback: BleIndicateCallback?)

BleIndicateCallback() {
    override fun onIndicateSuccess(
        bleDevice: BleDevice,
        characteristic: BluetoothGattCharacteristic) {
          // 打开通知操作成功
    }

    override fun onIndicateFailure(
        bleDevice: BleDevice,
        characteristic: BluetoothGattCharacteristic?,
        exception: BleException) {
           // 打开通知操作失败
    }

    override fun onIndicateCancel(
        bleDevice: BleDevice,
        characteristic: BluetoothGattCharacteristic) {
           // 打开通知后，取消的时候
    }

    override fun onCharacteristicChanged(
        bleDevice: BleDevice,
        characteristic: BluetoothGattCharacteristic,
        data: ByteArray?) {
           // 打开通知后，设备发过来的数据将在这里出现
    }

}

```

### 取消订阅通知indicate，并移除数据接收的回调监听
```
fun stopNotify(
        bleDevice: BleDevice,
        uuid_service: String,
        uuid_notify: String,
    ): Boolean
```
