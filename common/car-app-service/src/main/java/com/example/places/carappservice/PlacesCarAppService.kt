package com.example.places.carappservice

import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.SessionInfo
import androidx.car.app.connection.CarConnection
import androidx.car.app.validation.HostValidator
import android.content.Intent
import androidx.car.app.CarToast
import androidx.car.app.Screen
import com.example.places.carappservice.screen.MainScreen


class PlacesCarAppService : CarAppService() {

    override fun createHostValidator(): HostValidator {
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(sessionInfo: SessionInfo): Session {
        // PlacesSession will be an unresolved reference until the next step
        return PlacesSession()
    }
}

class PlacesSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        CarConnection(carContext).type.observe(this, ::onConnectionStateUpdated)
        // MainScreen will be an unresolved reference until the next step
        return MainScreen(carContext)
    }

    // Hàm xử lý khi trạng thái kết nối thay đổi
    private fun onConnectionStateUpdated(connectionState: Int) {
        val message = when(connectionState) {
            CarConnection.CONNECTION_TYPE_NOT_CONNECTED -> "Chưa kết nối với xe"
            CarConnection.CONNECTION_TYPE_NATIVE -> "Đang chạy trên Android Automotive OS"
            CarConnection.CONNECTION_TYPE_PROJECTION -> "Đang chạy trên Android Auto"
            else -> "Không rõ loại kết nối"
        }
        // Hiển thị thông báo (Toast) lên màn hình xe
        CarToast.makeText(carContext, message, CarToast.LENGTH_SHORT).show()
    }
}
