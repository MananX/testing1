package com.contlo.androidsdk.push

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.contlo.androidsdk.api.HttpClient
import com.contlo.androidsdk.api.TrackAPI
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL


class PushNotifications() : FirebaseMessagingService() {


    private var apiKey: String? = null

     var  messageReceived: String? = null


    @SuppressLint("LaunchActivityFromNotification")
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val context1 = this

        //Get Notification and payload
        val title = remoteMessage.data["title"]                         //Title
        val message = remoteMessage.data["body"]                        //Body
        val subtitle = remoteMessage.data["subtitle"]                   //Subtitle
        val imageUrl = remoteMessage.data["image"]                     //Large Image
        val deepLink = remoteMessage.data["primary_url"]                //Notification Deep Link
        val internalID = remoteMessage.data["internal_id"]              //Internal ID
        val ctatitle1 = remoteMessage.data["ctaTitle1"]                 //Button 1 Title
        val ctalink1 = remoteMessage.data["ctaLink1"]                   //Button 1 Link
        val ctatitle2 = remoteMessage.data["ctaTitle2"]                 //Button 2 Title
        val ctalink2 = remoteMessage.data["ctaLink2"]                   //Button 2 Link
        val largeIcon = remoteMessage.data["image"]                     //Large Icon


        val x = TrackAPI()
        if (internalID != null) {
            x.sendPushCallbacks(this,"received", internalID)
        }

        messageReceived = "true"
        Log.d("messageReceived", messageReceived!!)


        val sharedPreferences = this.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        apiKey = sharedPreferences.getString("API_KEY",null)


        Log.d("REMOTE", remoteMessage.notification.toString())
        Log.d("REMOTE", remoteMessage.data.toString())

        //Get the app's icon and set as small icon
        val appIcon = this.packageManager.getApplicationIcon(this.packageName)
        val appIconBitmap = (appIcon as BitmapDrawable).bitmap
        val appIconCompat = IconCompat.createWithBitmap(appIconBitmap)




        //Log Payload
        Log.d("PayloadTag", "Payload: \n ${title.toString()} \t ${message.toString()} \t ${imageUrl.toString()} \t  ${deepLink.toString()}  \t  ${internalID.toString()}  ")


        //Title and message are compulsory to create a notification
        if ((title != null) && ((message != null) || (imageUrl != null))) {

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            //FCM Channel
            val channelId = "contlo_channel_id"
            val channelName = "Contlo Channel"
            val description = "Contlo Channel Description"
            val importance = NotificationManager.IMPORTANCE_HIGH

            // Create a notification channel if Android Level > Oreo
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(channelId, channelName, importance)
                channel.description = description
                notificationManager.createNotificationChannel(channel)

                // Configure the notification channel with sound and vibration
                channel.enableVibration(true)
                channel.setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    Notification.AUDIO_ATTRIBUTES_DEFAULT
                )
            }

            val deleteIntent = Intent(this, NotificationDeleteReceiver::class.java)
            deleteIntent.putExtra("internal_id", internalID)
            deleteIntent.action = "com.contlo.androidsdk.DELETE_NOTIFICATION"
            val deletePendingIntent = PendingIntent.getBroadcast(this, 0, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)


            //Create the notification
            val notificationBuilder = NotificationCompat.Builder(this, channelId)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(appIconCompat)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setAutoCancel(true)
                .setDeleteIntent(deletePendingIntent)
                .setDefaults(Notification.DEFAULT_ALL)


            //Set Subtitle if not null
            if(subtitle != null)
            {
                notificationBuilder.setSubText(subtitle)
            }

            //CTA Button 1
            if(ctatitle1 != null && ctatitle1 != "")
            {
                val intent = Intent(Intent.ACTION_MAIN)
                intent.addCategory(Intent.CATEGORY_LAUNCHER)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                intent.setPackage(null) // This line sets the package name to null, which allows any app to handle the intent

                val pendingIntent = PendingIntent.getActivity(context1, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

                //CTA Button 1 with Deep Link
                if(ctalink1 != null && ctalink1 == ""){
                    val intent1 = Intent(Intent.ACTION_VIEW, Uri.parse(ctalink1))
                    intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    val pendingIntent1 = PendingIntent.getActivity(
                        this,
                        0,
                        intent1,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    notificationBuilder.addAction(0, ctatitle1, pendingIntent1)
                }

                else{
                    notificationBuilder.addAction(0, ctatitle1, pendingIntent )
                }


            }

            //CTA Button 2
            if(ctatitle2 != null && ctatitle2 != "")
            {
                val intent = Intent(Intent.ACTION_MAIN)
                intent.addCategory(Intent.CATEGORY_LAUNCHER)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                intent.setPackage(null) // This line sets the package name to null, which allows any app to handle the intent

                val pendingIntent = PendingIntent.getActivity(context1, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

                //CTA Button 2 with Deep Link
                if(ctalink2 != null && ctalink2 == ""){
                    val intent1 = Intent(Intent.ACTION_VIEW, Uri.parse(ctalink2))
                    intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    val pendingIntent1 = PendingIntent.getActivity(
                        this,
                        0,
                        intent1,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    notificationBuilder.addAction(0, ctatitle2, pendingIntent1)
                }

                else{
                    notificationBuilder.addAction(0, ctatitle2, pendingIntent )
                }
            }


            //Register Notification Click
            CoroutineScope(Dispatchers.IO).launch {

                    val clickIntent = Intent(context1, PushClicked::class.java)
                    clickIntent.putExtra("internal_id", internalID)
                    val pendingIntent = PendingIntent.getService(
                        context1,
                        0,
                        clickIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    notificationBuilder.setContentIntent(pendingIntent)

                }


            //Set Deep Link for the notification
            if (deepLink != null) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLink))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val pendingIntent1 = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                notificationBuilder.setContentIntent(pendingIntent1)
            }


            // Set the notification channel for Android Oreo and higher
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationBuilder.setChannelId(channelId)
            }



            if (imageUrl != null) {

                // Load large image
                CoroutineScope(Dispatchers.IO).launch {
                        val largeImage = loadImage(imageUrl)
                        if (largeImage != null) {
                            notificationBuilder.setStyle(
                                NotificationCompat.BigPictureStyle()
                                    .bigPicture(largeImage)
                                    .bigLargeIcon(null)
                            )
                        }
                        notificationManager.notify(0, notificationBuilder.build())
                    }


                // Load large Icon
                CoroutineScope(Dispatchers.IO).launch {

                    val largeImage = loadImage(imageUrl)
                    if (largeImage != null) {
                        notificationBuilder.setLargeIcon(largeImage)
                    }
                    notificationManager.notify(0, notificationBuilder.build())

                }

            } else {
                notificationManager.notify(0, notificationBuilder.build())
            }


//            //Load Large Icon
//            if (imageUrl != null) {
//
//                    CoroutineScope(Dispatchers.IO).launch {
//
//                        val largeImage = loadImage(imageUrl)
//                        if (largeImage != null) {
//                            notificationBuilder.setLargeIcon(largeImage)
//                        }
//                        notificationManager.notify(0, notificationBuilder.build())
//
//                    }
//
//            } else {
//                notificationManager.notify(0, notificationBuilder.build())
//            }

        }
    }



    //Helper Function to load Image in Notification
    private fun loadImage(imageUrl: String?): Bitmap? {
        try {
            val connection = URL(imageUrl).openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val input = connection.inputStream
            return BitmapFactory.decodeStream(input)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }


    override fun onNewToken(token: String) {
        super.onNewToken(token)

        Log.d("onNewToken", "Triggered")

        val sharedPreferences = this.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        apiKey = sharedPreferences.getString("API_KEY",null)


        val params = JSONObject()
        params.put("fcm_token", token)

        val editor = sharedPreferences.edit()
        editor.putString("FCM_TOKEN", token)
        editor.apply()

        println(params.toString())

        val url = "https://staging2.contlo.in/v1/register_mobile_push"

        val headers = HashMap<String, String>()
        headers["accept"] = "application/json"
        headers["X-API-KEY"] = "$apiKey"
        headers["content-type"] = "application/json"


        CoroutineScope(Dispatchers.IO).launch {

            val httpPostRequest = HttpClient()
            val response = httpPostRequest.sendPOSTRequest(url, headers, params)

            println(" ORegister FCM $response")
            Log.d("onNewToken", "Registered FCM - $response")

        }

        val sharedPreferences1 = this.getSharedPreferences("MyPrefs1", Context.MODE_PRIVATE)

        //Send App Installed Event
        if (!sharedPreferences1.contains("APP_INSTALLED_NEW")) {
            val handlerThread = HandlerThread("AppInstallHandlerThread")
            handlerThread.start()
            val handler = Handler(handlerThread.looper)
            handler.postDelayed({
                sendAppInstallEvent()
            }, 3000)
        }

        //Put Flag after App Install
        val editor1 = sharedPreferences1.edit()
        editor1.putString("APP_INSTALLED_NEW", "1")
        editor1.apply()



    }



    fun sendAppInstallEvent(){

        val sharedPreferences = this.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        apiKey = sharedPreferences.getString("API_KEY",null)


        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("TAG  - send App Install", "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            println("Value of Token App Install = $token")


            val url = "https://staging2.contlo.in/v1/track"

            val headers = java.util.HashMap<String, String>()
            headers["accept"] = "application/json"
            headers["X-API-KEY"] = "$apiKey"
            headers["content-type"] = "application/json"

            val params = JSONObject()
            params.put("fcm_token", token)

            val propString = "{\"version\":\"1.0.0\",\"platform\":\"android\",\"source\":\"-\"}"
            val prop = JSONObject(propString)

            params.put("event","mobile_app_installed")
            params.put("properties",prop)


            CoroutineScope(Dispatchers.IO).launch {

                val httpPostRequest = HttpClient()
                val response = httpPostRequest.sendPOSTRequest(url, headers, params)

                println("APP Install Event: $response")
                Log.d("onNewToken", "Triggered App Install - $response ")

            }


        })


    }






}
