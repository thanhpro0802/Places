package com.example.places.carappservice.screen

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.CarLocation
import androidx.car.app.model.Distance
import androidx.car.app.model.DistanceSpan
import androidx.car.app.model.ItemList
import androidx.car.app.model.Metadata
import androidx.car.app.model.Place
import androidx.car.app.model.PlaceListMapTemplate
import androidx.car.app.model.PlaceMarker
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.places.carappservice.R
import com.example.places.data.PlacesRepository
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch

// Enum quản lý tất cả loại bộ lọc địa điểm
enum class FilterType {
    ALL,
    RESTAURANT,
    FUEL,
    CAFE,
    HOSPITAL,
    ATM
}

class MainScreen(carContext: CarContext) : Screen(carContext) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(carContext)
    private var currentLocation: Location? = null
    private var lastFetchedLocation: Location? = null
    private var locationCallback: LocationCallback? = null
    private var placeList: List<com.example.places.data.model.Place> = emptyList()
    private var isLoading = false

    // Bộ lọc hiện tại (mặc định: Tất cả)
    private var currentFilter = FilterType.ALL

    init {
        requestLocationPermission()

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                stopLocationUpdates()
                super.onDestroy(owner)
            }
        })
    }

    private fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
    }

    private fun requestLocationPermission() {
        val permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        carContext.requestPermissions(permissions) { grantedPermissions, _ ->
            if (grantedPermissions.contains(Manifest.permission.ACCESS_FINE_LOCATION)) {
                getCurrentLocation()
            } else {
                CarToast.makeText(carContext, "App cần quyền vị trí để hoạt động!", CarToast.LENGTH_LONG).show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        val locationRequest = LocationRequest.Builder(
            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
            5000L
        ).apply {
            setMinUpdateIntervalMillis(2000L)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                if (location != null) {
                    currentLocation = location

                    // Kiểm tra khoảng cách so với lần lấy dữ liệu gần nhất
                    val lastLoc = lastFetchedLocation
                    if (lastLoc == null || lastLoc.distanceTo(location) > 1000f) { // Nếu đi quá 1000m
                        lastFetchedLocation = location
                        fetchPlacesFromOSM(location.latitude, location.longitude)
                    } else {
                        // Chỉ cập nhật UI khoảng cách nếu xe di chuyển chưa đủ xa
                        invalidate()
                    }
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            Looper.getMainLooper()
        )

        CarToast.makeText(carContext, "Đang bắt đầu theo dõi vị trí...", CarToast.LENGTH_SHORT).show()
    }

    private fun fetchPlacesFromOSM(lat: Double, lng: Double) {
        isLoading = true
        invalidate()

        lifecycleScope.launch {
            val repository = PlacesRepository()
            // Query mở rộng: restaurant, fuel, cafe, hospital, atm, supermarket
            placeList = repository.getPlacesNearby(lat, lng)
            isLoading = false
            invalidate()
        }
    }

    override fun onGetTemplate(): Template {
        val itemListBuilder = ItemList.Builder()

        // ── ACTION STRIP: Bộ lọc + Nút tìm kiếm ──────────────────────────
        val actionStrip = ActionStrip.Builder()
            .addAction(buildFilterAction("🍜", "Quán ăn", FilterType.RESTAURANT))
            .addAction(buildFilterAction("⛽", "Xăng", FilterType.FUEL))
            .addAction(buildFilterAction("☕", "Cafe", FilterType.CAFE))
            .addAction(
                // Nút tìm kiếm — push SearchScreen
                Action.Builder()
                    .setIcon(
                        CarIcon.Builder(
                            IconCompat.createWithResource(carContext, R.drawable.baseline_search_24)
                        ).build()
                    )
                    .setOnClickListener {
                        screenManager.push(SearchScreen(carContext))
                    }
                    .build()
            )
            .build()

        // ── LỌC DANH SÁCH ─────────────────────────────────────────────────
        val filteredList = placeList.filter { place ->
            when (currentFilter) {
                FilterType.ALL -> true
                FilterType.RESTAURANT -> place.description.contains("restaurant", ignoreCase = true)
                FilterType.FUEL -> place.description.contains("fuel", ignoreCase = true)
                FilterType.CAFE -> place.description.contains("cafe", ignoreCase = true)
                FilterType.HOSPITAL -> place.description.contains("hospital", ignoreCase = true)
                FilterType.ATM -> place.description.contains("atm", ignoreCase = true)
            }
        }

        // ── THÔNG BÁO KHI RỖNG ────────────────────────────────────────────
        if (isLoading) {
            itemListBuilder.setNoItemsMessage("Đang quét địa điểm xung quanh...")
        } else if (filteredList.isEmpty()) {
            itemListBuilder.setNoItemsMessage("Không tìm thấy địa điểm nào phù hợp")
        }

        val hasPermission = carContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        val headerTitle = if (hasPermission) "Địa điểm quanh xe" else "Đang chờ cấp quyền..."
        val currentApiLevel = carContext.carAppApiLevel

        // ── HIỂN THỊ DANH SÁCH (tối đa 6 theo giới hạn PlaceListMapTemplate) ──
        filteredList.take(6).forEach { placeData ->

            val customMarker = PlaceMarker.Builder()
                .setColor(CarColor.BLUE)
                .setLabel("P")
                .build()

            val distanceSpan: DistanceSpan
            if (currentLocation != null) {
                val destLocation = Location("").apply {
                    latitude = placeData.latitude
                    longitude = placeData.longitude
                }
                val distanceInKm = currentLocation!!.distanceTo(destLocation) / 1000.0
                distanceSpan = DistanceSpan.create(Distance.create(distanceInKm, Distance.UNIT_KILOMETERS))
            } else {
                distanceSpan = DistanceSpan.create(Distance.create(0.0, Distance.UNIT_KILOMETERS))
            }

            val textToSpan = "Cách bạn"
            val spannableString = SpannableString(textToSpan)
            spannableString.setSpan(distanceSpan, 0, textToSpan.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)

            val rowBuilder = Row.Builder()
                .setTitle(placeData.name)
                .addText(spannableString)
                .setOnClickListener {
                    screenManager.push(DetailScreen(carContext, placeData.id))
                }
                .setMetadata(
                    Metadata.Builder()
                        .setPlace(
                            Place.Builder(CarLocation.create(placeData.latitude, placeData.longitude))
                                .setMarker(customMarker)
                                .build()
                        )
                        .build()
                )

            if (currentApiLevel >= 5) {
                rowBuilder.setEnabled(true)
            }

            itemListBuilder.addItem(rowBuilder.build())
        }

        return PlaceListMapTemplate.Builder()
            .setTitle(headerTitle)
            .setItemList(itemListBuilder.build())
            .setCurrentLocationEnabled(hasPermission)
            .setActionStrip(actionStrip)
            .build()
    }

    // Helper: tạo action bộ lọc với prefix ✓ khi đang được chọn
    private fun buildFilterAction(emoji: String, label: String, filter: FilterType): Action {
        val isActive = currentFilter == filter
        val title = if (isActive) "✓ $label" else label
        return Action.Builder()
            .setTitle(title)
            .setOnClickListener {
                currentFilter = if (currentFilter == filter) FilterType.ALL else filter
                invalidate()
            }
            .build()
    }
}