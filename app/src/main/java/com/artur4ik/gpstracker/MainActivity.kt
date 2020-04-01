package com.artur4ik.gpstracker

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.*
import com.google.android.gms.tasks.OnCompleteListener
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*


class MainActivity : AppCompatActivity(), View.OnClickListener {
    private val PERMISSION_ID = 44
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var adapterLocationView:LocationAdapterView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        adapterLocationView = LocationAdapterView()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapterLocationView

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this@MainActivity)
        buttonClear.setOnClickListener(this)
        button.setOnClickListener(this)
    }

    override fun onClick(view: View?) {
        when(view?.id){
            button.id -> getLastLocation()
            buttonClear.id -> clearLocations()
        }
    }

    private fun clearLocations() {
        adapterLocationView.clearAlLocations()
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                mFusedLocationClient.lastLocation?.addOnCompleteListener { task ->
                    val location = task.result
                    if (location == null) {
                        requestNewLocationData()
                    } else {
                        adapterLocationView.addLocationItem(location)
                        recyclerView.scrollToPosition(0)
                    }
                }
            } else {
                Toast.makeText(this, "Включите GPS", Toast.LENGTH_LONG).show()
                val intent =
                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            requestPermissions()
        }
    }


    @SuppressLint("MissingPermission")
    private fun requestNewLocationData(){
        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 1
        mLocationRequest.fastestInterval = 0
        mLocationRequest.numUpdates = 1
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()
        )
    }

    private val mLocationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation = locationResult.lastLocation
            if (mLastLocation != null){
                adapterLocationView.addLocationItem(mLastLocation)
                recyclerView.scrollToPosition(0)
            }
        }
    }

    private fun checkPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            PERMISSION_ID
        )
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_ID) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation()
            }
        }
    }


    override fun onResume() {
        super.onResume()
        if (checkPermissions()) {
            getLastLocation()
        }
    }

    inner class LocationAdapterView : RecyclerView.Adapter<LocationViewHolder>(){

        private val mLocations = mutableListOf<Location>()
        private val mInflater = LayoutInflater.from(applicationContext)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
            val view = mInflater.inflate(R.layout.item_location_layout, parent, false)
            return LocationViewHolder(view)
        }

        override fun getItemCount() : Int {
            return mLocations.count()
        }

        override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
            holder.onBind(mLocations[position])
        }

        fun setLocations(listLocations:List<Location>){
            mLocations.clear()
            mLocations.addAll(listLocations)
            notifyDataSetChanged()
        }

        fun addLocationItem(location:Location) {
            mLocations.add(0, location)
            notifyItemInserted(0)
        }

        fun clearAlLocations() {
            mLocations.clear()
            notifyDataSetChanged()
        }
    }

    class LocationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val latitudeTV:TextView = itemView.findViewById<TextView>(R.id.latitudeTV)
        private val longitudeTV:TextView = itemView.findViewById<TextView>(R.id.longitudeTV)
        private val datetimeTV:TextView = itemView.findViewById<TextView>(R.id.datetimeTV)

        fun onBind(location:Location){
            datetimeTV.text = Date().toString()
            latitudeTV.text = location.latitude.toString()
            longitudeTV.text = location.longitude.toString()
        }
    }
}
