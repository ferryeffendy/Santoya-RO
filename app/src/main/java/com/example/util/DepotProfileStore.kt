package com.example.util

import android.content.Context
import com.example.data.entity.DepotProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Penyimpanan lokal offline-first untuk [DepotProfile] yang dipisahkan per UID pengguna
 * agar data profil depot langsung terbaca tanpa internet (saat mencetak struk/nota/PDF)
 * sekaligus menandai perubahan offline yang belum terkirim ke server.
 */
class DepotProfileStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _profileFlow = MutableStateFlow(DepotProfile())
    val profileFlow: StateFlow<DepotProfile> = _profileFlow.asStateFlow()

    private val _pendingSyncFlow = MutableStateFlow(false)
    val pendingSyncFlow: StateFlow<Boolean> = _pendingSyncFlow.asStateFlow()

    @Synchronized
    fun currentProfile(): DepotProfile = _profileFlow.value

    @Synchronized
    fun loadForUser(
        uid: String,
        email: String?,
        googleDisplayName: String?,
        appVersion: String,
        defaultRole: String = DepotProfile.ROLE_OWNER
    ): DepotProfile {
        if (uid.isBlank()) {
            _profileFlow.value = DepotProfile(appVersion = appVersion)
            _pendingSyncFlow.value = false
            return _profileFlow.value
        }

        val cleanEmail = email?.trim().orEmpty().take(160)
        val cleanGoogleName = googleDisplayName?.trim().orEmpty().take(120)
        val storedOwner = prefs.getString(keyOwnerName(uid), "").orEmpty().trim()
        val effectiveOwner = storedOwner.ifBlank { cleanGoogleName }.take(120)
        val storedDepotName = prefs.getString(keyDepotName(uid), "").orEmpty().trim().take(100)
        val storedPhone = DepotProfile.normalizePhoneInput(prefs.getString(keyPhone(uid), "").orEmpty())
        val storedAddress = prefs.getString(keyAddress(uid), "").orEmpty().trim().take(300)
        val storedFooter = prefs.getString(keyFooter(uid), "").orEmpty().trim().take(200)
        val storedRole = prefs.getString(keyRole(uid), defaultRole).orEmpty()
            .let { if (it == DepotProfile.ROLE_STAFF) DepotProfile.ROLE_STAFF else DepotProfile.ROLE_OWNER }
        val storedTier = prefs.getString(keyTier(uid), DepotProfile.TIER_FREE).orEmpty()
            .let { if (it.equals(DepotProfile.TIER_PRO, ignoreCase = true)) DepotProfile.TIER_PRO else DepotProfile.TIER_FREE }
        val storedType = prefs.getString(keyDepotType(uid), DepotProfile.TYPE_RO).orEmpty()
            .let {
                when (it.uppercase()) {
                    DepotProfile.TYPE_MINERAL -> DepotProfile.TYPE_MINERAL
                    DepotProfile.TYPE_BOTH -> DepotProfile.TYPE_BOTH
                    else -> DepotProfile.TYPE_RO
                }
            }
        val storedCapacity = prefs.getFloat(keyTankCapacity(uid), DepotProfile.DEFAULT_TANK_CAPACITY_LITERS.toFloat()).toDouble()
        val storedStock = prefs.getFloat(keyTankStock(uid), 0f).toDouble()

        val profile = DepotProfile(
            uid = uid,
            email = cleanEmail,
            googleDisplayName = cleanGoogleName,
            ownerName = effectiveOwner,
            depotName = storedDepotName,
            phoneNumber = storedPhone,
            address = storedAddress,
            receiptFooter = storedFooter,
            role = storedRole,
            subscriptionTier = storedTier,
            appVersion = appVersion.take(40),
            depotType = storedType,
            tankCapacityLiters = storedCapacity,
            currentTankStockLiters = storedStock
        )

        persistProfileFields(uid, profile)
        _profileFlow.value = profile
        _pendingSyncFlow.value = prefs.getBoolean(keyPendingSync(uid), false)
        return profile
    }

    @Synchronized
    fun saveOwnerEdit(
        uid: String,
        ownerName: String,
        depotName: String,
        phoneNumber: String,
        address: String,
        receiptFooter: String,
        depotType: String = _profileFlow.value.depotType,
        tankCapacityLiters: Double = _profileFlow.value.tankCapacityLiters
    ): DepotProfile {
        val current = _profileFlow.value
        val targetUid = uid.ifBlank { current.uid }
        val cleanOwner = ownerName.trim().ifBlank { current.googleDisplayName.trim() }.take(120)
        val normalizedType = when (depotType.trim().uppercase()) {
            DepotProfile.TYPE_MINERAL -> DepotProfile.TYPE_MINERAL
            DepotProfile.TYPE_BOTH -> DepotProfile.TYPE_BOTH
            else -> DepotProfile.TYPE_RO
        }
        val safeCapacity = tankCapacityLiters.coerceAtLeast(100.0)
        val updated = current.copy(
            uid = targetUid,
            ownerName = cleanOwner,
            depotName = depotName.trim().take(100),
            phoneNumber = DepotProfile.normalizePhoneInput(phoneNumber),
            address = address.trim().take(300),
            receiptFooter = receiptFooter.trim().take(200),
            role = DepotProfile.ROLE_OWNER,
            depotType = normalizedType,
            tankCapacityLiters = safeCapacity
        )
        if (targetUid.isNotBlank()) {
            persistProfileFields(targetUid, updated)
            prefs.edit().putBoolean(keyPendingSync(targetUid), true).apply()
            _pendingSyncFlow.value = true
        }
        _profileFlow.value = updated
        return updated
    }

    @Synchronized
    fun adjustTankStock(uid: String, deltaLiters: Double): DepotProfile {
        val current = _profileFlow.value
        val targetUid = uid.ifBlank { current.uid }
        val newStock = (current.currentTankStockLiters + deltaLiters).coerceAtLeast(0.0)
        val updated = current.copy(currentTankStockLiters = newStock)
        if (targetUid.isNotBlank()) {
            prefs.edit().putFloat(keyTankStock(targetUid), newStock.toFloat()).apply()
        }
        _profileFlow.value = updated
        return updated
    }

    @Synchronized
    fun setTankStock(uid: String, newStockLiters: Double, newCapacityLiters: Double? = null): DepotProfile {
        val current = _profileFlow.value
        val targetUid = uid.ifBlank { current.uid }
        val safeStock = newStockLiters.coerceAtLeast(0.0)
        val safeCapacity = newCapacityLiters?.coerceAtLeast(100.0) ?: current.tankCapacityLiters
        val updated = current.copy(
            currentTankStockLiters = safeStock,
            tankCapacityLiters = safeCapacity
        )
        if (targetUid.isNotBlank()) {
            prefs.edit()
                .putFloat(keyTankStock(targetUid), safeStock.toFloat())
                .putFloat(keyTankCapacity(targetUid), safeCapacity.toFloat())
                .apply()
        }
        _profileFlow.value = updated
        return updated
    }

    @Synchronized
    fun kurasToren(uid: String): DepotProfile {
        return setTankStock(uid = uid, newStockLiters = 0.0)
    }

    @Synchronized
    fun updateRole(uid: String, role: String): DepotProfile {
        val normalizedRole = if (role.equals(DepotProfile.ROLE_STAFF, ignoreCase = true)) {
            DepotProfile.ROLE_STAFF
        } else {
            DepotProfile.ROLE_OWNER
        }
        val current = _profileFlow.value
        val updated = if (current.uid == uid) {
            current.copy(role = normalizedRole)
        } else {
            current
        }
        if (uid.isNotBlank()) {
            prefs.edit().putString(keyRole(uid), normalizedRole).apply()
        }
        if (current.uid == uid) {
            _profileFlow.value = updated
        }
        return updated
    }

    @Synchronized
    fun applyRemoteUserProfile(uid: String, data: Map<String, Any?>): DepotProfile {
        val current = _profileFlow.value
        if (uid.isBlank() || (current.uid.isNotBlank() && current.uid != uid)) {
            return current
        }

        val remoteTier = (data["subscriptionTier"] as? String)?.trim()?.uppercase()
            ?.let { if (it == DepotProfile.TIER_PRO) DepotProfile.TIER_PRO else DepotProfile.TIER_FREE }
            ?: current.subscriptionTier

        val hasPending = hasPendingSync(uid)
        val remoteOwner = (data["ownerName"] as? String)?.trim().orEmpty().take(120)
        val remoteDepot = (data["depotName"] as? String)?.trim().orEmpty().take(100)
        val remotePhone = (data["phoneNumber"] as? String)?.trim().orEmpty().take(40)
        val remoteAddress = (data["address"] as? String)?.trim().orEmpty().take(300)
        val remoteFooter = (data["receiptFooter"] as? String)?.trim().orEmpty().take(200)

        val remoteType = (data["depotType"] as? String)?.trim()?.uppercase()
            ?.let {
                when (it) {
                    DepotProfile.TYPE_MINERAL -> DepotProfile.TYPE_MINERAL
                    DepotProfile.TYPE_BOTH -> DepotProfile.TYPE_BOTH
                    else -> DepotProfile.TYPE_RO
                }
            } ?: current.depotType
        val remoteCapacity = (data["tankCapacityLiters"] as? Number)?.toDouble() ?: current.tankCapacityLiters
        val remoteStock = (data["currentTankStockLiters"] as? Number)?.toDouble() ?: current.currentTankStockLiters

        val updated = if (hasPending) {
            current.copy(
                subscriptionTier = remoteTier,
                currentTankStockLiters = remoteStock
            )
        } else {
            current.copy(
                ownerName = remoteOwner.ifBlank { current.effectiveOwnerName() },
                depotName = remoteDepot.ifBlank { current.depotName },
                phoneNumber = remotePhone.ifBlank { current.phoneNumber },
                address = remoteAddress.ifBlank { current.address },
                receiptFooter = remoteFooter.ifBlank { current.receiptFooter },
                subscriptionTier = remoteTier,
                depotType = remoteType,
                tankCapacityLiters = remoteCapacity,
                currentTankStockLiters = remoteStock
            )
        }

        persistProfileFields(uid, updated)
        _profileFlow.value = updated
        return updated
    }

    @Synchronized
    fun applyRemoteDepotProfile(uid: String, role: String, data: Map<String, Any?>): DepotProfile {
        val current = _profileFlow.value
        if (uid.isBlank() || (current.uid.isNotBlank() && current.uid != uid)) {
            return current
        }

        val isStaff = role.equals(DepotProfile.ROLE_STAFF, ignoreCase = true)
        if (!isStaff && hasPendingSync(uid)) {
            return current
        }

        val hasExplicitDepotName = data.containsKey("depotName")
        val remoteDepotName = if (hasExplicitDepotName) {
            (data["depotName"] as? String)?.trim().orEmpty().take(100)
        } else {
            val fallbackName = (data["name"] as? String)?.trim().orEmpty()
            if (fallbackName.equals("Santoya RO", ignoreCase = true) ||
                fallbackName.equals(DepotProfile.DEFAULT_DEPOT_NAME, ignoreCase = true)
            ) {
                ""
            } else {
                fallbackName.take(100)
            }
        }

        val remoteOwnerName = (data["ownerName"] as? String)?.trim().orEmpty().take(120)
        val remotePhone = (data["phoneNumber"] as? String)?.trim().orEmpty().take(40)
        val remoteAddress = (data["address"] as? String)?.trim().orEmpty().take(300)
        val remoteFooter = (data["receiptFooter"] as? String)?.trim().orEmpty().take(200)
        val remoteTier = (data["subscriptionTier"] as? String)?.trim()?.uppercase()
            ?.let { if (it == DepotProfile.TIER_PRO) DepotProfile.TIER_PRO else DepotProfile.TIER_FREE }
        val remoteType = (data["depotType"] as? String)?.trim()?.uppercase()
            ?.let {
                when (it) {
                    DepotProfile.TYPE_MINERAL -> DepotProfile.TYPE_MINERAL
                    DepotProfile.TYPE_BOTH -> DepotProfile.TYPE_BOTH
                    else -> DepotProfile.TYPE_RO
                }
            }
        val remoteCapacity = (data["tankCapacityLiters"] as? Number)?.toDouble()
        val remoteStock = (data["currentTankStockLiters"] as? Number)?.toDouble()

        val updated = if (isStaff) {
            current.copy(
                role = DepotProfile.ROLE_STAFF,
                ownerName = remoteOwnerName.ifBlank { current.ownerName },
                depotName = if (hasExplicitDepotName) remoteDepotName else remoteDepotName.ifBlank { current.depotName },
                phoneNumber = if (data.containsKey("phoneNumber")) remotePhone else current.phoneNumber,
                address = if (data.containsKey("address")) remoteAddress else current.address,
                receiptFooter = if (data.containsKey("receiptFooter")) remoteFooter else current.receiptFooter,
                subscriptionTier = remoteTier ?: current.subscriptionTier,
                depotType = remoteType ?: current.depotType,
                tankCapacityLiters = remoteCapacity ?: current.tankCapacityLiters,
                currentTankStockLiters = remoteStock ?: current.currentTankStockLiters
            )
        } else {
            current.copy(
                ownerName = current.ownerName.ifBlank { remoteOwnerName }.ifBlank { current.googleDisplayName },
                depotName = current.depotName.ifBlank { remoteDepotName },
                phoneNumber = current.phoneNumber.ifBlank { remotePhone },
                address = current.address.ifBlank { remoteAddress },
                receiptFooter = current.receiptFooter.ifBlank { remoteFooter },
                subscriptionTier = remoteTier ?: current.subscriptionTier,
                depotType = remoteType ?: current.depotType,
                tankCapacityLiters = remoteCapacity ?: current.tankCapacityLiters,
                currentTankStockLiters = remoteStock ?: current.currentTankStockLiters
            )
        }

        persistProfileFields(uid, updated)
        _profileFlow.value = updated
        return updated
    }

    @Synchronized
    fun hasPendingSync(uid: String): Boolean {
        if (uid.isBlank()) return false
        return prefs.getBoolean(keyPendingSync(uid), false)
    }

    @Synchronized
    fun markPendingSync(uid: String, pending: Boolean) {
        if (uid.isBlank()) return
        prefs.edit().putBoolean(keyPendingSync(uid), pending).apply()
        if (_profileFlow.value.uid == uid || _profileFlow.value.uid.isBlank()) {
            _pendingSyncFlow.value = pending
        }
    }

    @Synchronized
    fun clearActiveSession() {
        _profileFlow.value = DepotProfile()
        _pendingSyncFlow.value = false
    }

    /**
     * Menggabungkan profil depot dari berkas cadangan JSON (saat pemulihan data oleh Pemilik)
     * ke profil aktif, mengisi field yang masih kosong atau memperbarui identitas depot.
     */
    @Synchronized
    fun mergeFromBackupJson(
        uid: String,
        ownerName: String?,
        depotName: String?,
        phoneNumber: String?,
        address: String?,
        receiptFooter: String?,
        depotType: String?,
        tankCapacityLiters: Double?
    ): DepotProfile {
        val current = _profileFlow.value
        val targetUid = uid.ifBlank { current.uid }
        if (!current.isOwnerRole()) return current

        val incomingOwner = ownerName?.trim().orEmpty().take(120)
        val incomingDepot = depotName?.trim().orEmpty().take(100)
        val incomingPhone = DepotProfile.normalizePhoneInput(phoneNumber.orEmpty())
        val incomingAddress = address?.trim().orEmpty().take(300)
        val incomingFooter = receiptFooter?.trim().orEmpty().take(200)
        val incomingType = depotType?.trim()?.uppercase()?.let {
            when (it) {
                DepotProfile.TYPE_MINERAL -> DepotProfile.TYPE_MINERAL
                DepotProfile.TYPE_BOTH -> DepotProfile.TYPE_BOTH
                DepotProfile.TYPE_RO -> DepotProfile.TYPE_RO
                else -> null
            }
        }
        val incomingCap = tankCapacityLiters?.takeIf { it.isFinite() && it >= 100.0 }

        val updated = current.copy(
            uid = targetUid,
            ownerName = current.ownerName.ifBlank { incomingOwner }.ifBlank { current.googleDisplayName },
            depotName = current.depotName.ifBlank { incomingDepot },
            phoneNumber = current.phoneNumber.ifBlank { incomingPhone },
            address = current.address.ifBlank { incomingAddress },
            receiptFooter = current.receiptFooter.ifBlank { incomingFooter },
            depotType = if (!current.hasConfiguredDepotName() && incomingType != null) incomingType else current.depotType,
            tankCapacityLiters = if (!current.hasConfiguredDepotName() && incomingCap != null) incomingCap else current.tankCapacityLiters
        )

        if (updated != current && targetUid.isNotBlank()) {
            persistProfileFields(targetUid, updated)
            prefs.edit().putBoolean(keyPendingSync(targetUid), true).apply()
            _pendingSyncFlow.value = true
            _profileFlow.value = updated
        }
        return _profileFlow.value
    }

    /**
     * Menggabungkan profil depot yang diisi selama Mode Offline ke profil akun Google tujuan
     * tanpa menimpa data akun Google yang sudah terisi sebelumnya.
     */
    @Synchronized
    fun mergeFromOfflineProfile(
        offlineUid: String,
        targetUid: String,
        email: String?,
        googleDisplayName: String?,
        appVersion: String
    ): DepotProfile {
        val storedTargetOwnerBeforeLoad = prefs.getString(keyOwnerName(targetUid), "").orEmpty().trim()
        val currentTarget = loadForUser(
            uid = targetUid,
            email = email,
            googleDisplayName = googleDisplayName,
            appVersion = appVersion
        )
        if (offlineUid.isBlank() || targetUid.isBlank() || offlineUid == targetUid) {
            return currentTarget
        }

        val offOwner = prefs.getString(keyOwnerName(offlineUid), "").orEmpty().trim()
        val offDepot = prefs.getString(keyDepotName(offlineUid), "").orEmpty().trim()
        val offPhone = DepotProfile.normalizePhoneInput(prefs.getString(keyPhone(offlineUid), "").orEmpty())
        val offAddress = prefs.getString(keyAddress(offlineUid), "").orEmpty().trim()
        val offFooter = prefs.getString(keyFooter(offlineUid), "").orEmpty().trim()
        val offType = prefs.getString(keyDepotType(offlineUid), "").orEmpty().trim()
        val offCapacity = prefs.getFloat(keyTankCapacity(offlineUid), 0f).toDouble()
        val offStock = prefs.getFloat(keyTankStock(offlineUid), 0f).toDouble()
        val offPending = prefs.getBoolean(keyPendingSync(offlineUid), false)

        val hasOfflineEdits = offPending ||
            offDepot.isNotBlank() ||
            offPhone.isNotBlank() ||
            offAddress.isNotBlank() ||
            offFooter.isNotBlank() ||
            offStock > 0.0

        val mergedOwner = storedTargetOwnerBeforeLoad
            .ifBlank { offOwner }
            .ifBlank { currentTarget.ownerName }
            .ifBlank { currentTarget.googleDisplayName }
        val mergedDepot = currentTarget.depotName.ifBlank { offDepot }
        val mergedPhone = currentTarget.phoneNumber.ifBlank { offPhone }
        val mergedAddress = currentTarget.address.ifBlank { offAddress }
        val mergedFooter = currentTarget.receiptFooter.ifBlank { offFooter }
        val mergedType = if (offType.isNotBlank() && offPending) offType else currentTarget.depotType
        val mergedCapacity = if (offCapacity >= 100.0 && offPending) offCapacity else currentTarget.tankCapacityLiters
        val mergedStock = if (offStock > 0.0) {
            if (currentTarget.currentTankStockLiters > 0.0) currentTarget.currentTankStockLiters else offStock
        } else {
            currentTarget.currentTankStockLiters
        }

        val merged = currentTarget.copy(
            ownerName = mergedOwner.take(120),
            depotName = mergedDepot.take(100),
            phoneNumber = mergedPhone.take(40),
            address = mergedAddress.take(300),
            receiptFooter = mergedFooter.take(200),
            depotType = mergedType,
            tankCapacityLiters = mergedCapacity,
            currentTankStockLiters = mergedStock
        )

        persistProfileFields(targetUid, merged)
        if (hasOfflineEdits) {
            prefs.edit().putBoolean(keyPendingSync(targetUid), true).apply()
            _pendingSyncFlow.value = true
        }
        _profileFlow.value = merged
        return merged
    }

    /**
     * Menghapus seluruh entri profil tersimpan untuk UID tertentu setelah proses migrasi selesai.
     */
    @Synchronized
    fun clearProfile(uid: String) {
        if (uid.isBlank()) return
        prefs.edit()
            .remove(keyOwnerName(uid))
            .remove(keyDepotName(uid))
            .remove(keyPhone(uid))
            .remove(keyAddress(uid))
            .remove(keyFooter(uid))
            .remove(keyRole(uid))
            .remove(keyTier(uid))
            .remove(keyDepotType(uid))
            .remove(keyTankCapacity(uid))
            .remove(keyTankStock(uid))
            .remove(keyPendingSync(uid))
            .apply()
        if (_profileFlow.value.uid == uid) {
            _pendingSyncFlow.value = false
        }
    }

    private fun persistProfileFields(uid: String, profile: DepotProfile) {
        prefs.edit()
            .putString(keyOwnerName(uid), profile.ownerName)
            .putString(keyDepotName(uid), profile.depotName)
            .putString(keyPhone(uid), profile.phoneNumber)
            .putString(keyAddress(uid), profile.address)
            .putString(keyFooter(uid), profile.receiptFooter)
            .putString(keyRole(uid), profile.role)
            .putString(keyTier(uid), profile.subscriptionTier)
            .putString(keyDepotType(uid), profile.depotType)
            .putFloat(keyTankCapacity(uid), profile.tankCapacityLiters.toFloat())
            .putFloat(keyTankStock(uid), profile.currentTankStockLiters.toFloat())
            .apply()
    }

    private fun keyOwnerName(uid: String) = "owner_name_$uid"
    private fun keyDepotName(uid: String) = "depot_name_$uid"
    private fun keyPhone(uid: String) = "phone_$uid"
    private fun keyAddress(uid: String) = "address_$uid"
    private fun keyFooter(uid: String) = "footer_$uid"
    private fun keyRole(uid: String) = "role_$uid"
    private fun keyTier(uid: String) = "tier_$uid"
    private fun keyDepotType(uid: String) = "depot_type_$uid"
    private fun keyTankCapacity(uid: String) = "tank_cap_$uid"
    private fun keyTankStock(uid: String) = "tank_stock_$uid"
    private fun keyPendingSync(uid: String) = "pending_sync_$uid"

    companion object {
        private const val PREFS_NAME = "santoya_depot_profile_prefs"
    }
}
