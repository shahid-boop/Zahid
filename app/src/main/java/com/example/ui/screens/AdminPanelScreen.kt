package com.example.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.i18n.AcademyStrings
import com.example.model.AdminLog
import com.example.model.AppSettings
import com.example.model.CourseItem
import com.example.model.CourseLessonItem
import com.example.model.PaymentRequest
import com.example.model.ResourceCategory
import com.example.model.ResourceItem
import com.example.model.SubscriptionPlan
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.ui.components.AcademySearchBar
import com.example.ui.components.AdminStatCard
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SuccessGreen

@Composable
fun AdminPanelScreen(
  adminProfile: UserProfile?,
  users: List<UserProfile>,
  resources: List<ResourceItem>,
  courses: List<CourseItem>,
  payments: List<PaymentRequest>,
  plans: List<SubscriptionPlan>,
  appSettings: AppSettings,
  adminLogs: List<AdminLog>,
  strings: AcademyStrings,
  onApprovePayment: (PaymentRequest, String) -> Unit,
  onRejectPayment: (PaymentRequest, String) -> Unit,
  onUpdateUser: (UserProfile, String, String) -> Unit,
  onSaveResource: (ResourceItem) -> Unit,
  onSaveCourse: (CourseItem) -> Unit,
  onSaveLesson: (CourseLessonItem) -> Unit = {},
  onSavePlan: (SubscriptionPlan) -> Unit,
  onSaveSettings: (AppSettings) -> Unit,
  onSeedCatalog: () -> Unit,
) {
  val role = UserRole.fromString(adminProfile?.role)
  if (adminProfile == null || !role.isStaff()) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text("Staff access required", style = MaterialTheme.typography.titleMedium, color = ErrorRed)
    }
    return
  }

  var selectedTab by remember { mutableIntStateOf(0) }
  var showAddResourceDialog by remember { mutableStateOf(false) }
  var showAddCourseDialog by remember { mutableStateOf(false) }
  var showAddLessonDialog by remember { mutableStateOf(false) }

  if (showAddResourceDialog) {
    AddResourceDialog(
      onDismiss = { showAddResourceDialog = false },
      onSave = { res ->
        onSaveResource(res)
        showAddResourceDialog = false
      },
    )
  }

  if (showAddCourseDialog) {
    AddCourseDialog(
      onDismiss = { showAddCourseDialog = false },
      onSave = { course ->
        onSaveCourse(course)
        showAddCourseDialog = false
      },
    )
  }

  if (showAddLessonDialog) {
    AddCourseLessonDialog(
      courses = courses,
      onDismiss = { showAddLessonDialog = false },
      onSave = { lesson ->
        onSaveLesson(lesson)
        showAddLessonDialog = false
      },
    )
  }

  val tabs = listOf(
    strings.adminOverviewTab,
    strings.adminPaymentsTab,
    strings.adminContentTab,
    strings.adminUsersTab,
    strings.adminSettingsTab,
  )

  Box(modifier = Modifier.fillMaxSize().testTag("admin_panel_screen"), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 900.dp).fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        Text(
          text = strings.adminPanelTitle,
          style = MaterialTheme.typography.headlineSmall,
          color = PrimaryNavy,
          fontWeight = FontWeight.Bold,
        )
      }

      item {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(tabs.indices.toList()) { idx ->
            FilterChip(
              selected = selectedTab == idx,
              onClick = { selectedTab = idx },
              label = { Text(tabs[idx]) },
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryNavy, selectedLabelColor = PureWhite),
              modifier = Modifier.testTag("admin_tab_$idx"),
            )
          }
        }
      }

      when (selectedTab) {
        0 -> { // Overview
          item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              AdminStatCard(label = strings.adminUsersTab, value = users.size.toString(), modifier = Modifier.weight(1f))
              AdminStatCard(label = strings.adminContentTab, value = "${resources.size} / ${courses.size}", accent = AccentOrange, modifier = Modifier.weight(1f))
            }
          }
          item {
            val pendingCount = payments.count { it.status == "PENDING" }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              AdminStatCard(label = "Pending Payments", value = pendingCount.toString(), accent = AccentOrange, modifier = Modifier.weight(1f))
              AdminStatCard(label = "Plans", value = plans.size.toString(), accent = SuccessGreen, modifier = Modifier.weight(1f))
            }
          }
          item {
            Button(
              onClick = onSeedCatalog,
              colors = ButtonDefaults.buttonColors(containerColor = AccentOrange, contentColor = PureWhite),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth().testTag("admin_seed_catalog_btn"),
            ) {
              Icon(Icons.Filled.Publish, contentDescription = null)
              Spacer(Modifier.width(8.dp))
              Text(strings.seedCatalogBtn)
            }
          }
          items(adminLogs.take(15), key = { it.id }) { log ->
            Card(
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              border = BorderStroke(1.dp, BorderSubtle),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("${log.action} • ${log.targetType}", style = MaterialTheme.typography.labelLarge, color = PrimaryNavy)
                Text(log.details, style = MaterialTheme.typography.bodySmall)
                Text("${log.actorEmail} • ${formatTimestamp(log.createdAt)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
        }

        1 -> { // Payments
          if (!role.canReviewPayments()) {
            item { Text("Permission denied for payment moderation.", color = ErrorRed) }
          } else {
            items(payments, key = { it.id }) { req ->
              AdminPaymentReviewCard(
                request = req,
                strings = strings,
                onApprove = { note -> onApprovePayment(req, note) },
                onReject = { note -> onRejectPayment(req, note) },
              )
            }
          }
        }

        2 -> { // Content
          if (!role.canManageContent()) {
            item { Text("Permission denied for content management.", color = ErrorRed) }
          } else {
            item {
              Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                  Button(
                    onClick = { showAddResourceDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy, contentColor = PureWhite),
                    modifier = Modifier.weight(1f).testTag("admin_add_resource_btn"),
                  ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(strings.addResourceBtn)
                  }
                  OutlinedButton(
                    onClick = { showAddCourseDialog = true },
                    modifier = Modifier.weight(1f).testTag("admin_add_course_btn"),
                  ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(strings.addCourseBtn)
                  }
                }
                if (courses.isNotEmpty()) {
                  OutlinedButton(
                    onClick = { showAddLessonDialog = true },
                    modifier = Modifier.fillMaxWidth().testTag("admin_add_lesson_btn"),
                  ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(strings.addLessonBtn)
                  }
                }
              }
            }
            items(resources, key = { it.id }) { res ->
              Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.padding(12.dp).fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(res.title, style = MaterialTheme.typography.titleSmall)
                    Text("${res.categoryId} • ${res.fileType} • ${if (res.premium) "Pro" else "Free"}", style = MaterialTheme.typography.bodySmall)
                  }
                  Switch(
                    checked = res.published && !res.archived,
                    onCheckedChange = { active ->
                      onSaveResource(res.copy(published = active, archived = !active))
                    },
                  )
                }
              }
            }
          }
        }

        3 -> { // Users
          if (!role.canManageUsersAndSettings()) {
            item { Text("Admin or Super Admin access required.", color = ErrorRed) }
          } else {
            item {
              var userQuery by remember { mutableStateOf("") }
              Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AcademySearchBar(query = userQuery, hint = "Search users by name or email...", onQueryChange = { userQuery = it })
                val shown = users.filter {
                  userQuery.isBlank() ||
                    it.fullName.contains(userQuery, true) ||
                    it.email.contains(userQuery, true)
                }
                shown.forEach { u ->
                  Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth(),
                  ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                      Text("${u.fullName} (${u.role})", style = MaterialTheme.typography.titleSmall, color = PrimaryNavy)
                      Text("${u.email} • Status: ${u.accountStatus}", style = MaterialTheme.typography.bodySmall)
                      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val isSuspended = u.accountStatus == "SUSPENDED"
                        OutlinedButton(
                          onClick = {
                            onUpdateUser(u, if (isSuspended) "ACTIVE" else "SUSPENDED", u.role)
                          },
                          enabled = u.role != "SUPER_ADMIN",
                        ) {
                          Text(if (isSuspended) strings.restoreBtn else strings.suspendBtn)
                        }
                        if (role == UserRole.SUPER_ADMIN && u.role != "SUPER_ADMIN") {
                          val nextRole = when (u.role) {
                            "USER" -> "CONTENT_MANAGER"
                            "CONTENT_MANAGER" -> "SUPPORT_AGENT"
                            "SUPPORT_AGENT" -> "ADMIN"
                            else -> "USER"
                          }
                          OutlinedButton(onClick = { onUpdateUser(u, u.accountStatus, nextRole) }) {
                            Text("Role -> $nextRole")
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }

        4 -> { // Settings (only place where WhatsApp number is editable & visible to Admin!)
          if (!role.canManageUsersAndSettings()) {
            item { Text("Admin or Super Admin access required.", color = ErrorRed) }
          } else {
            item {
              AdminSettingsEditor(
                initial = appSettings,
                strings = strings,
                onSave = onSaveSettings,
                onSavePlan = onSavePlan,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AdminPaymentReviewCard(
  request: PaymentRequest,
  strings: AcademyStrings,
  onApprove: (String) -> Unit,
  onReject: (String) -> Unit,
) {
  var note by remember { mutableStateOf(request.verificationNote ?: "") }
  val proofBitmap = remember(request.proofBase64) {
    request.proofBase64?.let { b64 ->
      try {
        val decoded = Base64.decode(b64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
      } catch (_: Exception) {
        null
      }
    }
  }

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, BorderSubtle),
    modifier = Modifier.fillMaxWidth().testTag("admin_payment_card_${request.id}"),
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text("${request.userName} (${request.userEmail})", style = MaterialTheme.typography.titleSmall, color = PrimaryNavy)
      Text("${request.planTitle} • ${request.provider} • PKR ${request.amountPkr}", style = MaterialTheme.typography.bodySmall)
      Text("Ref: ${request.transactionReference} • Status: ${request.status}", style = MaterialTheme.typography.labelLarge)

      proofBitmap?.let { bmp ->
        Image(
          bitmap = bmp.asImageBitmap(),
          contentDescription = "Payment screenshot",
          contentScale = ContentScale.Fit,
          modifier = Modifier.fillMaxWidth().height(160.dp),
        )
      }

      if (request.status == "PENDING") {
        OutlinedTextField(
          value = note,
          onValueChange = { note = it },
          label = { Text("Verification note") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
          Button(
            onClick = { onApprove(note) },
            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = PureWhite),
            modifier = Modifier.weight(1f).testTag("approve_payment_${request.id}"),
          ) {
            Text(strings.approveBtn)
          }
          OutlinedButton(
            onClick = { onReject(note) },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
            modifier = Modifier.weight(1f).testTag("reject_payment_${request.id}"),
          ) {
            Text(strings.rejectBtn)
          }
        }
      }
    }
  }
}

@Composable
private fun AdminSettingsEditor(
  initial: AppSettings,
  strings: AcademyStrings,
  onSave: (AppSettings) -> Unit,
  onSavePlan: (SubscriptionPlan) -> Unit,
) {
  var trialDays by remember(initial) { mutableStateOf(initial.trialDurationDays.toString()) }
  var trialEnabled by remember(initial) { mutableStateOf(initial.trialEnabled) }
  var jazzName by remember(initial) { mutableStateOf(initial.jazzCashAccountName) }
  var jazzNum by remember(initial) { mutableStateOf(initial.jazzCashAccountNumber) }
  var jazzInstr by remember(initial) { mutableStateOf(initial.jazzCashInstructions) }
  var easyName by remember(initial) { mutableStateOf(initial.easypaisaAccountName) }
  var easyNum by remember(initial) { mutableStateOf(initial.easypaisaAccountNumber) }
  var easyInstr by remember(initial) { mutableStateOf(initial.easypaisaInstructions) }
  var waNum by remember(initial) { mutableStateOf(initial.whatsappNumber) }
  var waMsg by remember(initial) { mutableStateOf(initial.whatsappDefaultMessage) }

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, BorderSubtle),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Text("Academy & Payment Configuration", style = MaterialTheme.typography.titleMedium, color = PrimaryNavy)
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Text("Enable 7-Day Free Trial", style = MaterialTheme.typography.bodyMedium)
        Switch(checked = trialEnabled, onCheckedChange = { trialEnabled = it })
      }
      OutlinedTextField(value = trialDays, onValueChange = { trialDays = it }, label = { Text("Trial Duration (Days)") }, modifier = Modifier.fillMaxWidth())
      OutlinedTextField(value = jazzName, onValueChange = { jazzName = it }, label = { Text("JazzCash Account Title") }, modifier = Modifier.fillMaxWidth())
      OutlinedTextField(value = jazzNum, onValueChange = { jazzNum = it }, label = { Text("JazzCash Number") }, modifier = Modifier.fillMaxWidth())
      OutlinedTextField(value = jazzInstr, onValueChange = { jazzInstr = it }, label = { Text("JazzCash Instructions") }, modifier = Modifier.fillMaxWidth())
      OutlinedTextField(value = easyName, onValueChange = { easyName = it }, label = { Text("Easypaisa Account Title") }, modifier = Modifier.fillMaxWidth())
      OutlinedTextField(value = easyNum, onValueChange = { easyNum = it }, label = { Text("Easypaisa Number") }, modifier = Modifier.fillMaxWidth())
      OutlinedTextField(value = easyInstr, onValueChange = { easyInstr = it }, label = { Text("Easypaisa Instructions") }, modifier = Modifier.fillMaxWidth())
      OutlinedTextField(value = waNum, onValueChange = { waNum = it }, label = { Text("Internal WhatsApp Support Number") }, modifier = Modifier.fillMaxWidth())
      OutlinedTextField(value = waMsg, onValueChange = { waMsg = it }, label = { Text("Default Support Message") }, modifier = Modifier.fillMaxWidth())

      Button(
        onClick = {
          onSave(
            initial.copy(
              trialEnabled = trialEnabled,
              trialDurationDays = trialDays.toLongOrNull()?.coerceIn(1L, 90L) ?: 7L,
              jazzCashAccountName = jazzName,
              jazzCashAccountNumber = jazzNum,
              jazzCashInstructions = jazzInstr,
              easypaisaAccountName = easyName,
              easypaisaAccountNumber = easyNum,
              easypaisaInstructions = easyInstr,
              whatsappNumber = waNum,
              whatsappDefaultMessage = waMsg,
            )
          )
        },
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy, contentColor = PureWhite),
        modifier = Modifier.fillMaxWidth().testTag("admin_save_settings_btn"),
      ) {
        Text(strings.saveSettingsBtn)
      }
    }
  }
}

@Composable
private fun AddResourceDialog(
  onDismiss: () -> Unit,
  onSave: (ResourceItem) -> Unit,
) {
  var title by remember { mutableStateOf("") }
  var desc by remember { mutableStateOf("") }
  var categoryId by remember { mutableStateOf("PDF_LESSONS") }
  var fileUrl by remember { mutableStateOf("asset://academy/autocad_floor_plan_lesson.pdf") }
  var fileName by remember { mutableStateOf("lesson.pdf") }
  var fileType by remember { mutableStateOf("PDF") }
  var sizeLabel by remember { mutableStateOf("100 KB") }
  var premium by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Publish Academy Resource") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
        OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") })
        OutlinedTextField(value = categoryId, onValueChange = { categoryId = it }, label = { Text("Category (PDF_LESSONS, DWG, DXF, MAX_3D, SKETCHUP, NAQSHAY, VIDEOS, PICTURES)") })
        OutlinedTextField(value = fileType, onValueChange = { fileType = it.uppercase() }, label = { Text("File Type (PDF, DWG, DXF, MAX, SKP, MP4, JPG)") })
        OutlinedTextField(value = fileName, onValueChange = { fileName = it }, label = { Text("File Name") })
        OutlinedTextField(value = fileUrl, onValueChange = { fileUrl = it }, label = { Text("HTTPS or asset:// URL") })
        OutlinedTextField(value = sizeLabel, onValueChange = { sizeLabel = it }, label = { Text("Size Label") })
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
          Text("Premium Resource")
          Switch(checked = premium, onCheckedChange = { premium = it })
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (title.isNotBlank() && fileUrl.isNotBlank()) {
            onSave(
              ResourceItem(
                title = title,
                description = desc,
                categoryId = ResourceCategory.fromId(categoryId).id,
                fileUrl = fileUrl,
                fileName = fileName.ifBlank { "resource.${fileType.lowercase()}" },
                fileType = fileType,
                fileSizeLabel = sizeLabel,
                fileSizeBytes = 102400L,
                premium = premium,
                published = true,
                archived = false,
              )
            )
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy, contentColor = PureWhite),
      ) {
        Text("Publish")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
private fun AddCourseDialog(
  onDismiss: () -> Unit,
  onSave: (CourseItem) -> Unit,
) {
  var title by remember { mutableStateOf("") }
  var desc by remember { mutableStateOf("") }
  var instructor by remember { mutableStateOf("Ar. Zahid Mahmood") }
  var duration by remember { mutableStateOf("2h 00m") }
  var premium by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Create Architecture Course") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Course Title") })
        OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") })
        OutlinedTextField(value = instructor, onValueChange = { instructor = it }, label = { Text("Instructor") })
        OutlinedTextField(value = duration, onValueChange = { duration = it }, label = { Text("Duration") })
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
          Text("Premium Course")
          Switch(checked = premium, onCheckedChange = { premium = it })
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (title.isNotBlank()) {
            onSave(
              CourseItem(
                title = title,
                description = desc,
                instructorName = instructor,
                durationLabel = duration,
                premium = premium,
                published = true,
              )
            )
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy, contentColor = PureWhite),
      ) {
        Text("Create")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
private fun AddCourseLessonDialog(
  courses: List<CourseItem>,
  onDismiss: () -> Unit,
  onSave: (CourseLessonItem) -> Unit,
) {
  var selectedCourseId by remember(courses) { mutableStateOf(courses.firstOrNull()?.id ?: "") }
  var title by remember { mutableStateOf("") }
  var desc by remember { mutableStateOf("") }
  var lessonType by remember { mutableStateOf("VIDEO") }
  var contentUrl by remember { mutableStateOf("asset://academy/cad_drafting_basics.mp4") }
  var duration by remember { mutableStateOf("12 min") }
  var premium by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Publish Course Lesson") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text("Target Course:", style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(courses, key = { it.id }) { c ->
            FilterChip(
              selected = selectedCourseId == c.id,
              onClick = { selectedCourseId = c.id },
              label = { Text(c.title.take(24)) },
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryNavy, selectedLabelColor = PureWhite),
            )
          }
        }
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Lesson Title") })
        OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") })
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf("VIDEO", "PDF", "DRAWING").forEach { type ->
            FilterChip(
              selected = lessonType == type,
              onClick = { lessonType = type },
              label = { Text(type) },
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AccentOrange, selectedLabelColor = PureWhite),
            )
          }
        }
        OutlinedTextField(value = contentUrl, onValueChange = { contentUrl = it }, label = { Text("HTTPS or asset:// URL") })
        OutlinedTextField(value = duration, onValueChange = { duration = it }, label = { Text("Duration Label") })
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
          Text("Premium Lesson")
          Switch(checked = premium, onCheckedChange = { premium = it })
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (selectedCourseId.isNotBlank() && title.isNotBlank() && contentUrl.isNotBlank()) {
            onSave(
              CourseLessonItem(
                courseId = selectedCourseId,
                moduleId = "",
                title = title.trim(),
                description = desc.trim().takeIf { it.isNotEmpty() },
                lessonType = lessonType,
                videoUrl = if (lessonType == "VIDEO") contentUrl.trim() else null,
                pdfUrl = if (lessonType != "VIDEO") contentUrl.trim() else null,
                durationLabel = duration.ifBlank { "10 min" },
                orderIndex = 1L,
                premium = premium,
                published = true,
              )
            )
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy, contentColor = PureWhite),
      ) {
        Text("Publish")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

