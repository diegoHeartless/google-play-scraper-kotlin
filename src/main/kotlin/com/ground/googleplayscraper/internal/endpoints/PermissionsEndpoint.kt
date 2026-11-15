package com.ground.googleplayscraper.internal.endpoints

import com.ground.googleplayscraper.DEFAULT_COUNTRY
import com.ground.googleplayscraper.DEFAULT_LANG
import com.ground.googleplayscraper.GpConstants
import com.ground.googleplayscraper.internal.BatchExecute
import com.ground.googleplayscraper.internal.HttpClient
import com.ground.googleplayscraper.internal.JsonBuilder
import com.ground.googleplayscraper.internal.Paths
import com.ground.googleplayscraper.model.Permission

/**
 * Port of `lib/permissions.js`: reads the declared permissions of an app from
 * the `xdSrCf` batchexecute call.
 */
internal class PermissionsEndpoint(private val http: HttpClient) {

    fun permissions(
        appId: String,
        short: Boolean = false,
        lang: String = DEFAULT_LANG,
        country: String = DEFAULT_COUNTRY
    ): List<Permission> {
        require(appId.isNotBlank()) { "appId missing" }

        val response = BatchExecute.callRaw(
            http = http,
            rpcId = RPC_ID,
            payload = JsonBuilder.buildPermissionsRequest(appId),
            lang = lang,
            country = country
        ) ?: return emptyList()

        val data = BatchExecute.extractPayload(response) ?: return emptyList()
        return if (short) shortPermissions(data) else fullPermissions(data)
    }

    companion object {

        private const val RPC_ID = "xdSrCf"

        private val PERMISSION_TYPES = listOf(
            GpConstants.Permission.COMMON.raw,
            GpConstants.Permission.OTHER.raw,
            GpConstants.Permission.DEVICE.raw,
            GpConstants.Permission.BLUETOOTH.raw,
            GpConstants.Permission.NFC.raw,
            GpConstants.Permission.WIFI.raw
        )

        /** Maps the `[permission, type]` entries of every permission group. */
        fun fullPermissions(data: kotlinx.serialization.json.JsonElement): List<Permission> {
            val result = mutableListOf<Permission>()
            for (type in PERMISSION_TYPES) {
                val group = Paths.array(data, type.toInt()) ?: continue
                for (entry in group) {
                    val items = Paths.array(entry, 2) ?: continue
                    for (item in items) {
                        val name = Paths.string(item, 1) ?: continue
                        result.add(Permission(permission = name, type = type))
                    }
                }
            }
            return result
        }

        /** Returns only the permission names of the common group. */
        fun shortPermissions(data: kotlinx.serialization.json.JsonElement): List<Permission> {
            val common = Paths.array(data, GpConstants.Permission.COMMON.raw.toInt()) ?: return emptyList()
            return common
                .filter { (it as? kotlinx.serialization.json.JsonArray)?.isNotEmpty() == true }
                .mapNotNull { entry -> Paths.string(entry, 0) }
                .map { Permission(permission = it, type = GpConstants.Permission.COMMON.raw) }
        }
    }
}
