package com.example.transbywifi

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.transbywifi.FileDesUtil.decrypt
import com.example.transbywifi.FileDesUtil.encrypt
import java.io.*
import java.nio.file.Paths
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

object FileDesUtil {
    //秘钥算法
    private const val KEY_ALGORITHM = "DES"

    //加密算法：algorithm/mode/padding 算法/工作模式/填充模式
//    private const val CIPHER_ALGORITHM = "DES/ECB/PCS5Padding"
    private const val CIPHER_ALGORITHM = "DES"
    private val KEY = byteArrayOf(56, 57, 58, 59, 60, 61, 62, 63) //DES 秘钥长度必须是8 位或以上

    /**
     * 文件进行加密并保存加密后的文件到指定目录
     *
     * @param fromFilePath 要加密的文件 如c:/test/待加密文件.txt
     * @param toFilePath   加密后存放的文件 如c:/加密后文件.txt
     */
    fun encrypt(fromFilePath: String, toFilePath: String) {
        Logger.log("encrypting")
        val fromFile = File(fromFilePath)
        if (!fromFile.exists()) {
            Logger.log("to be encrypt file no exist!")
            return
        }
        val toFile: File = getFile(toFilePath)
        val secretKey: SecretKey = SecretKeySpec(KEY, KEY_ALGORITHM)
        var ins: InputStream? = null
        var out: OutputStream? = null
        var cis: CipherInputStream? = null
        try {
            val cipher: Cipher = Cipher.getInstance(CIPHER_ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            ins = FileInputStream(fromFile)
            out = FileOutputStream(toFile)
            cis = CipherInputStream(ins, cipher)
            val buffer = ByteArray(1024)
            var r: Int
            while (cis.read(buffer).also { r = it } > 0) {
                out.write(buffer, 0, r)
            }
        } catch (e: Exception) {
            Logger.log(e.toString())
            println(e.toString())
        } finally {
            try {
                cis?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            try {
                ins?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            try {
                out?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        Logger.log("encrypt completed")
        println("encrypt completed")
    }

    private fun getFile(filePath: String): File {
        val fromFile = File(filePath)
        if (!fromFile.parentFile?.exists()!!) {
            fromFile.parentFile?.mkdirs()
        }
        return fromFile
    }

    /**
     * 文件进行解密并保存解密后的文件到指定目录
     *
     * @param fromFilePath 已加密的文件 如c:/加密后文件.txt
     * @param toFilePath   解密后存放的文件 如c:/ test/解密后文件.txt
     */
    fun decrypt(fromFilePath: String, toFilePath: String) {
        Logger.log("decrypting...")
        val fromFile = File(fromFilePath)
        if (!fromFile.exists()) {
            Logger.log("to be decrypt file no exist!")
            return
        }
        val toFile: File = getFile(toFilePath)
        val secretKey: SecretKey = SecretKeySpec(KEY, KEY_ALGORITHM)
        var ins: InputStream? = null
        var out: OutputStream? = null
        var cos: CipherOutputStream? = null
        try {
            val cipher: Cipher = Cipher.getInstance(CIPHER_ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, secretKey)
            ins = FileInputStream(fromFile)
            out = FileOutputStream(toFile)
            cos = CipherOutputStream(out, cipher)
            val buffer = ByteArray(1024)
            var r: Int
            while (ins.read(buffer).also { r = it } >= 0) {
                cos.write(buffer, 0, r)
            }
        } catch (e: Exception) {
            Logger.log(e.toString())
        } finally {
            try {
                cos?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            try {
                out?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            try {
                ins?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        Logger.log("decrypt completed")
    }
}

