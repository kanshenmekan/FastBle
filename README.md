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
    bleConnectStrategy = BleConnectStrategy.Builder().setConnectOverTime(10000)
                .setConnectBackpressureStrategy(BleConnectStrategy.CONNECT_BACKPRESSURE_DROP)
                .setReConnectCount(1).setReConnectInterval(2000).build()
    }
    //bleFactory为null默认是通过macd地址来区分不同设备，你可以通过传入一个bleFactory来自定义你的方式
    //可以混合mac地址和广播数据等方式，如getManufacturerSpecificData获取厂商自定义的数据字段
     bleFactory = object :BleFactory{
         override fun generateUniqueKey(bleDevice: BleDevice): String {
            return bleDevice.mac
         }
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
BleConnectStrategy setReConnectCount(count: Int) setReConnectInterval(interval: Long)
```

设置连接背压策略<br>

```
BleConnectStrategy connectBackpressureStrategy

1.BleConnectStrategy.CONNECT_BACKPRESSURE_DROP
  当存在mac相同的设备已经在连接的时候，忽略掉后面发起的连接，直至这次连接失败或者成功,已经存在连接成功，不会发起连接

2.BleConnectStrategy.CONNECT_BACKPRESSURE_LAST
  当存在mac相同的设备已经在连接的时候，取消之前的链接，直接用最新发起的，已经存在连接成功，不会发起连接
```

设置autoConnect

```
BleConnectStrategy.Builder().setAutoConnect(autoConnect: Boolean)
true 当发起连接的时候，如果无法找到设备，会保持连接状态，不回调结果（如果设置了超时，会一直等待到超时时间回调连接超时），等待设备可以连接之后，再回调结果。
false 直接连接，回调结果，默认为false
  
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

## 监听蓝牙的打开和关闭

```
BleManager.setBleStateCallback(object : BluetoothChangedObserver.BleStatusCallback {
    override fun onStateOn() {

    }

    override fun onStateTurningOn() {

    }

    override fun onStateOff() {

    }

    override fun onStateTurningOff() {

    }

})
Tips:
 - 不管有没有这个需求，都去设置一个蓝牙状态回调，库里面可以解决，直接关闭蓝牙，系统不走蓝牙断开的回调的问题
```

## 扫描及连接

### 配置扫描规则

```
val scanRuleConfig = BleScanRuleConfig.Builder()
    .setServiceUuids(serviceUUID) // 只扫描指定的服务的设备，可选
    .setDeviceName(names, true) // 只扫描指定广播名的设备，可选
    .setScanTimeOut(10000) // 扫描超时时间，可选，默认10秒
    .setDeviceMac()
//            .setScanSettings(scanSettings: ScanSettings)
    .build()
BleManager.bleScanRuleConfig = scanRuleConfig


Tips:
可以通过设置ScanSettings，设置搜索模式，或者回调模式，默认是SCAN_MODE_BALANCED搜索模式，回调是CALLBACK_TYPE_ALL_MATCHE，还有匹配模式等等，具体看库代码
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
}，5000)
第二个参数可选，默认为配置的扫描规则，可以每次配置时间
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

### 断开后重连问题

```
val connectStrategy =
BleConnectStrategy.Builder().setAutoConnect(true)
    .setConnectOverTime(-1)
    .setConnectBackpressureStrategy(BleConnectStrategy.CONNECT_BACKPRESSURE_DROP)
    .build()
BleManager.connect(bleDevice, reconnectBleGattCallback, connectStrategy)
- 我们把AutoConnect参数设置为true，等待设备可以连接的时候才会进行回调
- 设置一个最长的超时时间setConnectOverTime，单位为millisecond，如果超时未负数，则会一直处于连接状态，直至连接成功或失败，或者被取消这次连接
- 可以在`onDisConnected`回调方法中再次调用`connect`方法。
```

## 蓝牙操作

### 订阅通知notify

```
-timeout 指定这次操作的超时时间，超时不一定意味着最终失败，只是表示在指定时间内没有响应
 通过exception is BleException.TimeoutException 来判断是否是超时错误
 下面的操作同理
  fun notify(
        bleDevice: BleDevice,
        uuidService: String,
        uuidNotify: String,
        callback: BleNotifyCallback?,
        useCharacteristicDescriptor: Boolean = false,
        timeout: Long = operateTimeout
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
        uuidService: String,
        uuidNotify: String,
        useCharacteristicDescriptor: Boolean = false,
        timeout: Long = operateTimeout
    ): Boolean
```

### 订阅通知indicate

```
    fun indicate(
        bleDevice: BleDevice,
        uuidService: String,
        uuidIndicate: String,
        callback: BleIndicateCallback?,
        useCharacteristicDescriptor: Boolean = false,
        timeout: Long = operateTimeout
    )

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
 fun stopIndicate(
        bleDevice: BleDevice,
        uuidService: String,
        uuidIndicate: String,
        useCharacteristicDescriptor: Boolean = false,
        timeout: Long = operateTimeout
    ): Boolean
```

### 读

```
    fun read(
        bleDevice: BleDevice,
        uuidService: String,
        uuidRead: String,
        callback: BleReadCallback?,
        timeout: Long = operateTimeout
    )
BleReadCallback() {
    override fun onReadSuccess(
        bleDevice: BleDevice,
        characteristic: BluetoothGattCharacteristic,
        data: ByteArray) {
            // 读特征值数据成功       
    }

    override fun onReadFailure(
        bleDevice: BleDevice,
        characteristic: BluetoothGattCharacteristic?,
        exception: BleException) {
            // 读特征值数据失败
     }
}
```

### 写

```
   fun write(
        bleDevice: BleDevice,
        uuidService: String,
        uuidWrite: String,
        data: ByteArray?,
        split: Boolean = true,
        splitNum: Int = splitWriteNum,
        continueWhenLastFail: Boolean = false,
        intervalBetweenTwoPackage: Long = 0,
        callback: BleWriteCallback?,
        @BleWriteType writeType: Int = WRITE_TYPE_AUTO,
        timeout: Long = operateTimeout
    )

BleWriteCallback() {
    override fun onWriteSuccess(
        bleDevice: BleDevice,
        characteristic: BluetoothGattCharacteristic,
        current: Int,
        total: Int,
        justWrite: ByteArray,
        data: ByteArray
    ) {
            // 发送数据到设备成功
            // `current`表示当前发送第几包数据，`total`表示本次总共多少包数据，`justWrite`表示刚刚发送成功的数据包。
            //data 表示整个数据

    }

    override fun onWriteFailure(
        bleDevice: BleDevice,
        characteristic: BluetoothGattCharacteristic?,
        exception: BleException,
        current: Int,
        total: Int,
        justWrite: ByteArray?,
        data: ByteArray?,
        isTotalFail: Boolean
    ) {
            // 发送数据到设备失败 exception错误信息
            // `current`表示当前发送第几包数据，`total`表示本次总共多少包数据，`justWrite`表示刚刚发送失败的数据包
            //data 表示整个数据
            // isTotalFail 是否整个过程都失败了
    }

}
Tips:
- 在没有扩大MTU及扩大MTU无效的情况下，当遇到超过20字节的长数据需要发送的时候，需要进行分包。splitNum表示分包的字节数，默认20，参数`boolean split`表示是否使用分包发送，默认为true，默认对超过splitNum字节的数据进行分包发送。
- 对于分包发送的策略，可以选择发送上一包数据发送失败之后，后面的数据还需不需要发送，continueWhenLastFail，默认为false。
- 参数`intervalBetweenTwoPackage`表示延时多长时间发送下一包，单位ms，默认0。
- 参数writeType，表示使用哪种发送模式BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE，WRITE_TYPE_DEFAULT，WRITE_TYPE_SIGNED，默认是判断characteristic具备那种发送模式
  优先WRITE_TYPE_NO_RESPONSE，WRITE_TYPE_DEFAULT，最后是WRITE_TYPE_SIGNED
```

### 使用队列写入数据

```
fun addOperatorToQueue(
    bleDevice: BleDevice?,
    identifier: String = BleBluetooth.DEFAULT_QUEUE_IDENTIFIER,
    sequenceBleOperator: SequenceBleOperator,
): Boolean

val sequenceWriteOperator =
    SequenceWriteOperator.Builder().serviceUUID(service_uuid)
        .priority(PRIORITY_WRITE_DEFAULT)
        .characteristicUUID(characteristic_uuid).data(data).writeType(writeType)
        .bleWriteCallback(bleWriteCallback)
        .continuous(continuous)
        .delay(if (continuous) 10 else 100)
        .timeout(1000)
        .operateTimeout(2000)
        .split(true)
        .splitNum(20)
        .build()
BleManager.addOperatorToQueue(bleDevice, sequenceBleOperator = sequenceWriteOperator)
Tips:
- 当一个characteristic连续发送数据，两次间隔太短的话，容易造成失败，这个时候可以使用队列发送
- 可以为每个可写的characteristic创造一个队列，传入不同的identifier，也可以共用一个队列，每个设备有个默认队列

- priority 优先级越高，在队列的越前面
- writeType 和正常写入相同
- delay 任务之后等待多久
- 当continuous 为true的时候，等待任务完成之后（触发回调或者超时),才会进行delay任务，之后获取下一个任务
  当continuous 为false的时候，会直接进行delay任务，之后获取下一个任务
- timeout 当continuous为true之后，这个设置才有效果，如果任务在时间内没有回调，直接忽略掉，进行delay任务，之后获取下一个任务
  建议给一个适当的时长，以便任务有足够时间触发回调
  如果timeout为0，则会一直等待，直到任务回调触发
- operateTimeout,和timeout的区别是，timeout是这个任务在队列当时超时的时间，如果超过这个时间就会进行下一个，operateTimeout表示
  这个操作的执行时间，超过这个时间就会超时，和普通非队列写入方法的timeout作用相同，不能设置为<=0,否则会直接超时
- 如果对于同一个characteristic,既用了普通写入，又用了队列写入，1.4.5版本以下，那么队列的timeout（非operateTimeout）不能设置为小于等于0，否则可能造成队列不继续执行的问题
  除了写入操作以外的，如果是像demo里面自己实现了队列notify操作，对于同一个characteristic,既用了普通notify，又用了队列notify，那么也不要设置超时时间为小于等于0
```

### 队列的一些其他操作

```
//移除队列中某个任务
fun removeOperatorFromQueue(
    bleDevice: BleDevice?,
    identifier: String = BleBluetooth.DEFAULT_QUEUE_IDENTIFIER,
    sequenceBleOperator: SequenceBleOperator,
): Boolean

//清空某个队列全部任务
fun clearQueue(
    bleDevice: BleDevice?,
    identifier: String = BleBluetooth.DEFAULT_QUEUE_IDENTIFIER)

//清除所有队列任务
fun clearAllQueue(bleDevice: BleDevice?)

//暂停某个队列
fun pauseQueue(
    bleDevice: BleDevice?,
    identifier: String = BleBluetooth.DEFAULT_QUEUE_IDENTIFIER,
)
//恢复某个队列
fun resume(bleDevice: BleDevice?, identifier: String = BleBluetooth.DEFAULT_QUEUE_IDENTIFIER)
```

### 获取设备的信号强度Rssi

```
BleManager.readRssi(bleDevice,
    object : BleRssiCallback() {
        override fun onRssiFailure(bleDevice: BleDevice, exception: BleException) {
                // 读取设备的信号强度失败    
        }

        override fun onRssiSuccess(bleDevice: BleDevice, rssi: Int) {
             // 读取设备的信号强度成功       
        }
})
Tips：
获取设备的信号强度，需要在设备连接之后进行。
某些设备可能无法读取Rssi，不会回调onRssiSuccess(),而会因为超时而回调onRssiFailure()。
```

### 设置最大传输单元MTU

```
BleManager.setMtu(bleDevice,mtu,object : BleMtuChangedCallback() {
    override fun onSetMTUFailure(bleDevice: BleDevice, exception: BleException) {
                // 设置MTU失败
    }

    override fun onMtuChanged(bleDevice: BleDevice, mtu: Int) {
                // 设置MTU成功，并获得当前设备传输支持的MTU值
    }

})
Tips：
设置MTU，需要在设备连接之后进行操作。
默认每一个BLE设备都必须支持的MTU为23。
MTU为23，表示最多可以发送20个字节的数据。
在Android 低版本(API-17 到 API-20)上，没有这个限制。所以只有在API21以上的设备，才会有拓展MTU这个需求。
该方法的参数mtu，最小设置为23，最大设置为512。
并不是每台设备都支持拓展MTU，需要通讯双方都支持才行，也就是说，需要设备硬件也支持拓展MTU该方法才会起效果。调用该方法后，可以通过onMtuChanged(int mtu)查看最终设置完后，设备的最大传输单元被拓展到多少。如果设备不支持，可能无论设置多少，最终的mtu还是23。
```

### requestConnectionPriority

```
fun requestConnectionPriority(bleDevice: BleDevice, connectionPriority: Int): Boolean

Tips:
设置连接的优先级，一般用于高速传输大量数据的时候可以进行设置。 Must be one of{@link BluetoothGatt#CONNECTION_PRIORITY_BALANCED}, {@link BluetoothGatt#CONNECTION_PRIORITY_HIGH} or {@link BluetoothGatt#CONNECTION_PRIORITY_LOW_POWER}.
```

## 断开设备

### 断开某个设备

```
fun disconnect(bleDevice: BleDevice?)
```

### 断开所有设备

```
fun disconnectAllDevice()
```

### 退出使用，清理资源

```
//退出，无回调 
fun destroy()

//退出，有scan和connect的回调
fun release()
```

## [更多方法参考BleManager类](https://github.com/kanshenmekan/FastBle/blob/master/library/src/main/java/com/huyuhui/fastble/BleManager.kt)
