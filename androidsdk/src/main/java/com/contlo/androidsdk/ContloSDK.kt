package com.contlo.androidsdk

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.util.Log
import android.widget.Toast
import com.contlo.androidsdk.api.HttpClient
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONObject
import java.util.*
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException



class ContloSDK {

    //Context
    lateinit var context: Context

    //Shared Preference
    private lateinit var sharedPreferences: SharedPreferences

    //Mandatory Attributes
    private  var FCM_TOKEN: String? = null
    private var API_KEY: String? = null
    private var AD_ID: String? = null
    private var PACKAGE_NAME: String? = null
    private var APP_NAME: String? = null
    private var APP_VERSION: String? = null
    private var OS_VERSION: String? = null
    private var MANUFACTURER: String? = null
    private var MODEL_NAME: String? = null
    private var API_LEVEL: String? = null
    private var ANDROID_SDK_VERSION: String? = null
    private var NETWORK_TYPE: String? = null

    //Main INIT Function
    fun init(context1: Context) {

        Log.d("Contlo-Init","Triggered")

        //context
        context = context1
        sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        //Get API KEY
        getAPIKey()

        //Generate and Register FCM
        generateAndRegisterFCM()

        //Check App Update
        var oldAppVersion: String? = null

        if(sharedPreferences.getString("APP_VERSION",null) != null) {

            oldAppVersion = sharedPreferences.getString("APP_VERSION", null)

        }

        //Retrieve Mandatory Attributes
        retrieveMandatoryParams()

        //Check app update and fire event but not on install
        if(APP_VERSION != oldAppVersion && (sharedPreferences.contains("New Install")))
        {
            Log.d("APP UPDATED", "true")
            //Add Track API Call
        }

        //
        val editor = sharedPreferences.edit()
        editor.putString("New Install", "1")
        editor.apply()


        //Store Mandatory Params in Shared Preference
        putParamstoSP()


    }


    private  fun getAPIKey(){

        try {
                val appInfo = context.packageManager.getApplicationInfo(
                    context.packageName, PackageManager.GET_META_DATA
                )
                val metaData = appInfo.metaData
                API_KEY = metaData?.getString("contlo_api_key")
            } catch (e: PackageManager.NameNotFoundException) {
                // Handle the exception
            }

        }
    private fun retrieveMandatoryParams(){

        PACKAGE_NAME = context.packageName //1
        val packageManager = context.packageManager
        val applicationInfo = context.applicationInfo
        APP_NAME = packageManager.getApplicationLabel(applicationInfo).toString() //2
        APP_VERSION = packageManager.getPackageInfo(PACKAGE_NAME.toString(), 0).versionName //3
        OS_VERSION = Build.VERSION.RELEASE //4
        API_LEVEL = Build.VERSION.SDK_INT.toString() //5
        MODEL_NAME = Build.MODEL //6
        MANUFACTURER = Build.MANUFACTURER //7
        ANDROID_SDK_VERSION = Build.VERSION.SDK //8
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        NETWORK_TYPE = //9
            if (networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                "WiFi"
            } else if (networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true) {
                "Mobile data"
            } else {
                "Unknown"
            }

    }

   private fun putParamstoSP(){

        val editor = sharedPreferences.edit()
        editor.putString("API_KEY", API_KEY)
        editor.putString("PACKAGE_NAME", PACKAGE_NAME)
        editor.putString("APP_NAME", APP_NAME)
        editor.putString("APP_VERSION", APP_VERSION)
        editor.putString("OS_VERSION", OS_VERSION)
        editor.putString("MANUFACTURER", MANUFACTURER)
        editor.putString("MODEL_NAME", MODEL_NAME)
        editor.putString("API_LEVEL", API_LEVEL)
        editor.putString("ANDROID_SDK_VERSION", ANDROID_SDK_VERSION)
        editor.putString("NETWORK_TYPE", NETWORK_TYPE)
        editor.apply()

    }

    private fun generateAndRegisterFCM(){

        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("TAG", "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            FCM_TOKEN = task.result
            println("Value of Token = $FCM_TOKEN")

            //Put FCM in params
            val params = JSONObject()
            params.put("fcm_token", FCM_TOKEN)

            //Store FCM in Shared Preference
            sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            val editor1 = sharedPreferences.edit()
            editor1.putString("FCM_TOKEN", FCM_TOKEN)
            editor1.apply()
            println(params.toString())

            //Make API Request
            val url = "https://staging2.contlo.in/v1/register_mobile_push"

            val headers = HashMap<String, String>()
            headers["accept"] = "application/json"
            headers["X-API-KEY"] = "$API_KEY"
            headers["content-type"] = "application/json"

            CoroutineScope(Dispatchers.IO).launch {

                    val httpPostRequest = HttpClient()
                    val response = httpPostRequest.sendPOSTRequest(url, headers, params)

                    val jsonObject = JSONObject(response)
                    var externalId: String? = null
                    if(jsonObject.has("external_id")){
                        externalId = jsonObject.getString("external_id")
                    }

                    val editor2 = sharedPreferences.edit()
                    editor2.putString("Contlo External ID",externalId)
                    editor2.apply()

                    println("Response FCM Registration: $response      External_Id: $externalId")


                }

        })

    }

    fun getAdID(context: Context){

        // Retrieve the advertising ID in a background thread
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val adInfo = AdvertisingIdClient.getAdvertisingIdInfo(context)
                val advertisingId = adInfo.id
                if (advertisingId != null) {
                    Log.d("Advertising ID, Init", advertisingId)

                    val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

                    val apiKey1 = sharedPreferences.getString("API_KEY",null)

                    val editor = sharedPreferences.edit()
                    editor.putString("AD_ID", advertisingId)
                    editor.apply()

                    FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                        if (!task.isSuccessful) {
                                    Log.w("TAG - getAdID - Contlo SDK", "Fetching FCM registration token failed", task.exception)
                            return@OnCompleteListener
                        }

                        // Get new FCM registration token
                        val token = task.result
                        println("Value of Token AD ID = $token")

                        val url = "https://staging2.contlo.in/v1/identify"

                        val headers = HashMap<String, String>()
                        headers["accept"] = "application/json"
                        headers["X-API-KEY"] = "$apiKey1"
                        headers["content-type"] = "application/json"

                        val params = JSONObject()
                        params.put("fcm_token", token)
                        params.put("ad_id",advertisingId)

                        CoroutineScope(Dispatchers.IO).launch {

                            val httpPostRequest = HttpClient()
                            val response = httpPostRequest.sendPOSTRequest(url, headers, params)

                            println("Response Send AD-ID: $response")
                                    Log.d("onNewToken", "AD-ID response - $response")

                        }

                    })


                }
            } catch (e: IOException) {
                // Error retrieving advertising ID
                e.printStackTrace()
            }
        }
    }

}