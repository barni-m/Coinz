package uk.ac.ed.inf.coinz

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.gson.JsonObject
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineListener
import com.mapbox.android.core.location.LocationEnginePriority
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.*
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.Icon
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
import kotlinx.android.synthetic.main.activity_login_signup.*
import kotlinx.android.synthetic.main.activity_map.*

import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.time.LocalDate

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

    // User Authentication (Firebase)
    private lateinit var mAuth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        // Setting up User Authentication
        mAuth = FirebaseAuth.getInstance()

        // Login button
        //buttonLogin.setOnClickListener{ switchToLoginForm() }
        // Logout button
        logout_button.setOnClickListener {it ->
            mAuth.signOut()
            it.visibility = View.GONE
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

                val featureCollection: FeatureCollection = FeatureCollection.fromJson(geoJsonCoinsString)
                val featureList: List<Feature>? = featureCollection.features()
                if (featureList != null){
                    for (feature: Feature in featureList){
                        val icon = IconFactory.getInstance(this@MapActivity)
                        val icon1 = icon.fromResource(R.drawable.coin)

                        val point: Point = feature.geometry() as Point
                        val properties: JsonObject? = feature.properties()
                        val coinID =properties?.get("id").toString()
                        mapboxMap.addMarker(MarkerOptions()
                                .position(LatLng(point.latitude(),point.longitude()))
                                .icon(icon1)
                                .title(coinID)
                        )
                    }
                }else{
                    // If coins are not available, display a message to the user.
                    Toast.makeText(this@MapActivity, "Coins not available.",
                            Toast.LENGTH_SHORT).show()
                }



                map.setOnMarkerClickListener { marker ->
                    if (locationEngine != null){
                        val lastLocation= locationEngine!!.lastLocation
                        val lastLocationLatLng: LatLng = LatLng(lastLocation.latitude,lastLocation.longitude)
                        val markerLatLng = marker.position
                        val distanceToMarker = lastLocationLatLng.distanceTo(markerLatLng)
                        // Show a toast with the title of the selected marker
                        Toast.makeText(this@MapActivity,distanceToMarker.toString() , Toast.LENGTH_LONG).show()
                        /* @TODO: Coin collection */
                        if (distanceToMarker < 25.0){
                            val a = marker.title
                            Toast.makeText(this@MapActivity,a , Toast.LENGTH_LONG).show()
                        }

                    }
                    true

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

        // Check if user is signed in (non-null) and update UI accordingly.
        var currentUser: FirebaseUser? = mAuth.currentUser
        updateUI(currentUser)

    }




    fun updateUI(currentUser: FirebaseUser?){
        if (currentUser == null){
            val intent = Intent(this, LoginSignupActivity::class.java)
            startActivity(intent)
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
        locationLayerPlugin?.cameraMode = CameraMode.TRACKING_GPS
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
