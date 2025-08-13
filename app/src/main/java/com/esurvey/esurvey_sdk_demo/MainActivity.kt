@file:OptIn(ExperimentalMaterial3Api::class)

package com.esurvey.esurvey_sdk_demo

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.esurvey.sdk.out.ESurvey
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.ClipboardUtils
import com.blankj.utilcode.util.PermissionUtils
import com.blankj.utilcode.util.ToastUtils
import com.esurvey.esurvey_sdk.utils.Api
import com.esurvey.sdk.out.data.BluetoothInfo
import com.esurvey.sdk.out.data.Constant
import com.esurvey.sdk.out.data.EAntennaDeviceInfo
import com.esurvey.sdk.out.data.ESEnvironment
import com.esurvey.sdk.out.data.LocationState
import com.esurvey.sdk.out.exception.EsException
import com.esurvey.sdk.out.listener.ESAntennaAuthListener
import com.esurvey.sdk.out.listener.ESAntennaConnectListener
import com.esurvey.sdk.out.listener.ESAntennaDisConnectListener
import com.esurvey.sdk.out.listener.ESAntennaMeasureEnableListener
import com.esurvey.sdk.out.listener.ESAntennaMeasureListener
import com.esurvey.sdk.out.listener.ESAntennaOriginMessageListener
import com.esurvey.sdk.out.listener.ESAntennaOtaListener
import com.esurvey.sdk.out.listener.ESBatteryQueryListener
import com.esurvey.sdk.out.listener.ESBluetoothScanResultListener
import com.esurvey.sdk.out.listener.ESDeviceSettingListener
import com.esurvey.sdk.out.listener.ESDeviceWifiModeChangeListener
import com.esurvey.sdk.out.listener.ESDeviceWifiModeQueryListener
import com.esurvey.sdk.out.listener.ESGNSSModeChangeListener
import com.esurvey.sdk.out.listener.ESGNSSModeQueryListener
import com.esurvey.sdk.out.listener.ESLocationChangeListener
import com.esurvey.sdk.out.listener.ESMobileHighStatusListener
import com.esurvey.sdk.out.listener.ESUsbAttachChangeListener
import com.lxj.xpopup.XPopup
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.layout.RowScope
import com.esurvey.esurvey_sdk_demo.ui.theme.Esurvey_sdk_demoTheme
import com.esurvey.esurvey_sdk_demo.BuildConfig
import kotlin.text.format


class MainActivity : ComponentActivity() {
    // 环境Key

    val instance = ESurvey.getInstance()

    var bluetoothFlag by mutableStateOf(false)
    var isSurfaceSwitch by mutableStateOf(false)
    var isSimpleDataSwitch by mutableStateOf(false)
    var deviceHeight by mutableDoubleStateOf(0.0)
    var measureFlag by mutableStateOf(false)
    var usbFlag by mutableStateOf(false)

    // GNSS工作模式 0：流动站 1：基准站
    var gnssWorkMode by mutableIntStateOf(0)

    // wifiMode 0：内置天线 1：外置天线
    var wifiMode by mutableIntStateOf(0)

    var locationState by mutableStateOf<LocationState?>(null)
    var usbAttachFlag by mutableStateOf(false)
    var permissionFlag by mutableStateOf(false)
    var isConnectSuccessState by mutableStateOf(false)
    var locationSource by mutableIntStateOf(-1)

    val bluetoothDeviceList = mutableStateListOf<BluetoothInfo>()
    val logList = mutableStateListOf<String>()

    var messageState by mutableStateOf("暂无日志")


    val lon = 112.994693
    val lat = 28.149546


    val appUserId = System.nanoTime().toString() + Random.nextInt(1, 999999)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        if (BuildConfig.FLAVOR == AppFlavor.GUANG_DONG) {
            instance.setEnvironment(ESEnvironment.GUANG_DONG)
        }
        instance.setKey(Api.key)

        instance.setOnUsbAttachChangeListener(object : ESUsbAttachChangeListener {
            override fun onChange(isAttach: Boolean) {
                usbAttachFlag = isAttach
            }
        })


        onPermissionRequest()
        TipsSoundsService.init(this)


        MainScope().launch {
            Api.getAppToken {
                this@MainActivity.runOnUiThread {
                    XPopup.Builder(this@MainActivity)
                        .asConfirm(
                            "获取APPTOKEN 失败",
                            "获取APPTOKEN 失败，无法启动手机高精度，天线也只能使用FM版",
                            "",
                            "确定",
                            {
                            },
                            {},
                            true
                        )
                        .show()
                }
            }
        }
        setContent {
            Esurvey_sdk_demoTheme {
                val rememberDrawerState = rememberDrawerState(DrawerValue.Closed)
                ModalNavigationDrawer(
                    drawerState = rememberDrawerState,
                    drawerContent = {
                        ModalDrawerSheet() {
                            Text(
                                when (BuildConfig.FLAVOR) {
                                    AppFlavor.GUANG_DONG -> "广东易测开放平台"
                                    else -> "易测"
                                }, modifier = Modifier.padding(16.dp)
                            )
                            Spacer(Modifier.padding(vertical = 10.dp))
                            LogBox()
                        }
                    }
                ) {
                    val scope = rememberCoroutineScope()
                    Scaffold(
                        floatingActionButton = {
                            ExtendedFloatingActionButton(
                                text = { Text("日志", color = Color.White) },
                                icon = { Icon(Icons.Filled.Add, contentDescription = "", tint = Color.White) },
                                containerColor = Color(0xFF3B5F8A),
                                contentColor = Color.White,
                                shape = RoundedCornerShape(12.dp),
                                onClick = {
                                    scope.launch {
                                        rememberDrawerState.apply {
                                            if (isClosed) open() else close()
                                        }
                                    }
                                }
                            )
                        }
                    ) { contentPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFF3F4F6))
                        ) {
                            Column(
                                Modifier
                                    .padding(contentPadding)
                                    .align(Alignment.TopCenter)
                                    .widthIn(max = 360.dp)
                                    .padding( vertical = 8.dp)
                            ) {
                                if (Api.key.isEmpty() || Api.secret.isEmpty()) {
                                    NoKeyApp()
                                } else {
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
            }
        }
    }


    private fun onPermissionRequest() {
        val permissions: MutableList<String> = ArrayList()
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
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
                    syncLog("请打开定位权限")
                }

            })
            .request()
    }

    var isOta by mutableStateOf(false)
    var otaPercent by mutableIntStateOf(0)


    var deviceInfo by mutableStateOf<EAntennaDeviceInfo?>(null)

    @Composable
    fun Ota() {
        LaunchedEffect(true) {
            instance.setOnAntennaOtaListener(object : ESAntennaOtaListener {
                override fun onStart() {
                    isOta = true
                    deviceInfo = instance.deviceInfo
                }

                override fun onEnd(isSuccess: Boolean, message: String) {
                    isOta = false
                    ToastUtils.showLong(message)
                }

                override fun onProgress(percent: Int) {
                    otaPercent = percent
                }

            })
        }


        if (isOta) {
            Dialog(onDismissRequest = { }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(375.dp)
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        CircularProgressIndicator(
                            modifier = Modifier.width(64.dp),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            progress = { otaPercent / 100f },
                        )
                        Text("${otaPercent}%", modifier = Modifier.padding(top = 40.dp))
                        Text("固件升级中，请稍后", modifier = Modifier.padding(top = 10.dp))
                        Text(
                            "设备型号: ${deviceInfo!!.deviceType}",
                            modifier = Modifier.padding(top = 10.dp),
                            fontSize = 10.sp
                        )
                        Text(
                            "当前设备版本号: ${deviceInfo!!.version}",
                            modifier = Modifier.padding(top = 4.dp),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

    }

    @Composable
    fun UnPermissionApp() {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PrimaryButton(onClick = {
                onPermissionRequest()
            }) {
                Text("请求权限")
            }
        }
    }


    @Composable
    fun NoKeyApp() {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PrimaryButton(onClick = {
                AppUtils.exitApp()
            }) {
                Text("请先在Demo里 com.esurvey.esurvey_sdk.utils.Key 中配置 key 和 secret， 配置完再重启应用")
            }
        }
    }

    @Composable
    fun App() {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DeviceStatusBox()
            ConnectionControlBox()
            LocationServiceBox()
            DeviceFunctionBox()
            Ota()
            if (bluetoothDeviceList.isNotEmpty()) {
                ModalBottomSheet(onDismissRequest = {
                    bluetoothDeviceList.clear()
                }) {
                    LazyColumn {
                        items(bluetoothDeviceList.size) { it ->
                            ListItem(
                                headlineContent = { Text(bluetoothDeviceList[it].name!!) },
                                modifier = Modifier.clickable {
                                    try {
                                        instance.bluetoothConnect(
                                            this@MainActivity,
                                            bluetoothDeviceList[it],
                                            lon,
                                            lat,
                                            appUserId,
                                            Api.sdkToken
                                        )
                                        syncLog("正在连接蓝牙设备")
                                        instance.stopBluetoothScan()
                                        bluetoothDeviceList.clear()
                                    } catch (e: EsException) {
                                        syncLog(" appUserId 或者 Key 未传将抛出异常:${e.message}")
                                    }
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
    fun LogBox() {
        LaunchedEffect(true) {
            instance.setOnAntennaOriginMessageListener(object : ESAntennaOriginMessageListener {
                override fun onMessage(message: String) {
                    logList.add(0, message)
                }
            })
        }

        AnimatedVisibility(logList.isNotEmpty()) {
            Card(
                Modifier
                    .padding(horizontal = 10.dp)
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        "清空",
                        modifier = Modifier.clickable { logList.clear() },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        "复制全部",
                        modifier = Modifier.clickable {
                            val joinToString = logList.joinToString("\n")
                            ClipboardUtils.copyText(joinToString)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                LazyColumn(
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(logList.size) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                logList[it],
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "复制",
                                modifier = Modifier.clickable {
                                    ClipboardUtils.copyText(logList[it])
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(logList.isEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text("暂无日志", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }


    @Composable
    fun LocationServiceBox() {
        var mobileHighIsStart by remember {
            mutableStateOf(false)
        }


        var mobileAssistLocation by remember {
            mutableStateOf(false)
        }


        LaunchedEffect(true) {
            instance.setOnLocationStateChangeListener(object : ESLocationChangeListener {
                override fun onChange(locationStateParam: LocationState) {
                    locationState = locationStateParam
                }
            })

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

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "位置服务",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Light,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactToggle("平面坐标", isSurfaceSwitch) { isSurfaceSwitch = it }
                    CompactToggle("精简信息", isSimpleDataSwitch) { isSimpleDataSwitch = it }
                }

                LocationInfoDisplay()

                // 手机高精度定位控制
                if (mobileHighIsStart) {
                    PrimaryButton(
                        onClick = {
                            instance.stopMobileHighLocation()
                            locationState = null
                            mobileHighIsStart = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                    ) {
                        Text("关闭手机高精度定位", style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    PrimaryButton(
                        onClick = {
                            val start = {
                                mobileHighIsStart = true
                                lifecycleScope.launch {
                                    Api.getSdkToken(appUserId) { flag ->
                                        this@MainActivity.runOnUiThread {
                                            try {
                                                instance.startMobileHighLocation(
                                                    this@MainActivity,
                                                    appUserId,
                                                    Api.sdkToken
                                                )
                                                if (!flag) {
                                                    syncLog("获取SDKToen 失败无法启动手机高精度")
                                                    mobileHighIsStart = false
                                                }
                                            } catch (e: EsException) {
                                                syncLog(" appUserId 或者 Key 未传将抛出异常:${e.message}")
                                                mobileHighIsStart = false
                                            }
                                        }
                                    }
                                }
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                if (Environment.isExternalStorageManager()) {
                                    start()
                                } else {
                                    XPopup.Builder(this@MainActivity)
                                        .asConfirm(
                                            "请先授予文件权限",
                                            "请先授予文件权限",
                                            "",
                                            "确定",
                                            {
                                                try {
                                                    val intent =
                                                        Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                                    ActivityUtils.getTopActivity()
                                                        .startActivity(intent)
                                                } catch (e: Exception) {
                                                    val intent = Intent(Settings.ACTION_SETTINGS)
                                                    this@MainActivity.startActivity(intent)
                                                }
                                            },
                                            {},
                                            true
                                        )
                                        .show()
                                }
                            } else {
                                PermissionUtils.permission(
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.READ_EXTERNAL_STORAGE
                                )
                                    .callback(object : PermissionUtils.SimpleCallback {
                                        override fun onGranted() {
                                            start()
                                        }

                                        override fun onDenied() {
                                            XPopup.Builder(this@MainActivity)
                                                .asConfirm(
                                                    "请先授予文件权限",
                                                    "请找到本App,并且打开文件权限",
                                                    "",
                                                    "确定",
                                                    {
                                                        PermissionUtils.launchAppDetailsSettings()
                                                    },
                                                    {},
                                                    true
                                                )
                                                .show()
                                        }
                                    }).request()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                    ) {
                        Text("开启手机高精度定位", style = MaterialTheme.typography.bodySmall)
                    }
                }

                if (BuildConfig.FLAVOR == AppFlavor.GUANG_DONG) {
                    PrimaryButton(onClick = {
                        lifecycleScope.launch {
                            Api.getSdkToken(appUserId) { flag ->
                                this@MainActivity.runOnUiThread {
                                    try {
                                        instance.startAssistLocation(
                                            this@MainActivity,
                                            appUserId,
                                            Api.sdkToken
                                        )
                                        if (!flag) {
                                            syncLog("获取SDKToen 失败无法启动手机辅助定位")
                                        }
                                        mobileAssistLocation = true
                                    } catch (e: EsException) {
                                        syncLog("手机辅助定位启动失败:${e.message}")
                                    }
                                }
                            }
                        }
                    }, enabled = !mobileAssistLocation, modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)) {
                        Text(if (mobileAssistLocation) "已使用手机位置辅助定位" else "使用手机位置辅助定位", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun LocationInfoDisplay() {
        if (locationState != null) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isSurfaceSwitch) {
                    val doubles = GaussConvert.BL_xy3(
                        locationState?.lat?.toDouble()!!,
                        locationState?.lon?.toDouble()!!
                    )
                    SuggestionChip(
                        onClick = { },
                        label = {
                            Text(
                                "x/y ${"%.2f".format(doubles[0])} - ${"%.2f".format(doubles[1])}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    )
                } else {
                    SuggestionChip(
                        onClick = { },
                        label = {
                            Text(
                                "Lon/Lat ${locationState?.lon} - ${locationState?.lat}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    )
                }
                SuggestionChip(
                    onClick = { },
                    label = {
                        Text(
                            "水平误差 ±${"%.2f".format(locationState?.getxInaccuracies())}m",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                )
                if (!isSimpleDataSwitch) {
                    SuggestionChip(
                        onClick = { },
                        label = {
                            Text(
                                "高程 ${locationState?.height}(±${"%.2f".format(locationState?.getyInaccuracies())}m)",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    )
                }
                SuggestionChip(
                    onClick = { },
                    label = {
                        Text(
                            "定位状态 ${locationState!!.locationStatusShow}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                )
                if (!isSimpleDataSwitch) {
                    SuggestionChip(
                        onClick = { },
                        label = {
                            Text(
                                "解算卫星 ${locationState?.satelliteNum}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    )
                    SuggestionChip(
                        onClick = { },
                        label = {
                            Text(
                                "PDOP值 ${locationState!!.pdop}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    )
                    SuggestionChip(
                        onClick = { },
                        label = {
                            Text(
                                "数据来源 ${if (locationState!!.source == Constant.LOCATION_SOURCE_ANTENNA) "天线" else "手机自带高精度"}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    )
                }

                if (locationState!!.source == Constant.LOCATION_SOURCE_ANTENNA) {
                    SuggestionChip(
                        onClick = { },
                        label = {
                            Text(
                                "网络数据参与结算: ${if (locationState!!.networkDiffSync) "是" else "否"}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    )
                }
            }
        } else {
            SuggestionChip(
                onClick = { },
                label = { Text("暂无位置信息", style = MaterialTheme.typography.bodySmall) }
            )
        }
    }

    @Composable
    fun DeviceFunctionBox() {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "设备功能",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Light
                )

                // 激光测距
                LaserMeasurementSection()

                HorizontalDivider(Modifier.padding(vertical = 2.dp))

                // 设备设置
                DeviceSettingsSection()
            }
        }
    }

    @Composable
    fun LaserMeasurementSection() {
        Text(
            "激光测距",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PrimaryButton(
                onClick = {
                    instance.enableMeasure(object : ESAntennaMeasureEnableListener {
                        override fun onStart() {
                            measureFlag = true
                            syncLog("激光测距已可用")
                        }
                    })
                },
                enabled = !measureFlag,
                modifier = Modifier
                    .weight(1f)
                    .height(30.dp)
            ) {
                Text("使能", style = MaterialTheme.typography.labelSmall)
            }

            PrimaryButton(
                onClick = {
                    measureFlag = false
                    instance.disableMeasure()
                },
                enabled = measureFlag,
                modifier = Modifier
                    .weight(1f)
                    .height(30.dp)
            ) {
                Text("停止", style = MaterialTheme.typography.labelSmall)
            }

            PrimaryButton(
                onClick = {
                    instance.measure(object : ESAntennaMeasureListener {
                        override fun onMeasure(isSuccess: Boolean, distance: String) {
                            if (isSuccess) {
                                syncLog("距离${distance}毫米")
                                messageState = "距离${distance}毫米"
                            } else {
                                syncLog("测量失败")
                            }
                        }
                    })
                },
                enabled = measureFlag,
                modifier = Modifier
                    .weight(1f)
                    .height(30.dp)
            ) {
                Text("测量", style = MaterialTheme.typography.labelSmall)
            }
        }
    }

    @Composable
    fun DeviceSettingsSection() {
        Text(
            "设备设置",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )

        // 第一行按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PrimaryButton(
                onClick = {
                    XPopup.Builder(this@MainActivity)
                        .asInputConfirm("请输入仪器高（单位米）", "请输入仪器高（单位米）") {
                            it
                            try {
                                val toDouble = it.toDouble()
                                instance.setDeviceHeight(toDouble)
                                deviceHeight = toDouble
                            } catch (e: Exception) {
                                ToastUtils.showLong("请输入数字")
                            }
                        }
                        .show()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(30.dp)
            ) {
                Text(
                    if (deviceHeight > 0) "仪器高(${deviceHeight}m)" else "仪器高设置",
                    style = MaterialTheme.typography.labelSmall
                )
            }

            PrimaryButton(
                onClick = {
                    instance.changeDeviceGNSSWorkMode(
                        if (gnssWorkMode == 0) 1 else 0,
                        object : ESGNSSModeChangeListener {
                            override fun onChange(success: Boolean, message: String) {
                                if (success) {
                                    gnssWorkMode = if (gnssWorkMode == 0) 1 else 0
                                    syncLog("GNSS工作模式已切换为${if (gnssWorkMode == 0) "流动站" else "基准站"}")
                                } else {
                                    syncLog("GNSS工作模式切换失败: ${message}")
                                }
                            }
                        })
                    MainScope().launch {
                        delay(1000)
                        queryGnssWorkMode()
                    }
                },
                enabled = bluetoothFlag || usbFlag,
                modifier = Modifier
                    .weight(1f)
                    .height(30.dp)
            ) {
                Text(
                    if (gnssWorkMode == 0) "开启基准站" else "关闭基准站",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        // 第二行按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PrimaryButton(
                onClick = {
                    if (gnssWorkMode == 1) {
                        ToastUtils.showLong("基准站模式下，不可切换，请先关闭基准站模式")
                        return@PrimaryButton
                    }
                    instance.changeDeviceWifiMode(
                        if (wifiMode == 0) 1 else 0,
                        object : ESDeviceWifiModeChangeListener {
                            override fun onChange(success: Boolean, message: String) {
                                if (success) {
                                    wifiMode = if (wifiMode == 0) 1 else 0
                                    syncLog("WIFI模式已切换为${if (wifiMode == 0) "外置天线" else "内置天线"}")
                                } else {
                                    syncLog("WIFI模式切换失败: ${message}")
                                }
                            }
                        })
                },
                enabled = bluetoothFlag || usbFlag,
                modifier = Modifier
                    .weight(1f)
                    .height(30.dp)
            ) {
                Text(
                    if (wifiMode == 0) "切换内置wifi" else "切换外置wifi",
                    style = MaterialTheme.typography.labelSmall
                )
            }

            PrimaryButton(
                onClick = {
                    instance.queryBatteryInfo(object : ESBatteryQueryListener {
                        override fun onChange(battery: String) {
                            ToastUtils.showShort("当前电量: ${battery}%")
                        }
                    })
                },
                enabled = bluetoothFlag || usbFlag,
                modifier = Modifier
                    .weight(1f)
                    .height(30.dp)
            ) {
                Text("查询电量", style = MaterialTheme.typography.labelSmall)
            }
        }

        // 基准站坐标配置
        AnimatedVisibility(gnssWorkMode == 1) {
            PrimaryButton(
                onClick = {
                    XPopup.Builder(this@MainActivity)
                        .asInputConfirm(
                            "请输入经纬度高层每项数据，以空格分割",
                            "格式: 经度 维度 高程"
                        ) {
                            it
                            try {
                                val datas = it.split(" ")
                                if (datas.size != 3) {
                                    ToastUtils.showShort("经纬度高程输入异常")
                                }
                                val lon = datas[0].toDouble()
                                val lat = datas[1].toDouble()
                                val height = datas[2].toDouble()

                                XPopup.Builder(this@MainActivity)
                                    .asConfirm(
                                        "输入确认",
                                        "您输入的经度Lon:${lon} 纬度Lat: ${lat} 高程${height} 确定要配置吗？"
                                    ) {
                                        instance.setStaticModeLocation(
                                            lon,
                                            lat,
                                            height,
                                            object : ESDeviceSettingListener {
                                                override fun onChange(
                                                    type: Int,
                                                    isSuccess: Boolean
                                                ) {
                                                    if (type == Constant.STATIC_LOCATION_SETTING) {
                                                        ToastUtils.showLong("位置设置${if (isSuccess) "成功" else "失败"}")
                                                    }
                                                }
                                            })
                                    }
                                    .show()
                            } catch (e: Exception) {
                                ToastUtils.showLong("输入异常, 经纬度高程必须是数字")
                            }
                        }
                        .show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .padding(top = 2.dp)
            ) {
                Text("配置基准站坐标", style = MaterialTheme.typography.labelSmall)
            }
        }
    }


    @Composable
    fun ConnectionControlBox() {
        var autoBluetoothFlag by remember {
            mutableStateOf(false)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "连接控制",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Light
                )

                // USB连接区域
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PrimaryButton(
                        onClick = {
                            if (Api.appToken.isEmpty()) {
                                ToastUtils.showShort("无法获取到AppToken, FM信号弱的情况下可能无法固定解")
                            }
                            lifecycleScope.launch {
                                syncLog("正在获取SDKToken")
                                Api.getSdkToken(appUserId) { flag ->
                                    try {
                                        instance.usbConnect(
                                            this@MainActivity,
                                            lon,
                                            lat,
                                            autoBluetoothFlag,
                                            appUserId,
                                            Api.sdkToken
                                        )
                                    } catch (e: EsException) {
                                        syncLog("USB连接失败: ${e.message}")
                                    }
                                }
                            }
                        },
                        enabled = usbAttachFlag && !usbFlag,
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                    ) {
                        Text("USB连接", style = MaterialTheme.typography.bodySmall)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("自动蓝牙", style = MaterialTheme.typography.labelSmall)
                        Switch(
                            checked = autoBluetoothFlag,
                            onCheckedChange = { autoBluetoothFlag = it },
                            enabled = usbAttachFlag && !usbFlag,
                            modifier = Modifier.scale(0.8f)
                        )
                    }
                }

                // 蓝牙连接区域
                PrimaryButton(
                    onClick = {
                        if (Api.appToken.isEmpty()) {
                            ToastUtils.showShort("无法获取到AppToken, FM信号弱的情况下可能无法固定解")
                        }
                        lifecycleScope.launch {
                            syncLog("正在获取SDKToken")
                            Api.getSdkToken(appUserId) { flag ->
                                val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                                if (bluetoothAdapter == null) {
                                    ToastUtils.showShort("设备不支持蓝牙")
                                    return@getSdkToken
                                } else if (!bluetoothAdapter.isEnabled) {
                                    ToastUtils.showShort("请先开启蓝牙")
                                    return@getSdkToken
                                }
                                instance.startBluetoothScan(
                                    this@MainActivity,
                                    object : ESBluetoothScanResultListener {
                                        override fun onChange(info: BluetoothInfo) {
                                            if (bluetoothDeviceList.find { it -> it.name == info.name } == null) {
                                                bluetoothDeviceList.add(info)
                                            }
                                        }
                                    })
                            }
                        }
                    },
                    enabled = !bluetoothFlag,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                ) {
                    Text("蓝牙扫描连接", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }

    @Composable
    fun DeviceStatusBox() {
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
                    syncLog(message)

                    locationSource =
                        if (source == Constant.ANTENNA_SOURCE_BLUETOOTH && !instance.usbConnectStatus) {
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
                        syncLog("正在查询天线GNSS工作模式")
                        queryGnssWorkMode()

                        MainScope().launch {
                            delay(500)
                            syncLog("正在查询天线Wifi模式")
                            instance.queryDeviceWifiMode(object : ESDeviceWifiModeQueryListener {
                                override fun onChange(type: Int) {
                                    wifiMode = type
                                }
                            })
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
                        syncLog("设备已断开链接")

                        isConnectSuccessState = false
                        locationState = null
                        measureFlag = false

                        otaPercent = 0
                        isOta = false
                    }
                    if (disConnectSource == Constant.ANTENNA_SOURCE_BLUETOOTH) {
                        bluetoothFlag = false
                        if (instance.usbConnectStatus) {
                            locationSource = Constant.ANTENNA_SOURCE_USB
                            syncLog("已自动切换到USB方式")
                            messageState = "已自动切换到USB方式"
                            syncLog("已自动切换到USB方式")
                        }
                    } else {
                        usbFlag = false
                        syncLog("USB已断开")
                        if (instance.blueToothStatus) {
                            locationSource = Constant.ANTENNA_SOURCE_BLUETOOTH
                            syncLog("已自动切换到蓝牙方式")
                            messageState = "已自动切换到蓝牙方式"
                            syncLog("已自动切换到蓝牙方式")
                        }
                    }
                }
            })

            instance.setOnAntennaAuthListener(object : ESAntennaAuthListener {
                override fun onAuthentication(
                    isAuthentication: Boolean,
                    message: String
                ) {
                    if (!isAuthentication) {
                        locationSource = -1
                        isStartSuccessState = false
                        messageState = "认证失败，无法启动设备: ${message}"
                        syncLog("认证失败，无法启动设备: ${message}")

                        isConnectSuccessState = false
                        locationState = null
                        measureFlag = false
                        usbFlag = false
                        bluetoothFlag = false
                    }
                }
            })
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "设备状态",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Light
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    CompactStatusItem(
                        "Type-C设备",
                        if (usbAttachFlag) "已挂载" else "未挂载",
                        usbAttachFlag
                    )
                    CompactStatusItem("USB连接", if (usbFlag) "已连接" else "未连接", usbFlag)
                    CompactStatusItem(
                        "连接方式",
                        if (locationSource == -1) "未知" else if (locationSource == Constant.ANTENNA_SOURCE_USB) "TYPE-C" else "蓝牙",
                        locationSource != -1
                    )
                    CompactStatusItem(
                        "连接状态",
                        if (isConnectSuccessState) "成功" else "未成功",
                        isConnectSuccessState
                    )
                    CompactStatusItem(
                        "启动状态",
                        if (isStartSuccessState) "成功" else "未成功",
                        isStartSuccessState
                    )
                    if (messageState.isNotEmpty() && messageState != "暂无日志") {
                        Text(
                            messageState,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 1.dp)
                        )
                    }
                }
            }
        }
    }


    private fun queryGnssWorkMode() {
        instance.queryDeviceGNSSWorkMode(object : ESGNSSModeQueryListener {
            override fun onChange(type: Int) {
                gnssWorkMode = type
            }
        })
    }

    private fun syncLog(message: String) {
        if (message.isEmpty()) {
            return
        }
        logList.add(0, message)
        ToastUtils.showShort(message)
    }

    @Composable
    fun CompactStatusItem(label: String, value: String, isSuccess: Boolean) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }

    @Composable
    fun CompactToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (checked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.scale(0.6f)
            )
        }
    }

    @Composable
    fun PrimaryButton(
        onClick: () -> Unit,
        enabled: Boolean = true,
        modifier: Modifier = Modifier,
        content: @Composable RowScope.() -> Unit
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3B5F8A),
                contentColor = Color.White,
                disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            ),
            content = content
        )
    }
}

