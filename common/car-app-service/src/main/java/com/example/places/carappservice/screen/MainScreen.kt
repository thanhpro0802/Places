package com.example.places.carappservice.screen

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.text.Spannable
import android.text.SpannableString
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.places.data.PlacesRepository
import com.google.android.gms.location.LocationServices

// 1. THÊM ENUM: Dùng để quản lý trạng thái lọc hiện tại
enum class FilterType { ALL, RESTAURANT, FUEL }

class MainScreen(carContext: CarContext) : Screen(carContext) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(carContext)
    private var currentLocation: Location? = null
    private var placeList: List<com.example.places.data.model.Place> = emptyList()
    private var isLoading = false

    // 2. THÊM BIẾN: Lưu bộ lọc hiện tại (Mặc định là hiện Tất cả)
    private var currentFilter = FilterType.ALL

    init {
        requestLocationPermission()
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
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                currentLocation = location
                fetchPlacesFromOSM(location.latitude, location.longitude)
            } else {
                CarToast.makeText(carContext, "Đang xác định vị trí...", CarToast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchPlacesFromOSM(lat: Double, lng: Double) {
        isLoading = true
        invalidate()

        lifecycleScope.launch {
            val repository = PlacesRepository()
            placeList = repository.getPlacesNearby(lat, lng)

            isLoading = false
            invalidate()
        }
    }

    override fun onGetTemplate(): Template {
        val itemListBuilder = ItemList.Builder()

        // 3. TẠO THANH BỘ LỌC (ACTION STRIP) TRÊN BẢN ĐỒ
        val actionStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    // Đổi tên và thêm dấu tick nếu đang chọn
                    .setTitle(if (currentFilter == FilterType.RESTAURANT) "✅ Quán ăn" else "Quán ăn")
                    .setOnClickListener {
                        // Bấm để bật/tắt bộ lọc
                        currentFilter = if (currentFilter == FilterType.RESTAURANT) FilterType.ALL else FilterType.RESTAURANT
                        invalidate() // Vẽ lại UI ngay lập tức
                    }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle(if (currentFilter == FilterType.FUEL) "✅ Cây xăng" else "Cây xăng")
                    .setOnClickListener {
                        currentFilter = if (currentFilter == FilterType.FUEL) FilterType.ALL else FilterType.FUEL
                        invalidate()
                    }
                    .build()
            )
            .build()

        // 4. LỌC DANH SÁCH THEO TRẠNG THÁI (Trích xuất chuỗi từ biến description)
        val filteredList = placeList.filter { place ->
            when (currentFilter) {
                FilterType.ALL -> true
                FilterType.RESTAURANT -> place.description.contains("restaurant", ignoreCase = true)
                FilterType.FUEL -> place.description.contains("fuel", ignoreCase = true)
            }
        }

        // Xử lý thông báo khi list rỗng
        if (isLoading) {
            itemListBuilder.setNoItemsMessage("Đang quét địa điểm xung quanh...")
        } else if (filteredList.isEmpty()) {
            itemListBuilder.setNoItemsMessage("Không tìm thấy địa điểm nào phù hợp")
        }

        val hasPermission = carContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val headerTitle = if (hasPermission) "Địa điểm quanh xe" else "Đang chờ cấp quyền..."
        val currentApiLevel = carContext.carAppApiLevel

        // 5. DÙNG DANH SÁCH ĐÃ LỌC (filteredList) ĐỂ HIỂN THỊ
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
                val distanceInKm = (currentLocation!!.distanceTo(destLocation)) / 1000.0
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
            .setActionStrip(actionStrip) // 6. GẮN BỘ LỌC VÀO BẢN ĐỒ
            .build()
    }
}