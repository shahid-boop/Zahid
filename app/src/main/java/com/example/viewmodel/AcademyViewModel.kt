package com.example.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AcademyFileDownloadManager
import com.example.data.AcademyRepository
import com.example.data.LocalPreferencesManager
import com.example.i18n.AppLanguage
import com.example.model.AcademyNotification
import com.example.model.AcademyStarterCatalog
import com.example.model.AccessEvaluator
import com.example.model.AdminLog
import com.example.model.AppSettings
import com.example.model.CourseItem
import com.example.model.CourseLessonItem
import com.example.model.CourseModuleItem
import com.example.model.CourseProgress
import com.example.model.DownloadState
import com.example.model.FavoriteItem
import com.example.model.LocalDownloadedFile
import com.example.model.PaymentRequest
import com.example.model.ResourceItem
import com.example.model.SubscriptionPlan
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.model.UserRole
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

sealed interface ViewerTarget {
  data class Pdf(val title: String, val file: File) : ViewerTarget
  data class Video(val title: String, val file: File) : ViewerTarget
  data class ImageGallery(val title: String, val files: List<Pair<String, File>>, val initialIndex: Int) : ViewerTarget
  data class Dxf(val title: String, val file: File) : ViewerTarget
  data class Unsupported(val resource: ResourceItem, val localFile: LocalDownloadedFile?) : ViewerTarget
}

@OptIn(ExperimentalCoroutinesApi::class)
class AcademyViewModel(
  private val repository: AcademyRepository,
  val downloadManager: AcademyFileDownloadManager,
  private val prefs: LocalPreferencesManager,
  private val currentUserId: String,
  private val firebaseUser: FirebaseUser?,
) : ViewModel() {

  private val _bannerMessage = MutableStateFlow<String?>(null)
  val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

  private val _viewerTarget = MutableStateFlow<ViewerTarget?>(null)
  val viewerTarget: StateFlow<ViewerTarget?> = _viewerTarget.asStateFlow()

  private val _isPreparingViewer = MutableStateFlow(false)
  val isPreparingViewer: StateFlow<Boolean> = _isPreparingViewer.asStateFlow()

  private val _isSubmittingPayment = MutableStateFlow(false)
  val isSubmittingPayment: StateFlow<Boolean> = _isSubmittingPayment.asStateFlow()

  val selectedLanguage: StateFlow<AppLanguage> = prefs.languageFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), AppLanguage.EN)

  val customFolderUri: StateFlow<String?> = prefs.customFolderUriFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), null)

  val localDownloads: StateFlow<List<LocalDownloadedFile>> = prefs.localDownloadsFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), emptyList())

  val trialCategoryDownloads: StateFlow<Map<String, Int>> = prefs.trialCategoryDownloadsFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), emptyMap())

  val downloadState: StateFlow<DownloadState> = downloadManager.downloadState

  val userProfileState: StateFlow<UiState<UserProfile?>> = repository.observeUserProfile(currentUserId)
    .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
    .catch { error ->
      val firestoreError = generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
      if (firestoreError == null && error is CancellationException) throw error
      val root = firestoreError ?: error
      Log.w(TAG, "Failed to load user profile", root)
      emit(UiState.Error(root.localizedMessage ?: "Failed to load user profile"))
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), UiState.Loading)

  val appSettings: StateFlow<AppSettings> = repository.observeAppSettings()
    .catch { emit(AppSettings()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), AppSettings())

  val resources: StateFlow<List<ResourceItem>> = repository.observePublishedResources(isAdminView = false)
    .map { list -> (list + AcademyStarterCatalog.fullCatalogResources).distinctBy { it.id } }
    .catch { emit(AcademyStarterCatalog.fullCatalogResources) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), AcademyStarterCatalog.fullCatalogResources)

  val courses: StateFlow<List<CourseItem>> = repository.observeCourses(isAdminView = false)
    .map { list -> list.ifEmpty { AcademyStarterCatalog.starterCourses } }
    .catch { emit(AcademyStarterCatalog.starterCourses) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), AcademyStarterCatalog.starterCourses)

  val subscriptionPlans: StateFlow<List<SubscriptionPlan>> = repository.observeSubscriptionPlans(isAdminView = false)
    .map { list -> list.ifEmpty { AcademyStarterCatalog.starterPlans } }
    .catch { emit(AcademyStarterCatalog.starterPlans) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), AcademyStarterCatalog.starterPlans)

  val userPayments: StateFlow<List<PaymentRequest>> = repository.observeUserPayments(currentUserId)
    .catch { emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), emptyList())

  val notifications: StateFlow<List<AcademyNotification>> = repository.observeNotifications(currentUserId)
    .catch { emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), emptyList())

  val favorites: StateFlow<List<FavoriteItem>> = repository.observeFavorites(currentUserId)
    .catch { emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), emptyList())

  val courseProgress: StateFlow<List<CourseProgress>> = repository.observeAllCourseProgress(currentUserId)
    .catch { emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), emptyList())

  // Staff/Admin-gated flows: only attach listeners if current profile has a staff role!
  private val isStaffFlow: Flow<Boolean> = userProfileState.map { state ->
    val profile = (state as? UiState.Success)?.data
    UserRole.fromString(profile?.role).isStaff()
  }

  val adminUsers: StateFlow<List<UserProfile>> = isStaffFlow
    .flatMapLatest { isStaff ->
      if (isStaff) repository.observeAllUsersAdmin().catch { emit(emptyList()) } else flowOf(emptyList())
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), emptyList())

  val adminPayments: StateFlow<List<PaymentRequest>> = isStaffFlow
    .flatMapLatest { isStaff ->
      if (isStaff) repository.observeAllPaymentsAdmin().catch { emit(emptyList()) } else flowOf(emptyList())
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), emptyList())

  val adminResources: StateFlow<List<ResourceItem>> = isStaffFlow
    .flatMapLatest { isStaff ->
      if (isStaff) repository.observePublishedResources(isAdminView = true).catch { emit(emptyList()) } else flowOf(emptyList())
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), emptyList())

  val adminCourses: StateFlow<List<CourseItem>> = isStaffFlow
    .flatMapLatest { isStaff ->
      if (isStaff) repository.observeCourses(isAdminView = true).catch { emit(emptyList()) } else flowOf(emptyList())
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), emptyList())

  val adminLogs: StateFlow<List<AdminLog>> = isStaffFlow
    .flatMapLatest { isStaff ->
      if (isStaff) repository.observeAdminLogs().catch { emit(emptyList()) } else flowOf(emptyList())
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), emptyList())

  init {
    if (firebaseUser != null) {
      viewModelScope.launch {
        val result = repository.ensureUserProfile(firebaseUser, selectedLanguage.value.code)
        result.getOrNull()?.let { profile ->
          val remoteLang = AppLanguage.fromCode(profile.preferredLanguage)
          if (remoteLang != selectedLanguage.value) {
            prefs.setLanguage(remoteLang)
          }
        }
      }
    }
  }

  fun observeModules(courseId: String): Flow<List<CourseModuleItem>> =
    repository.observeCourseModules(courseId).map { list ->
      list.ifEmpty { AcademyStarterCatalog.starterModules.filter { it.courseId == courseId } }
    }.catch { emit(AcademyStarterCatalog.starterModules.filter { it.courseId == courseId }) }

  fun observeLessons(courseId: String): Flow<List<CourseLessonItem>> =
    repository.observeCourseLessons(courseId).map { list ->
      list.ifEmpty { AcademyStarterCatalog.starterLessons.filter { it.courseId == courseId } }
    }.catch { emit(AcademyStarterCatalog.starterLessons.filter { it.courseId == courseId }) }

  fun clearBanner() {
    _bannerMessage.value = null
  }

  fun showBanner(message: String) {
    _bannerMessage.value = message
  }

  fun closeViewer() {
    _viewerTarget.value = null
  }

  fun setLanguage(lang: AppLanguage) {
    viewModelScope.launch {
      prefs.setLanguage(lang)
      repository.updatePreferredLanguage(currentUserId, lang.code)
    }
  }

  fun setCustomFolderUri(uri: Uri?) {
    viewModelScope.launch {
      prefs.setCustomFolderUri(uri?.toString())
      _bannerMessage.value = if (uri != null) {
        "Custom download folder saved."
      } else {
        "Download folder reset to Internal Storage / Download / Zahid Architecture Academy."
      }
    }
  }

  fun updateProfileName(newName: String) {
    viewModelScope.launch {
      val result = repository.updateProfileNameAndLanguage(currentUserId, newName, selectedLanguage.value.code)
      _bannerMessage.value = result.fold(
        onSuccess = { "Profile updated." },
        onFailure = { it.localizedMessage ?: "Failed to update profile." },
      )
    }
  }

  fun activateFreeTrial() {
    viewModelScope.launch {
      val days = appSettings.value.trialDurationDays
      val res = repository.activateSevenDayTrial(currentUserId, days)
      _bannerMessage.value = res.fold(
        onSuccess = { "$days-Day Free Trial activated!" },
        onFailure = { it.localizedMessage ?: "Could not activate trial." },
      )
    }
  }

  fun toggleFavorite(resource: ResourceItem) {
    val currentlyFav = favorites.value.any { it.resourceId == resource.id }
    viewModelScope.launch {
      repository.toggleFavorite(currentUserId, resource, currentlyFav)
    }
  }

  fun toggleLessonComplete(courseId: String, lessonId: String, totalLessons: Int) {
    val existing = courseProgress.value.firstOrNull { it.courseId == courseId }?.completedLessonIds ?: emptyList()
    viewModelScope.launch {
      repository.toggleLessonComplete(currentUserId, courseId, lessonId, totalLessons, existing)
    }
  }

  fun markNotificationRead(notificationId: String) {
    viewModelScope.launch {
      repository.markNotificationRead(currentUserId, notificationId)
    }
  }

  fun markAllNotificationsRead() {
    viewModelScope.launch {
      repository.markAllNotificationsRead(currentUserId, notifications.value)
    }
  }

  fun viewResource(resource: ResourceItem, profile: UserProfile?) {
    if (!AccessEvaluator.canAccessResource(profile, resource)) {
      _bannerMessage.value = "Activate your 7-Day Free Trial or subscribe to view this premium resource."
      return
    }
    val local = localDownloads.value.firstOrNull { it.resourceId == resource.id }
    viewModelScope.launch {
      _isPreparingViewer.value = true
      try {
        when (resource.fileType.uppercase()) {
          "DWG", "MAX", "SKP" -> {
            _viewerTarget.value = ViewerTarget.Unsupported(resource, local)
          }
          "JPG" -> {
            val imageResources = resources.value.filter { it.fileType.equals("JPG", ignoreCase = true) }
            val ordered = if (imageResources.any { it.id == resource.id }) imageResources else listOf(resource)
            val resolvedPairs = mutableListOf<Pair<String, File>>()
            var targetIdx = 0
            for (item in ordered) {
              val itemLocal = localDownloads.value.firstOrNull { it.resourceId == item.id }
              val res = downloadManager.resolvePreviewFile(item.fileUrl, item.fileName, itemLocal)
              res.getOrNull()?.let { f ->
                if (item.id == resource.id) targetIdx = resolvedPairs.size
                resolvedPairs.add(item.title to f)
              }
            }
            if (resolvedPairs.isNotEmpty()) {
              _viewerTarget.value = ViewerTarget.ImageGallery(resource.title, resolvedPairs, targetIdx)
            } else {
              _bannerMessage.value = "Could not load image preview."
            }
          }
          "PDF" -> {
            val res = downloadManager.resolvePreviewFile(resource.fileUrl, resource.fileName, local)
            res.fold(
              onSuccess = { file -> _viewerTarget.value = ViewerTarget.Pdf(resource.title, file) },
              onFailure = { _bannerMessage.value = it.localizedMessage ?: "Failed to open PDF." },
            )
          }
          "MP4" -> {
            val res = downloadManager.resolvePreviewFile(resource.fileUrl, resource.fileName, local)
            res.fold(
              onSuccess = { file -> _viewerTarget.value = ViewerTarget.Video(resource.title, file) },
              onFailure = { _bannerMessage.value = it.localizedMessage ?: "Failed to open video." },
            )
          }
          "DXF" -> {
            val res = downloadManager.resolvePreviewFile(resource.fileUrl, resource.fileName, local)
            res.fold(
              onSuccess = { file -> _viewerTarget.value = ViewerTarget.Dxf(resource.title, file) },
              onFailure = { _bannerMessage.value = it.localizedMessage ?: "Failed to open DXF blueprint." },
            )
          }
          else -> _viewerTarget.value = ViewerTarget.Unsupported(resource, local)
        }
      } finally {
        _isPreparingViewer.value = false
      }
    }
  }

  fun viewCourseLesson(lesson: CourseLessonItem, profile: UserProfile?, onLocked: () -> Unit) {
    val isStaff = UserRole.fromString(profile?.role).isStaff()
    if (lesson.premium && !AccessEvaluator.hasPremiumAccess(profile) && !isStaff) {
      onLocked()
      return
    }
    val url = if (lesson.lessonType == "VIDEO") lesson.videoUrl ?: lesson.pdfUrl else lesson.pdfUrl ?: lesson.videoUrl
    if (url.isNullOrBlank()) {
      _bannerMessage.value = "Lesson file is not configured yet."
      return
    }
    val fileType = when {
      url.endsWith(".mp4", true) -> "MP4"
      url.endsWith(".dxf", true) -> "DXF"
      url.endsWith(".jpg", true) || url.endsWith(".png", true) -> "JPG"
      else -> "PDF"
    }
    val synthetic = ResourceItem(
      id = "lesson_${lesson.id}",
      title = lesson.title,
      description = lesson.description ?: "",
      categoryId = if (fileType == "MP4") "VIDEOS" else "PDF_LESSONS",
      fileUrl = url,
      fileName = url.substringAfterLast('/'),
      fileType = fileType,
      premium = lesson.premium,
    )
    viewResource(synthetic, profile)
  }

  fun viewLocalDownloadedFile(localFile: LocalDownloadedFile) {
    val synthetic = ResourceItem(
      id = localFile.resourceId,
      title = localFile.title,
      categoryId = localFile.categoryId,
      fileUrl = localFile.savedPathOrUri,
      fileName = localFile.fileName,
      fileType = localFile.fileType,
      fileSizeBytes = localFile.fileSizeBytes,
      premium = false,
    )
    viewResource(synthetic, null)
  }

  fun downloadResource(resource: ResourceItem, profile: UserProfile?) {
    if (!AccessEvaluator.canAccessResource(profile, resource)) {
      _bannerMessage.value = "Premium download blocked: Please start your 7-Day Free Trial or subscribe."
      return
    }
    val downloadedIds = localDownloads.value.map { it.resourceId }.toSet()
    val counts = trialCategoryDownloads.value
    if (!AccessEvaluator.canDownloadInCategory(profile, resource, counts, downloadedIds)) {
      val catKey = AccessEvaluator.quotaCategoryKey(resource)
      _bannerMessage.value = "Trial quota reached for $catKey (1 free download per category). Subscribe via JazzCash or Easypaisa to unlock all 20 items!"
      return
    }
    val existing = localDownloads.value.firstOrNull { it.resourceId == resource.id }
    if (existing != null && downloadManager.verifyFileExists(existing)) {
      _bannerMessage.value = "${existing.fileName} is already downloaded in My Downloads."
      return
    }
    viewModelScope.launch {
      val result = downloadManager.downloadResource(resource, customFolderUri.value)
      result.fold(
        onSuccess = { saved ->
          prefs.incrementTrialCategoryDownload(AccessEvaluator.quotaCategoryKey(resource))
          repository.logDownloadToCloud(currentUserId, saved)
          _bannerMessage.value = "Saved to ${saved.displayLocation}"
        },
        onFailure = { err ->
          _bannerMessage.value = err.localizedMessage ?: "Download failed."
        },
      )
    }
  }

  fun cancelDownload() {
    downloadManager.cancelActiveDownload()
    _bannerMessage.value = "Download cancelled."
  }

  fun deleteLocalDownload(localFile: LocalDownloadedFile) {
    viewModelScope.launch {
      downloadManager.deleteLocalCopy(localFile)
      _bannerMessage.value = "Removed local copy of ${localFile.fileName}."
    }
  }

  fun submitPayment(
    context: Context,
    profile: UserProfile,
    plan: SubscriptionPlan,
    provider: String,
    transactionReference: String,
    proofUri: Uri?,
  ) {
    val validationError = AccessEvaluator.validatePaymentInput(transactionReference, plan.pricePkr)
    if (validationError != null) {
      _bannerMessage.value = validationError
      return
    }
    viewModelScope.launch {
      _isSubmittingPayment.value = true
      try {
        val (proofLabel, proofBase64) = if (proofUri != null) {
          encodeProofThumbnail(context, proofUri)
        } else {
          null to null
        }
        val res = repository.submitPaymentRequest(
          profile = profile,
          plan = plan,
          provider = provider,
          transactionReference = transactionReference,
          proofLabel = proofLabel,
          proofBase64 = proofBase64,
        )
        _bannerMessage.value = res.fold(
          onSuccess = { "Payment request submitted! Admin verification pending." },
          onFailure = { it.localizedMessage ?: "Failed to submit payment." },
        )
      } finally {
        _isSubmittingPayment.value = false
      }
    }
  }

  private suspend fun encodeProofThumbnail(context: Context, uri: Uri): Pair<String?, String?> =
    withContext(Dispatchers.IO) {
      try {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@withContext null to null
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@withContext "screenshot.jpg" to null
        val maxDim = 640
        val scale = minOf(maxDim.toFloat() / decoded.width.coerceAtLeast(1), maxDim.toFloat() / decoded.height.coerceAtLeast(1), 1f)
        val scaled = if (scale < 1f) {
          Bitmap.createScaledBitmap(decoded, (decoded.width * scale).toInt().coerceAtLeast(1), (decoded.height * scale).toInt().coerceAtLeast(1), true)
        } else decoded
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 70, out)
        val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        "payment_proof_${System.currentTimeMillis()}.jpg" to b64
      } catch (_: Exception) {
        null to null
      }
    }

  // --- Admin Actions ---

  fun adminApprovePayment(adminProfile: UserProfile, request: PaymentRequest, note: String) {
    viewModelScope.launch {
      val duration = subscriptionPlans.value.firstOrNull { it.id == request.planId }?.durationDays ?: 30L
      val res = repository.approvePaymentRequest(adminProfile, request, duration, note)
      _bannerMessage.value = res.fold(
        onSuccess = { "Payment approved & subscription activated." },
        onFailure = { it.localizedMessage ?: "Failed to approve payment." },
      )
    }
  }

  fun adminRejectPayment(adminProfile: UserProfile, request: PaymentRequest, reason: String) {
    viewModelScope.launch {
      val res = repository.rejectPaymentRequest(adminProfile, request, reason)
      _bannerMessage.value = res.fold(
        onSuccess = { "Payment request rejected." },
        onFailure = { it.localizedMessage ?: "Failed to reject payment." },
      )
    }
  }

  fun adminUpdateUser(adminProfile: UserProfile, target: UserProfile, status: String, role: String) {
    viewModelScope.launch {
      val res = repository.updateUserStatusAndRole(adminProfile, target, status, role)
      _bannerMessage.value = res.fold(
        onSuccess = { "Updated ${target.fullName}." },
        onFailure = { it.localizedMessage ?: "Failed to update user." },
      )
    }
  }

  fun adminSaveResource(adminProfile: UserProfile, resource: ResourceItem) {
    viewModelScope.launch {
      val res = repository.saveResource(adminProfile, resource)
      _bannerMessage.value = res.fold(
        onSuccess = { "Resource saved." },
        onFailure = { it.localizedMessage ?: "Failed to save resource." },
      )
    }
  }

  fun adminSaveCourse(adminProfile: UserProfile, course: CourseItem) {
    viewModelScope.launch {
      val res = repository.saveCourse(adminProfile, course)
      _bannerMessage.value = res.fold(
        onSuccess = { "Course saved." },
        onFailure = { it.localizedMessage ?: "Failed to save course." },
      )
    }
  }

  fun adminSaveLesson(adminProfile: UserProfile, lesson: CourseLessonItem) {
    viewModelScope.launch {
      val res = repository.saveCourseLesson(adminProfile, lesson)
      _bannerMessage.value = res.fold(
        onSuccess = { "Lesson published to course." },
        onFailure = { it.localizedMessage ?: "Failed to save lesson." },
      )
    }
  }

  fun adminSaveSettings(adminProfile: UserProfile, settings: AppSettings) {
    viewModelScope.launch {
      val res = repository.saveAppSettings(adminProfile, settings)
      _bannerMessage.value = res.fold(
        onSuccess = { "Academy settings saved." },
        onFailure = { it.localizedMessage ?: "Failed to save settings." },
      )
    }
  }

  fun adminSavePlan(adminProfile: UserProfile, plan: SubscriptionPlan) {
    viewModelScope.launch {
      val res = repository.saveSubscriptionPlan(adminProfile, plan)
      _bannerMessage.value = res.fold(
        onSuccess = { "Subscription plan saved." },
        onFailure = { it.localizedMessage ?: "Failed to save plan." },
      )
    }
  }

  fun adminSeedCatalog() {
    viewModelScope.launch {
      val res = repository.seedStarterCatalogIfEmpty(force = true)
      _bannerMessage.value = res.fold(
        onSuccess = { "Starter Academy catalog published to Firestore!" },
        onFailure = { it.localizedMessage ?: "Failed to seed catalog." },
      )
    }
  }

  private fun <T> Flow<T>.unwrapFirestoreErrors(fallbackMsg: String): Flow<T> =
    catch { error ->
      val firestoreError = generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
      if (firestoreError == null && error is CancellationException) throw error
      val root = firestoreError ?: error
      Log.w(TAG, fallbackMsg, root)
      throw root
    }

  private companion object {
    const val TAG = "AcademyVM"
    const val STOP_MS = 5000L
  }
}
