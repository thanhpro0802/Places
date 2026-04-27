package com.example.places.carappservice.screen

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.Row
import androidx.car.app.model.SearchTemplate
import androidx.car.app.model.Template
import com.example.places.data.PlacesCache
import com.example.places.data.model.Place

/**
 * SearchScreen — tìm kiếm địa điểm theo tên từ danh sách đã cache.
 * Dùng SearchTemplate của Car App Library (API Level 1+).
 */
class SearchScreen(carContext: CarContext) : Screen(carContext) {

    private var searchQuery: String = ""
    private var searchResults: List<Place> = emptyList()

    override fun onGetTemplate(): Template {
        val itemListBuilder = ItemList.Builder()

        if (searchQuery.isBlank()) {
            // Chưa nhập gì — hiển thị gợi ý
            itemListBuilder.setNoItemsMessage("Nhập tên địa điểm để tìm kiếm")
        } else if (searchResults.isEmpty()) {
            itemListBuilder.setNoItemsMessage("Không tìm thấy \"$searchQuery\"")
        } else {
            // Hiển thị kết quả (Car App Library giới hạn tối đa 6 row)
            searchResults.take(6).forEach { place ->
                itemListBuilder.addItem(
                    Row.Builder()
                        .setTitle(place.name)
                        .addText(place.description)
                        .setOnClickListener {
                            screenManager.push(DetailScreen(carContext, place.id))
                        }
                        .build()
                )
            }
        }

        return SearchTemplate.Builder(
            object : SearchTemplate.SearchCallback {
                override fun onSearchTextChanged(searchText: String) {
                    searchQuery = searchText
                    // Lọc từ danh sách đã cache — không cần mạng
                    searchResults = PlacesCache.places.filter { place ->
                        place.name.contains(searchText, ignoreCase = true) ||
                            place.description.contains(searchText, ignoreCase = true)
                    }
                    invalidate()
                }

                override fun onSearchSubmitted(searchText: String) {
                    // Đã xử lý trong onSearchTextChanged — không cần thêm
                }
            }
        )
            .setHeaderAction(Action.BACK)
            .setShowKeyboardByDefault(true)
            .setItemList(itemListBuilder.build())
            .build()
    }
}
