
package com.example.transbywifi

import android.os.Build
import androidx.annotation.RequiresApi
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 *AES加密解密
 */
object AESCrypt{

    //一个英文字节占8bit,AES需要128bit，也就是需要16位长度
    val password = "1234567887654321"//密钥

    //AES加密
    @RequiresApi(Build.VERSION_CODES.O)
    fun AESencrypt(input:String): String{

        //初始化cipher对象
        val cipher = Cipher.getInstance("AES")
        // 生成密钥
        val keySpec: SecretKeySpec? = SecretKeySpec(password.toByteArray(),"AES")
        cipher.init(Cipher.ENCRYPT_MODE,keySpec)
        //加密解密
        val encrypt = cipher.doFinal(input.toByteArray())
        val result = Base64.getEncoder().encode(encrypt)

        return String(result)
    }

    //AES解密
    @RequiresApi(Build.VERSION_CODES.O)
    fun AESdecrypt(input: String): String{

        //初始化cipher对象
        val cipher = Cipher.getInstance("AES")
        // 生成密钥
        val keySpec:SecretKeySpec? = SecretKeySpec(password.toByteArray(),"AES")
        cipher.init(Cipher.DECRYPT_MODE,keySpec)
        //加密解密
        val encrypt = cipher.doFinal(Base64.getDecoder().decode(input.toByteArray()))
        //AES解密不需要用Base64解码
        val result = String(encrypt)

        return result
    }

}

//@RequiresApi(Build.VERSION_CODES.O)
//fun main(args: Array<String>) {
//
//
//    val input = "AES加密解密测试"
//
//    val encrypt = AESCrypt.encrypt(input)
//    val decrypt = AESCrypt.decrypt(encrypt)
//
//    println("AES加密结果:"+encrypt)
//    println("AES解密结果:"+decrypt)
//
//}
    