
package uk.ac.ed.inf.coinz

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.ConnectivityManager
import android.os.AsyncTask
import android.os.Bundle
import android.support.annotation.VisibleForTesting
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.JsonObject
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineListener
import com.mapbox.android.core.location.LocationEnginePriority
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin
import kotlinx.android.synthetic.main.activity_map.*
import org.joda.time.DateTime
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*
import kotlin.collections.HashMap
import kotlin.concurrent.schedule

@Suppress("UNCHECKED_CAST")
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

    // Shared Prefs
    private val preferencesFile = "PrefsFile" // for storing preferences
    private var levelUp: Boolean = false

    private var loggedIn: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        // Initialising User Authentication
        mAuth = FirebaseAuth.getInstance()
        // Initialising User
        currentUser = mAuth.currentUser
        if (currentUser != null){
            loggedIn = true
        }

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
            // switch to login/sign-up screen
            updateUIIfNoUserLoggedIn()
        }

        // Switch to Menu button:
        menu_button.setOnClickListener {
            goToBottomNavigationActivity()
        }
        // in case of imprecise touch
        menu_button_container.setOnClickListener {
            goToBottomNavigationActivity()
        }


        val connectivityManager : ConnectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        if (activeNetworkInfo != null && activeNetworkInfo.isConnected) {
            // Downloading coins and setting up map with coins:
            mapUrlString = createTodaysLink()
            geoJsonCoinsString = DownloadFileTask(DownloadCompleteRunner).execute(mapUrlString).get()
        }else{
            Snackbar.make(mapMainLayout, "Please connect to a network!", Snackbar.LENGTH_INDEFINITE).show()
        }



        Mapbox.getInstance(applicationContext, getString(R.string.access_token))
        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { mapboxMap ->
            map = mapboxMap

            // User interface options
            map.uiSettings.isCompassEnabled = true
            map.uiSettings.isZoomControlsEnabled = true

            // Make location info available:
            enableLocation()

            if (activeNetworkInfo != null && activeNetworkInfo.isConnected) {
                // create markers and set on click listeners
                showAvailableCoinsOnMapAndSetOnMarkerClickFunctions(mapboxMap)
            }else Snackbar.make(mapMainLayout, "Please connect to a network!", Snackbar.LENGTH_INDEFINITE).show()
        }
    }


    private fun showAvailableCoinsOnMapAndSetOnMarkerClickFunctions(mapboxMap: MapboxMap) {
        val collectedCoinsRef = userDB.collection("wallet").document("todaysCollectedCoins")
        collectedCoinsRef.get().addOnCompleteListener { coinsCollected ->
            // the id's of the coins already collected that are not to be shown on the map:
            // ids that were added to wallet but not yet moved to bank
            var collectedIds: MutableSet<String> = mutableSetOf()
            if (coinsCollected.result!!.exists()) {
                val mapOfCollectedCoins = coinsCollected.result?.data as HashMap<String, HashMap<String, Any>>
                collectedIds = mapOfCollectedCoins.keys
            }
            // coins that were added to bank
            collectedCoinsRef.parent.document("todaysCollectedAddedToBank").get().addOnCompleteListener {
                if (it.result!!.exists()) {
                    val mapOfAddedToBankIds = it.result?.data as HashMap
                    val addedToBankIds = mapOfAddedToBankIds.keys
                    collectedIds = collectedIds.union(addedToBankIds) as MutableSet<String>
                }

                // extracting rates from the downloaded geoJson
                val ratesJSONAsString = JSONObject(geoJsonCoinsString).getString("rates")
                // saving the rates in the shared preferences
                val settings = this@MapActivity.getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
                val editor = settings?.edit()
                editor?.putString("ratesJSONAsString", ratesJSONAsString)
                editor?.apply()

                // extracting features from the downloaded geoJson
                val featureCollection: FeatureCollection = FeatureCollection.fromJson(geoJsonCoinsString)
                val featureList: List<Feature>? = featureCollection.features()
                if (featureList != null) {
                    // looping thorough the features representing the coins
                    for (feature: Feature in featureList) {
                        // extraction of points, the id and the currency from the  current feature
                        val point: Point = feature.geometry() as Point
                        val properties: JsonObject? = feature.properties()
                        val coinID = properties?.get("id")?.asString
                        val coinCurrency = properties?.get("currency")?.asString
                        // creation of the marker
                        val iconName = "coin_" + coinCurrency?.toLowerCase()
                        val iconResource = resources.getIdentifier(iconName, "drawable", packageName)
                        val icon1 = IconFactory.getInstance(this@MapActivity).fromResource(iconResource)
                        // adding marker to map
                        if (coinID !in collectedIds) {
                            mapboxMap.addMarker(MarkerOptions()
                                    .position(LatLng(point.latitude(), point.longitude()))
                                    .icon(icon1)
                                    .title(coinID)
                                    // the title (not visible to the user) is the coin id for
                                    // future reference
                            )
                        }
                    }
                } else {
                    // If coins are not available, display a message to the user.
                    Toast.makeText(this@MapActivity, "Coins not available.",
                            Toast.LENGTH_SHORT).show()
                }


                // click listener setup that allows coin collection
                coinMarkerClickListener(featureList)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun coinMarkerClickListener(featureList: List<Feature>?) {
        map.setOnMarkerClickListener { marker ->
            if (locationEngine != null) {
                // calculating distance to marker
                val lastLocation = locationEngine!!.lastLocation
                val lastLocationLatLng = LatLng(lastLocation.latitude, lastLocation.longitude)
                val markerLatLng = marker.position
                val distanceToMarker = lastLocationLatLng.distanceTo(markerLatLng)
                Log.d(tag, "Distance to clicked marker: $distanceToMarker meter(s)")
                // getting the data on the marker clicked on if distance to clicked coin is less
                // than 25 meters
                if (distanceToMarker < requiredMarkerDistance) {
                    val id = marker.title
                    Log.d(tag, "Clicked coin id: $id")
                    if (featureList != null) {
                       loop@ for (feature: Feature in featureList) {
                            if (feature.properties()?.get("id")?.asString == id) {
                                // remove clicked marker
                                marker.remove()
                                // extract properties
                                val coinValue = feature.properties()!!.get("value").asDouble
                                val coinCurrency = feature.properties()!!.get("currency").asString
                                // tell user that the coin could be collected
                                Toast.makeText(this@MapActivity, "Distance: %.0fm\nAdded to wallet."
                                        .format(distanceToMarker), Toast.LENGTH_LONG).show()
                                val coin = HashMap<String, Any>()
                                // add coin to database (Firestore)
                                coin[coinCurrency] = coinValue
                                val date = Date()
                                val dateTimestamp = Timestamp(date)
                                coin["date"] = dateTimestamp
                                updateWallet(coin, id)
                                // once coin is found the for loop can stop
                                break@loop
                            }
                        }
                    }
                } else {
                    // if clicked coin is too far then notify user
                    Toast.makeText(this@MapActivity, "Distance: %.0fm\nToo far away to collect.".format(distanceToMarker), Toast.LENGTH_LONG).show()
                }
            }
            true
        }
    }


    @SuppressWarnings("MissingPermission")
    override fun onStart() {
        super.onStart()
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            locationEngine?.requestLocationUpdates()
            locationLayerPlugin?.onStart()
        }
        mapView.onStart()
        // listen to incoming coins
        if (currentUser != null) CoinMessageListener().realTimeUpdateListener(this)

        // getting info from shared prefs:
        // getting preferences that define if the user has "leveled up"
        val settings= this.getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        // if they already leveled up earlier coinDistanceLimit will be in shared prefs folder
        if(settings.contains("coinDistanceLimit")){
            requiredMarkerDistance = settings.getInt("coinDistanceLimit", 25).toDouble()
        }
        // otherwise if they have just leveled up then user is notified:
        if (settings.contains("levelUp")){
            levelUp = settings!!.getBoolean("levelUp", false)
            if (levelUp){
                // if user has leveled up set collectible coin distance limit to 50
                requiredMarkerDistance = 50.0
                // notify user about "hidden level-up"
                val alert = AlertDialog.Builder(this)
                alert.apply {
                    setPositiveButton("Yay!",null)
                    setCancelable(true)
                    setTitle("Hidden Level Up")
                    setMessage("You are now able to collect coins as far as 50 meters away with this device!")
                    create().show()
                }
                // remove levelUp from shared prefs
                settings.edit().remove("levelUp").apply()
                val editor = settings.edit()
                // put coinDistanceLimit to shared prefs for future reference
                editor?.putInt("coinDistanceLimit", 50)
                editor?.apply()
            }
        }



    }

    // switching to the activity containing the bank, the wallet and the messenger
    private fun goToBottomNavigationActivity(){
        val intent = Intent(this, BottomNavigationActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_left,R.anim.slide_out_right)
    }



    // switching to the login/sign-up activity
    private fun updateUIIfNoUserLoggedIn(){
        val intent = Intent(this, LoginSignupActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    // updating wallet with currently collected coin
    private fun updateWallet(coin: HashMap<String,Any>, id: String){
        val idToCoinMap =  HashMap<String,Any>()
        idToCoinMap[id]=coin
        userDB.collection("wallet")
                .document("todaysCollectedCoins").set(idToCoinMap, SetOptions.merge())
    }


    @SuppressLint("SimpleDateFormat")
    private fun deleteOldCoinsInWallet(){
        // get the collected coins
        val collectedCoinsRef = userDB.collection("wallet").document("todaysCollectedCoins")
        collectedCoinsRef.get().addOnCompleteListener{
           if (it.result!!.exists()) {
               val mapOfCollectedCoins = it.result?.data as HashMap<String, HashMap<String, Any>>
               loop@ for ((_, coin) in mapOfCollectedCoins) {
                   // get today's date
                   val coinDate: Date = coin["date"] as Date
                   val formatter = SimpleDateFormat("yyyy/MM/dd")
                   val todayString = formatter.format(Date())
                   val todayDate = formatter.parse(todayString)
                   // if today's date is larger than a coin's collecton date then it's expired
                   if (coinDate < todayDate) {
                       // so we delete the  todaysCollectedCoins document
                      collectedCoinsRef.delete()
                       // as well as the document that contains the ids of to
                       // today's cons added to the bank
                       userDB.collection("wallet").document("todaysCollectedAddedToBank").delete()
                       break@loop
                   }
               }
           }

        }.addOnFailureListener {
            Toast.makeText(this,"ERROR: Failed to delete old coins.", Toast.LENGTH_LONG).show()
        }
        // reset bank counter if new day
        resetBankCoinCounter()
        // In case user plays through midnight delete coins
        val tomorrowDate = DateTime(Date()).plusDays(1).toLocalDate().toDate()
        Timer("SettingUp", false).schedule(tomorrowDate) {
            deleteOldCoinsInWallet()
            resetBankCoinCounter()
        }
    }

    //  reset the counter in bank counting the no. of coins added to the bank today
    private fun resetBankCoinCounter(){
        val path = userDB.collection("bank").document("numberOfCoinsAddedTodayToBank")
        path.get().addOnCompleteListener {
            if (it.isSuccessful) {
                val dateToday = DateTime(Date()).toLocalDate()
                if (it.result!!.exists()) {
                    val counterNHashMap = it.result?.data as java.util.HashMap<String, Any>
                    val counterDate = DateTime(counterNHashMap["date"] as Date).toLocalDate()

                    if(dateToday > counterDate){
                        path.update("n",0)
                    }
                }
            }

        }

    }

    // create link for today's coins
    private fun createTodaysLink(): String {
        val currentDate: LocalDate = LocalDate.now()
        val year: Int = currentDate.year
        val month: Int  = currentDate.monthValue
        val day: Int = currentDate.dayOfMonth
        var dayString: String = day.toString()
        if (day <= 9){
            dayString = "0$dayString"
        }
        return "$mapUrlString$year/$month/$dayString/$fileName"

    }

    // create interface for the listener for async download task completion
    interface DownloadCompleteListener {
        fun downloadComplete(result: String)
    }

    // object implementing the DownloadCompleteListener interface
    object DownloadCompleteRunner: DownloadCompleteListener {
        // initialising result variable
        private var result : String? = null
        // download complete function setting the result variable
        override fun downloadComplete(result: String) {
            this.result = result
        }
    }

    // async download task for retrieving geoJSON containing rates and coin map markers
    class DownloadFileTask(private val caller: DownloadCompleteListener):
            AsyncTask<String,Void,String>() {
        // part of the async task that is run in the background
        override fun doInBackground(vararg urls: String): String = try {
            // calling function that downloads the content of the given url
            loadFileFromNetwork(urls[0])
        } catch (e: IOException) {
            "Unable to load content. Check your network connection."
        }

        private fun loadFileFromNetwork(urlString: String): String {
            // opens stream to the content of the geoJSON on the network
            val stream : InputStream = downloadUrl(urlString)
            // read input stream and close the stream
            return stream.bufferedReader().use { it.readText() }
        }

        // downloading the geoJSON from the network
        @Throws(IOException::class)
        private fun downloadUrl(urlString: String) : InputStream {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.readTimeout = 10000
            conn.connectTimeout = 15000
            conn.requestMethod = "GET"
            conn.doInput = true
            conn.connect()
            return conn.inputStream
        }
        // calling the download complete listener's method and sending it the downloaded file
        override fun onPostExecute(result: String) {
            super.onPostExecute(result)
            caller.downloadComplete(result)
        }
    }


    // enabling the location service
    private fun enableLocation() {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            Log.d(tag,"Permissions are granted")
            initializeLocationEngine()
            initializeLocationLayer()
        } else {
            permissionManager = PermissionsManager(this)
            permissionManager.requestLocationPermissions(this)
        }
    }

    // initialising the location engine for location tracking
    @SuppressWarnings("MissingPermission")
    private fun initializeLocationEngine() {
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

    // initialising the loaction layer of the map
    @SuppressWarnings("MissingPermission")
    private fun initializeLocationLayer() {
        locationLayerPlugin = LocationLayerPlugin(mapView, map, locationEngine)
        locationLayerPlugin?.setLocationLayerEnabled(true)
        locationLayerPlugin?.cameraMode = CameraMode.TRACKING
        locationLayerPlugin?.renderMode = RenderMode.NORMAL
    }

    // function for setting the camera position over the user's loaction
    private fun setCameraPosition(location: Location) {
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                LatLng(location.latitude, location.longitude),55.0))
    }

    // log the permissions
    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        Log.d(tag,"Permissions:$permissionsToExplain")
    }

    // if results were not granted on creation of the activity but were granted later, then  retry
    // enabling location
    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            enableLocation()
        }
    }

    // ask user for permission to use device loaction
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        permissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    // when the location is changed then reset camera position
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


    // adding mapView activity lifecycle change method callers
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

    // saving map's state
    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        if (outState != null) {
            mapView.onSaveInstanceState(outState)
        }
    }

    // animating inter-activity transitions
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left,R.anim.slide_out_right)
    }

    // variable for testing (SignUpTest.java & LoginTest.java)
    @VisibleForTesting
    fun getLoggedIn() = loggedIn


}
