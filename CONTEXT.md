# Places (Android Automotive) — AI Developer Context

Chào AI (Cursor / GitHub Copilot / Antigravity / LLM agent), đây là tệp ngữ cảnh dành riêng khi hỗ trợ viết code cho dự án **Places — Android Automotive OS App**. Đọc kỹ và tuân thủ các quy định dưới đây trước khi sinh code.

---

## 1. Nền tảng & Mục tiêu

- **Nền tảng target:** Android Automotive OS (AAOS) — **không phải Android phone**.
- **Min SDK:** API 29 (Android 10 Q). Mọi ô tô hỗ trợ Car App Library đều chạy API 29+.
- **Thư viện cốt lõi:** [Car App Library](https://developer.android.com/training/cars/apps) (`androidx.car.app`).
- **Ngôn ngữ:** Kotlin (100%). TUYỆT ĐỐI không tạo file Java mới.
- **Build system:** Gradle KTS (`.gradle.kts`). KHÔNG dùng Groovy DSL.

---

## 2. Kiến trúc Module

Dự án gồm 3 module — **KHÔNG được trộn lẫn logic giữa các module**:

| Module | Vai trò | Được phép dùng |
|---|---|---|
| `:automotive` | Manifest + entry point AAOS | Chỉ AndroidManifest, res — không có Kotlin source |
| `:common:data` | Data layer — model, repository, cache | Không có Android UI dependency |
| `:common:car-app-service` | Car App Library UI — screens, service | Được dùng `car.app`, `:common:data` |

> `:app` (phone companion) **không phải target chính** — không cần phát triển thêm.

---

## 3. Car App Library — Quy tắc bắt buộc

### 3.1 Template giới hạn
Car App Library **bị giới hạn nghiêm ngặt** bởi Google để an toàn lái xe:
- `PlaceListMapTemplate`: tối đa **6 Row**
- `PaneTemplate`: tối đa **2 Row** + **2 Action**
- `SearchTemplate`: tối đa **6 Row** kết quả
- **KHÔNG được** dùng Jetpack Compose hay View system — chỉ dùng Car App Library Templates.

### 3.2 Màn hình (Screen)
- Mỗi màn hình là một class kế thừa `Screen(carContext)`.
- Điều hướng qua `screenManager.push(Screen)` và `screenManager.pop()`.
- Gọi `invalidate()` để yêu cầu render lại sau khi state thay đổi.
- **KHÔNG được** gọi network/IO trong `onGetTemplate()` — chỉ đọc state đã có sẵn.

### 3.3 Coroutines trong Screen
- Dùng `lifecycleScope.launch { }` để gọi suspend function (network, IO).
- Sau khi có data: cập nhật state variable → gọi `invalidate()`.

### 3.4 Quyền vị trí
- Xin quyền qua `carContext.requestPermissions(listOf(...)) { granted, _ -> }`.
- **KHÔNG dùng** `ActivityCompat.requestPermissions` — sẽ crash trên xe.

---

## 4. Data Layer — PlacesRepository & PlacesCache

### 4.1 Singleton Cache — BẮT BUỘC
```kotlin
// ĐÚNG — dùng PlacesCache singleton
val place = PlacesCache.places.find { it.id == placeId }

// SAI — tạo instance mới mất cache
val place = PlacesRepository().getPlace(placeId) // ❌
```

`PlacesCache` là `object` Kotlin — sống suốt vòng đời process. Mọi thay đổi vào `placeList` phải cập nhật `PlacesCache.places`.

### 4.2 Nguồn dữ liệu — Overpass API
- Endpoint: `https://overpass.private.coffee/api/interpreter` (POST, form-encoded)
- Bán kính tìm kiếm: **1500m** quanh vị trí xe
- Các loại địa điểm hỗ trợ: `restaurant`, `fuel`, `cafe`, `hospital`, `atm`, `supermarket`
- Khi `amenity` null, fallback sang `tags.shop` (dành cho supermarket)
- Timeout mạng: connect 30s, read 60s

### 4.3 Fallback offline
Khi network lỗi hoặc GPS null, gọi `getPlaces()` trả 3 địa điểm Hà Nội mẫu.

---

## 5. FilterType — Bộ lọc địa điểm

```kotlin
enum class FilterType { ALL, RESTAURANT, FUEL, CAFE, HOSPITAL, ATM }
```

- Filter mặc định: `ALL`
- Logic lọc: so sánh `place.description.contains(keyword, ignoreCase = true)`
- Toggle: bấm filter đang active → quay về `ALL`
- Hiển thị prefix `✓` khi filter đang được chọn

**Khi thêm loại địa điểm mới:**
1. Thêm `FilterType` enum value
2. Thêm query node vào `getPlacesNearby()` trong `PlacesRepository`
3. Thêm `when` branch trong MainScreen filter logic
4. Thêm `buildFilterAction()` vào ActionStrip

---

## 6. Màn hình hiện có

| Screen | Template | Chức năng |
|---|---|---|
| `MainScreen` | `PlaceListMapTemplate` | Bản đồ + danh sách + bộ lọc + nút tìm kiếm |
| `DetailScreen` | `PaneTemplate` | Tên, tọa độ, loại, Navigate, Bookmark |
| `SearchScreen` | `SearchTemplate` | Tìm kiếm offline từ `PlacesCache` |

---

## 7. Deep Link (geo: scheme)

- `AndroidManifest.xml` (:automotive) khai báo `intent-filter` cho `android.intent.action.VIEW` với `data android:scheme="geo"`.
- `PlacesSession.onNewIntent()` xử lý intent mới khi app đang chạy:
  - Nếu scheme là `geo:` → `screenManager.popToRoot()`

---

## 8. Navigation Intent (Chỉ đường)

```kotlin
// ĐÚNG — dùng CarContext.ACTION_NAVIGATE trên xe
carContext.startCarApp(place.toIntent(CarContext.ACTION_NAVIGATE))

// SAI — Intent.ACTION_VIEW không hoạt động trên xe ❌
```

---

## 9. AndroidManifest (:automotive) — Quy tắc cứng

- `android:theme` **KHÔNG được** đặt trên thẻ `<application>` (xóa nếu có).
- `CarAppActivity` PHẢI có `android:theme="@android:style/Theme.DeviceDefault.NoActionBar"`.
- `CarAppActivity` PHẢI có `android:launchMode="singleTask"`.
- `CarAppService` khai báo `category` là `androidx.car.app.category.POI` (Points of Interest).
- `meta-data distractionOptimized = true` PHẢI có trong `CarAppActivity`.

---

## 10. Những điều TUYỆT ĐỐI KHÔNG làm

- ❌ Không dùng Jetpack Compose trong module `:common:car-app-service` hay `:automotive`
- ❌ Không tạo `PlacesRepository()` instance mới để đọc cache — luôn dùng `PlacesCache`
- ❌ Không gọi `ActivityCompat.requestPermissions` — dùng `carContext.requestPermissions`
- ❌ Không hardcode tọa độ GPS hay URL API vào UI layer
- ❌ Không thêm `android:theme` vào thẻ `<application>` trong manifest automotive
- ❌ Không vượt quá giới hạn Row/Action của từng Template

---

> **Trách nhiệm của AI:** Sinh code đúng Car App Library API, an toàn với giới hạn của AAOS, tuyệt đối không dùng Android View/Compose. Code phải chạy được trên Automotive Emulator API 29+ với Google Automotive App Host.
