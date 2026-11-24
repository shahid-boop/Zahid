package com.example.model

import androidx.annotation.Keep
import com.example.i18n.AcademyStrings
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.concurrent.TimeUnit

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

enum class UserRole {
  SUPER_ADMIN,
  ADMIN,
  CONTENT_MANAGER,
  SUPPORT_AGENT,
  USER;

  fun isStaff(): Boolean = this != USER
  fun canManageContent(): Boolean = this == SUPER_ADMIN || this == ADMIN || this == CONTENT_MANAGER
  fun canReviewPayments(): Boolean = this == SUPER_ADMIN || this == ADMIN || this == SUPPORT_AGENT
  fun canManageUsersAndSettings(): Boolean = this == SUPER_ADMIN || this == ADMIN

  companion object {
    fun fromString(raw: String?): UserRole =
      entries.firstOrNull { it.name == raw } ?: USER
  }
}

enum class ResourceCategory(
  val id: String,
  val folderName: String,
  val homeFeatured: Boolean,
) {
  PDF_LESSONS("PDF_LESSONS", "PDF Lessons", true),
  VIDEOS("VIDEOS", "Videos", true),
  PICTURES("PICTURES", "Pictures", true),
  NAQSHAY("NAQSHAY", "Naqshay Maps", true),
  MAX_3D("MAX_3D", "3D Max Files", true),
  DWG("DWG", "DWG Files", true),
  DXF("DXF", "DXF Files", true),
  LECTURES("LECTURES", "Lectures", true),
  DRAWINGS("DRAWINGS", "Drawings", true),
  SKETCHUP("SKETCHUP", "SketchUp Files", true);

  fun localizedLabel(s: AcademyStrings): String = when (this) {
    PDF_LESSONS -> s.catPdfLessons
    VIDEOS -> s.catVideos
    PICTURES -> s.catPictures
    NAQSHAY -> s.catNaqshay
    MAX_3D -> s.cat3dMax
    DWG -> s.catDwg
    DXF -> s.catDxf
    LECTURES -> s.catLectures
    DRAWINGS -> s.catDrawings
    SKETCHUP -> s.catSketchUp
  }

  companion object {
    fun fromId(id: String?): ResourceCategory =
      entries.firstOrNull { it.id == id } ?: PDF_LESSONS

    fun homeCategories(): List<ResourceCategory> = entries.filter { it.homeFeatured }
  }
}

enum class ResourceFileType(val ext: String, val mimeType: String) {
  PDF("pdf", "application/pdf"),
  MP4("mp4", "video/mp4"),
  JPG("jpg", "image/jpeg"),
  DWG("dwg", "application/acad"),
  DXF("dxf", "application/dxf"),
  MAX("max", "application/octet-stream"),
  SKP("skp", "application/octet-stream");

  companion object {
    fun fromString(raw: String?): ResourceFileType =
      entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: PDF
  }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val uid: String = "",
  val fullName: String = "",
  val email: String = "",
  val photoUrl: String? = null,
  val role: String = "USER",
  val accountStatus: String = "ACTIVE",
  val preferredLanguage: String = "EN",
  val trialStatus: String = "NOT_STARTED",
  val trialStartAt: Timestamp? = null,
  val trialExpiresAt: Timestamp? = null,
  val trialActivatedMethod: String? = null,
  val subscriptionStatus: String = "NONE",
  val subscriptionPlanId: String? = null,
  val subscriptionExpiresAt: Timestamp? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class ResourceItem(
  @DocumentId val id: String = "",
  val title: String = "",
  val description: String = "",
  val categoryId: String = "PDF_LESSONS",
  val fileUrl: String = "",
  val thumbnailUrl: String? = null,
  val fileName: String = "",
  val fileType: String = "PDF",
  val fileSizeLabel: String = "0 KB",
  val fileSizeBytes: Long = 0L,
  val premium: Boolean = false,
  val published: Boolean = true,
  val archived: Boolean = false,
  val uploadedBy: String = "",
  val plotSize: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class CourseItem(
  @DocumentId val id: String = "",
  val title: String = "",
  val description: String = "",
  val instructorName: String = "",
  val instructorBio: String? = null,
  val thumbnailUrl: String? = null,
  val categoryId: String = "LECTURES",
  val level: String? = "Intermediate",
  val totalLessons: Long = 0L,
  val durationLabel: String = "2h 30m",
  val premium: Boolean = false,
  val published: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class CourseModuleItem(
  @DocumentId val id: String = "",
  val courseId: String = "",
  val title: String = "",
  val orderIndex: Long = 0L,
  val published: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class CourseLessonItem(
  @DocumentId val id: String = "",
  val courseId: String = "",
  val moduleId: String = "",
  val title: String = "",
  val description: String? = null,
  val lessonType: String = "VIDEO",
  val videoUrl: String? = null,
  val pdfUrl: String? = null,
  val durationLabel: String = "10 min",
  val orderIndex: Long = 0L,
  val premium: Boolean = false,
  val published: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class CourseProgress(
  @DocumentId val id: String = "",
  val userId: String = "",
  val courseId: String = "",
  val completedLessonIds: List<String> = emptyList(),
  val lastLessonId: String? = null,
  val progressPercent: Long = 0L,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class SubscriptionPlan(
  @DocumentId val id: String = "",
  val title: String = "",
  val description: String = "",
  val pricePkr: Long = 0L,
  val durationDays: Long = 30L,
  val accessLevel: String = "ALL_ACCESS",
  val available: Boolean = true,
  val badgeText: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class SubscriptionRecord(
  @DocumentId val id: String = "",
  val userId: String = "",
  val planId: String = "",
  val status: String = "ACTIVE",
  val startAt: Timestamp? = null,
  val expiresAt: Timestamp? = null,
  val paymentRequestId: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class PaymentRequest(
  @DocumentId val id: String = "",
  val userId: String = "",
  val userEmail: String = "",
  val userName: String = "",
  val planId: String = "",
  val planTitle: String = "",
  val provider: String = "JAZZCASH",
  val amountPkr: Long = 0L,
  val transactionReference: String = "",
  val proofLabel: String? = null,
  val proofBase64: String? = null,
  val status: String = "PENDING",
  val verificationNote: String? = null,
  val reviewedBy: String? = null,
  val reviewedAt: Timestamp? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class AcademyNotification(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val body: String = "",
  val type: String = "WELCOME",
  val read: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class FavoriteItem(
  @DocumentId val id: String = "",
  val userId: String = "",
  val resourceId: String = "",
  val title: String = "",
  val categoryId: String = "",
  val fileType: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class DownloadLog(
  @DocumentId val id: String = "",
  val userId: String = "",
  val resourceId: String = "",
  val title: String = "",
  val categoryId: String = "",
  val fileType: String = "",
  val fileName: String = "",
  val fileSizeBytes: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class AppSettings(
  @DocumentId val id: String = "academy_config",
  val academyName: String = "Zahid Architecture Academy",
  val supportEmail: String? = "support@zahidarchitecture.academy",
  val whatsappNumber: String = "+923467803931",
  val whatsappDefaultMessage: String = "Hello Zahid Architecture Academy, I need assistance with my account.",
  val trialDurationDays: Long = 7L,
  val trialEnabled: Boolean = true,
  val jazzCashAccountName: String = "Zahid Architecture Academy",
  val jazzCashAccountNumber: String = "Configure in Admin Settings",
  val jazzCashInstructions: String = "Send the exact plan amount via JazzCash Mobile Account, then enter your Transaction ID (TID) and attach the payment screenshot below.",
  val easypaisaAccountName: String = "Zahid Architecture Academy",
  val easypaisaAccountNumber: String = "Configure in Admin Settings",
  val easypaisaInstructions: String = "Send the exact plan amount via Easypaisa App/USSD, then enter your Transaction Reference ID and attach the screenshot for admin approval.",
  val defaultLanguage: String = "EN",
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class AdminLog(
  @DocumentId val id: String = "",
  val actorUid: String = "",
  val actorEmail: String = "",
  val action: String = "",
  val targetType: String = "",
  val targetId: String = "",
  val details: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

data class LocalDownloadedFile(
  val resourceId: String,
  val title: String,
  val categoryId: String,
  val fileType: String,
  val fileName: String,
  val fileSizeBytes: Long,
  val savedPathOrUri: String,
  val displayLocation: String,
  val isTreeUri: Boolean,
  val downloadedAtMs: Long,
)

sealed interface DownloadState {
  data object Idle : DownloadState
  data class InProgress(
    val resourceId: String,
    val fileName: String,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val percent: Int,
  ) : DownloadState
  data class Completed(val localFile: LocalDownloadedFile) : DownloadState
  data class Failed(val resourceId: String, val message: String) : DownloadState
}

enum class EffectiveAccessState {
  PRO_ACTIVE,
  TRIAL_ACTIVE,
  TRIAL_EXPIRING_SOON,
  TRIAL_EXPIRED,
  NOT_STARTED,
  SUSPENDED,
}

object AccessEvaluator {
  fun effectiveAccessState(profile: UserProfile?, nowMs: Long = System.currentTimeMillis()): EffectiveAccessState {
    if (profile == null) return EffectiveAccessState.NOT_STARTED
    if (profile.accountStatus == "SUSPENDED") return EffectiveAccessState.SUSPENDED

    val subExpMs = profile.subscriptionExpiresAt?.toDate()?.time
    if (profile.subscriptionStatus == "ACTIVE" && (subExpMs == null || subExpMs > nowMs)) {
      return EffectiveAccessState.PRO_ACTIVE
    }

    val trialExpMs = profile.trialExpiresAt?.toDate()?.time
    if (profile.trialStatus in listOf("ACTIVE", "EXPIRING_SOON") && trialExpMs != null) {
      val remainingMs = trialExpMs - nowMs
      return when {
        remainingMs <= 0L -> EffectiveAccessState.TRIAL_EXPIRED
        remainingMs <= TimeUnit.HOURS.toMillis(48) -> EffectiveAccessState.TRIAL_EXPIRING_SOON
        else -> EffectiveAccessState.TRIAL_ACTIVE
      }
    }

    if (profile.trialStatus == "EXPIRED" || profile.subscriptionStatus == "EXPIRED") {
      return EffectiveAccessState.TRIAL_EXPIRED
    }

    return EffectiveAccessState.NOT_STARTED
  }

  fun hasPremiumAccess(profile: UserProfile?, nowMs: Long = System.currentTimeMillis()): Boolean {
    if (profile == null) return false
    if (profile.accountStatus == "SUSPENDED") return false
    if (UserRole.fromString(profile.role).isStaff()) return true
    return when (effectiveAccessState(profile, nowMs)) {
      EffectiveAccessState.PRO_ACTIVE,
      EffectiveAccessState.TRIAL_ACTIVE,
      EffectiveAccessState.TRIAL_EXPIRING_SOON -> true
      EffectiveAccessState.TRIAL_EXPIRED,
      EffectiveAccessState.NOT_STARTED,
      EffectiveAccessState.SUSPENDED -> false
    }
  }

  fun canAccessResource(profile: UserProfile?, resource: ResourceItem, nowMs: Long = System.currentTimeMillis()): Boolean {
    if (profile?.accountStatus == "SUSPENDED") return false
    if (!resource.premium) return true
    return hasPremiumAccess(profile, nowMs)
  }

  fun quotaCategoryKey(resource: ResourceItem): String =
    resource.plotSize?.takeIf { it.isNotBlank() }?.let { "Maps - $it" } ?: resource.categoryId

  fun remainingTrialDownloadsInCategory(
    profile: UserProfile?,
    quotaKey: String,
    downloadedCategoryCounts: Map<String, Int>,
    nowMs: Long = System.currentTimeMillis(),
  ): Int? {
    if (profile?.accountStatus == "SUSPENDED") return 0
    if (UserRole.fromString(profile?.role).isStaff()) return null
    val state = effectiveAccessState(profile, nowMs)
    return when (state) {
      EffectiveAccessState.PRO_ACTIVE -> null // Unlimited
      EffectiveAccessState.TRIAL_ACTIVE,
      EffectiveAccessState.TRIAL_EXPIRING_SOON -> (1 - (downloadedCategoryCounts[quotaKey] ?: 0)).coerceAtLeast(0)
      else -> 0
    }
  }

  fun canDownloadInCategory(
    profile: UserProfile?,
    resource: ResourceItem,
    downloadedCategoryCounts: Map<String, Int>,
    alreadyDownloadedIds: Set<String> = emptySet(),
    nowMs: Long = System.currentTimeMillis(),
  ): Boolean {
    if (profile?.accountStatus == "SUSPENDED") return false
    if (resource.id in alreadyDownloadedIds) return true
    if (UserRole.fromString(profile?.role).isStaff()) return true
    val state = effectiveAccessState(profile, nowMs)
    if (state == EffectiveAccessState.PRO_ACTIVE) return true
    if (state == EffectiveAccessState.TRIAL_ACTIVE || state == EffectiveAccessState.TRIAL_EXPIRING_SOON) {
      val key = quotaCategoryKey(resource)
      return (downloadedCategoryCounts[key] ?: 0) < 1
    }
    return !resource.premium
  }

  fun trialDaysRemaining(profile: UserProfile?, nowMs: Long = System.currentTimeMillis()): Int {
    val exp = profile?.trialExpiresAt?.toDate()?.time ?: return 0
    val diff = exp - nowMs
    if (diff <= 0L) return 0
    return kotlin.math.ceil(diff.toDouble() / TimeUnit.DAYS.toMillis(1).toDouble()).toInt()
  }

  fun validatePaymentInput(transactionReference: String, amountPkr: Long): String? {
    val trimmed = transactionReference.trim()
    if (trimmed.length < 4) return "Enter a valid transaction reference (at least 4 characters)."
    if (amountPkr <= 0L) return "Invalid plan amount."
    return null
  }
}

object AcademyStarterCatalog {
  val starterResources: List<ResourceItem> = listOf(
    ResourceItem(
      id = "starter_dwg_floor_plan",
      title = "AutoCAD Floor Plan DWG File",
      description = "Complete 10 Marla contemporary residential floor plan with layered walls, furniture blocks, electrical layout, and plumbing lines.",
      categoryId = "DWG",
      fileUrl = "asset://academy/autocad_floor_plan.dwg",
      fileName = "autocad_floor_plan.dwg",
      fileType = "DWG",
      fileSizeLabel = "2.1 KB",
      fileSizeBytes = 2124L,
      premium = true,
      published = true,
      archived = false,
      uploadedBy = "academy",
    ),
    ResourceItem(
      id = "starter_3dmax_interior",
      title = "3D Max Interior File",
      description = "Modern Pakistani villa living lounge 3ds Max V-Ray interior lighting setup with timber louvers and travertine materials.",
      categoryId = "MAX_3D",
      fileUrl = "asset://academy/3dmax_interior_scene.max",
      fileName = "3dmax_interior_scene.max",
      fileType = "MAX",
      fileSizeLabel = "2.1 KB",
      fileSizeBytes = 2114L,
      premium = true,
      published = true,
      archived = false,
      uploadedBy = "academy",
    ),
    ResourceItem(
      id = "starter_dxf_5_marla",
      title = "House Map 5 Marla DXF",
      description = "25x45 ft 5 Marla residential Naqsha in universal vector DXF format with Master Bed, TV Lounge, Drawing Room, Kitchen & Car Porch.",
      categoryId = "DXF",
      fileUrl = "asset://academy/house_map_5_marla.dxf",
      fileName = "house_map_5_marla.dxf",
      fileType = "DXF",
      fileSizeLabel = "2.2 KB",
      fileSizeBytes = 2200L,
      premium = false,
      published = true,
      archived = false,
      uploadedBy = "academy",
    ),
    ResourceItem(
      id = "starter_pdf_standards",
      title = "5 & 10 Marla Planning Standards PDF",
      description = "3-page architectural handbook covering plot setbacks, AutoCAD layering standards, and V-Ray daylight setup.",
      categoryId = "PDF_LESSONS",
      fileUrl = "asset://academy/autocad_floor_plan_lesson.pdf",
      fileName = "autocad_floor_plan_lesson.pdf",
      fileType = "PDF",
      fileSizeLabel = "2.7 KB",
      fileSizeBytes = 2750L,
      premium = false,
      published = true,
      archived = false,
      uploadedBy = "academy",
    ),
    ResourceItem(
      id = "starter_video_cad_basics",
      title = "Architectural CAD & Villa Walkthrough Video",
      description = "Video lecture demonstrating floor plan drafting workflow transitioning into contemporary 3D facade visualization.",
      categoryId = "VIDEOS",
      fileUrl = "asset://academy/cad_drafting_basics.mp4",
      fileName = "cad_drafting_basics.mp4",
      fileType = "MP4",
      fileSizeLabel = "145 KB",
      fileSizeBytes = 148480L,
      premium = false,
      published = true,
      archived = false,
      uploadedBy = "academy",
    ),
    ResourceItem(
      id = "starter_pic_villa_render",
      title = "Modern Villa Elevation Gallery Render",
      description = "High-resolution golden hour architectural elevation render of a two-story contemporary residence.",
      categoryId = "PICTURES",
      fileUrl = "asset://academy/modern_villa_elevation.jpg",
      fileName = "modern_villa_elevation.jpg",
      fileType = "JPG",
      fileSizeLabel = "902 KB",
      fileSizeBytes = 923648L,
      premium = false,
      published = true,
      archived = false,
      uploadedBy = "academy",
    ),
    ResourceItem(
      id = "starter_naqsha_isometric",
      title = "Isometric Cutaway House Naqsha Map",
      description = "Detailed isometric residential Naqsha cutaway showing column grid, staircase circulation, and courtyard zoning.",
      categoryId = "NAQSHAY",
      fileUrl = "asset://academy/5_marla_blueprint_map.jpg",
      fileName = "5_marla_blueprint_map.jpg",
      fileType = "JPG",
      fileSizeLabel = "848 KB",
      fileSizeBytes = 868352L,
      premium = true,
      published = true,
      archived = false,
      uploadedBy = "academy",
      plotSize = "5 Marla",
    ),
    ResourceItem(
      id = "starter_sketchup_villa",
      title = "SketchUp Modern Villa 3D Model",
      description = "Clean SketchUp 3D architectural massing model with timber louvers, pergola terrace, and glazed facade.",
      categoryId = "SKETCHUP",
      fileUrl = "asset://academy/sketchup_house_model.skp",
      fileName = "sketchup_house_model_01.skp",
      fileType = "SKP",
      fileSizeLabel = "2.0 KB",
      fileSizeBytes = 2054L,
      premium = true,
      published = true,
      archived = false,
      uploadedBy = "academy",
    ),
    ResourceItem(
      id = "starter_lecture_guide",
      title = "Architectural Spatial Planning Lecture #1",
      description = "Comprehensive lecture handbook on residential zoning, sunlight orientation, and circulation corridors.",
      categoryId = "LECTURES",
      fileUrl = "asset://academy/autocad_floor_plan_lesson.pdf",
      fileName = "architecture_lecture_01.pdf",
      fileType = "PDF",
      fileSizeLabel = "2.7 KB",
      fileSizeBytes = 2750L,
      premium = false,
      published = true,
      archived = false,
      uploadedBy = "academy",
    ),
    ResourceItem(
      id = "starter_drawing_sheet",
      title = "Elevation & Section Working Drawing #1",
      description = "Detailed architectural working drawing with dimension lines, plinth levels, and joinery callouts.",
      categoryId = "DRAWINGS",
      fileUrl = "asset://academy/house_map_5_marla.dxf",
      fileName = "working_drawing_01.dxf",
      fileType = "DXF",
      fileSizeLabel = "2.2 KB",
      fileSizeBytes = 2200L,
      premium = false,
      published = true,
      archived = false,
      uploadedBy = "academy",
    ),
  )

  val plotSizes: List<String> = listOf("3 Marla", "5 Marla", "10 Marla", "1 Kanal")

  private fun generateLibrarySeries(): List<ResourceItem> {
    val result = mutableListOf<ResourceItem>()
    result.addAll(starterResources)

    data class Spec(
      val catId: String,
      val prefix: String,
      val label: String,
      val assetUrl: String,
      val ext: String,
      val fileType: String,
      val sizeLabel: String,
      val sizeBytes: Long,
      val startIndex: Int,
    )

    val specs = listOf(
      Spec("LECTURES", "lec", "Architecture Studio Lecture", "asset://academy/autocad_floor_plan_lesson.pdf", "pdf", "PDF", "2.7 KB", 2750L, 2),
      Spec("DRAWINGS", "drw", "Architectural Working Drawing", "asset://academy/house_map_5_marla.dxf", "dxf", "DXF", "2.2 KB", 2200L, 2),
      Spec("VIDEOS", "vid", "CAD & 3D Visualization Video", "asset://academy/cad_drafting_basics.mp4", "mp4", "MP4", "145 KB", 148480L, 2),
      Spec("DWG", "dwg", "AutoCAD DWG Blueprint", "asset://academy/autocad_floor_plan.dwg", "dwg", "DWG", "2.1 KB", 2124L, 2),
      Spec("DXF", "dxf", "Vector Floor Plan DXF", "asset://academy/house_map_5_marla.dxf", "dxf", "DXF", "2.2 KB", 2200L, 2),
      Spec("MAX_3D", "max", "3ds Max V-Ray Scene", "asset://academy/3dmax_interior_scene.max", "max", "MAX", "2.1 KB", 2114L, 2),
      Spec("SKETCHUP", "skp", "SketchUp 3D Villa Model", "asset://academy/sketchup_house_model.skp", "skp", "SKP", "2.0 KB", 2054L, 2),
      Spec("PDF_LESSONS", "pdf", "Planning Standards PDF Lesson", "asset://academy/autocad_floor_plan_lesson.pdf", "pdf", "PDF", "2.7 KB", 2750L, 2),
      Spec("PICTURES", "pic", "Modern Villa Elevation Render", "asset://academy/modern_villa_elevation.jpg", "jpg", "JPG", "902 KB", 923648L, 2),
    )

    specs.forEach { s ->
      for (i in s.startIndex..20) {
        result.add(
          ResourceItem(
            id = "cat_${s.prefix}_$i",
            title = "${s.label} #$i",
            description = "Zahid Architecture Academy ${s.label} Sheet #$i — ready for in-app inspection and offline studio download.",
            categoryId = s.catId,
            fileUrl = s.assetUrl,
            fileName = "${s.prefix}_sheet_%02d.${s.ext}".format(i),
            fileType = s.fileType,
            fileSizeLabel = s.sizeLabel,
            fileSizeBytes = s.sizeBytes,
            premium = i > 1,
            published = true,
            archived = false,
            uploadedBy = "academy",
          )
        )
      }
    }

    val plotStyles = listOf(
      "Contemporary Front",
      "Corner Ventilation Plan",
      "Double Unit Residence",
      "Spanish Arched Villa",
      "Minimalist Courtyard House",
    )
    plotSizes.forEach { size ->
      val slug = size.lowercase().replace(" ", "_")
      val existingCount = result.count { it.plotSize == size }
      for (i in (existingCount + 1)..20) {
        val isVector = i % 2 == 0
        val styleName = plotStyles[(i - 1) % plotStyles.size]
        result.add(
          ResourceItem(
            id = "map_${slug}_$i",
            title = "$size Map #$i — $styleName",
            description = "Complete $size residential Naqsha #$i featuring dimensioned bedrooms, kitchen, TV lounge, staircase & car porch.",
            categoryId = "NAQSHAY",
            fileUrl = if (isVector) "asset://academy/house_map_5_marla.dxf" else "asset://academy/5_marla_blueprint_map.jpg",
            fileName = "${slug}_map_%02d.${if (isVector) "dxf" else "jpg"}".format(i),
            fileType = if (isVector) "DXF" else "JPG",
            fileSizeLabel = if (isVector) "2.2 KB" else "848 KB",
            fileSizeBytes = if (isVector) 2200L else 868352L,
            premium = i > 1,
            published = true,
            archived = false,
            uploadedBy = "academy",
            plotSize = size,
          )
        )
      }
    }

    return result
  }

  val fullCatalogResources: List<ResourceItem> by lazy { generateLibrarySeries() }

  val starterCourses: List<CourseItem> = listOf(
    CourseItem(
      id = "course_autocad_mastery",
      title = "AutoCAD 2D Residential Planning & Drafting",
      description = "Step-by-step architectural drafting course covering 5 Marla, 10 Marla, and 1 Kanal working drawings, municipal submission sheets, and DXF/DWG exports.",
      instructorName = "Ar. Zahid Mahmood",
      instructorBio = "Principal Architect & CAD Educator with 14+ years of residential and commercial design experience.",
      categoryId = "LECTURES",
      level = "Beginner to Pro",
      totalLessons = 3L,
      durationLabel = "4h 15m",
      premium = false,
      published = true,
    ),
    CourseItem(
      id = "course_3dmax_vray",
      title = "3ds Max & V-Ray Photorealistic Exterior Visualization",
      description = "Learn architectural modeling from AutoCAD plans, PBR materials, V-Ray Sun/Sky lighting, and high-resolution villa renders.",
      instructorName = "Ar. Zahid Mahmood",
      instructorBio = "Principal Architect & Visualization Director.",
      categoryId = "MAX_3D",
      level = "Intermediate",
      totalLessons = 2L,
      durationLabel = "6h 40m",
      premium = true,
      published = true,
    ),
    CourseItem(
      id = "course_naqshay_design",
      title = "Pakistani House Maps (Naqshay) & Space Optimization",
      description = "Master cross-ventilation, solar orientation, staircase geometry, and structural column grids for urban plots.",
      instructorName = "Engr. Hamza Tariq",
      instructorBio = "Senior Structural & Residential Planner.",
      categoryId = "NAQSHAY",
      level = "All Levels",
      totalLessons = 2L,
      durationLabel = "3h 20m",
      premium = true,
      published = true,
    ),
  )

  val starterModules: List<CourseModuleItem> = listOf(
    CourseModuleItem(id = "mod_cad_1", courseId = "course_autocad_mastery", title = "Module 1: Plot Layout & Layer Standards", orderIndex = 1L, published = true),
    CourseModuleItem(id = "mod_max_1", courseId = "course_3dmax_vray", title = "Module 1: CAD Import & Facade Modeling", orderIndex = 1L, published = true),
    CourseModuleItem(id = "mod_naq_1", courseId = "course_naqshay_design", title = "Module 1: 5 Marla & 10 Marla Zoning", orderIndex = 1L, published = true),
  )

  val starterLessons: List<CourseLessonItem> = listOf(
    CourseLessonItem(
      id = "les_cad_1",
      courseId = "course_autocad_mastery",
      moduleId = "mod_cad_1",
      title = "1. Drafting Workflow & Villa Walkthrough",
      description = "Introduction to architectural CAD layers, wall thicknesses, and residential spatial flow.",
      lessonType = "VIDEO",
      videoUrl = "asset://academy/cad_drafting_basics.mp4",
      pdfUrl = "asset://academy/autocad_floor_plan_lesson.pdf",
      durationLabel = "12 min",
      orderIndex = 1L,
      premium = false,
      published = true,
    ),
    CourseLessonItem(
      id = "les_cad_2",
      courseId = "course_autocad_mastery",
      moduleId = "mod_cad_1",
      title = "2. 5 & 10 Marla Planning Standards Guide",
      description = "Detailed PDF reference covering room clearances, car porch sizes, and ventilation ratios.",
      lessonType = "PDF",
      pdfUrl = "asset://academy/autocad_floor_plan_lesson.pdf",
      durationLabel = "18 min",
      orderIndex = 2L,
      premium = false,
      published = true,
    ),
    CourseLessonItem(
      id = "les_cad_3",
      courseId = "course_autocad_mastery",
      moduleId = "mod_cad_1",
      title = "3. Inspecting 5 Marla Vector DXF Blueprint",
      description = "Open and inspect the interactive 5 Marla DXF layout with room dimensions.",
      lessonType = "DRAWING",
      pdfUrl = "asset://academy/house_map_5_marla.dxf",
      durationLabel = "15 min",
      orderIndex = 3L,
      premium = false,
      published = true,
    ),
    CourseLessonItem(
      id = "les_max_1",
      courseId = "course_3dmax_vray",
      moduleId = "mod_max_1",
      title = "1. Exterior Elevation Lighting & Render Walkthrough",
      description = "V-Ray daylight angles and facade geometry breakdown.",
      lessonType = "VIDEO",
      videoUrl = "asset://academy/cad_drafting_basics.mp4",
      pdfUrl = "asset://academy/autocad_floor_plan_lesson.pdf",
      durationLabel = "25 min",
      orderIndex = 1L,
      premium = true,
      published = true,
    ),
    CourseLessonItem(
      id = "les_max_2",
      courseId = "course_3dmax_vray",
      moduleId = "mod_max_1",
      title = "2. 3D Max Lighting Checklist PDF",
      description = "Camera ISO, shutter speed, and PBR shader settings.",
      lessonType = "PDF",
      pdfUrl = "asset://academy/autocad_floor_plan_lesson.pdf",
      durationLabel = "14 min",
      orderIndex = 2L,
      premium = true,
      published = true,
    ),
    CourseLessonItem(
      id = "les_naq_1",
      courseId = "course_naqshay_design",
      moduleId = "mod_naq_1",
      title = "1. 5 Marla Isometric Zoning & Columns",
      description = "Practical column grid and courtyard lighting layout.",
      lessonType = "DRAWING",
      pdfUrl = "asset://academy/house_map_5_marla.dxf",
      durationLabel = "20 min",
      orderIndex = 1L,
      premium = true,
      published = true,
    ),
    CourseLessonItem(
      id = "les_naq_2",
      courseId = "course_naqshay_design",
      moduleId = "mod_naq_1",
      title = "2. Residential By-Laws & Setbacks PDF",
      description = "Essential building code guidelines for urban plots.",
      lessonType = "PDF",
      pdfUrl = "asset://academy/autocad_floor_plan_lesson.pdf",
      durationLabel = "16 min",
      orderIndex = 2L,
      premium = true,
      published = true,
    ),
  )

  val starterPlans: List<SubscriptionPlan> = listOf(
    SubscriptionPlan(
      id = "plan_monthly_pro",
      title = "Monthly Pro",
      description = "30-day unlimited access to all 20+ DWG, DXF, 3D Max, SketchUp files, Plot Maps, and courses.",
      pricePkr = 999L,
      durationDays = 30L,
      accessLevel = "ALL_ACCESS",
      available = true,
      badgeText = "Popular",
    ),
    SubscriptionPlan(
      id = "plan_yearly_studio",
      title = "Yearly Studio",
      description = "365-day unlimited academy membership with all new house maps and CAD libraries.",
      pricePkr = 9999L,
      durationDays = 365L,
      accessLevel = "ALL_ACCESS",
      available = true,
      badgeText = "Best Value",
    ),
  )
}

data class PlotConversionResult(
  val sqFeet: Double,
  val sqMeters: Double,
  val marla: Double,
  val kanal: Double,
)

data class MaterialEstimateResult(
  val totalCoveredSqFt: Double,
  val bricksCount: Long,
  val cementBags: Long,
  val sandCuFt: Long,
  val crushCuFt: Long,
  val steelKg: Long,
)

data class StaircaseResult(
  val riserCount: Int,
  val exactRiserInches: Double,
  val treadCount: Int,
  val recommendedTreadInches: Double,
  val totalRunFeet: Double,
)

object ArchitectureCalculators {
  fun convertFromMarla(marla: Double, sqFtPerMarla: Double = 225.0): PlotConversionResult {
    val safeMarla = marla.coerceAtLeast(0.0)
    val safeUnit = sqFtPerMarla.coerceAtLeast(1.0)
    val sqFt = safeMarla * safeUnit
    val sqM = sqFt * 0.09290304
    val kanal = safeMarla / 20.0
    return PlotConversionResult(
      sqFeet = sqFt,
      sqMeters = sqM,
      marla = safeMarla,
      kanal = kanal,
    )
  }

  fun convertFromSqFeet(sqFeet: Double, sqFtPerMarla: Double = 225.0): PlotConversionResult {
    val safeSqFt = sqFeet.coerceAtLeast(0.0)
    val safeUnit = sqFtPerMarla.coerceAtLeast(1.0)
    val marla = safeSqFt / safeUnit
    return convertFromMarla(marla, safeUnit)
  }

  fun estimateGreyStructure(coveredSqFtPerFloor: Double, floors: Int): MaterialEstimateResult {
    val safeArea = coveredSqFtPerFloor.coerceAtLeast(0.0) * floors.coerceIn(1, 10)
    // Standard Pakistan residential grey-structure architectural thumb-rules per sq.ft
    return MaterialEstimateResult(
      totalCoveredSqFt = safeArea,
      bricksCount = kotlin.math.round(safeArea * 34.0).toLong(),
      cementBags = kotlin.math.round(safeArea * 0.42).toLong(),
      sandCuFt = kotlin.math.round(safeArea * 1.85).toLong(),
      crushCuFt = kotlin.math.round(safeArea * 1.25).toLong(),
      steelKg = kotlin.math.round(safeArea * 3.8).toLong(),
    )
  }

  fun calculateStaircase(floorHeightFeet: Double, targetRiserInches: Double = 7.0): StaircaseResult {
    val heightIn = (floorHeightFeet.coerceIn(6.0, 25.0)) * 12.0
    val safeTarget = targetRiserInches.coerceIn(5.0, 8.5)
    val risers = kotlin.math.round(heightIn / safeTarget).toInt().coerceAtLeast(2)
    val exactRiser = heightIn / risers.toDouble()
    val treads = (risers - 1).coerceAtLeast(1)
    // Blondel comfort formula: 2R + T = 24.5" -> T = 24.5 - 2R (clamped 9.5"..12.0")
    val treadDepth = (24.5 - 2.0 * exactRiser).coerceIn(9.5, 12.0)
    val totalRunFt = (treads * treadDepth) / 12.0
    return StaircaseResult(
      riserCount = risers,
      exactRiserInches = exactRiser,
      treadCount = treads,
      recommendedTreadInches = treadDepth,
      totalRunFeet = totalRunFt,
    )
  }

  fun estimateConstructionCost(
    coveredSqFt: Double,
    greyRatePerSqFt: Long = 2650L,
    finishingRatePerSqFt: Long = 3150L,
  ): ConstructionCostResult {
    val safeArea = coveredSqFt.coerceAtLeast(0.0)
    val safeGrey = greyRatePerSqFt.coerceAtLeast(0L)
    val safeFinish = finishingRatePerSqFt.coerceAtLeast(0L)
    val greyTotal = kotlin.math.round(safeArea * safeGrey).toLong()
    val finishTotal = kotlin.math.round(safeArea * safeFinish).toLong()
    return ConstructionCostResult(
      coveredSqFt = safeArea,
      greyStructurePkr = greyTotal,
      finishingPkr = finishTotal,
      turnkeyTotalPkr = greyTotal + finishTotal,
    )
  }

  fun convertDrawingScale(
    realLengthFeet: Double,
    scaleDenominator: Int,
  ): ScaleConversionResult {
    val safeFeet = realLengthFeet.coerceAtLeast(0.0)
    val safeScale = scaleDenominator.coerceIn(1, 5000)
    val realMeters = safeFeet * 0.3048
    val paperMm = (realMeters * 1000.0) / safeScale.toDouble()
    val paperInches = (safeFeet * 12.0) / safeScale.toDouble()
    return ScaleConversionResult(
      realLengthFeet = safeFeet,
      realLengthMeters = realMeters,
      scaleDenominator = safeScale,
      paperLengthCm = paperMm / 10.0,
      paperLengthInches = paperInches,
    )
  }
}

data class ConstructionCostResult(
  val coveredSqFt: Double,
  val greyStructurePkr: Long,
  val finishingPkr: Long,
  val turnkeyTotalPkr: Long,
)

data class ScaleConversionResult(
  val realLengthFeet: Double,
  val realLengthMeters: Double,
  val scaleDenominator: Int,
  val paperLengthCm: Double,
  val paperLengthInches: Double,
)

data class ArchitectureByLaw(
  val plotSize: String,
  val dimensions: String,
  val frontSetback: String,
  val rearSetback: String,
  val sideSetback: String,
  val maxCoverage: String,
  val maxHeight: String,
  val minRoomSizes: String,
)

object ByLawsCatalog {
  val standards: List<ArchitectureByLaw> = listOf(
    ArchitectureByLaw(
      plotSize = "3 Marla",
      dimensions = "20' × 33.75' (675 sq.ft)",
      frontSetback = "5 ft",
      rearSetback = "Open OTS / 5 ft",
      sideSetback = "0 ft (Attached)",
      maxCoverage = "80% – 85%",
      maxHeight = "35 ft (G + 1 + Mumty)",
      minRoomSizes = "Bedroom: 10'×11' • Kitchen: 6'×8' • Bath: 4'6\"×6' • Porch: 9'×13'",
    ),
    ArchitectureByLaw(
      plotSize = "5 Marla",
      dimensions = "25' × 45' (1,125 sq.ft)",
      frontSetback = "5 ft – 10 ft",
      rearSetback = "5 ft",
      sideSetback = "0 ft (Attached)",
      maxCoverage = "75% – 80%",
      maxHeight = "38 ft (G + 1 + Mumty)",
      minRoomSizes = "Bedroom: 11'×13' • TV Lounge: 13'×15' • Kitchen: 7'×10' • Porch: 10'×15'",
    ),
    ArchitectureByLaw(
      plotSize = "10 Marla",
      dimensions = "35' × 65' (2,275 sq.ft)",
      frontSetback = "10 ft",
      rearSetback = "7 ft",
      sideSetback = "5 ft (One Side)",
      maxCoverage = "70%",
      maxHeight = "38 ft (G + 1 + Mumty)",
      minRoomSizes = "Master Bed: 13'×16' • Drawing: 12'×16' • Kitchen: 9'×12' • Porch: 12'×18'",
    ),
    ArchitectureByLaw(
      plotSize = "1 Kanal",
      dimensions = "50' × 90' (4,500 sq.ft)",
      frontSetback = "15 ft – 20 ft",
      rearSetback = "10 ft",
      sideSetback = "5 ft + 10 ft",
      maxCoverage = "60% – 65%",
      maxHeight = "38 ft – 42 ft (Basement + G + 1)",
      minRoomSizes = "Master Bed: 15'×18' • Lounge: 16'×20' • Kitchen: 11'×14' • Servant Qtr: 8'×10'",
    ),
  )
}

data class CadShortcut(
  val software: String,
  val key: String,
  val command: String,
  val purpose: String,
)

object CadShortcutsCatalog {
  val items: List<CadShortcut> = listOf(
    CadShortcut("AutoCAD", "L", "LINE", "Draw straight wall & grid segments"),
    CadShortcut("AutoCAD", "PL", "PLINE", "Draw continuous closed polyline boundary"),
    CadShortcut("AutoCAD", "O", "OFFSET", "Offset 9\" / 4.5\" walls and plot setbacks"),
    CadShortcut("AutoCAD", "TR", "TRIM", "Trim intersecting walls and door openings"),
    CadShortcut("AutoCAD", "EX", "EXTEND", "Extend lines cleanly to boundary edges"),
    CadShortcut("AutoCAD", "MI", "MIRROR", "Mirror duplex plans, doors, or staircases"),
    CadShortcut("AutoCAD", "B / I", "BLOCK / INSERT", "Create & place reusable furniture or door blocks"),
    CadShortcut("AutoCAD", "LA", "LAYER", "Manage Walls, Doors, Electrical & Plumbing layers"),
    CadShortcut("AutoCAD", "DI", "DIST", "Measure room clear span and corridor width"),
    CadShortcut("AutoCAD", "H", "HATCH", "Apply RCC concrete, brick, or tile floor hatch"),
    CadShortcut("AutoCAD", "F", "FILLET", "Join corner wall lines cleanly with radius 0"),
    CadShortcut("AutoCAD", "CTRL+1", "PROPERTIES", "Inspect line weight, layer & block scale"),
    CadShortcut("3ds Max", "W / E / R", "Move / Rotate / Scale", "Transform exterior elevation elements"),
    CadShortcut("3ds Max", "M", "Material Editor", "Assign V-Ray travertine, wood & glass shaders"),
    CadShortcut("3ds Max", "F9 / F10", "Render / Setup", "Quick render preview and V-Ray output settings"),
    CadShortcut("3ds Max", "G", "Toggle Grid", "Show or hide modeling viewport reference grid"),
    CadShortcut("3ds Max", "ALT+W", "Maximize Viewport", "Toggle single full viewport vs 4-view quad"),
    CadShortcut("3ds Max", "SHIFT+Q", "Quick Render", "Render active camera perspective immediately"),
    CadShortcut("SketchUp", "P", "Push / Pull", "Extrude 2D CAD floor plan walls into 3D mass"),
    CadShortcut("SketchUp", "T", "Tape Measure", "Create architectural guides & check clearance"),
    CadShortcut("SketchUp", "F", "Offset Tool", "Offset parapet walls, window frames & louvers"),
    CadShortcut("SketchUp", "G / B", "Component / Paint", "Make reusable window components & apply textures"),
    CadShortcut("SketchUp", "M", "Move / Array", "Move & copy staircase treads or louvers evenly"),
    CadShortcut("SketchUp", "O / H", "Orbit / Pan", "Inspect 3D exterior pergola & facade elevation"),
  )
}


