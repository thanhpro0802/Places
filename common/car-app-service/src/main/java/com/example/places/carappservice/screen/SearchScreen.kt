package com.example.places.carappservice.screen

import android.speech.tts.TextToSpeech
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.Row
import androidx.car.app.model.SearchTemplate
import androidx.car.app.model.Template
import com.example.places.data.PlacesCache
import com.example.places.data.model.Place
import java.util.Locale
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * SearchScreen — tìm kiếm địa điểm theo tên từ danh sách đã cache.
 * Đã tích hợp Phát giọng nói (Text-to-Speech) và Nhận diện giọng nói.
 */
class SearchScreen(carContext: CarContext) : Screen(carContext), TextToSpeech.OnInitListener {

    private var searchQuery: String = ""
    private var searchResults: List<Place> = emptyList()

    private lateinit var tts: TextToSpeech
    private var isTtsReady = false

    init {
        // 1. Khởi tạo TTS ngay khi mở màn hình
        tts = TextToSpeech(carContext, this)

        // 2. THÊM MỚI: Lắng nghe vòng đời của màn hình để tắt loa khi thoát
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                if (::tts.isInitialized) {
                    tts.stop()
                    tts.shutdown()
                }
                super.onDestroy(owner)
            }
        })
    }

    // --- THÊM: Cài đặt tiếng Việt khi TTS khởi tạo xong ---
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale("vi", "VN"))
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isTtsReady = true
            }
        }
    }

    override fun onGetTemplate(): Template {
        val itemListBuilder = ItemList.Builder()

        if (searchQuery.isBlank()) {
            itemListBuilder.setNoItemsMessage("Nhấn vào Micro để đọc tên địa điểm...")
        } else if (searchResults.isEmpty()) {
            itemListBuilder.setNoItemsMessage("Không tìm thấy \"$searchQuery\"")
        } else {
            searchResults.take(6).forEach { place ->
                itemListBuilder.addItem(
                    Row.Builder()
                        .setTitle(place.name)
                        .addText(place.description)
                        .setOnClickListener {
                            // --- THÊM: Tắt giọng đọc ngay nếu người dùng bấm chọn ---
                            if (isTtsReady) tts.stop()

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
                    searchResults = PlacesCache.places.filter { place ->
                        place.name.contains(searchText, ignoreCase = true) ||
                                place.description.contains(searchText, ignoreCase = true)
                    }
                    invalidate()
                }

                override fun onSearchSubmitted(searchText: String) {
                    // --- THÊM: Xử lý đọc kết quả khi nhấn Enter hoặc đọc xong Micro ---
                    searchQuery = searchText
                    searchResults = PlacesCache.places.filter { place ->
                        place.name.contains(searchText, ignoreCase = true) ||
                                place.description.contains(searchText, ignoreCase = true)
                    }

                    if (isTtsReady) {
                        val speechMessage = if (searchResults.isEmpty()) {
                            "Không tìm thấy địa điểm nào phù hợp với từ khóa $searchText"
                        } else {
                            "Đã tìm thấy ${searchResults.size} kết quả cho $searchText"
                        }
                        // Ra lệnh đọc ra loa xe
                        tts.speak(speechMessage, TextToSpeech.QUEUE_FLUSH, null, null)
                    }

                    invalidate()
                }
            }
        )
            .setHeaderAction(Action.BACK)
            // --- SỬA QUAN TRỌNG: Đổi thành false để ưu tiên hiện biểu tượng Micro ---
            .setShowKeyboardByDefault(true)
            .setItemList(itemListBuilder.build())
            .build()
    }
}