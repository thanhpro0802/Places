package com.example.places.carappservice.screen

import android.text.Spannable
import android.text.SpannableString
import androidx.car.app.CarContext
import androidx.car.app.Screen
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
import com.example.places.data.PlacesRepository
import kotlin.random.Random

class MainScreen(carContext: CarContext) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val placesRepository = PlacesRepository()
        val itemListBuilder = ItemList.Builder()
            .setNoItemsMessage("No places to show")

        // Lấy cấp độ API hiện tại của hệ điều hành ô tô để kiểm tra tính năng
        val currentApiLevel = carContext.carAppApiLevel

        placesRepository.getPlaces()
            .forEachIndexed { index, placeData ->

                // 1. KHÁM PHÁ: Tùy chỉnh PlaceMarker
                // Tạo một marker màu xanh dương với chữ "P" (Parking) ở giữa
                val customMarker = PlaceMarker.Builder()
                    .setColor(CarColor.BLUE)
                    .setLabel("P")
                    .build()

                // Tạo builder cho Row
                val rowBuilder = Row.Builder()
                    .setTitle(placeData.name)
                    // Bắt buộc phải có DistanceSpan
                    .addText(SpannableString(" ").apply {
                        setSpan(
                            DistanceSpan.create(
                                Distance.create(Math.random() * 100, Distance.UNIT_KILOMETERS)
                            ), 0, 1, Spannable.SPAN_INCLUSIVE_INCLUSIVE
                        )
                    })
                    .setOnClickListener { screenManager.push(DetailScreen(carContext, placeData.id)) }
                    // Gắn Metadata với Marker đã tùy chỉnh
                    .setMetadata(
                        Metadata.Builder()
                            .setPlace(
                                Place.Builder(CarLocation.create(placeData.latitude, placeData.longitude))
                                    .setMarker(customMarker) // <-- Gắn customMarker vào đây
                                    .build()
                            )
                            .build()
                    )

                // 2. KHÁM PHÁ: Bật/Tắt Row an toàn
                // Tạo ngẫu nhiên một trạng thái: một số địa điểm sẽ bị "đóng cửa" (tắt)
                val isPlaceEnabled = true

                // CHỈ gọi setEnabled() nếu hệ thống đang chạy API level 5 trở lên
                if (currentApiLevel >= 5) {
                    rowBuilder.setEnabled(isPlaceEnabled)
                }

                // Xây dựng Row và thêm vào danh sách
                itemListBuilder.addItem(rowBuilder.build())
            }

        return PlaceListMapTemplate.Builder()
            .setTitle("Places")
            .setItemList(itemListBuilder.build())
            .build()
    }
}