package uk.ac.ed.inf.coinz

import android.annotation.SuppressLint
import android.content.Intent
import android.location.Location
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import com.google.gson.JsonObject
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineListener
import com.mapbox.android.core.location.LocationEnginePriority
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.*
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.annotations.IconFactory
import kotlinx.android.synthetic.main.activity_map.*
import org.joda.time.DateTime

import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.time.LocalDate
import kotlin.collections.HashMap
import kotlin.concurrent.schedule

class MapActivity : AppCompatActivity(), PermissionsListener, LocationEngineListener {

    private val tag = "MapActivity"

    private lateinit var mapView: MapView
    private lateinit var map: MapboxMap
    private lateinit var permissionManager: PermissionsManager
    private lateinit var originLocation: Location

    // Downloading and saving GeoJson file
    private var mapUrlString: String = "http://homepages.inf.ed.ac.uk/stg/coinz/"
    private val fileName: String = "coinzmap.geojson"
    private lateinit var geoJsonCoinsString: String

    private var locationEngine: LocationEngine? = null
    private var locationLayerPlugin: LocationLayerPlugin? = null

    // Firebase:
    // User Authentication
    private lateinit var mAuth: FirebaseAuth
    private var email: String? = null
    // Database (Firestore)
    private lateinit var db: FirebaseFirestore
    // User
    private var currentUser: FirebaseUser?= null
    private lateinit var  userDB: DocumentReference

    // Required proximity of marker
    private var requiredMarkerDistance = 25.0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        // Initialising User Authentication
        mAuth = FirebaseAuth.getInstance()
        // Initialising User
        currentUser = mAuth.currentUser
        // Initialising User Database
        db = FirebaseFirestore.getInstance()
        email = currentUser?.email
        if (email != null){
            userDB = db.collection("users").document(email!!)
        }
        if (currentUser != null){
            /* Delete coins in wallet from yesterday and
            set task to delete coins from wallet if user plays overnight
            and recursively set task for next day:*/
            deleteOldCoinsInWallet()

        }else{
            updateUIIfNoUserLoggedIn()
        }




        // Logout button:
        logout_button.setOnClickListener {it ->
            mAuth.signOut()
            //it.visibility = View.GONE
            switchToLoginForm()
        }


        // Downloading coins and setting up map with coins:
        mapUrlString = createTodaysLink()
        geoJsonCoinsString = DownloadFileTask(DownloadCompleteRunner).execute(mapUrlString).get()

        Mapbox.getInstance(applicationContext, getString(R.string.access_token))
        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(
            object: OnMapReadyCallback {

            @SuppressLint("MissingPermission")
            override fun onMapReady(mapboxMap: MapboxMap) {

                map = mapboxMap

                // User interface options
                map.uiSettings.isCompassEnabled = true
                map.uiSettings.isZoomControlsEnabled = true

                // Make location info available:
                enableLocation()

                val collectedCoinsRef = userDB.collection("wallet").document("todaysCollectedCoins")
                collectedCoinsRef.get().addOnCompleteListener {
                    val mapOfCollectedCoins = it.result?.data as HashMap<String, HashMap<String, Any>>
                    val collectedIds= mapOfCollectedCoins.keys
                    val featureCollection: FeatureCollection = FeatureCollection.fromJson(geoJsonCoinsString)
                    val featureList: List<Feature>? = featureCollection.features()
                    if (featureList != null){
                        for (feature: Feature in featureList){

                            val icon = IconFactory.getInstance(this@MapActivity)
                            val icon1 = icon.fromResource(R.drawable.coin)

                            val point: Point = feature.geometry() as Point
                            val properties: JsonObject? = feature.properties()
                            val coinID =properties?.get("id")?.asString
                            if (coinID !in collectedIds) {
                                mapboxMap.addMarker(MarkerOptions()
                                        .position(LatLng(point.latitude(), point.longitude()))
                                        .icon(icon1)
                                        .title(coinID)
                                )
                            }
                        }
                    }else{
                        // If coins are not available, display a message to the user.
                        Toast.makeText(this@MapActivity, "Coins not available.",
                                Toast.LENGTH_SHORT).show()
                    }



                    map.setOnMarkerClickListener { marker ->
                        if (locationEngine != null){
                            // calculating distance to marker
                            val lastLocation= locationEngine!!.lastLocation
                            val lastLocationLatLng: LatLng = LatLng(lastLocation.latitude,lastLocation.longitude)
                            val markerLatLng = marker.position
                            val distanceToMarker = lastLocationLatLng.distanceTo(markerLatLng)
                            // Show a toast with the distance to the selected marker
                            //Toast.makeText(this@MapActivity,distanceToMarker.toString() , Toast.LENGTH_LONG).show()

                            if (distanceToMarker < requiredMarkerDistance){
                                val id = marker.title
                                //Toast.makeText(this@MapActivity,id , Toast.LENGTH_LONG).show()
                                if (featureList != null) {
                                    for (feature: Feature in featureList) {
                                        if (feature.properties()?.get("id")?.asString ==  id){
                                            marker.remove()
                                            val coinValue = feature.properties()!!.get("value").asDouble
                                            val coinCurrency= feature.properties()!!.get("currency").asString
                                            Toast.makeText(this@MapActivity,"Distance: %.0fm\nAdded to wallet.".format(distanceToMarker) , Toast.LENGTH_LONG).show()
                                            val coin = HashMap<String,Any>()
                                            coin[coinCurrency] = coinValue
                                            val date = Date()
                                            val dateTimestamp = Timestamp(date)
                                            coin["date"] = dateTimestamp
                                            updateWallet(coin, id)
                                        }

                                    }
                                }
                            }else{
                                Toast.makeText(this@MapActivity,"Distance: %.0fm\nToo far away to collect.".format(distanceToMarker) , Toast.LENGTH_LONG).show()
                            }

                        }
                        true

                    }
                }

            }
        })


    }




    @SuppressWarnings("MissingPermission")
    override fun onStart() {
        super.onStart()
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            locationEngine?.requestLocationUpdates()
            locationLayerPlugin?.onStart()
        }
        mapView.onStart()



    }




    private fun updateUIIfNoUserLoggedIn(){
        val intent = Intent(this, LoginSignupActivity::class.java)
        startActivity(intent)
    }


    private fun updateWallet(coin: HashMap<String,Any>, id: String){
        //val coinMapEntries = coin.entries.iterator().next()
        //val currency = coinMapEntries.key


        val idToCoinMap =  HashMap<String,Any>()
        idToCoinMap[id]=coin


        val document= userDB.collection("wallet")
                .document("todaysCollectedCoins").set(idToCoinMap, SetOptions.merge())


        //val docres = document.result?.getDouble(currency)
        //userDB.collection("bank").document("currencies").set(coin)

    }


    private fun deleteOldCoinsInWallet(){
        val collectedCoinsRef = userDB.collection("wallet").document("todaysCollectedCoins")
        collectedCoinsRef.get().addOnCompleteListener{
            val mapOfCollectedCoins = it.result?.data as HashMap<String, HashMap<String,Any>>
            for ((id, coin) in mapOfCollectedCoins) {
                val coinDate: Date = coin.get("date") as Date
                val formatter = SimpleDateFormat("yyyy/MM/dd")
                val todayString = formatter.format(Date())
                val todayDate = formatter.parse(todayString)

                if (coinDate < todayDate){
                    val deleteCoin =  HashMap<String,Any>()
                    deleteCoin[id] = FieldValue.delete()
                    collectedCoinsRef.update(deleteCoin)
                }


            }

        }.addOnFailureListener {
            Toast.makeText(this,"ERROR: Failed to delete old coins.", Toast.LENGTH_LONG).show()
        }

        // In case user plays through midnight delete coins
        val tomorrowDate = DateTime(Date()).plusDays(1).toLocalDate().toDate()
        Timer("SettingUp", false).schedule(tomorrowDate) {
            deleteOldCoinsInWallet()
        }
    }



    private fun switchToLoginForm(){
        val intent = Intent(this, LoginSignupActivity::class.java)
        startActivity(intent)
    }







    private fun createTodaysLink(): String {

        val currentDate: LocalDate = LocalDate.now()
        val year: Int = currentDate.year
        val month: Int  = currentDate.monthValue
        val day: Int = currentDate.dayOfMonth
        val dayString: String = day.toString()
        if (day <= 9){
            val dayString: String = "0" + day.toString()
        }
        return mapUrlString + year + "/" + month + "/" + dayString + "/" + fileName

    }


    interface DownloadCompleteListener {
        fun downloadComplete(result: String)
    }

    object DownloadCompleteRunner: DownloadCompleteListener {
        var result : String? = null
        override fun downloadComplete(result: String) {
            this.result = result
        }
    }

    inner class DownloadFileTask(private val caller: DownloadCompleteListener):
            AsyncTask<String,Void,String>() {

        override fun doInBackground(vararg urls: String): String = try {
            loadFileFromNetwork(urls[0])
        } catch (e: IOException) {
            "Unable to load content. Check your network connection."
        }

        private fun loadFileFromNetwork(urlString: String): String {
            val stream : InputStream = downloadUrl(urlString)
            // read input stream and close the stream
            return stream.bufferedReader().use { it.readText() }
        }

        @Throws(IOException::class)
        private fun downloadUrl(urlString: String) : InputStream {
            var url = URL(urlString)
            var conn = url.openConnection() as HttpURLConnection
            conn.readTimeout = 10000
            conn.connectTimeout = 15000
            conn.requestMethod = "GET"
            conn.doInput = true
            conn.connect()
            return conn.inputStream
        }

        override fun onPostExecute(result: String) {
            super.onPostExecute(result)

            caller.downloadComplete(result)
        }
    }



    fun enableLocation() {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            Log.d(tag,"Permissions are granted")
            intializeLocationEngine()
            initalizeLocationLayer()
        } else {
            permissionManager = PermissionsManager(this)
            permissionManager.requestLocationPermissions(this)
        }
    }


    @SuppressWarnings("MissingPermission")
    private fun intializeLocationEngine() {
        locationEngine = LocationEngineProvider(this)
                .obtainBestLocationEngineAvailable()
        locationEngine?.apply {
            interval = 5000
            fastestInterval = 1000
            priority = LocationEnginePriority.HIGH_ACCURACY
            activate()
        }

        val lastLocation = locationEngine?.lastLocation
        if (lastLocation != null) {
            originLocation = lastLocation
            setCameraPosition(lastLocation)
        } else {
            locationEngine?.addLocationEngineListener(this)
        }
    }

    @SuppressWarnings("MissingPermission")
    private fun initalizeLocationLayer() {
        /*if (mapView == null){Log.d(tag,"mapView is null")}
        else{
            if(map == null){Log.d(tag,"map is null")}
            else{

            }
        }*/
        locationLayerPlugin = LocationLayerPlugin(mapView, map, locationEngine)
        locationLayerPlugin?.setLocationLayerEnabled(true)
        locationLayerPlugin?.cameraMode = CameraMode.TRACKING
        locationLayerPlugin?.renderMode = RenderMode.NORMAL
    }

    private fun setCameraPosition(location: Location) {
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                LatLng(location.latitude, location.longitude),55.0))
    }


    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        Log.d(tag,"Permissions:$permissionsToExplain")
        // present toast
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            enableLocation()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        permissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onLocationChanged(location: Location?) {
        location?.let {
            originLocation = location
            setCameraPosition(location)
        }
    }

    @SuppressWarnings("MissingPermission")
    override fun onConnected() {
        locationEngine?.requestLocationUpdates()
    }



    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        locationEngine?.removeLocationUpdates()
        locationLayerPlugin?.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        locationEngine?.deactivate()
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        if (outState != null) {
            mapView.onSaveInstanceState(outState)
        }
    }




}
