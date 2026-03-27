package com.example.places.data

import android.util.Log // Đã thêm import Log
import com.example.places.data.model.OverpassResponse
import com.example.places.data.model.Place
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// 1. Giao thức POST chuẩn chỉnh của bạn
interface OverpassService {
    @POST("interpreter")
    @FormUrlEncoded
    suspend fun getNearby(@Field("data") query: String): OverpassResponse
}

class PlacesRepository {

    // 2. KHÔI PHỤC BIẾN LƯU TRỮ BỊ THIẾU
    companion object {
        var cachedPlaces: List<Place> = emptyList()
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val service = Retrofit.Builder()
        .baseUrl("https://overpass.private.coffee/api/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OverpassService::class.java)

    suspend fun getPlacesNearby(lat: Double, lng: Double): List<Place> {
        val osmQuery = """
            [out:json][timeout:60];
            (
              node(around:1500,$lat,$lng)[amenity=restaurant];
              node(around:1500,$lat,$lng)[amenity=fuel];
              node(around:1500,$lat,$lng)[amenity=cafe];
            );
            out;
        """.trimIndent()

        return try {
            val response = service.getNearby(osmQuery)
            Log.d("OSM", "Tổng elements: ${response.elements.size}")

            val realPlaces = response.elements
                .filter { it.tags?.name != null }
                .map { element ->
                    Place(
                        // Xử lý mã ID bằng hashCode() để tránh lỗi tràn số âm
                        id = element.id.hashCode(),
                        name = element.tags?.name ?: "",
                        description = "Loại: ${element.tags?.amenity ?: "N/A"}",
                        latitude = element.lat,
                        longitude = element.lon
                    )
                }

            Log.d("OSM", "Sau khi lọc: ${realPlaces.size}")

            // Lưu vào biến tĩnh
            cachedPlaces = realPlaces
            realPlaces

        } catch (e: Exception) {
            Log.e("OSM", "Lỗi: ${e.message}")
            // Gọi hàm dự phòng khi lỗi
            getPlaces()
        }
    }

    // 3. KHÔI PHỤC HÀM DỰ PHÒNG BỊ THIẾU
    fun getPlaces(): List<Place> {
        val dummyList = listOf(
            Place(0, "Đại học Bách khoa Hà Nội", "Dữ liệu mẫu", 21.005588, 105.843442),
            Place(1, "Hồ Hoàn Kiếm", "Dữ liệu mẫu", 21.028511, 105.852319),
            Place(2, "Tòa nhà FPT Tower", "Dữ liệu mẫu", 21.027600, 105.792300)
        )
        cachedPlaces = dummyList
        return dummyList
    }

    // 4. KHÔI PHỤC HÀM TÌM KIẾM CHO DETAIL SCREEN
    fun getPlace(placeId: Int): Place? {
        return cachedPlaces.find { it.id == placeId }
    }
}