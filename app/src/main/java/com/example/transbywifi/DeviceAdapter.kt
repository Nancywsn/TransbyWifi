package com.example.transbywifi

import android.net.wifi.p2p.WifiP2pDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.transbywifi.utils.WifiP2pUtils
import kotlinx.android.synthetic.main.item_device.view.*

//自定义控件的适配器

//思路1：设置复选框；定义复选响应函数，增加复选确定按钮，改变原来点击反应函数为确定按钮反应的方法，弹出确认框显示选中的设备名，一对一连接发送断开再连接
interface OnItemClickListener {

    fun onItemClick(position: Int)


}

class DeviceAdapter(private val wifiP2pDeviceList: List<WifiP2pDevice>) :
    RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {

    var onItemClickListener: OnItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = wifiP2pDeviceList[position]
        holder.tvDeviceName.text = device.deviceName
//        holder.tvDeviceAddress.text = device.deviceAddress
        holder.tvDeviceDetails.text = WifiP2pUtils.getDeviceStatus(deviceStatus = device.status)
        holder.itemView.setOnClickListener {
            onItemClickListener?.onItemClick(position = position)
        }
    }

    override fun getItemCount(): Int {
        return wifiP2pDeviceList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDeviceName: TextView
//        val tvDeviceAddress: TextView
        val tvDeviceDetails: TextView
        init {
            tvDeviceName = itemView.tvDeviceName
//            tvDeviceAddress = itemView.tvDeviceAddress
            tvDeviceDetails = itemView.tvDeviceDetails
        }
    }
}