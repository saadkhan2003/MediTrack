package com.meditrack.app.data.sync

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.meditrack.app.BuildConfig
import com.meditrack.app.data.local.dao.DoseLogDao
import com.meditrack.app.data.local.dao.MedicineDao
import com.meditrack.app.data.local.entity.DoseLogEntity
import com.meditrack.app.data.local.entity.MedicineEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreSyncService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val medicineDao: MedicineDao,
    private val doseLogDao: DoseLogDao
) {
    companion object {
        private const val TAG = "FirestoreSync"
        private const val COLLECTION_MEDICINES = "medicines"
        private const val COLLECTION_DOSE_LOGS = "doseLogs"
    }

    private val firebaseEnabled: Boolean = BuildConfig.FEATURE_FIREBASE

    private val firebaseAuth: FirebaseAuth? by lazy {
        if (!firebaseEnabled) return@lazy null
        runCatching {
            FirebaseApp.initializeApp(context)
            val auth = FirebaseAuth.getInstance()
            Log.d(TAG, "FirebaseAuth initialized, current user: ${auth.currentUser?.email}")
            auth
        }.getOrNull()
    }

    private val firestore: FirebaseFirestore? by lazy {
        if (!firebaseEnabled) return@lazy null
        runCatching {
            FirebaseApp.initializeApp(context)
            val fs = FirebaseFirestore.getInstance()
            val app = FirebaseApp.getInstance()
            Log.d(TAG, "Firestore initialized for project: ${app.options.projectId}")
            Log.d(TAG, "Firestore app name: ${app.name}")
            fs
        }.getOrNull()
    }

    private val uid: String?
        get() = firebaseAuth?.currentUser?.uid

    private var medicinesRegistration: ListenerRegistration? = null
    private var doseLogsRegistration: ListenerRegistration? = null
    private val realtimeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun userMedicinesRef() =
        uid?.let { id -> firestore?.collection("users")?.document(id)?.collection(COLLECTION_MEDICINES) }

    private fun userDoseLogsRef() =
        uid?.let { id -> firestore?.collection("users")?.document(id)?.collection(COLLECTION_DOSE_LOGS) }

    // ── Medicine Sync ──

    suspend fun syncMedicineToCloud(medicine: MedicineEntity) {
        if (!firebaseEnabled) {
            Log.w(TAG, "Skipping medicine sync: Firebase feature disabled")
            return
        }
        val currentUid = uid
        val ref = userMedicinesRef()
        if (ref == null) {
            Log.w(TAG, "Skipping medicine sync: no authenticated user (uid=$currentUid)")
            return
        }
        try {
            val docPath = "users/$currentUid/$COLLECTION_MEDICINES/${medicine.id}"
            Log.d(TAG, "Syncing medicine to path: $docPath")
            
            val data = mapOf(
                "id" to medicine.id,
                "name" to medicine.name,
                "dosage" to medicine.dosage,
                "frequency" to medicine.frequency,
                "scheduledTimes" to medicine.scheduledTimes,
                "startDate" to medicine.startDate,
                "endDate" to medicine.endDate,
                "totalStock" to medicine.totalStock,
                "remainingStock" to medicine.remainingStock,
                "refillThreshold" to medicine.refillThreshold,
                "notes" to medicine.notes,
                "color" to medicine.color,
                "isActive" to medicine.isActive,
                "createdAt" to medicine.createdAt
            )
            
            // Write to Firestore
            ref.document(medicine.id.toString()).set(data, SetOptions.merge()).await()
            Log.d(TAG, "✓ Write operation completed for medicine '${medicine.name}' at: $docPath")
            
            // VERIFY: Read it back to confirm it's actually there
            try {
                val verifyDoc = ref.document(medicine.id.toString()).get().await()
                if (verifyDoc.exists()) {
                    Log.d(TAG, "✓ VERIFICATION SUCCESS: Medicine '${medicine.name}' exists in Firestore")
                    Log.d(TAG, "  Data fields: ${verifyDoc.data?.keys?.joinToString()}")
                } else {
                    Log.e(TAG, "✗ VERIFICATION FAILED: Document was written but does not exist when reading back!")
                    Log.e(TAG, "  This indicates a Firestore Security Rules problem - rules may be blocking reads")
                }
            } catch (verifyError: Exception) {
                Log.e(TAG, "✗ VERIFICATION FAILED: Cannot read back document - likely Security Rules blocking reads", verifyError)
                Log.e(TAG, "  Error type: ${verifyError.javaClass.simpleName}")
                Log.e(TAG, "  Error message: ${verifyError.message}")
            }
            
            Log.d(TAG, "  Project: ${FirebaseApp.getInstance().options.projectId}")
        } catch (e: Exception) {
            Log.e(TAG, "✗ Failed to sync medicine to cloud at users/$currentUid/$COLLECTION_MEDICINES/${medicine.id}", e)
            Log.e(TAG, "  Error type: ${e.javaClass.simpleName}")
            Log.e(TAG, "  Error message: ${e.message}")
            if (e.message?.contains("PERMISSION_DENIED") == true) {
                Log.e(TAG, "  >> This is a SECURITY RULES issue - check your Firestore rules!")
            }
        }
    }

    suspend fun deleteMedicineFromCloud(medicineId: Int) {
        if (!firebaseEnabled) return
        val ref = userMedicinesRef() ?: return
        try {
            ref.document(medicineId.toString()).delete().await()
            Log.d(TAG, "Deleted medicine $medicineId from cloud")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete medicine from cloud: ${e.message}")
        }
    }

    // ── DoseLog Sync ──

    suspend fun syncDoseLogToCloud(doseLog: DoseLogEntity) {
        if (!firebaseEnabled) {
            Log.w(TAG, "Skipping dose log sync: Firebase feature disabled")
            return
        }
        val ref = userDoseLogsRef()
        if (ref == null) {
            Log.w(TAG, "Skipping dose log sync: no authenticated user (uid=${uid})")
            return
        }
        try {
            val data = mapOf(
                "id" to doseLog.id,
                "medicineId" to doseLog.medicineId,
                "medicineName" to doseLog.medicineName,
                "scheduledTime" to doseLog.scheduledTime,
                "loggedTime" to doseLog.loggedTime,
                "status" to doseLog.status
            )
            ref.document(doseLog.id.toString()).set(data, SetOptions.merge()).await()
            Log.d(TAG, "Synced dose log ${doseLog.id} to cloud")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync dose log to cloud", e)
        }
    }

    // ── Full Sync (on login / app start) ──

    suspend fun pullFromCloud() = withContext(Dispatchers.IO) {
        if (!firebaseEnabled) {
            Log.w(TAG, "Skipping cloud pull: Firebase feature disabled")
            return@withContext
        }
        val medicinesRef = userMedicinesRef()
        val doseLogsRef = userDoseLogsRef()
        if (medicinesRef == null || doseLogsRef == null) {
            Log.w(TAG, "Skipping cloud pull: no authenticated user (uid=${uid})")
            return@withContext
        }

        try {
            // Pull medicines
            val medicineSnaps = medicinesRef.get().await()
            for (doc in medicineSnaps.documents) {
                val entity = MedicineEntity(
                    id = (doc.getLong("id") ?: 0).toInt(),
                    name = doc.getString("name") ?: "",
                    dosage = doc.getString("dosage") ?: "",
                    frequency = doc.getString("frequency") ?: "ONCE_DAILY",
                    scheduledTimes = doc.getString("scheduledTimes") ?: "[]",
                    startDate = doc.getLong("startDate") ?: System.currentTimeMillis(),
                    endDate = doc.getLong("endDate"),
                    totalStock = (doc.getLong("totalStock") ?: 30).toInt(),
                    remainingStock = (doc.getLong("remainingStock") ?: 30).toInt(),
                    refillThreshold = (doc.getLong("refillThreshold") ?: 5).toInt(),
                    notes = doc.getString("notes"),
                    color = (doc.getLong("color") ?: 0xFF1954A3).toInt(),
                    isActive = doc.getBoolean("isActive") ?: true,
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                )
                medicineDao.insertMedicine(entity)
            }
            Log.d(TAG, "Pulled ${medicineSnaps.size()} medicines from cloud")

            // Pull dose logs
            val doseLogSnaps = doseLogsRef.get().await()
            for (doc in doseLogSnaps.documents) {
                val entity = DoseLogEntity(
                    id = (doc.getLong("id") ?: 0).toInt(),
                    medicineId = (doc.getLong("medicineId") ?: 0).toInt(),
                    medicineName = doc.getString("medicineName") ?: "",
                    scheduledTime = doc.getLong("scheduledTime") ?: 0L,
                    loggedTime = doc.getLong("loggedTime"),
                    status = doc.getString("status") ?: "PENDING"
                )
                try {
                    doseLogDao.insertDoseLog(entity)
                } catch (e: android.database.sqlite.SQLiteConstraintException) {
                    Log.w(TAG, "Skipping dose log ${entity.id}: missing medicine ${entity.medicineId}")
                }
            }
            Log.d(TAG, "Pulled ${doseLogSnaps.size()} dose logs from cloud")
        } catch (e: Exception) {
            Log.e(TAG, "Cloud pull failed", e)
        }
    }

    suspend fun pushAllToCloud() = withContext(Dispatchers.IO) {
        if (!firebaseEnabled) {
            Log.w(TAG, "Skipping cloud push: Firebase feature disabled")
            return@withContext
        }
        if (uid == null) {
            Log.w(TAG, "Skipping cloud push: no authenticated user")
            return@withContext
        }
        try {
            // Push all medicines
            val medicines = medicineDao.getAllMedicinesOnce()
            medicines.forEach { syncMedicineToCloud(it) }

            // Push all dose logs
            val doseLogs = doseLogDao.getAllDoseLogsOnce()
            doseLogs.forEach { syncDoseLogToCloud(it) }

            Log.d(TAG, "Pushed ${medicines.size} medicines and ${doseLogs.size} dose logs to cloud")
        } catch (e: Exception) {
            Log.e(TAG, "Cloud push failed", e)
        }
    }

    fun startRealtimeSync() {
        if (!firebaseEnabled) return
        val medicinesRef = userMedicinesRef() ?: return
        val doseLogsRef = userDoseLogsRef() ?: return

        stopRealtimeSync()

        medicinesRegistration = medicinesRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener

            realtimeScope.launch {
                snapshot.documentChanges.forEach { change ->
                    val doc = change.document
                    val medicineId = (doc.getLong("id") ?: return@forEach).toInt()
                    when (change.type) {
                        DocumentChange.Type.ADDED,
                        DocumentChange.Type.MODIFIED -> {
                            val entity = MedicineEntity(
                                id = medicineId,
                                name = doc.getString("name") ?: "",
                                dosage = doc.getString("dosage") ?: "",
                                frequency = doc.getString("frequency") ?: "ONCE_DAILY",
                                scheduledTimes = doc.getString("scheduledTimes") ?: "[]",
                                startDate = doc.getLong("startDate") ?: System.currentTimeMillis(),
                                endDate = doc.getLong("endDate"),
                                totalStock = (doc.getLong("totalStock") ?: 0).toInt(),
                                remainingStock = (doc.getLong("remainingStock") ?: 0).toInt(),
                                refillThreshold = (doc.getLong("refillThreshold") ?: 5).toInt(),
                                notes = doc.getString("notes"),
                                color = (doc.getLong("color") ?: 0xFF1954A3).toInt(),
                                isActive = doc.getBoolean("isActive") ?: true,
                                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                            )
                            medicineDao.insertMedicine(entity)
                        }

                        DocumentChange.Type.REMOVED -> {
                            medicineDao.hardDeleteMedicineById(medicineId)
                        }
                    }
                }
            }
        }

        doseLogsRegistration = doseLogsRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener

            realtimeScope.launch {
                snapshot.documentChanges.forEach { change ->
                    val doc = change.document
                    val doseLogId = (doc.getLong("id") ?: return@forEach).toInt()
                    when (change.type) {
                        DocumentChange.Type.ADDED,
                        DocumentChange.Type.MODIFIED -> {
                            val entity = DoseLogEntity(
                                id = doseLogId,
                                medicineId = (doc.getLong("medicineId") ?: 0).toInt(),
                                medicineName = doc.getString("medicineName") ?: "",
                                scheduledTime = doc.getLong("scheduledTime") ?: 0L,
                                loggedTime = doc.getLong("loggedTime"),
                                status = doc.getString("status") ?: "PENDING"
                            )
                            try {
                                val insertedId = doseLogDao.insertDoseLog(entity)
                                if (insertedId == -1L) {
                                    doseLogDao.updateDoseLog(entity)
                                }
                            } catch (e: android.database.sqlite.SQLiteConstraintException) {
                                Log.w(TAG, "Skipping dose log realtime sync for ${entity.id}: missing medicine ${entity.medicineId}")
                            }
                        }

                        DocumentChange.Type.REMOVED -> {
                            doseLogDao.deleteDoseLogById(doseLogId)
                        }
                    }
                }
            }
        }
    }

    fun stopRealtimeSync() {
        medicinesRegistration?.remove()
        doseLogsRegistration?.remove()
        medicinesRegistration = null
        doseLogsRegistration = null
    }

    suspend fun clearLocalSyncData() = withContext(Dispatchers.IO) {
        try {
            medicineDao.deleteAllMedicines()
            doseLogDao.deleteAllDoseLogs()
        } catch (e: Exception) {
            Log.e(TAG, "Failed clearing local sync data: ${e.message}")
        }
    }

    suspend fun clearCloudDataForCurrentUser() = withContext(Dispatchers.IO) {
        if (!firebaseEnabled) return@withContext
        val medicinesRef = userMedicinesRef() ?: return@withContext
        val doseLogsRef = userDoseLogsRef() ?: return@withContext

        runCatching {
            val medDocs = medicinesRef.get().await().documents
            medDocs.forEach { it.reference.delete().await() }

            val logDocs = doseLogsRef.get().await().documents
            logDocs.forEach { it.reference.delete().await() }
        }.onFailure {
            Log.e(TAG, "Failed clearing cloud data: ${it.message}")
        }
    }
}
