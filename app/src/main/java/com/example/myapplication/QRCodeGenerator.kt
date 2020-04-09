package com.example.myapplication

import android.content.Context
import android.graphics.Bitmap
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.google.gson.Gson
import com.example.myapplication.Networking.NetworkInformation

// Thanks to https://www.callicoder.com/generate-qr-code-in-java-using-zxing/
// https://stackoverflow.com/questions/28232116/android-using-zxing-generate-qr-code/30529128
// https://demonuts.com/kotlin-generate-qr-code/
class QRCodeGenerator {
    companion object QRFactory {
        val gson = Gson()
        private fun makeQRCodeFromText(text: String, width: Int, height: Int, context: Context): Bitmap {
            val bitmatrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, width, height)
            val pixels = IntArray(width * height)
            for (y in 0 until bitmatrix.height){
                val offset = y * bitmatrix.width
                for (x in 0 until bitmatrix.width){
                    pixels[offset + x] = if (bitmatrix.get(x, y))
                        ContextCompat.getColor(context, R.color.black)
                    else
                        ContextCompat.getColor(context, R.color.white)
                }
            }
            return Bitmap.createBitmap(bitmatrix.width, bitmatrix.height, Bitmap.Config.ARGB_8888).also {
                it.setPixels(pixels, 0, 500, 0, 0, height, width)
            }
        }
        fun generateInitialConnectionQRCode(width: Int, height: Int, context: Context): Bitmap{
            val networkInfo = NetworkInformation.getNetworkInfo(context, debugUse = true)
            val jsonTree = gson.toJsonTree(networkInfo).also{
                it.asJsonObject.addProperty("type", "network_info")
            }
            val connectionInfo = gson.toJson(jsonTree)
            return makeQRCodeFromText(text = connectionInfo, width=500, height = 500, context = context)
        }
    }
}

/*
10.0.2.1 -> Router/gateway address
10.0.2.2 -> Host loopback address

10.0.2.15 -> Network/Ethernet Interface ->

Listen on interface. 10.0.2.15
On the B console, set up a redirection from A:localhost:<localPort> to B:10.0.2.15:<serverPort>
10.0.2.2:5000 -> 10.0.2.2:5001

127.0.0.1:5000 will go to B at 10.0.2.2:5001

10.0.2.2:5000 requests will go to 10.0.2.2:5001

So we need the port to be 10.0.2.2:5000 to be given.
 */