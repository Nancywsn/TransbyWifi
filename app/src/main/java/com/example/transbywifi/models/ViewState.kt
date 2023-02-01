package com.example.transbywifi.models

import java.io.File

//用密封类表示socket发送的消息类型
sealed class ViewState {

    object Idle : ViewState()

    object Connecting : ViewState()

    object Receiving : ViewState()

    class Success(val file: File) : ViewState()

    class Failed(val throwable: Throwable) : ViewState()

}