package com.example.transbywifi

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.wifi.WifiManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity


//ddd
open class BaseActivity : AppCompatActivity() {

    private var loadingDialog: ProgressDialog? = null   //"?"加在变量名后，系统在任何情况不会报它的空指针异常

    //显示进度条
    protected fun showLoadingDialog(message: String = "", cancelable: Boolean = true) {
        loadingDialog?.dismiss()   //安全调用符，取消进度条对话框
        loadingDialog = ProgressDialog(this).apply {
            setMessage(message)     // 设置消息内容
            setCancelable(cancelable)   //设置是否可以通过点击Back键取消
            setCanceledOnTouchOutside(false)    // 设置在点击Dialog外是否取消Dialog进度条
            show()
        }
    }

    //取消进度条
    protected fun dismissLoadingDialog() {
        loadingDialog?.dismiss()
    }

    //定义父类方法，全局可用
    protected fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    //开启Activity
    protected fun <T : Activity> startActivity(clazz: Class<T>) {
        startActivity(Intent(this, clazz))
    }

    fun getIpAddress(): String? {
        //ipv6格式的地址
//        try {
//            val en: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
//            while (en.hasMoreElements()) {
//                val nwInterface: NetworkInterface = en.nextElement()
//                val enumIpAdder: Enumeration<InetAddress> = nwInterface.inetAddresses
//                while (enumIpAdder.hasMoreElements()) {
//                    val netAddress: InetAddress = enumIpAdder.nextElement()
//                    if (!netAddress.isLoopbackAddress  && !netAddress.isLinkLocalAddress) {
//                    return netAddress.hostAddress.toString()
//                    }
//                }
//            }
//        } catch (e: SocketException) {
//            e.printStackTrace()
//        }
//        return null

        //获取ipv4格式的地址
        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        val ipAddress = wifiInfo.ipAddress
        return (ipAddress and 0xff).toString() + "." +
                (ipAddress shr 8 and 0xff) + "." +
                (ipAddress shr 16 and 0xff) + "." +
                (ipAddress shr 24 and 0xff)
    }



}