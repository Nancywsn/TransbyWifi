package com.example.transbywifi

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager

/**
广播接收器
 */

//2.5
// Wifi P2P 的接口高度异步化，到现在已经用到了三个系统的回调函数，
// 一个用于 WifiP2pManager 的初始化，两个用于在广播中异步请求数据，
// 为了简化操作，此处统一使用一个自定义的回调函数，方法含义与系统的回调函数一致
interface DirectActionListener : WifiP2pManager.ChannelListener {   //发送和接收共用一套广播机制，回调函数分别在2个Activity被重写

    fun wifiP2pEnabled(enabled: Boolean)

    fun onConnectionInfoAvailable(wifiP2pInfo: WifiP2pInfo)
    fun onSelfDeviceAvailable(wifiP2pDevice: WifiP2pDevice)
    fun onDisconnection()
    fun onPeersAvailable(wifiP2pDeviceList: Collection<WifiP2pDevice>)

}

class DirectBroadcastReceiver(
    private val wifiP2pManager: WifiP2pManager,
    private val wifiP2pChannel: WifiP2pManager.Channel,
    private val directActionListener: DirectActionListener
) : BroadcastReceiver() {

    companion object {
        //创建 Intent 过滤器，然后注册广播，添加与广播接收器检查内容相同的 Intent
        fun getIntentFilter(): IntentFilter {
            val intentFilter = IntentFilter()
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
            return intentFilter
        }
    }

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {

            //2.1判断当前 Wifi P2P是否开启并受支持
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                val enabled = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)==
                        WifiP2pManager.WIFI_P2P_STATE_ENABLED
                directActionListener.wifiP2pEnabled(enabled)
                if (!enabled) { //当前状态未开启
                    directActionListener.onPeersAvailable(emptyList())
                }
                Logger.log("WIFI_P2P_STATE_CHANGED_ACTION： $enabled")
            }

            //2.2设备周围的可用设备列表发生了变化，
            // 可以通过 requestPeers 方法得到可用的设备列表，之后就可以选择当中的某一个设备进行连接操作
            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                Logger.log("WIFI_P2P_PEERS_CHANGED_ACTION")

                //4.3 获取列表
                wifiP2pManager.requestPeers(wifiP2pChannel) { peers ->
                    //4.4 刷新列表
                    directActionListener.onPeersAvailable(peers.deviceList)
                }
            }

            //2.3Wifi P2P 的连接状态发生了变化，可能是连接到了某设备，或者是与某设备断开了连接
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                val networkInfo =
                    intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
                Logger.log("WIFI_P2P_CONNECTION_CHANGED_ACTION ： " + networkInfo?.isConnected)
                if (networkInfo != null && networkInfo.isConnected) {   //NetworkInfo 的isConnected()可以判断时连接还是断开时接收到的广播
                    //4.6
                    wifiP2pManager.requestConnectionInfo(wifiP2pChannel) { info ->
                        //如果是与某设备连接上了，则可以通过 requestConnectionInfo 方法获取到连接信息 WifiP2pInfo
                        if (info != null) {
                            directActionListener.onConnectionInfoAvailable(info)
                        }
                    }
                    Logger.log("已连接 P2P 设备")
                } else {
                    directActionListener.onDisconnection()
                    Logger.log("与 P2P 设备已断开连接")
                }
            }

            //2.4获取到本设备变化后的设备信息
            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                val wifiP2pDevice =
                    intent.getParcelableExtra<WifiP2pDevice>(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)
                if (wifiP2pDevice != null) {
                    directActionListener.onSelfDeviceAvailable(wifiP2pDevice)
                }
                Logger.log("WIFI_P2P_THIS_DEVICE_CHANGED_ACTION ： ${wifiP2pDevice.toString()}")
            }
        }
    }

}