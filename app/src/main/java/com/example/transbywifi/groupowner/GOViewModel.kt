package com.example.transbywifi.groupowner

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.net.toFile
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.transbywifi.Constants
import com.example.transbywifi.FileDesUtil
import com.example.transbywifi.FileDesUtil.decrypt
import com.example.transbywifi.models.FileTransfer
import com.example.transbywifi.models.ViewState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.random.Random


class FileReceiverViewModel(context: Application) :
    AndroidViewModel(context) {

    private val _viewState = MutableSharedFlow<ViewState>()

    val viewState: SharedFlow<ViewState> = _viewState

    private val _log = MutableSharedFlow<String>()

    val log: SharedFlow<String> = _log

    private var job: Job? = null

    fun receiveip(devicenamelist: MutableSet<String>,iplist: MutableSet<String>) {

        if (job != null) {
            return
        }
        job = viewModelScope.launch(context = Dispatchers.IO) {

            var serverSocket: ServerSocket? = null
            var clientInputStream: InputStream? = null
            try {

                log(log = "开启 Socket")

                serverSocket = ServerSocket()   //创建服务端socket
                serverSocket.bind(InetSocketAddress(Constants.PORT))    //绑定端口号
                serverSocket.reuseAddress = true
                serverSocket.soTimeout = 60000  //超时

                log(log = "socket accept，60秒内如果未成功则断开链接")

                val client = serverSocket.accept()  //监听客户端请求
                clientInputStream = client.getInputStream()// 建立好连接后，从socket中获取输入流，并建立缓冲区进行读取

                log(log = "开始传输ip")

                val bytes = ByteArray(1024)
                val sb = StringBuilder()
                var len: Int

                //只有当客户端关闭它的输出流的时候，服务端才能取得结尾的-1
                while (clientInputStream.read(bytes).also { len = it } != -1) {
                    // 注意指定编码格式，发送方和接收方一定要统一，建议使用UTF-8
                    sb.append(String(bytes, 0, len, charset("UTF-8")))
                }
                log("接收到ip地址: $sb")

                var index=sb.indexOf("0a")
                devicenamelist.add(sb.substring(0,index))
                iplist.add(sb.substring(index+2))

                log("组员信息: $devicenamelist")
                log("ip信息: $iplist")

                val outputStream = client.getOutputStream()
                outputStream.write("Hello Client,I get the message.".toByteArray(charset("UTF-8")))
                outputStream.close()

                client.close()

            } catch (e: Throwable) {
                log(log = "异常: " + e.message)
                _viewState.emit(value = ViewState.Failed(throwable = e))
            } finally {

                serverSocket?.close()
                clientInputStream?.close()

            }
        }
        job?.invokeOnCompletion {
            job = null
        }
//        return sb.toString()
    }
    fun send(iplist: MutableSet<String>, fileUri: Uri) {

                if (job != null) {
                    return
                }

                //viewModelScope是coroutineScope函数，将外部协程挂起，只有当它作用域内的所有代码和子协程都执行完毕之后，
                // coroutineScope函数之后的代码才能得到运行。coroutineScope函数只会阻塞当前协程，既不影响其他协程，也不影响任何线程
                // 因此不会造成任何性能上的问题
                job = viewModelScope.launch {
                    _log.emit(value = "iplist: $iplist")
                    //不管是GlobalScope.launch函数还是launch函数，它们都会返回
                    //一个Job对象，只需要调用Job对象的cancel()方法就可以取消协程
                    withContext(context = Dispatchers.IO) {//withContext特殊的作用域构建器，挂起函数，async函数的一种简化版写法
                        //强制要求的线程参数Dispatchers.IO表示会使用一种较高并发的线程策略
                        _viewState.emit(value = ViewState.Idle)

                        for(ipAddress in iplist){
                            if (ipAddress.isNotBlank()) {   //如果ipaddress不为空，则开始发送文件

                                _log.emit(value = "withContext iplist: $iplist")
                                var socket: Socket? = null
                                var outputStream: OutputStream? = null
                                var objectOutputStream: ObjectOutputStream? = null
                                var fileInputStream: FileInputStream? = null

                                try {   //try catch 语句来捕获异常并处理
                                    val cacheFile = //构建File对象指定缓存内文件路径，fileUri是内存中选定的文件路径
                                        saveFileToCacheDir(
                                            context = getApplication(),
                                            fileUri = fileUri
                                        )
                                    val tofile = File(
                                        getDiskCacheDir(context = getApplication()),
                                        cacheFile.name
                                    )

                                    code(cacheFile, tofile)

                                    val fileTransfer =
                                        FileTransfer(fileName = cacheFile.name)  //一种文件信息模型

                                    _viewState.emit(value = ViewState.Connecting)
                                    _log.emit(value = "接收端的ip地址: $ipAddress")
                                    _log.emit(value = "待发送的文件: $fileTransfer")
                                    _log.emit(value = "DES编码后的文件位置: $tofile")
                                    _log.emit(value = "开启 Socket")

                                    socket = Socket()   //创建
                                    socket.bind(null)   //初始化绑定

                                    _log.emit(value = "socket connect，如果三十秒内未连接成功则放弃")

                                    socket.connect(
                                        InetSocketAddress(ipAddress, Constants.PORT),
                                        60000
                                    ) //ipAddress目的接收地址，超时时间设为30秒

                                    _viewState.emit(value = ViewState.Receiving)
                                    _log.emit(value = "连接成功，开始传输文件")

                                    outputStream = socket.getOutputStream() //socket输出流
                                    //ObjectOutputStream把对象转成字节数据的输出到文件中保存，对象的输出过程称为序列化，可实现对象的持久存储。
                                    objectOutputStream = ObjectOutputStream(outputStream)
                                    objectOutputStream.writeObject(fileTransfer)    //  writeObject 方法用于将对象写入流中

                                    fileInputStream = FileInputStream(tofile)    //读取硬盘上的文件，应该使用输入流
                                    val buffer = ByteArray(1024 * 100)  //指定多少字节的数组，存储文件内容
                                    var length: Int
                                    while (true) {  //按指定大小分块输出文件内容
                                        length =
                                            fileInputStream.read(buffer)   //该read方法返回的是往数组中存了多少字节，内存到缓存
                                        if (length > 0) {
                                            outputStream.write(buffer, 0, length)   //缓存到socket
                                        } else {
                                            break
                                        }
                                        _log.emit(value = "正在传输文件，length : $length")
                                    }
                                    _log.emit(value = "文件发送成功")
                                    _viewState.emit(value = ViewState.Success(file = cacheFile))
                                } catch (e: Throwable) {
                                    e.printStackTrace() //指出异常的类型、性质、栈层次及出现在程序中的位置
                                    _log.emit(value = "异常: " + e.message)   //getMessage() 方法：输出错误的性质。
                                    _viewState.emit(value = ViewState.Failed(throwable = e))
                                } finally { //无论是否发生异常（除特殊情况外），finally 语句块中的代码都会被执行，一般用于清理资源
                                    fileInputStream?.close()    //关闭流
                                    outputStream?.close()
                                    objectOutputStream?.close()
                                    socket?.close()
                                }
                            }
                        }
                    }
                }
                job?.invokeOnCompletion {   //用于监听其完成或者其取消状态
                    job = null
                }

    }
            //3.2
            //使用 IntentService 在后台监听客户端的 Socket 连接请求，并通过输入输出流来传输文件。
            //此处的代码比较简单，就只是在指定端口一直堵塞监听客户端的连接请求，获取待传输的文件信息模型 FileTransfer ，之后就进行实际的数据传输
            @SuppressLint("RestrictedApi")
            fun startListener() {
                if (job != null) {
                    return
                }
                job = viewModelScope.launch(context = Dispatchers.IO) {
                    _viewState.emit(value = ViewState.Idle)

                    var serverSocket: ServerSocket? = null
                    var clientInputStream: InputStream? = null
                    var objectInputStream: ObjectInputStream? = null
                    var fileOutputStream: FileOutputStream? = null
                    try {

                        _viewState.emit(value = ViewState.Connecting)
                        log(log = "开启 Socket")

                        serverSocket = ServerSocket()   //创建服务端socket
                        serverSocket.bind(InetSocketAddress(Constants.PORT))    //绑定端口号
                        serverSocket.reuseAddress = true
                        serverSocket.soTimeout = 60000  //超时

                        log(log = "socket accept，60秒内如果未成功则断开链接")

                        val client = serverSocket.accept()  //监听客户端请求

                        _viewState.emit(value = ViewState.Receiving)

                        clientInputStream = client.getInputStream()
                        objectInputStream = ObjectInputStream(clientInputStream)

                        val fileTransfer = objectInputStream.readObject() as FileTransfer

                        val filename=fileTransfer.fileName

                        val tofile = File(getDiskCacheDir(context = getApplication()),filename)
                        val file = File(getCacheDir(context = getApplication()),filename) //创建缓存中的file对象

                        log(log = "连接成功，待接收的文件: $fileTransfer")
                        log(log = "解码后的文件将保存到: $tofile")
                        log(log = "开始传输文件")

                        fileOutputStream = FileOutputStream(file)   //FileOutputStream流用来写入数据到File对象表示的文件
                        val buffer = ByteArray(1024 * 100)
                        while (true) {
                            val length = clientInputStream.read(buffer) //socket到缓存
                            if (length > 0) {
                                fileOutputStream.write(buffer, 0, length)
                            } else {
                                break
                            }
                            log(log = "正在传输文件，length : $length")
                        }

                        decode(fromfile = file, tofile = tofile)

                        _viewState.emit(value = ViewState.Success(file = tofile))
                        log(log = "文件接收成功")


                        log(log = "文件解码成功")

                    } catch (e: Throwable) {
                        log(log = "异常: " + e.message)
                        _viewState.emit(value = ViewState.Failed(throwable = e))
                    } finally {
                        serverSocket?.close()
                        clientInputStream?.close()
                        objectInputStream?.close()
                        fileOutputStream?.close()
                    }
                }
                job?.invokeOnCompletion {
                    job = null
                }

            }

            private suspend fun saveFileToCacheDir(context: Context, fileUri: Uri): File {  //返回一个file对象
                return withContext(context = Dispatchers.IO) {
                    val documentFile = DocumentFile.fromSingleUri(context, fileUri) //创建DocumentFile代表在给定的单一文件Uri
                        ?: throw NullPointerException("fileName for given input Uri is null")
                    val fileName = documentFile.name
                    val outputFile = File(  //创建file对象，Random.nextInt生成1到200内的随机数
                        context.cacheDir, Random.nextInt(1, 200).toString() + "_" + fileName)
                    if (outputFile.exists()) {
                        outputFile.delete()
                    }
                    outputFile.createNewFile()  //创建缓存文件
                    val outputFileUri = Uri.fromFile(outputFile)//fromFile(outputFile)表示ducumentfile
                    copyFile(context, fileUri, outputFileUri)   //将本地文件复制到缓存
                    return@withContext outputFile
                }
            }

            private suspend fun copyFile(context: Context, inputUri: Uri, outputUri: Uri) {
                //withContext函数通过Dispatchers切换到指定的线程，并在闭包内的逻辑执行结束之后，自动把线程切回去继续执行，返回最后一行的值
                withContext(context = Dispatchers.IO) {

                    val inputStream = context.contentResolver.openInputStream(inputUri)
                        ?: throw NullPointerException("InputStream for given input Uri is null")
                    val outputStream = FileOutputStream(outputUri.toFile())
                    val buffer = ByteArray(1024)
                    var length: Int
                    while (true) {
                        length = inputStream.read(buffer)
                        if (length > 0) {
                            outputStream.write(buffer, 0, length)
                        } else {
                            break
                        }
                    }
                    inputStream.close()
                    outputStream.close()
                }
            }

            private fun code(fromfile:File,tofile:File){
                val fpath=fromfile.path
                val topath=tofile.path
                FileDesUtil.encrypt(fpath, topath)
            }

            private fun getCacheDir(context: Context): File {
                val cacheDir = File(context.cacheDir, "FileTransfer")   //创建面向缓存的file对象
                cacheDir.mkdirs()   //创建文件
                return cacheDir
            }

            private fun decode(fromfile:File,tofile:File){
                val fpath=fromfile.path
                val topath=tofile.path
                decrypt(fpath,topath)
            }

            private suspend fun log(log: String) {
                _log.emit(value = log)
            }

            private fun getDiskCacheDir(context: Context): String? {//获取缓存路径
                val cachePath: String? = if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState() || !Environment.isExternalStorageRemovable()) {
                        context.externalCacheDir!!.path
                    } else {
                        context.cacheDir.path
                    }
                return cachePath
            }

    }
