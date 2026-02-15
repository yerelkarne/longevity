package com.leosoft.longevity.steps

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord

class HealthConnectStepsManager(context: Context) {
    private val client = HealthConnectClient.getOrCreate(context)

    val permissions = setOf(HealthPermission.getReadPermission(StepsRecord::class))

    suspend fun hasPermissions(): Boolean {
        val granted = client.permissionController.getGrantedPermissions()
        return granted.containsAll(permissions)
    }

    fun permissionsContract() = PermissionController.createRequestPermissionResultContract()
}
