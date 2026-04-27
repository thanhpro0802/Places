package com.example.places.carappservice

import android.content.Intent
import android.util.Log
import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.SessionInfo
import androidx.car.app.connection.CarConnection
import androidx.car.app.validation.HostValidator
import androidx.car.app.CarToast
import androidx.car.app.Screen
import com.example.places.carappservice.screen.MainScreen


class PlacesCarAppService : CarAppService() {

    override fun createHostValidator(): HostValidator {
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(sessionInfo: SessionInfo): Session {
        return PlacesSession()
    }
}

class PlacesSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        CarConnection(carContext).type.observe(this, ::onConnectionStateUpdated)
        return MainScreen(carContext)
    }

    // Xử lý deep link khi app đang chạy (ví dụ: mở từ geo: URL)
    override fun onNewIntent(intent: Intent) {
        Log.d("PlacesSession", "onNewIntent: ${intent.data}")
        // Nếu nhận geo: intent, quay về MainScreen (xóa back stack)
        if (intent.data?.scheme == "geo") {
            screenManager.popToRoot()
        }
    }

    private fun onConnectionStateUpdated(connectionState: Int) {
        val message = when (connectionState) {
            CarConnection.CONNECTION_TYPE_NOT_CONNECTED -> "Chưa kết nối với xe"
            CarConnection.CONNECTION_TYPE_NATIVE -> "Đang chạy trên Android Automotive OS"
            CarConnection.CONNECTION_TYPE_PROJECTION -> "Đang chạy trên Android Auto"
            else -> "Không rõ loại kết nối"
        }
        CarToast.makeText(carContext, message, CarToast.LENGTH_SHORT).show()
    }
}
