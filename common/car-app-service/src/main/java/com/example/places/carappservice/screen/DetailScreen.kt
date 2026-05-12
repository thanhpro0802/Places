package com.example.places.carappservice.screen

import android.content.Context
import android.content.SharedPreferences
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import com.example.places.carappservice.R
import com.example.places.data.PlacesCache
import com.example.places.data.model.toIntent

class DetailScreen(carContext: CarContext, private val placeId: Int) : Screen(carContext) {

    // 1. Khởi tạo SharedPreferences
    private val sharedPrefs: SharedPreferences = carContext.getSharedPreferences(
        "places_preferences",
        Context.MODE_PRIVATE
    )

    // 2. Kiểm tra trạng thái lưu ban đầu khi mở màn hình
    private var isBookmarked = checkInitialBookmarkState()

    private fun checkInitialBookmarkState(): Boolean {
        // Lấy danh sách các ID đã lưu (mặc định là danh sách rỗng nếu chưa có gì)
        val bookmarkedIds = sharedPrefs.getStringSet("bookmarked_ids", emptySet()) ?: emptySet()
        return bookmarkedIds.contains(placeId.toString())
    }

    // 3. Xử lý Logic khi bấm nút Ngôi sao
    private fun toggleBookmark() {
        // Lấy danh sách hiện tại ra và chuyển thành MutableSet để có thể thêm/xóa
        val bookmarkedIds = sharedPrefs.getStringSet("bookmarked_ids", emptySet())?.toMutableSet() ?: mutableSetOf()

        isBookmarked = !isBookmarked

        if (isBookmarked) {
            bookmarkedIds.add(placeId.toString())
            CarToast.makeText(carContext, "Đã lưu vào danh sách", CarToast.LENGTH_SHORT).show()
        } else {
            bookmarkedIds.remove(placeId.toString())
            CarToast.makeText(carContext, "Đã bỏ lưu", CarToast.LENGTH_SHORT).show()
        }

        // Lưu ngược lại danh sách mới vào SharedPreferences
        sharedPrefs.edit().putStringSet("bookmarked_ids", bookmarkedIds).apply()

        // Báo cho thư viện biết cần vẽ lại màn hình (cập nhật icon ngôi sao)
        invalidate()
    }

    override fun onGetTemplate(): Template {
        val place = PlacesCache.places.find { it.id == placeId }
            ?: return MessageTemplate.Builder("Place not found")
                .setHeaderAction(Action.BACK)
                .setTitle("Thông báo")
                .build()

        val navigateAction = Action.Builder()
            .setTitle("Navigate")
            .setIcon(
                CarIcon.Builder(
                    IconCompat.createWithResource(
                        carContext,
                        R.drawable.baseline_navigation_24
                    )
                ).build()
            )
            .setOnClickListener { carContext.startCarApp(place.toIntent(CarContext.ACTION_NAVIGATE)) }
            .build()

        val bookmarkAction = Action.Builder()
            .setIcon(
                CarIcon.Builder(
                    IconCompat.createWithResource(
                        carContext,
                        // Đổi icon dựa trên trạng thái (Tô đậm nếu đã lưu, viền trống nếu chưa)
                        if (isBookmarked) R.drawable.baseline_star_24 else R.drawable.outline_bookmark_add_24
                    )
                ).build()
            )
            .setOnClickListener {
                // Gọi hàm xử lý logic lưu trữ
                toggleBookmark()
            }
            .build()

        return PaneTemplate.Builder(
            Pane.Builder()
                .addAction(navigateAction)
                .addRow(
                    Row.Builder()
                        .setTitle("Coordinates")
                        .addText("${place.latitude}, ${place.longitude}")
                        .build()
                ).addRow(
                    Row.Builder()
                        .setTitle("Description")
                        .addText(place.description)
                        .build()
                ).build()
        )
            .setTitle(place.name)
            .setHeaderAction(Action.BACK)
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(bookmarkAction)
                    .build()
            )
            .build()
    }
}