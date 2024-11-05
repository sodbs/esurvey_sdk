@file:OptIn(ExperimentalMaterial3Api::class)

package com.esurvey.esurvey_sdk_demo

import com.esurvey.esurvey_sdk_demo.ui.theme.Esurvey_sdk_demoTheme


import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.esurvey.sdk.out.ESurvey
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import cn.com.heaton.blelibrary.ble.callback.BleScanCallback
import cn.com.heaton.blelibrary.ble.model.BleDevice
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.PermissionUtils
import com.blankj.utilcode.util.SDCardUtils
import com.blankj.utilcode.util.ToastUtils
import com.esurvey.sdk.out.data.Constant
import com.esurvey.sdk.out.data.LocationState
import com.esurvey.sdk.out.listener.ESAntennaAuthListener
import com.esurvey.sdk.out.listener.ESAntennaConnectListener
import com.esurvey.sdk.out.listener.ESAntennaDisConnectListener
import com.esurvey.sdk.out.listener.ESLocationChangeListener
import com.esurvey.sdk.out.listener.ESMobileHighStatusListener
import com.esurvey.sdk.out.listener.ESUsbAttachChangeListener
import com.lxj.xpopup.XPopup


class MainActivity : ComponentActivity() {
    val instance = ESurvey.getInstance()

    var bluetoothFlag by mutableStateOf(false)
    var usbFlag by mutableStateOf(false)

    var locationState by  mutableStateOf<LocationState?>(null)
    var usbAttachFlag by mutableStateOf(false)
    var permissionFlag by mutableStateOf(false)
    var isConnectSuccessState by mutableStateOf(false)
    var locationSource   by mutableStateOf(-1)

    val bluetoothDeviceList = mutableStateListOf<BleDevice>()



    var messageState by    mutableStateOf("暂无日志")
    val lon = 112.994693
    val lat = 28.149546
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        instance.setOnUsbAttachChangeListener(object : ESUsbAttachChangeListener {
            override fun onChange(isAttach: Boolean) {
                usbAttachFlag = isAttach
            }
        })

        onPermissionRequest()
        TipsSoundsService.init(this)

        setContent {
            Esurvey_sdk_demoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        Modifier
                            .padding(innerPadding)
                            .padding(horizontal = 20.dp, vertical = 20.dp)
                    ) {
                        if (permissionFlag) {
                            App()
                        } else {
                            UnPermissionApp()
                        }
                    }
                }
            }
        }
    }


    private fun onPermissionRequest() {
        val permissions: MutableList<String> = ArrayList()
        permissions.add(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        permissions.add(android.Manifest.permission.ACCESS_FINE_LOCATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(android.Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(android.Manifest.permission.BLUETOOTH_ADVERTISE)
            permissions.add(android.Manifest.permission.BLUETOOTH_CONNECT)
        }

        PermissionUtils.permission(
            *permissions.toTypedArray()
        )
            .callback(object : PermissionUtils.FullCallback {
                override fun onGranted(granted: MutableList<String>) {
                    permissionFlag = true
                }

                override fun onDenied(
                    deniedForever: MutableList<String>,
                    denied: MutableList<String>
                ) {
                    permissionFlag = false
                    ToastUtils.showLong("请打开定位权限")
                }

            })
            .request()
    }


    @Composable
    fun UnPermissionApp() {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = {
                onPermissionRequest()
            }) {
                Text("请求权限")
            }
        }
    }

    @Composable
    fun App() {


        Column {
            StatusBox()
            LocationBox()
            USBBox()
            BlueToothBox()
            MobileHighLocation()
            if (bluetoothDeviceList.isNotEmpty()) {
                ModalBottomSheet(onDismissRequest = {
                    bluetoothDeviceList.clear()
                }) {
                    LazyColumn {
                        items(bluetoothDeviceList.size) { it ->
                            ListItem(
                                headlineContent = { Text(bluetoothDeviceList[it].bleName) },
                                leadingContent = {
                                    Icon(
                                        painter = painterResource(R.drawable.icon_bluetooth),
                                        tint = MaterialTheme.colorScheme.primary,
                                        contentDescription = "Localized description",
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.clickable{
                                    instance.bluetoothConnect(this@MainActivity, bluetoothDeviceList[it], lon, lat)
                                    ToastUtils.showLong("正在连接蓝牙设备")
                                    instance.getBleInstance(this@MainActivity).stopScan()
                                    bluetoothDeviceList.clear()
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }


    @Composable
    fun MobileHighLocation() {

        var mobileHighIsStart by remember {
            mutableStateOf(false)
        }

        LaunchedEffect(true) {

            instance.setOnMobileHighStatusChangeListener(object : ESMobileHighStatusListener {
                override fun onChange(status: Int) {
                    if (status == Constant.MOBILE_HIGH_OPEN) {
                        mobileHighIsStart = true
                    } else {
                        mobileHighIsStart = false
                    }
                }

                override fun onPlaySounds(soundType: Int) {
                    TipsSoundsService.getInstance().playJtSounds(soundType)
                }
            })
        }
        Column {
            if (mobileHighIsStart) {
                Button(onClick = {
                    instance.stopMobileHighLocation()
                    locationState = null
                    mobileHighIsStart = false
                }) {
                    Text("关闭手机高精度定位")
                }
            } else {
                Button(onClick = {

                    val start = {

                        val rtkUserId = ""
                        val rtkSecret = ""
                        if (rtkSecret.isEmpty() || rtkUserId.isEmpty()) {
                            ToastUtils.showLong("请先配置rtkUserId和rtkSecret")
                        } else {
                            val sdCardPathByEnvironment =
                                SDCardUtils.getSDCardPathByEnvironment()

                            mobileHighIsStart = true
                            instance.startMobileHighLocation(this@MainActivity, rtkUserId, rtkSecret, sdCardPathByEnvironment)
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if (Environment.isExternalStorageManager()) {
                            start()
                        } else {
                            // 无权限，打开权限设置界面
                            XPopup.Builder(this@MainActivity)
                                .asConfirm("请先授予文件权限", "请先授予文件权限", "", "确定", {
                                    try {
                                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                        ActivityUtils.getTopActivity().startActivity(intent)
                                    } catch (e: Exception) {
                                        // 在某些设备上，可能需要手动指导用户打开该设置页面
                                        val intent = Intent(Settings.ACTION_SETTINGS)
                                        this@MainActivity.startActivity(intent)
                                    }
                                }, {}, true)
                                .show()
                        }
                    } else {
                        PermissionUtils.permission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                            .callback(object : PermissionUtils.SimpleCallback {
                                override fun onGranted() {
                                    start()
                                }

                                override fun onDenied() {
                                    XPopup.Builder(this@MainActivity)
                                        .asConfirm("请先授予文件权限", "请找到本App,并且打开文件权限", "", "确定", {
                                            PermissionUtils.launchAppDetailsSettings()
                                        }, {}, true)
                                        .show()
                                }
                            }).request()


                    }

                }) {
                    Text("开启手机高精度定位")
                }
            }
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun LocationBox() {
        LaunchedEffect(true) {
            instance.setOnLocationStateChangeListener(object : ESLocationChangeListener {
                override fun onChange(locationStateParam: LocationState) {
                    locationState = locationStateParam
                }
            })
        }
        if (locationState != null) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                SuggestionChip(
                    onClick = { },
                    label = { Text("Lon / Lat ${locationState?.lon} - ${locationState?.lat}") }
                )
                SuggestionChip(
                    onClick = { },
                    label = { Text("水平误差 ±${"%.2f".format(locationState?.getxInaccuracies())}m") }
                )
                SuggestionChip(
                    onClick = { },
                    label = {
                        Text(
                            "高程  (高程误差)   ${locationState?.height}(±${
                                "%.2f".format(
                                    locationState?.getyInaccuracies()
                                )
                            }m)"
                        )
                    }
                )
                SuggestionChip(
                    onClick = { },
                    label = { Text("定位状态 ${locationState!!.locationStatusShow}") }
                )
                SuggestionChip(
                    onClick = { },
                    label = { Text("解算卫星 ${locationState?.satelliteNum}") }
                )
                SuggestionChip(
                    onClick = { },
                    label = { Text("PDOP值 ${locationState!!.pdop}") }
                )
                SuggestionChip(
                    onClick = { },
                    label = { Text("数据来源 ${if (locationState!!.source == Constant.LOCATION_SOURCE_ANTENNA) "天线" else "手机自带高精度"}") }
                )
                SuggestionChip(
                    onClick = { },
                    label = { Text(" ${if (locationState!!.source == Constant.LOCATION_SOURCE_ANTENNA) "天线" else "手机自带高精度"}") }
                )

                if (locationState!!.source == Constant.LOCATION_SOURCE_ANTENNA) {
                    SuggestionChip(
                        onClick = { },
                        label = { Text("天线网络数据是否参与结算:  ${if (locationState!!.networkDiffSync) "是" else "否"}") }
                    )
                }
            }
        } else {
            SuggestionChip(
                onClick = { },
                label = { Text("暂无位置信息") }
            )
        }
    }

    @Composable
    fun USBBox() {
        var autoBluetoothFlag by remember {
            mutableStateOf(false)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (!usbFlag) {
                Button(onClick = {
                    instance.usbConnect(this@MainActivity, lon, lat, autoBluetoothFlag)
                }, enabled = usbAttachFlag && !usbFlag) {
                    Text("连接天线")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            MaterialTheme.colorScheme.primaryContainer
                        )
                        .padding(horizontal = 6.dp)
                ) {
                    Text("是否自动连接蓝牙")
                    Switch(autoBluetoothFlag, onCheckedChange = {
                        autoBluetoothFlag = it
                    }, enabled = usbAttachFlag && !usbFlag)
                }
            }

        }
    }

    @Composable
    fun BlueToothBox() {


        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Button(onClick = {
                val bleInstance = instance.getBleInstance(this@MainActivity)
                bleInstance.startScan(object : BleScanCallback<BleDevice>() {
                    override fun onLeScan(device: BleDevice?, rssi: Int, scanRecord: ByteArray?) {
                        if (device == null){
                            return
                        }
                        if (device.bleName.isNullOrEmpty()) {
                            return
                        }
                        if (bluetoothDeviceList.contains(device)) {
                            return
                        }
                        bluetoothDeviceList.add(device)
                    }
                })

            }, enabled = !bluetoothFlag) {
                Text("蓝牙连接")
            }


        }

    }

    @Composable
    fun StatusBox() {


        var isStartSuccessState by remember {
            mutableStateOf(false)
        }

        LaunchedEffect(true) {
            instance.setOnAntennaConnectListener(object : ESAntennaConnectListener {
                override fun onChange(
                    source: Int,
                    isConnectSuccess: Boolean,
                    isStartSuccess: Boolean,
                    message: String
                ) {

                    isConnectSuccessState = isConnectSuccess
                    isStartSuccessState = isStartSuccess
                    messageState = message

                    // typec 是为连接
                    locationSource = if (source == Constant.ANTENNA_SOURCE_BLUETOOTH && !instance.usbConnectStatus) {
                        source
                    } else {
                        source
                    }
                    if (isConnectSuccessState) {
                        if (source == Constant.ANTENNA_SOURCE_BLUETOOTH) {
                            bluetoothFlag = true
                        } else {
                            usbFlag = true
                        }
                    }
                }
            })

            instance.setOnAntennaDisConnectListener(object : ESAntennaDisConnectListener {
                override fun onDisConnect(disConnectSource: Int) {
                    if (!instance.blueToothStatus && !instance.usbConnectStatus) {
                        locationSource = -1
                        isStartSuccessState = false
                        messageState = "设备已断开链接"
                        isConnectSuccessState = false
                        locationState = null

                    }
                    if (disConnectSource == Constant.ANTENNA_SOURCE_BLUETOOTH) {
                        bluetoothFlag = false
                        if (instance.usbConnectStatus) {
                            locationSource = Constant.ANTENNA_SOURCE_USB
                            ToastUtils.showLong("已自动切换到USB方式")
                            messageState = "已自动切换到USB方式"
                        }
                    } else {
                        usbFlag = false
                        ToastUtils.showLong("USB已断开")
                        if (instance.blueToothStatus) {
                            locationSource = Constant.ANTENNA_SOURCE_BLUETOOTH
                            ToastUtils.showLong("已自动切换到蓝牙方式")
                            messageState = "已自动切换到蓝牙方式"

                        }
                    }
                }
            })


            instance.setOnAntennaAuthListener(object: ESAntennaAuthListener {
                override fun onAuthentication(
                    isAuthentication: Boolean,
                    message: String
                ) {
                    if (!isAuthentication) {
                        ToastUtils.showLong("认证失败: ${message}")
                    }
                }

            })
        }

        Column(
            Modifier
                .clip(
                    RoundedCornerShape(10.dp)
                )
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("当前机器是否挂载Type-C设备: ${if (usbAttachFlag) "已挂载" else "未挂载"}")
            Text("当前Usb连接状态: ${if (usbFlag) "已连接" else "未连接"}")
            Text("当前天线连接方式: ${if (locationSource == -1) "未知" else if (locationSource == Constant.ANTENNA_SOURCE_USB) "TYPE-C" else "蓝牙"}")
            Text("是否连接成功: ${if (isConnectSuccessState) "成功" else "未成功"}")
            Text("是否启动成功: ${if (isStartSuccessState) "成功" else "未成功"}")
            Text("日志信息: $messageState")
        }

    }

}
