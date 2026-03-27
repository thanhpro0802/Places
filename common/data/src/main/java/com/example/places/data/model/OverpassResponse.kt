package com.example.places.data.model

import com.google.gson.annotations.SerializedName

data class OverpassResponse(
    @SerializedName("elements") val elements: List<Element>
)

data class Element(
    @SerializedName("id") val id: Long,
    @SerializedName("lat") val lat: Double,
    @SerializedName("lon") val lon: Double,
    @SerializedName("tags") val tags: Tags?
)

data class Tags(
    @SerializedName("name") val name: String?,
    @SerializedName("amenity") val amenity: String?,
    @SerializedName("shop") val shop: String?
)