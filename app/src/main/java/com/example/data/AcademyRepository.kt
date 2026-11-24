package com.example.data

import android.content.Context
import com.example.R
import com.example.model.AcademyNotification
import com.example.model.AcademyStarterCatalog
import com.example.model.AdminLog
import com.example.model.AppSettings
import com.example.model.CourseItem
import com.example.model.CourseLessonItem
import com.example.model.CourseModuleItem
import com.example.model.CourseProgress
import com.example.model.FavoriteItem
import com.example.model.LocalDownloadedFile
import com.example.model.PaymentRequest
import com.example.model.ResourceItem
import com.example.model.SubscriptionPlan
import com.example.model.UserProfile
import com.example.model.UserRole
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.concurrent.TimeUnit

class AcademyRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireUserId(): String =
    auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in with Google.")

  private fun isBootstrappedSuperAdmin(user: FirebaseUser?): Boolean =
    user != null && user.isEmailVerified && user.email == SUPER_ADMIN_EMAIL

  suspend fun ensureUserProfile(user: FirebaseUser, preferredLanguageCode: String): Result<UserProfile> {
    val uid = user.uid
    val path = "users/$uid"
    return try {
      val docRef = db.collection("users").document(uid)
      val snap = docRef.get().await()
      if (!snap.exists()) {
        val role = if (isBootstrappedSuperAdmin(user)) "SUPER_ADMIN" else "USER"
        val payload = mapOf(
          "uid" to uid,
          "fullName" to (user.displayName?.takeIf { it.isNotBlank() } ?: "Academy Student"),
          "email" to (user.email?.takeIf { it.isNotBlank() } ?: "student@zahidacademy.com"),
          "photoUrl" to user.photoUrl?.toString(),
          "role" to role,
          "accountStatus" to "ACTIVE",
          "preferredLanguage" to preferredLanguageCode,
          "trialStatus" to "NOT_STARTED",
          "subscriptionStatus" to "NONE",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ).filterValues { it != null }
        docRef.set(payload).await()

        // Create welcome notification
        createUserNotification(
          userId = uid,
          title = "Welcome to Zahid Architecture Academy",
          body = "Explore AutoCAD DWG/DXF, 3D Max, Naqshay maps, and activate your 7-Day Free Trial.",
          type = "WELCOME",
        )

        // If bootstrapped Super Admin, seed starter academy catalog if settings don't exist yet
        if (isBootstrappedSuperAdmin(user)) {
          seedStarterCatalogIfEmpty()
        }
      } else if (isBootstrappedSuperAdmin(user)) {
        val existingRole = snap.getString("role")
        if (existingRole != "SUPER_ADMIN") {
          docRef.update(mapOf("role" to "SUPER_ADMIN", "updatedAt" to FieldValue.serverTimestamp())).await()
        }
        seedStarterCatalogIfEmpty()
        val settingsSnap = db.collection("appSettings").document("academy_config").get().await()
        val currentWa = settingsSnap.getString("whatsappNumber").orEmpty()
        if (settingsSnap.exists() && (currentWa.endsWith("63931") || currentWa.contains("1234567"))) {
          db.collection("appSettings").document("academy_config").update(
            mapOf(
              "whatsappNumber" to "+923467803931",
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          ).await()
        }
      }

      val fresh = docRef.get().await()
        .toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        ?: UserProfile(uid = uid, email = user.email ?: "", fullName = user.displayName ?: "User")
      Result.success(fresh)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  fun observeUserProfile(userId: String): Flow<UserProfile?> {
    if (userId.isBlank()) return flowOf(null)
    val path = "users/$userId"
    return db.collection("users").document(userId)
      .snapshots()
      .map { it.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        if (auth.currentUser?.uid == userId) {
          handleFirestoreError(firestoreError ?: error, OperationType.GET, path)
        }
        throw error
      }
  }

  fun observeAppSettings(): Flow<AppSettings> {
    val path = "appSettings/academy_config"
    return db.collection("appSettings").document("academy_config")
      .snapshots()
      .map { snap ->
        snap.toObject(AppSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          ?: AppSettings()
      }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        if (auth.currentUser != null) {
          handleFirestoreError(firestoreError ?: error, OperationType.GET, path)
        }
        emit(AppSettings())
      }
  }

  fun observePublishedResources(isAdminView: Boolean = false): Flow<List<ResourceItem>> {
    val query = if (isAdminView) {
      db.collection("resources")
    } else {
      db.collection("resources")
        .whereEqualTo("published", true)
        .whereEqualTo("archived", false)
    }
    return query.snapshots()
      .map { snap ->
        snap.toObjects(ResourceItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
      }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        if (auth.currentUser != null) {
          handleFirestoreError(firestoreError ?: error, OperationType.LIST, "resources")
        }
        throw error
      }
  }

  fun observeCourses(isAdminView: Boolean = false): Flow<List<CourseItem>> {
    val query = if (isAdminView) {
      db.collection("courses")
    } else {
      db.collection("courses").whereEqualTo("published", true)
    }
    return query.snapshots()
      .map { snap ->
        snap.toObjects(CourseItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
      }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        if (auth.currentUser != null) {
          handleFirestoreError(firestoreError ?: error, OperationType.LIST, "courses")
        }
        throw error
      }
  }

  fun observeCourseModules(courseId: String, isAdminView: Boolean = false): Flow<List<CourseModuleItem>> {
    if (courseId.isBlank()) return flowOf(emptyList())
    val query = if (isAdminView) {
      db.collection("courseModules").whereEqualTo("courseId", courseId)
    } else {
      db.collection("courseModules")
        .whereEqualTo("courseId", courseId)
        .whereEqualTo("published", true)
    }
    return query.snapshots()
      .map { snap ->
        snap.toObjects(CourseModuleItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedBy { it.orderIndex }
      }
  }

  fun observeCourseLessons(courseId: String, isAdminView: Boolean = false): Flow<List<CourseLessonItem>> {
    if (courseId.isBlank()) return flowOf(emptyList())
    val query = if (isAdminView) {
      db.collection("courseLessons").whereEqualTo("courseId", courseId)
    } else {
      db.collection("courseLessons")
        .whereEqualTo("courseId", courseId)
        .whereEqualTo("published", true)
    }
    return query.snapshots()
      .map { snap ->
        snap.toObjects(CourseLessonItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedBy { it.orderIndex }
      }
  }

  fun observeAllCourseProgress(userId: String): Flow<List<CourseProgress>> {
    if (userId.isBlank()) return flowOf(emptyList())
    return db.collection("users").document(userId).collection("courseProgress")
      .snapshots()
      .map { snap ->
        snap.toObjects(CourseProgress::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
  }

  fun observeSubscriptionPlans(isAdminView: Boolean = false): Flow<List<SubscriptionPlan>> {
    val query = if (isAdminView) {
      db.collection("subscriptionPlans")
    } else {
      db.collection("subscriptionPlans").whereEqualTo("available", true)
    }
    return query.snapshots()
      .map { snap ->
        snap.toObjects(SubscriptionPlan::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedBy { it.pricePkr }
      }
  }

  fun observeUserPayments(userId: String): Flow<List<PaymentRequest>> {
    if (userId.isBlank()) return flowOf(emptyList())
    return db.collection("paymentRequests")
      .whereEqualTo("userId", userId)
      .snapshots()
      .map { snap ->
        snap.toObjects(PaymentRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
      }
  }

  fun observeNotifications(userId: String): Flow<List<AcademyNotification>> {
    if (userId.isBlank()) return flowOf(emptyList())
    return db.collection("users").document(userId).collection("notifications")
      .snapshots()
      .map { snap ->
        snap.toObjects(AcademyNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
      }
  }

  fun observeFavorites(userId: String): Flow<List<FavoriteItem>> {
    if (userId.isBlank()) return flowOf(emptyList())
    return db.collection("users").document(userId).collection("favorites")
      .snapshots()
      .map { snap ->
        snap.toObjects(FavoriteItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
  }

  // --- Admin Observation Streams ---
  fun observeAllUsersAdmin(): Flow<List<UserProfile>> =
    db.collection("users").snapshots()
      .map { snap ->
        snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
      }

  fun observeAllPaymentsAdmin(): Flow<List<PaymentRequest>> =
    db.collection("paymentRequests").snapshots()
      .map { snap ->
        snap.toObjects(PaymentRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
      }

  fun observeAdminLogs(): Flow<List<AdminLog>> =
    db.collection("adminLogs").snapshots()
      .map { snap ->
        snap.toObjects(AdminLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
      }

  // --- User Actions ---

  suspend fun updateProfileNameAndLanguage(
    userId: String,
    fullName: String,
    preferredLanguage: String,
  ): Result<Unit> = runMutation("users/$userId", OperationType.UPDATE) {
    val uid = requireUserId()
    check(uid == userId) { "Cross-user profile edit rejected" }
    val cleanName = fullName.trim()
    require(cleanName.length in 1..100) { "Full name must be 1 to 100 characters." }
    db.collection("users").document(uid).update(
      mapOf(
        "fullName" to cleanName,
        "preferredLanguage" to preferredLanguage,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    ).await()
  }

  suspend fun updatePreferredLanguage(userId: String, languageCode: String): Result<Unit> =
    runMutation("users/$userId", OperationType.UPDATE) {
      val uid = requireUserId()
      check(uid == userId)
      db.collection("users").document(uid).update(
        mapOf(
          "preferredLanguage" to languageCode,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    }

  suspend fun activateSevenDayTrial(userId: String, trialDays: Long): Result<Unit> =
    runMutation("users/$userId", OperationType.UPDATE) {
      val uid = requireUserId()
      check(uid == userId)
      val userDoc = db.collection("users").document(uid).get().await()
      val currentTrial = userDoc.getString("trialStatus") ?: "NOT_STARTED"
      require(currentTrial == "NOT_STARTED") { "7-Day Free Trial has already been used for this account." }

      val startTime = Timestamp.now()
      val expiryDate = Date(startTime.toDate().time + TimeUnit.DAYS.toMillis(trialDays.coerceIn(1L, 90L)))
      val expiryTimestamp = Timestamp(expiryDate)

      db.collection("users").document(uid).update(
        mapOf(
          "trialStatus" to "ACTIVE",
          "trialStartAt" to FieldValue.serverTimestamp(),
          "trialExpiresAt" to expiryTimestamp,
          "trialActivatedMethod" to "USER_BUTTON",
          "subscriptionStatus" to "TRIAL",
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()

      createUserNotification(
        userId = uid,
        title = "$trialDays-Day Free Trial Activated",
        body = "Your free trial is active! Explore and download premium DWG, DXF, 3D Max files and courses.",
        type = "TRIAL",
      )
    }

  suspend fun toggleFavorite(userId: String, resource: ResourceItem, currentlyFavorited: Boolean): Result<Unit> =
    runMutation("users/$userId/favorites/${resource.id}", OperationType.WRITE) {
      val uid = requireUserId()
      check(uid == userId)
      val docRef = db.collection("users").document(uid).collection("favorites").document(resource.id)
      if (currentlyFavorited) {
        docRef.delete().await()
      } else {
        docRef.set(
          mapOf(
            "userId" to uid,
            "resourceId" to resource.id,
            "title" to resource.title.take(160),
            "categoryId" to resource.categoryId,
            "fileType" to resource.fileType,
            "createdAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }
    }

  suspend fun logDownloadToCloud(userId: String, localFile: LocalDownloadedFile): Result<Unit> =
    runMutation("users/$userId/downloads", OperationType.CREATE) {
      val uid = requireUserId()
      check(uid == userId)
      val docRef = db.collection("users").document(uid).collection("downloads").document()
      docRef.set(
        mapOf(
          "userId" to uid,
          "resourceId" to localFile.resourceId,
          "title" to localFile.title.take(160),
          "categoryId" to localFile.categoryId,
          "fileType" to localFile.fileType,
          "fileName" to localFile.fileName.take(200),
          "fileSizeBytes" to localFile.fileSizeBytes.coerceAtLeast(0L),
          "createdAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    }

  suspend fun toggleLessonComplete(
    userId: String,
    courseId: String,
    lessonId: String,
    totalLessons: Int,
    currentCompleted: List<String>,
  ): Result<Unit> = runMutation("users/$userId/courseProgress/$courseId", OperationType.WRITE) {
    val uid = requireUserId()
    check(uid == userId)
    val updated = if (lessonId in currentCompleted) {
      currentCompleted - lessonId
    } else {
      (currentCompleted + lessonId).distinct().take(200)
    }
    val pct = if (totalLessons > 0) ((updated.size * 100) / totalLessons).coerceIn(0, 100).toLong() else 0L
    db.collection("users").document(uid).collection("courseProgress").document(courseId).set(
      mapOf(
        "userId" to uid,
        "courseId" to courseId,
        "completedLessonIds" to updated,
        "lastLessonId" to lessonId,
        "progressPercent" to pct,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    ).await()
  }

  suspend fun submitPaymentRequest(
    profile: UserProfile,
    plan: SubscriptionPlan,
    provider: String,
    transactionReference: String,
    proofLabel: String?,
    proofBase64: String?,
  ): Result<Unit> = runMutation("paymentRequests", OperationType.CREATE) {
    val uid = requireUserId()
    check(uid == profile.uid)
    val cleanRef = transactionReference.trim()
    require(cleanRef.length in 4..100) { "Transaction reference must be 4 to 100 characters." }
    require(provider in listOf("JAZZCASH", "EASYPAISA")) { "Invalid payment provider" }
    require(plan.pricePkr > 0L) { "Plan amount must be greater than 0." }

    // Ensure plan exists in Firestore (if using starter plan, verify doc exists before submit)
    val planRef = db.collection("subscriptionPlans").document(plan.id)
    val planSnap = planRef.get().await()
    if (!planSnap.exists()) {
      throw IllegalStateException("Subscription plan is not yet published in Firestore. Ask Admin to publish starter catalog.")
    }

    val reqRef = db.collection("paymentRequests").document()
    val payload = mapOf(
      "userId" to uid,
      "userEmail" to profile.email,
      "userName" to profile.fullName.ifBlank { "Student" },
      "planId" to plan.id,
      "planTitle" to plan.title,
      "provider" to provider,
      "amountPkr" to plan.pricePkr,
      "transactionReference" to cleanRef,
      "proofLabel" to proofLabel?.take(200),
      "proofBase64" to proofBase64?.take(280000),
      "status" to "PENDING",
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null }

    reqRef.set(payload).await()

    createUserNotification(
      userId = uid,
      title = "Payment Submitted ($provider)",
      body = "Your payment request for ${plan.title} (PKR ${plan.pricePkr}) with ref $cleanRef is pending admin verification.",
      type = "PAYMENT",
    )
  }

  suspend fun markNotificationRead(userId: String, notificationId: String): Result<Unit> =
    runMutation("users/$userId/notifications/$notificationId", OperationType.UPDATE) {
      val uid = requireUserId()
      check(uid == userId)
      db.collection("users").document(uid).collection("notifications").document(notificationId)
        .update(mapOf("read" to true, "updatedAt" to FieldValue.serverTimestamp()))
        .await()
    }

  suspend fun markAllNotificationsRead(userId: String, notifications: List<AcademyNotification>): Result<Unit> =
    runMutation("users/$userId/notifications", OperationType.UPDATE) {
      val uid = requireUserId()
      check(uid == userId)
      val unread = notifications.filter { !it.read }
      if (unread.isEmpty()) return@runMutation
      val batch = db.batch()
      unread.forEach { item ->
        val ref = db.collection("users").document(uid).collection("notifications").document(item.id)
        batch.update(ref, mapOf("read" to true, "updatedAt" to FieldValue.serverTimestamp()))
      }
      batch.commit().await()
    }

  // --- Admin Operations ---

  suspend fun approvePaymentRequest(
    adminProfile: UserProfile,
    request: PaymentRequest,
    planDurationDays: Long,
    note: String,
  ): Result<Unit> = runMutation("paymentRequests/${request.id}", OperationType.UPDATE) {
    val adminUid = requireUserId()
    val now = Timestamp.now()
    val expiresAt = Timestamp(Date(now.toDate().time + TimeUnit.DAYS.toMillis(planDurationDays.coerceAtLeast(1L))))

    val batch = db.batch()
    val reqRef = db.collection("paymentRequests").document(request.id)
    batch.update(
      reqRef,
      mapOf(
        "status" to "APPROVED",
        "verificationNote" to note.ifBlank { "Verified & approved" }.take(500),
        "reviewedBy" to adminUid,
        "reviewedAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    )

    val subRef = db.collection("subscriptions").document()
    batch.set(
      subRef,
      mapOf(
        "userId" to request.userId,
        "planId" to request.planId,
        "status" to "ACTIVE",
        "startAt" to FieldValue.serverTimestamp(),
        "expiresAt" to expiresAt,
        "paymentRequestId" to request.id,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    )

    val txRef = db.collection("paymentTransactions").document()
    batch.set(
      txRef,
      mapOf(
        "userId" to request.userId,
        "paymentRequestId" to request.id,
        "planId" to request.planId,
        "provider" to request.provider,
        "amountPkr" to request.amountPkr,
        "transactionReference" to request.transactionReference,
        "approvedBy" to adminUid,
        "createdAt" to FieldValue.serverTimestamp(),
      )
    )

    val userRef = db.collection("users").document(request.userId)
    batch.update(
      userRef,
      mapOf(
        "subscriptionStatus" to "ACTIVE",
        "subscriptionPlanId" to request.planId,
        "subscriptionExpiresAt" to expiresAt,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    )

    val notifRef = db.collection("users").document(request.userId).collection("notifications").document()
    batch.set(
      notifRef,
      mapOf(
        "userId" to request.userId,
        "title" to "Subscription Activated",
        "body" to "Your ${request.planTitle} payment (${request.transactionReference}) was approved! Full Pro access is now active.",
        "type" to "SUBSCRIPTION",
        "read" to false,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    )

    val logRef = db.collection("adminLogs").document()
    batch.set(
      logRef,
      mapOf(
        "actorUid" to adminUid,
        "actorEmail" to adminProfile.email,
        "action" to "APPROVE_PAYMENT",
        "targetType" to "paymentRequests",
        "targetId" to request.id,
        "details" to "Approved ${request.provider} PKR ${request.amountPkr} for ${request.userEmail}",
        "createdAt" to FieldValue.serverTimestamp(),
      )
    )

    batch.commit().await()
  }

  suspend fun rejectPaymentRequest(
    adminProfile: UserProfile,
    request: PaymentRequest,
    reason: String,
  ): Result<Unit> = runMutation("paymentRequests/${request.id}", OperationType.UPDATE) {
    val adminUid = requireUserId()
    val cleanReason = reason.ifBlank { "Transaction reference could not be verified." }.take(500)
    val batch = db.batch()
    batch.update(
      db.collection("paymentRequests").document(request.id),
      mapOf(
        "status" to "REJECTED",
        "verificationNote" to cleanReason,
        "reviewedBy" to adminUid,
        "reviewedAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    )
    val notifRef = db.collection("users").document(request.userId).collection("notifications").document()
    batch.set(
      notifRef,
      mapOf(
        "userId" to request.userId,
        "title" to "Payment Request Rejected",
        "body" to "Your ${request.provider} payment (${request.transactionReference}) was rejected: $cleanReason",
        "type" to "PAYMENT",
        "read" to false,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    )
    val logRef = db.collection("adminLogs").document()
    batch.set(
      logRef,
      mapOf(
        "actorUid" to adminUid,
        "actorEmail" to adminProfile.email,
        "action" to "REJECT_PAYMENT",
        "targetType" to "paymentRequests",
        "targetId" to request.id,
        "details" to "Rejected ${request.transactionReference}: $cleanReason",
        "createdAt" to FieldValue.serverTimestamp(),
      )
    )
    batch.commit().await()
  }

  suspend fun updateUserStatusAndRole(
    adminProfile: UserProfile,
    targetUser: UserProfile,
    newStatus: String,
    newRole: String,
  ): Result<Unit> = runMutation("users/${targetUser.uid}", OperationType.UPDATE) {
    val adminUid = requireUserId()
    val batch = db.batch()
    batch.update(
      db.collection("users").document(targetUser.uid),
      mapOf(
        "accountStatus" to newStatus,
        "role" to newRole,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    )
    val roleRef = db.collection("roles").document(targetUser.uid)
    if (newRole == "USER") {
      batch.delete(roleRef)
    } else {
      batch.set(
        roleRef,
        mapOf(
          "userId" to targetUser.uid,
          "role" to newRole,
          "assignedBy" to adminUid,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      )
    }
    val logRef = db.collection("adminLogs").document()
    batch.set(
      logRef,
      mapOf(
        "actorUid" to adminUid,
        "actorEmail" to adminProfile.email,
        "action" to "UPDATE_USER_ACCESS",
        "targetType" to "users",
        "targetId" to targetUser.uid,
        "details" to "Set ${targetUser.email} status=$newStatus role=$newRole",
        "createdAt" to FieldValue.serverTimestamp(),
      )
    )
    batch.commit().await()
  }

  suspend fun saveResource(adminProfile: UserProfile, resource: ResourceItem): Result<Unit> =
    runMutation("resources", OperationType.WRITE) {
      val adminUid = requireUserId()
      val isNew = resource.id.isBlank()
      val docRef = if (isNew) db.collection("resources").document() else db.collection("resources").document(resource.id)
      val payload = mapOf(
        "title" to resource.title.trim().take(160),
        "description" to resource.description.trim().take(2000),
        "categoryId" to resource.categoryId,
        "fileUrl" to resource.fileUrl.trim().take(1000),
        "thumbnailUrl" to resource.thumbnailUrl?.take(1000),
        "fileName" to resource.fileName.trim().take(200),
        "fileType" to resource.fileType,
        "fileSizeLabel" to resource.fileSizeLabel.trim().take(40),
        "fileSizeBytes" to resource.fileSizeBytes.coerceAtLeast(0L),
        "premium" to resource.premium,
        "published" to resource.published,
        "archived" to resource.archived,
        "uploadedBy" to (resource.uploadedBy.ifBlank { adminUid }),
        "createdAt" to (resource.createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp(),
      ).filterValues { it != null }
      docRef.set(payload).await()
      writeAdminLog(adminUid, adminProfile.email, if (isNew) "CREATE_RESOURCE" else "UPDATE_RESOURCE", "resources", docRef.id, resource.title)
    }

  suspend fun saveCourse(adminProfile: UserProfile, course: CourseItem): Result<Unit> =
    runMutation("courses", OperationType.WRITE) {
      val adminUid = requireUserId()
      val isNew = course.id.isBlank()
      val docRef = if (isNew) db.collection("courses").document() else db.collection("courses").document(course.id)
      val payload = mapOf(
        "title" to course.title.trim().take(160),
        "description" to course.description.trim().take(3000),
        "instructorName" to course.instructorName.trim().take(100),
        "instructorBio" to course.instructorBio?.take(500),
        "thumbnailUrl" to course.thumbnailUrl?.take(1000),
        "categoryId" to course.categoryId,
        "level" to course.level?.take(50),
        "totalLessons" to course.totalLessons.coerceIn(0L, 500L),
        "durationLabel" to course.durationLabel.take(50),
        "premium" to course.premium,
        "published" to course.published,
        "createdAt" to (course.createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp(),
      ).filterValues { it != null }
      docRef.set(payload).await()
      writeAdminLog(adminUid, adminProfile.email, if (isNew) "CREATE_COURSE" else "UPDATE_COURSE", "courses", docRef.id, course.title)
    }

  suspend fun saveCourseLesson(adminProfile: UserProfile, lesson: CourseLessonItem): Result<Unit> =
    runMutation("courseLessons", OperationType.WRITE) {
      val adminUid = requireUserId()
      val isNew = lesson.id.isBlank()
      val docRef = if (isNew) db.collection("courseLessons").document() else db.collection("courseLessons").document(lesson.id)
      val payload = mapOf(
        "courseId" to lesson.courseId.trim(),
        "moduleId" to lesson.moduleId.trim().ifBlank { "mod_general" },
        "title" to lesson.title.trim().take(160),
        "description" to lesson.description?.trim()?.take(2000),
        "lessonType" to lesson.lessonType,
        "videoUrl" to lesson.videoUrl?.trim()?.take(1000),
        "pdfUrl" to lesson.pdfUrl?.trim()?.take(1000),
        "durationLabel" to lesson.durationLabel.trim().take(50).ifBlank { "10 min" },
        "orderIndex" to lesson.orderIndex.coerceIn(0L, 500L),
        "premium" to lesson.premium,
        "published" to lesson.published,
        "createdAt" to (lesson.createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp(),
      ).filterValues { it != null }
      docRef.set(payload).await()
      writeAdminLog(adminUid, adminProfile.email, if (isNew) "CREATE_LESSON" else "UPDATE_LESSON", "courseLessons", docRef.id, lesson.title)
    }

  suspend fun saveAppSettings(adminProfile: UserProfile, settings: AppSettings): Result<Unit> =
    runMutation("appSettings/academy_config", OperationType.WRITE) {
      val adminUid = requireUserId()
      val payload = mapOf(
        "academyName" to settings.academyName.trim().take(120),
        "supportEmail" to settings.supportEmail?.trim()?.take(150),
        "whatsappNumber" to settings.whatsappNumber.trim().take(25),
        "whatsappDefaultMessage" to settings.whatsappDefaultMessage.trim().take(500),
        "trialDurationDays" to settings.trialDurationDays.coerceIn(1L, 90L),
        "trialEnabled" to settings.trialEnabled,
        "jazzCashAccountName" to settings.jazzCashAccountName.trim().take(100),
        "jazzCashAccountNumber" to settings.jazzCashAccountNumber.trim().take(40),
        "jazzCashInstructions" to settings.jazzCashInstructions.trim().take(1000),
        "easypaisaAccountName" to settings.easypaisaAccountName.trim().take(100),
        "easypaisaAccountNumber" to settings.easypaisaAccountNumber.trim().take(40),
        "easypaisaInstructions" to settings.easypaisaInstructions.trim().take(1000),
        "defaultLanguage" to settings.defaultLanguage,
        "updatedAt" to FieldValue.serverTimestamp(),
      ).filterValues { it != null }
      db.collection("appSettings").document("academy_config").set(payload).await()
      writeAdminLog(adminUid, adminProfile.email, "UPDATE_SETTINGS", "appSettings", "academy_config", "Updated academy configuration")
    }

  suspend fun saveSubscriptionPlan(adminProfile: UserProfile, plan: SubscriptionPlan): Result<Unit> =
    runMutation("subscriptionPlans", OperationType.WRITE) {
      val adminUid = requireUserId()
      val isNew = plan.id.isBlank()
      val docRef = if (isNew) db.collection("subscriptionPlans").document() else db.collection("subscriptionPlans").document(plan.id)
      val payload = mapOf(
        "title" to plan.title.trim().take(100),
        "description" to plan.description.trim().take(1000),
        "pricePkr" to plan.pricePkr.coerceIn(0L, 1000000L),
        "durationDays" to plan.durationDays.coerceIn(1L, 3650L),
        "accessLevel" to plan.accessLevel.trim().take(60),
        "available" to plan.available,
        "badgeText" to plan.badgeText?.take(40),
        "createdAt" to (plan.createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp(),
      ).filterValues { it != null }
      docRef.set(payload).await()
      writeAdminLog(adminUid, adminProfile.email, "SAVE_PLAN", "subscriptionPlans", docRef.id, plan.title)
    }

  suspend fun seedStarterCatalogIfEmpty(force: Boolean = false): Result<Unit> =
    runMutation("seed", OperationType.WRITE) {
      val adminUid = requireUserId()
      val settingsRef = db.collection("appSettings").document("academy_config")
      if (!force && settingsRef.get().await().exists()) return@runMutation

      val batch = db.batch()
      if (!settingsRef.get().await().exists()) {
        val def = AppSettings()
        batch.set(
          settingsRef,
          mapOf(
            "academyName" to def.academyName,
            "supportEmail" to def.supportEmail,
            "whatsappNumber" to def.whatsappNumber,
            "whatsappDefaultMessage" to def.whatsappDefaultMessage,
            "trialDurationDays" to def.trialDurationDays,
            "trialEnabled" to def.trialEnabled,
            "jazzCashAccountName" to def.jazzCashAccountName,
            "jazzCashAccountNumber" to def.jazzCashAccountNumber,
            "jazzCashInstructions" to def.jazzCashInstructions,
            "easypaisaAccountName" to def.easypaisaAccountName,
            "easypaisaAccountNumber" to def.easypaisaAccountNumber,
            "easypaisaInstructions" to def.easypaisaInstructions,
            "defaultLanguage" to def.defaultLanguage,
            "updatedAt" to FieldValue.serverTimestamp(),
          ).filterValues { it != null }
        )
      }

      AcademyStarterCatalog.starterPlans.forEach { p ->
        batch.set(
          db.collection("subscriptionPlans").document(p.id),
          mapOf(
            "title" to p.title,
            "description" to p.description,
            "pricePkr" to p.pricePkr,
            "durationDays" to p.durationDays,
            "accessLevel" to p.accessLevel,
            "available" to p.available,
            "badgeText" to p.badgeText,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ).filterValues { it != null }
        )
      }

      AcademyStarterCatalog.starterResources.forEach { r ->
        batch.set(
          db.collection("resources").document(r.id),
          mapOf(
            "title" to r.title,
            "description" to r.description,
            "categoryId" to r.categoryId,
            "fileUrl" to r.fileUrl,
            "fileName" to r.fileName,
            "fileType" to r.fileType,
            "fileSizeLabel" to r.fileSizeLabel,
            "fileSizeBytes" to r.fileSizeBytes,
            "premium" to r.premium,
            "published" to r.published,
            "archived" to r.archived,
            "uploadedBy" to adminUid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
      }

      AcademyStarterCatalog.starterCourses.forEach { c ->
        batch.set(
          db.collection("courses").document(c.id),
          mapOf(
            "title" to c.title,
            "description" to c.description,
            "instructorName" to c.instructorName,
            "instructorBio" to c.instructorBio,
            "categoryId" to c.categoryId,
            "level" to c.level,
            "totalLessons" to c.totalLessons,
            "durationLabel" to c.durationLabel,
            "premium" to c.premium,
            "published" to c.published,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ).filterValues { it != null }
        )
      }

      AcademyStarterCatalog.starterModules.forEach { m ->
        batch.set(
          db.collection("courseModules").document(m.id),
          mapOf(
            "courseId" to m.courseId,
            "title" to m.title,
            "orderIndex" to m.orderIndex,
            "published" to m.published,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
      }

      AcademyStarterCatalog.starterLessons.forEach { l ->
        batch.set(
          db.collection("courseLessons").document(l.id),
          mapOf(
            "courseId" to l.courseId,
            "moduleId" to l.moduleId,
            "title" to l.title,
            "description" to l.description,
            "lessonType" to l.lessonType,
            "videoUrl" to l.videoUrl,
            "pdfUrl" to l.pdfUrl,
            "durationLabel" to l.durationLabel,
            "orderIndex" to l.orderIndex,
            "premium" to l.premium,
            "published" to l.published,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ).filterValues { it != null }
        )
      }

      batch.commit().await()
    }

  private suspend fun createUserNotification(
    userId: String,
    title: String,
    body: String,
    type: String,
  ) {
    val ref = db.collection("users").document(userId).collection("notifications").document()
    ref.set(
      mapOf(
        "userId" to userId,
        "title" to title.take(160),
        "body" to body.take(500),
        "type" to type.take(40),
        "read" to false,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    ).await()
  }

  private suspend fun writeAdminLog(
    actorUid: String,
    actorEmail: String,
    action: String,
    targetType: String,
    targetId: String,
    details: String,
  ) {
    db.collection("adminLogs").document().set(
      mapOf(
        "actorUid" to actorUid,
        "actorEmail" to actorEmail.take(150),
        "action" to action.take(80),
        "targetType" to targetType.take(60),
        "targetId" to targetId.take(128),
        "details" to details.take(500),
        "createdAt" to FieldValue.serverTimestamp(),
      )
    ).await()
  }

  private suspend fun <T> runMutation(
    path: String,
    op: OperationType,
    block: suspend () -> T,
  ): Result<T> {
    return try {
      Result.success(block())
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, op, path)
      Result.failure(e)
    }
  }

  companion object {
    const val SUPER_ADMIN_EMAIL = "sahiljutt060@gmail.com"
  }
}
