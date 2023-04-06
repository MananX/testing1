package com.contlo.androidsdk.push

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.contlo.androidsdk.ContloSDK
import com.contlo.androidsdk.api.HttpClient
import com.contlo.androidsdk.api.TrackAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class PushClicked : Service() {

    private var apiKey: String? = null

    private var internalID: String? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        internalID = intent?.getStringExtra("internal_id")

        val x = TrackAPI()
        internalID?.let { x.sendPushCallbacks(this,"clicked", it) }

        val sharedPreferences = getSharedPreferences("MyPrefs",Context.MODE_PRIVATE)

        apiKey = sharedPreferences.getString("API_KEY",null)

        // Make the API call here
        val url = "https://staging2.contlo.in/v1/event/mobile_push_click"

        val headers = HashMap<String, String>()
        headers["accept"] = "application/json"
        headers["X-API-KEY"] = "$apiKey"
        headers["content-type"] = "application/json"

        val params = JSONObject()
        params.put("internal_id",internalID)
        Log.d("pushclick",params.toString())
        Toast.makeText(this, "Params: $params", Toast.LENGTH_SHORT).show()


        CoroutineScope(Dispatchers.IO).launch {

            val httpPostRequest = HttpClient()
            val response = httpPostRequest.sendPOSTRequest(url, headers, params)

            println(" Push Clicked Response: $response")

        }

        // Stop the service
        stopSelf()

        return START_NOT_STICKY
    }


    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }





}