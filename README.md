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
