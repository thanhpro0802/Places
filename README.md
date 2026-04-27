# Places — Android Automotive OS App

Ứng dụng **Places** hiển thị địa điểm gần nhất (nhà hàng, cây xăng, cafe, bệnh viện, ATM, siêu thị) trên màn hình ô tô chạy **Android Automotive OS** và **Android Auto**, sử dụng dữ liệu thực từ OpenStreetMap (Overpass API).

---

## 📐 Cấu trúc Module

```
Places/
├── app/                        # Android phone app (Compose UI) — demo companion
├── automotive/                 # 🚗 AAOS standalone APK (target chính)
│   └── src/main/
│       ├── AndroidManifest.xml # Khai báo CarAppActivity, deep link geo:
│       └── res/xml/
│           └── automotive_app_desc.xml
└── common/
    ├── data/                   # Data layer — dùng chung cả automotive & app
    │   └── src/main/java/.../data/
    │       ├── PlacesRepository.kt   # Retrofit + Overpass API
    │       ├── PlacesCache.kt        # Singleton cache (object)
    │       └── model/
    │           ├── Place.kt
    │           └── OverpassResponse.kt
    └── car-app-service/        # Car App Library logic — dùng chung
        └── src/main/java/.../carappservice/
            ├── PlacesCarAppService.kt   # CarAppService + PlacesSession
            └── screen/
                ├── MainScreen.kt        # Bản đồ + danh sách + bộ lọc
                ├── DetailScreen.kt      # Chi tiết địa điểm
                └── SearchScreen.kt      # Tìm kiếm theo tên
```

---

## ⚙️ Build & Chạy

### Yêu cầu
- Android Studio **Hedgehog** trở lên (hoặc **Bumblebee+**)
- SDK Android **API 29+** (Android 10)
- **Google Automotive App Host** cài trên emulator

### Build module automotive
```
# Trong Android Studio:
Run → Edit Configurations → Module: automotive → Run

# Hoặc Gradle CLI (cần ANDROID_HOME set):
./gradlew :automotive:assembleDebug
```

### Chạy trên Automotive Emulator
1. **AVD Manager** → tạo device loại **Automotive** (API 29+)
2. Cài **Google Automotive App Host** từ Play Store nếu emulator chưa có
3. Deploy module `:automotive`
4. App xuất hiện trong launcher của AAOS

> **Lưu ý:** Module `:app` chỉ là demo phone companion — mục tiêu chính là `:automotive`.

---

## 🗺️ Tính năng

| Tính năng | Mô tả |
|---|---|
| **Bản đồ + Danh sách** | `PlaceListMapTemplate` hiển thị địa điểm quanh xe kèm khoảng cách GPS thực |
| **Bộ lọc** | Quán ăn / Cây xăng / Cafe / Bệnh viện / ATM (toggle trên ActionStrip) |
| **Tìm kiếm** | `SearchTemplate` — tìm theo tên, lọc offline từ cache |
| **Chi tiết địa điểm** | `PaneTemplate` — tọa độ, mô tả, nút Navigate, Bookmark |
| **Chỉ đường** | Mở navigation qua `CarContext.ACTION_NAVIGATE` → Google Maps |
| **Deep link** | Nhận `geo:` intent từ app khác, quay về `MainScreen` |
| **Dự phòng offline** | 3 địa điểm Hà Nội mẫu khi không có mạng/GPS |

---

## 🌐 Nguồn dữ liệu — Overpass API

API endpoint: `https://overpass.private.coffee/api/interpreter`

Query tìm trong bán kính **1500m** quanh vị trí xe:
```
node[amenity=restaurant]
node[amenity=fuel]
node[amenity=cafe]
node[amenity=hospital]
node[amenity=atm]
node[shop=supermarket]
```

Timeout: 60s | Connect timeout: 30s

---

## 🧱 Các thư viện chính

| Thư viện | Version | Dùng cho |
|---|---|---|
| `androidx.car.app:app` | catalog | Car App Library (Android Auto) |
| `androidx.car.app:app-automotive` | catalog | AAOS CarAppActivity |
| `com.squareup.retrofit2:retrofit` | — | HTTP client Overpass API |
| `com.google.code.gson` | — | JSON parsing |
| `com.google.android.gms:play-services-location` | 21.0.1 | FusedLocationProviderClient |

---

## 📋 Checklist trước khi submit lên Play Store

- [ ] `android:label` đúng tên app trên `CarAppActivity`
- [ ] `android:theme="@android:style/Theme.DeviceDefault.NoActionBar"` trên `CarAppActivity`
- [ ] Không có `android:theme` trên thẻ `<application>` (xóa)
- [ ] `automotive_app_desc.xml` có `<uses name="template"/>`
- [ ] `uses-feature android:name="android.hardware.type.automotive" required="true"`
- [ ] `uses-feature android:name="android.software.car.templates_host" required="true"`
- [ ] Phân phối qua **Android Automotive OS channel** riêng trên Play Console
