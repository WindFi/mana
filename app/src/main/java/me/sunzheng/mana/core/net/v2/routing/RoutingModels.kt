package me.sunzheng.mana.core.net.v2.routing

import com.google.gson.annotations.SerializedName

/**
 * mira routing API v1 的报文模型（契约见需求文档 §4）。
 *
 * 容错原则：所有字段可选、未知取值一律容忍（服务端可能新增 availability 取值）。
 */
data class BackendDto(
    @SerializedName("id") val id: String,
    @SerializedName("label") val label: String? = null,
    @SerializedName("region") val region: String? = null,
    @SerializedName("availability") val availability: String? = null,
    /** 缺省视为 true（见 FR-2）。 */
    @SerializedName("selectable") val selectable: Boolean? = null
) {
    val isSelectable: Boolean get() = selectable != false

    val isOffline: Boolean get() = availability == AVAILABILITY_OFFLINE

    /** 展示用名称：label 缺失时回落到 id。 */
    val displayLabel: String get() = label?.takeIf { it.isNotBlank() } ?: id

    companion object {
        const val AVAILABILITY_HEALTHY = "healthy"
        const val AVAILABILITY_OFFLINE = "offline"
    }
}

data class BackendsResponse(
    @SerializedName("version") val version: Int? = null,
    @SerializedName("backends") val backends: List<BackendDto>? = null
)

data class RoutePreference(
    /** "auto" | "backend"。 */
    @SerializedName("mode") val mode: String,
    @SerializedName("backendId") val backendId: String? = null
)

data class RouteRequest(
    /** 只含 path + query，不含 scheme/host。 */
    @SerializedName("resource") val resource: String,
    @SerializedName("preference") val preference: RoutePreference,
    @SerializedName("excludeBackendIds") val excludeBackendIds: List<String> = emptyList()
)

data class SelectionDto(
    @SerializedName("mode") val mode: String? = null,
    @SerializedName("reason") val reason: String? = null
)

data class RouteResponse(
    @SerializedName("routeToken") val routeToken: String? = null,
    @SerializedName("playbackUrl") val playbackUrl: String? = null,
    @SerializedName("selectedBackend") val selectedBackend: BackendDto? = null,
    @SerializedName("selection") val selection: SelectionDto? = null,
    @SerializedName("issuedAt") val issuedAt: String? = null,
    @SerializedName("freshUntil") val freshUntil: String? = null,
    @SerializedName("expiresAt") val expiresAt: String? = null
)
