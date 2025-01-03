@file:OptIn(ExperimentalMaterial3Api::class)

package com.esurvey.esurvey_sdk_demo;


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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.Build
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
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.esurvey.sdk.out.ESurvey
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.ClipboardUtils
import com.blankj.utilcode.util.NetworkUtils
import com.blankj.utilcode.util.PermissionUtils
import com.blankj.utilcode.util.SDCardUtils
import com.blankj.utilcode.util.ToastUtils
import com.esurvey.esurvey_sdk.utils.Api
import com.esurvey.esurvey_sdk_demo.ui.theme.Esurvey_sdk_demoTheme
import com.esurvey.sdk.out.ByteUtils
import com.esurvey.sdk.out.data.BluetoothInfo
import com.esurvey.sdk.out.data.Constant
import com.esurvey.sdk.out.data.EAntennaDeviceInfo
import com.esurvey.sdk.out.data.LocationState
import com.esurvey.sdk.out.data.UsbMessageEvent
import com.esurvey.sdk.out.exception.EsException
import com.esurvey.sdk.out.listener.ESAntennaAuthListener
import com.esurvey.sdk.out.listener.ESAntennaConnectListener
import com.esurvey.sdk.out.listener.ESAntennaDisConnectListener
import com.esurvey.sdk.out.listener.ESAntennaMeasureEnableListener
import com.esurvey.sdk.out.listener.ESAntennaMeasureListener
import com.esurvey.sdk.out.listener.ESAntennaOriginMessageListener
import com.esurvey.sdk.out.listener.ESAntennaOtaListener
import com.esurvey.sdk.out.listener.ESBluetoothScanResultListener
import com.esurvey.sdk.out.listener.ESLocationChangeListener
import com.esurvey.sdk.out.listener.ESMobileHighStatusListener
import com.esurvey.sdk.out.listener.ESUsbAttachChangeListener
import com.lxj.xpopup.XPopup
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.scan.ScanFilter
import com.polidea.rxandroidble2.scan.ScanSettings
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlin.random.Random


class MainActivity : ComponentActivity() {
    val instance = ESurvey.getInstance()

    var bluetoothFlag by mutableStateOf(false)
    var isSurfaceSwitch by mutableStateOf(false)
    var measureFlag by mutableStateOf(false)
    var usbFlag by mutableStateOf(false)

    var locationState by mutableStateOf<LocationState?>(null)
    var usbAttachFlag by mutableStateOf(false)
    var permissionFlag by mutableStateOf(false)
    var isConnectSuccessState by mutableStateOf(false)
    var locationSource by mutableStateOf(-1)

    val bluetoothDeviceList = mutableStateListOf<BluetoothInfo>()
    val logList = mutableStateListOf<String>()

    var messageState by mutableStateOf("暂无日志")



    val lon = 112.994693
    val lat = 28.149546


    val appUserId = System.nanoTime().toString() + "_demo_" + Random.nextInt(1, 999999)



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        instance.setKey(Api.key)

        instance.setOnUsbAttachChangeListener(object : ESUsbAttachChangeListener {
            override fun onChange(isAttach: Boolean) {
                usbAttachFlag = isAttach
            }
        })



        onPermissionRequest()
        TipsSoundsService.init(this)


        MainScope().launch{
            Api.getAppToken{
                this@MainActivity.runOnUiThread{
                    XPopup.Builder(this@MainActivity)
                        .asConfirm("获取APPTOKEN 失败", "获取APPTOKEN 失败，无法启动手机高精度，天线也只能使用FM版", "", "确定", {
                        }, {}, true)
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
                        ModalDrawerSheet {
                            Text("易测", modifier = Modifier.padding(16.dp))
                            Spacer(Modifier.padding(vertical = 10.dp))
                            LogBox()
                        }
                    }
                ) {
                    val scope = rememberCoroutineScope()
                    Scaffold(
                        floatingActionButton = {
                            ExtendedFloatingActionButton(
                                text = { Text("日志") },
                                icon = { Icon(Icons.Filled.Add, contentDescription = "") },
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
                        Column(
                            Modifier
                                .padding(contentPadding)
                                .padding(horizontal = 20.dp, vertical = 20.dp)
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
            instance.setOnAntennaOtaListener(object: ESAntennaOtaListener {
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
            Dialog(onDismissRequest = {  }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(375.dp)
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {

                        CircularProgressIndicator(
                            modifier = Modifier.width(64.dp),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            progress = { otaPercent / 100f },
                        )
                        Text("${otaPercent}%", modifier = Modifier.padding(top = 40.dp))
                        Text("固件升级中，请稍后", modifier = Modifier.padding(top = 10.dp))
                        Text("设备型号: ${deviceInfo!!.deviceType}", modifier = Modifier.padding(top = 10.dp), fontSize = 10.sp)
                        Text("当前设备版本号: ${deviceInfo!!.version}", modifier = Modifier.padding(top = 4.dp), fontSize = 10.sp)
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
            Button(onClick = {
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
            Button(onClick = {
                AppUtils.exitApp()
            }) {
                Text("请先在Demo里 com.esurvey.esurvey_sdk.utils.Key 中配置 key 和 secret， 配置完再重启应用")
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
            MeasureBox()
            Ota()

            if (bluetoothDeviceList.isNotEmpty()) {
                ModalBottomSheet(onDismissRequest = {
                    bluetoothDeviceList.clear()
                }) {
                    LazyColumn {
                        items(bluetoothDeviceList.size) { it ->
                            ListItem(
                                headlineContent = { Text(bluetoothDeviceList[it].name!!) },
                                leadingContent = {
                                    Icon(
                                        painter = painterResource(R.drawable.icon_bluetooth),
                                        tint = MaterialTheme.colorScheme.primary,
                                        contentDescription = "Localized description",
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
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
                                        // appUserId 或者 Key 未传将抛出异常
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
                    .padding(top = 10.dp)) {

                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, end = 10.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Icon(Icons.Default.Clear, contentDescription = "", modifier = Modifier.clickable {
                        logList.clear()
                    })
                    Spacer(Modifier.padding(horizontal = 2.dp))
                    Icon(Icons.Default.Build, contentDescription = "", modifier = Modifier.clickable {
                        val joinToString = logList.joinToString("|")
                        ClipboardUtils.copyText(joinToString)
                    })

                }
                LazyColumn(
                    Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(logList.size) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(logList[it], modifier = Modifier.weight(1f))
                            Icon(
                                Icons.Outlined.Build,
                                contentDescription = "",
                                modifier = Modifier.clickable {
                                    ClipboardUtils.copyText(logList[it])
                                })
                        }
                    }
                }
            }
        }


        AnimatedVisibility(logList.isEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text("暂无日志")
            }
        }
    }


    @Composable
    fun MeasureBox() {
        Card(
            Modifier
                .padding(top = 10.dp)
                .fillMaxWidth()) {
            Spacer(Modifier.padding(top = 10.dp))
            Text("激光测距",  modifier = Modifier.padding(start = 10.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .padding(bottom = 10.dp), horizontalArrangement = Arrangement.SpaceAround) {
                Button(onClick = {
                    instance.enableMeasure(object : ESAntennaMeasureEnableListener {
                        override fun onStart() {
                            measureFlag = true
                            syncLog("激光测距已可用")
                        }
                    })
                }, enabled = !measureFlag) {
                    Text("使能")
                }

                Button(onClick = {
                    measureFlag = false
                    instance.disableMeasure()
                }, enabled = measureFlag) {
                    Text("停止")
                }

                Button(onClick = {
                    /**
                     * 先使能，再测量
                     */
                    instance.measure(object : ESAntennaMeasureListener {
                        override fun onMeasure(
                            isSuccess: Boolean,
                            distance: String
                        ) {
                            if (isSuccess) {
                                syncLog("距离${distance}毫米")
                                messageState = "距离${distance}毫米"
                                syncLog("距离${distance}毫米")
                            } else {
                                syncLog("测量失败")
                            }
                        }
                    })
                }, enabled = measureFlag) {
                    Text("测量")
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
                        mobileHighIsStart = true
                        lifecycleScope.launch{
                            Api.getSdkToken(appUserId) { flag ->
                                this@MainActivity.runOnUiThread{
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
                                    }  catch (e: EsException) {
                                        // appUserId 或者 Key 未传将抛出异常
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
                            // 无权限，打开权限设置界面
                            XPopup.Builder(this@MainActivity)
                                .asConfirm("请先授予文件权限", "请先授予文件权限", "", "确定", {
                                    try {
                                        val intent =
                                            Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
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
                        PermissionUtils.permission(
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            android.Manifest.permission.READ_EXTERNAL_STORAGE
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

        var showLocation by remember {
            mutableStateOf(true)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .padding(top = 10.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    MaterialTheme.colorScheme.primaryContainer
                )
                .padding(horizontal = 6.dp)
        ) {
            Text("平面坐标")
            Switch(isSurfaceSwitch, onCheckedChange = {
                isSurfaceSwitch = it
            } )
        }
        if (locationState != null) {

            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                if (isSurfaceSwitch) {
                    val doubles = GaussConvert.BL_xy3(locationState?.lat?.toDouble()!!, locationState?.lon?.toDouble()!!);
                    SuggestionChip(
                        onClick = { },
                        label = { Text("x / y ${"%.2f".format(doubles[0])} - ${"%.2f".format(doubles[1])}") }
                    )
                } else {
                    SuggestionChip(
                        onClick = { },
                        label = { Text("Lon / Lat ${locationState?.lon} - ${locationState?.lat}") }
                    )
                }
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
                    if (Api.appToken.isEmpty()) {
                        ToastUtils.showShort("无法获取到AppToken, FM信号弱的情况下可能无法固定解")
                    }
                    lifecycleScope.launch{
                        syncLog("正在获取SDKToken")
                        Api.getSdkToken(appUserId) { flag ->
                            try {
                                instance.usbConnect(this@MainActivity, lon, lat, autoBluetoothFlag, appUserId, Api.sdkToken)
                            } catch (e: EsException) {
                                // appUserId 或者 Key 未传将抛出异常
                                syncLog("USB连接失败: ${e.message}")
                            }
                        }
                    }
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
                if (Api.appToken.isEmpty()) {
                    ToastUtils.showShort("无法获取到AppToken, FM信号弱的情况下可能无法固定解")
                }
                lifecycleScope.launch{
                    syncLog("正在获取SDKToken")
                    Api.getSdkToken(appUserId) { flag ->
                        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                        if (bluetoothAdapter == null) {
                            // 设备不支持蓝牙
                            ToastUtils.showShort("设备不支持蓝牙")
                            return@getSdkToken
                        } else if (!bluetoothAdapter.isEnabled()) {
                            // 蓝牙未启用
                            ToastUtils.showShort("请先开启蓝牙")
                            return@getSdkToken
                        }
                        instance.startBluetoothScan(this@MainActivity,
                            object : ESBluetoothScanResultListener {
                                override fun onChange(info: BluetoothInfo) {
                                    if (bluetoothDeviceList.find { it -> it.name == info.name } == null) {
                                        bluetoothDeviceList.add(info)
                                    }
                                }
                            })
                    }
                }
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
                    syncLog(message)

                    // typec 是为连接
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


    private fun syncLog(message: String) {
        if (message.isEmpty()) {
            return
        }
        logList.add(0, message)
        ToastUtils.showShort(message)

    }

}

