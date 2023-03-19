package com.example.transbywifi.groupowner

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.transbywifi.*
import com.example.transbywifi.models.ViewState
import kotlinx.android.synthetic.main.activity_file_gowner.*
import kotlinx.coroutines.*

import kotlin.coroutines.resume


class FileReceiverActivity : BaseActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_gowner)

        initView()
        initDevice()
        initEvent()
    }


    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SuspiciousIndentation")
    private fun initView() {
        supportActionBar?.title = "P2P Group Owner"

        btnCreatGroup.setOnClickListener {
            createGroup()
            val ipaddress=getIpAddress()
            log("本机ip地址：$ipaddress")
        }
        btnRemoveGroup.setOnClickListener {
            removeGroup()
        }
        btnGOchoose.setOnClickListener {
            alertdialog(devicenamelist )
        }
        btnStartReceive.setOnClickListener {
            fileReceiverViewModel.startListener()   //开启监听
        }
        btnReceiveip.setOnClickListener {
            clearLog()
            fileReceiverViewModel.receiveip(devicenamelist,iplist)
        }
    }

    //创建广播接收器
    private fun initDevice() {
        //当接收到这几个广播时，我们都需要到 WifiP2pManager （对等网络管理器）来进行相应的信息请求，
        // 此外还需要用到 Channel 对象作为请求参数
        val mWifiP2pManager = getSystemService(WIFI_P2P_SERVICE) as? WifiP2pManager//类型转换，转换不成功则返回null
        if (mWifiP2pManager == null) {
            finish()
            return
        }
        wifiP2pManager = mWifiP2pManager
        wifiP2pChannel = wifiP2pManager.initialize(this, mainLooper, directActionListener)
        broadcastReceiver = DirectBroadcastReceiver(
            wifiP2pManager = wifiP2pManager,
            wifiP2pChannel = wifiP2pChannel,
            directActionListener = directActionListener
        )
        registerReceiver(broadcastReceiver, DirectBroadcastReceiver.getIntentFilter())
    }

    private fun initEvent() {
        lifecycleScope.launch {
            fileReceiverViewModel.viewState.collect {   //收到消息
                when (it) {
                    ViewState.Idle -> {
                        clearLog()
                        dismissLoadingDialog()
                    }

                    ViewState.Connecting -> {
                        showLoadingDialog(message = "")
                    }

                    is ViewState.Receiving -> {
                        showLoadingDialog(message = "")
                    }

                    is ViewState.Success -> {
                        dismissLoadingDialog()
                        ivImage.load(data = it.file)
                    }

                    is ViewState.Failed -> {
                        dismissLoadingDialog()
                    }
                }
            }
        }
        lifecycleScope.launch {
            fileReceiverViewModel.log.collect {
                log(it)
            }
        }
        lifecycleScope.launch {
            fileReceiverViewModel.nametext.collect {
                ClientName.text = it
            }
        }
    }


    private var iplist = mutableSetOf<String>()
    private var devicenamelist = mutableSetOf<String>()

    private val getContentLaunch = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { imageUri ->
        if (imageUri != null) {
            fileReceiverViewModel.send(iplist, fileUri = imageUri)
        }
    }


    private fun alertdialog(devicenamelist: MutableSet<String>){
        AlertDialog.Builder(this).apply {
            setTitle("GroupMember")//为这个对话框设置标题、内容
            setMessage("是否对以下设备发送文件？")
            setMessage(devicenamelist.toString())
            setCancelable(true)//可否使用Back键关闭对话框等属性
            setPositiveButton("yes") { dialog, which ->
                getContentLaunch.launch("image/*")  //跳转文件选择界，获取相册
                //"image/*"指定只显示图片
            }
            setNegativeButton("Cancel") { dialog, which ->
                Toast.makeText(application, "取消发送", Toast.LENGTH_SHORT).show()
            }//为对话框设置确定按钮\取消按钮的点击事件
            show()//将对话框显示
        }
    }

    private val fileReceiverViewModel by viewModels<FileReceiverViewModel>()

    private lateinit var wifiP2pManager: WifiP2pManager

    private lateinit var wifiP2pChannel: WifiP2pManager.Channel

    private var connectionInfoAvailable = false

    private var broadcastReceiver: BroadcastReceiver? = null

    private val directActionListener = object : DirectActionListener {
        override fun wifiP2pEnabled(enabled: Boolean) {
            log("wifiP2pEnabled: $enabled")
        }

        override fun onConnectionInfoAvailable(wifiP2pInfo: WifiP2pInfo) {
            log("群组信息（onConnectionInfoAvailable）")
            log("isGroupOwner：" + wifiP2pInfo.isGroupOwner)
            log("groupFormed：" + wifiP2pInfo.groupFormed)
            log("GOip:"+wifiP2pInfo.groupOwnerAddress.hostAddress)
            //WifiP2p为每个组所有者分配相同的地址,192.168.49.1并使用192.168.49.0/24DHCP中的池向加入组所有者的设备发出地址
            if (wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner) {  //如果有组建立且自己是Go设备
                connectionInfoAvailable = true
            }
        }

        override fun onDisconnection() {
            connectionInfoAvailable = false
            log("未连接onDisconnection")
        }

        //wifiP2pDevice的意思是【本机设备信息】
        override fun onSelfDeviceAvailable(wifiP2pDevice: WifiP2pDevice) {
            log("本设备信息")
            log("onSelfDeviceAvailable")
            log("DeviceName: " + wifiP2pDevice.deviceName)
            log("DeviceAddress: " + wifiP2pDevice.deviceAddress)//本机mac地址
            log("Status: " + wifiP2pDevice.status)//（0是连接 ，1是邀请中，3是未连接，但是可用，4是不可用）
        }

        override fun onPeersAvailable(wifiP2pDeviceList: Collection<WifiP2pDevice>) {
            log("可用设备数量:" + wifiP2pDeviceList.size)
            for (wifiP2pDevice in wifiP2pDeviceList) {
                log("DeviceName: " + wifiP2pDevice.deviceName)
                log("DeviceAddress: " + wifiP2pDevice.deviceAddress)
                log("Status: " + wifiP2pDevice.status)
            }
        }

        override fun onChannelDisconnected() {
            log("onChannelDisconnected")
        }
    }



    override fun onDestroy() {
        super.onDestroy()
        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver)
        }
        removeGroup()
    }

    @SuppressLint("MissingPermission")
    private fun createGroup() {
        lifecycleScope.launch {
            removeGroupIfNeed()

            //3.1 服务器端要主动创建群组，并等待客户端的连接
            //直接指定某台设备作为服务器端（群主），即直接指定某台设备用来接收文件
            wifiP2pManager.createGroup(wifiP2pChannel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    val log = "createGroup onSuccess"
                    log(log = log)
                    showToast(message = log)
                }

                override fun onFailure(reason: Int) {
                    val log = "createGroup onFailure: $reason"
                    log(log = log)
                    showToast(message = log)
                }
            })
        }
    }

    private fun removeGroup() {
        lifecycleScope.launch {
            removeGroupIfNeed()
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun removeGroupIfNeed() {
        return suspendCancellableCoroutine { continuation ->    //将回调函数转换为协程
            wifiP2pManager.requestGroupInfo(wifiP2pChannel) { group ->
                if (group == null) {
                    continuation.resume(value = Unit)   //处理回调
                } else {
                    wifiP2pManager.removeGroup(wifiP2pChannel, object : WifiP2pManager.ActionListener {
                        override fun onSuccess() {
                            val log = "removeGroup onSuccess"
                            log(log = log)
                            ClientName.text="未建立群组"
                            showToast(message = log)
                            continuation.resume(value = Unit)
                        }

                        override fun onFailure(reason: Int) {
                            val log = "removeGroup onFailure: $reason"
                            log(log = log)
                            showToast(message = log)
                            continuation.resume(value = Unit)
                        }
                    })
                }
            }
        }
    }

    private fun log(log: String) {
        tvLog.append(log)
        tvLog.append("\n\n")
    }

    private fun clearLog() {
        tvLog.text = ""
    }

}
