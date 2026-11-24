package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.i18n.AcademyStrings
import com.example.model.AccessEvaluator
import com.example.model.CourseItem
import com.example.model.CourseLessonItem
import com.example.model.CourseModuleItem
import com.example.model.CourseProgress
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.ui.components.AcademySearchBar
import com.example.ui.components.CourseCard
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.NavyTint
import com.example.ui.theme.OrangeTint
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SuccessGreen

@Composable
fun CoursesScreen(
  courses: List<CourseItem>,
  courseProgress: List<CourseProgress>,
  strings: AcademyStrings,
  onCourseClick: (CourseItem) -> Unit,
) {
  var query by remember { mutableStateOf("") }
  val progressByCourse = remember(courseProgress) { courseProgress.associateBy { it.courseId } }

  val filteredCourses = remember(courses, query) {
    val q = query.trim().lowercase()
    if (q.isEmpty()) courses
    else courses.filter {
      it.title.lowercase().contains(q) ||
        it.description.lowercase().contains(q) ||
        it.instructorName.lowercase().contains(q)
    }
  }

  val activeCourse = remember(courses, courseProgress) {
    val latestProg = courseProgress.maxByOrNull { it.updatedAt?.seconds ?: 0L }
    if (latestProg != null) courses.firstOrNull { it.id == latestProg.courseId } else null
  }
  val startedCount = remember(courseProgress) { courseProgress.count { it.completedLessonIds.isNotEmpty() } }
  val completedLessonsCount = remember(courseProgress) { courseProgress.sumOf { it.completedLessonIds.size } }
  val completedCoursesCount = remember(courseProgress) { courseProgress.count { it.progressPercent >= 100L } }

  Box(modifier = Modifier.fillMaxSize().testTag("courses_screen"), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 840.dp).fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 96.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item {
        Text(
          text = strings.coursesTitle,
          style = MaterialTheme.typography.headlineSmall,
          color = PrimaryNavy,
          fontWeight = FontWeight.Bold,
        )
      }

      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = NavyTint),
          border = BorderStroke(1.dp, BorderSubtle),
          modifier = Modifier.fillMaxWidth().testTag("courses_progress_summary_card"),
        ) {
          Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text("${courses.size}", style = MaterialTheme.typography.titleLarge, color = PrimaryNavy, fontWeight = FontWeight.Bold)
              Text(strings.navCourses, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text("$startedCount", style = MaterialTheme.typography.titleLarge, color = AccentOrange, fontWeight = FontWeight.Bold)
              Text("In Progress", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text("$completedLessonsCount", style = MaterialTheme.typography.titleLarge, color = PrimaryNavy, fontWeight = FontWeight.Bold)
              Text(strings.lessonsLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text("$completedCoursesCount", style = MaterialTheme.typography.titleLarge, color = SuccessGreen, fontWeight = FontWeight.Bold)
              Text("Certificates", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      }

      item {
        AcademySearchBar(
          query = query,
          hint = strings.searchHint,
          onQueryChange = { query = it },
        )
      }

      if (activeCourse != null && query.isBlank()) {
        val prog = progressByCourse[activeCourse.id]
        item {
          Card(
            onClick = { onCourseClick(activeCourse) },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PrimaryNavy),
            modifier = Modifier.fillMaxWidth().testTag("continue_learning_card"),
          ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                text = strings.continueLearning,
                style = MaterialTheme.typography.labelMedium,
                color = AccentOrange,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = activeCourse.title,
                style = MaterialTheme.typography.titleMedium,
                color = PureWhite,
                fontWeight = FontWeight.Bold,
              )
              LinearProgressIndicator(
                progress = { ((prog?.progressPercent ?: 0L).toInt() / 100f).coerceIn(0f, 1f) },
                color = AccentOrange,
                trackColor = PureWhite.copy(alpha = 0.25f),
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp)),
              )
            }
          }
        }
      }

      items(filteredCourses, key = { it.id }) { course ->
        CourseCard(
          course = course,
          progress = progressByCourse[course.id],
          strings = strings,
          onClick = { onCourseClick(course) },
        )
      }
    }
  }
}

@Composable
fun CourseDetailScreen(
  course: CourseItem?,
  modules: List<CourseModuleItem>,
  lessons: List<CourseLessonItem>,
  progress: CourseProgress?,
  profile: UserProfile?,
  strings: AcademyStrings,
  onBack: () -> Unit,
  onViewLesson: (CourseLessonItem) -> Unit,
  onToggleLessonComplete: (CourseLessonItem, Int) -> Unit,
  onUpgradeClick: () -> Unit,
) {
  if (course == null) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      OutlinedButton(onClick = onBack) { Text("Back") }
    }
    return
  }

  val hasAccess = AccessEvaluator.hasPremiumAccess(profile) || UserRole.fromString(profile?.role).isStaff()
  val completedIds = remember(progress) { progress?.completedLessonIds?.toSet() ?: emptySet() }
  val totalLessonsCount = lessons.size.coerceAtLeast(course.totalLessons.toInt()).coerceAtLeast(1)
  val thumbRes = when {
    course.id.contains("3dmax", true) || course.categoryId == "MAX_3D" -> R.drawable.img_course_3dmax_1795445786941
    course.id.contains("naq", true) || course.categoryId == "NAQSHAY" -> R.drawable.img_course_naqshay_1795445785729
    else -> R.drawable.img_course_autocad_1795445786167
  }

  Box(modifier = Modifier.fillMaxSize().testTag("course_detail_screen"), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 840.dp).fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 96.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          IconButton(onClick = onBack, modifier = Modifier.testTag("course_detail_back_btn")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimaryNavy)
          }
          Text(
            text = course.title,
            style = MaterialTheme.typography.titleMedium,
            color = PrimaryNavy,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )
        }
      }

      item {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, BorderSubtle),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column {
            Box(modifier = Modifier.fillMaxWidth().height(170.dp)) {
              if (!course.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                  model = course.thumbnailUrl,
                  contentDescription = course.title,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize(),
                )
              } else {
                Image(
                  painter = painterResource(id = thumbRes),
                  contentDescription = course.title,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize(),
                )
              }
            }
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(course.description, style = MaterialTheme.typography.bodyMedium)
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.School, contentDescription = null, tint = PrimaryNavy, modifier = Modifier.size(18.dp))
                Text(
                  text = "${course.instructorName} • ${course.level ?: ""} • ${course.durationLabel}",
                  style = MaterialTheme.typography.labelLarge,
                  color = PrimaryNavy,
                )
              }
              course.instructorBio?.takeIf { it.isNotBlank() }?.let { bio ->
                Text(bio, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              val pct = ((completedIds.size * 100) / totalLessonsCount).coerceIn(0, 100)
              LinearProgressIndicator(
                progress = { pct / 100f },
                color = AccentOrange,
                trackColor = NavyTint,
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
              )
              Text(
                text = "${completedIds.size}/$totalLessonsCount ${strings.lessonsLabel} ($pct%)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      if (completedIds.size >= totalLessonsCount && totalLessonsCount > 0) {
        item {
          Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = PrimaryNavy),
            border = BorderStroke(2.dp, AccentOrange),
            modifier = Modifier.fillMaxWidth().testTag("course_certificate_card"),
          ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Filled.WorkspacePremium, contentDescription = strings.certificateTitle, tint = AccentOrange, modifier = Modifier.size(32.dp))
                Column {
                  Text(
                    text = strings.certificateTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = PureWhite,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = strings.certificateSubtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = PureWhite.copy(alpha = 0.85f),
                  )
                }
              }
              Surface(
                color = PureWhite.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text(
                    text = profile?.fullName?.ifBlank { "Academy Student" } ?: "Academy Student",
                    style = MaterialTheme.typography.titleSmall,
                    color = AccentOrange,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = course.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = PureWhite,
                  )
                  Text(
                    text = "Instructor: ${course.instructorName} • Zahid Architecture Academy",
                    style = MaterialTheme.typography.labelSmall,
                    color = PureWhite.copy(alpha = 0.8f),
                  )
                }
              }
            }
          }
        }
      }

      if (course.premium && !hasAccess) {
        item {
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = OrangeTint),
            border = BorderStroke(1.dp, AccentOrange.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(Icons.Filled.Lock, contentDescription = null, tint = AccentOrange)
              Text(strings.premiumLockedDesc, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
              Button(
                onClick = onUpgradeClick,
                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange, contentColor = PureWhite),
              ) {
                Text(strings.unlockAccessBtn)
              }
            }
          }
        }
      }

      if (modules.isNotEmpty()) {
        modules.forEach { module ->
          item(key = "mod_${module.id}") {
            Text(
              text = module.title,
              style = MaterialTheme.typography.titleMedium,
              color = PrimaryNavy,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(top = 6.dp),
            )
          }
          val modLessons = lessons.filter { it.moduleId == module.id }
          items(modLessons, key = { it.id }) { lesson ->
            LessonItemRow(
              lesson = lesson,
              isCompleted = lesson.id in completedIds,
              isLocked = lesson.premium && !hasAccess,
              strings = strings,
              onViewClick = { onViewLesson(lesson) },
              onToggleComplete = { onToggleLessonComplete(lesson, totalLessonsCount) },
            )
          }
        }
      } else {
        items(lessons, key = { it.id }) { lesson ->
          LessonItemRow(
            lesson = lesson,
            isCompleted = lesson.id in completedIds,
            isLocked = lesson.premium && !hasAccess,
            strings = strings,
            onViewClick = { onViewLesson(lesson) },
            onToggleComplete = { onToggleLessonComplete(lesson, totalLessonsCount) },
          )
        }
      }
    }
  }
}

@Composable
private fun LessonItemRow(
  lesson: CourseLessonItem,
  isCompleted: Boolean,
  isLocked: Boolean,
  strings: AcademyStrings,
  onViewClick: () -> Unit,
  onToggleComplete: () -> Unit,
) {
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, BorderSubtle),
    modifier = Modifier.fillMaxWidth().testTag("lesson_card_${lesson.id}"),
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(
          imageVector = when {
            isLocked -> Icons.Filled.Lock
            lesson.lessonType == "VIDEO" -> Icons.Filled.PlayCircle
            else -> Icons.Filled.PictureAsPdf
          },
          contentDescription = lesson.lessonType,
          tint = if (isLocked) AccentOrange else PrimaryNavy,
        )
        Column(modifier = Modifier.weight(1f)) {
          Text(lesson.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
          Text(
            text = "${lesson.lessonType} • ${lesson.durationLabel}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        IconButton(onClick = onToggleComplete, modifier = Modifier.testTag("lesson_complete_btn_${lesson.id}")) {
          Icon(
            imageVector = if (isCompleted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = if (isCompleted) strings.completedLabel else strings.markCompleted,
            tint = if (isCompleted) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      lesson.description?.takeIf { it.isNotBlank() }?.let { desc ->
        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Button(
          onClick = onViewClick,
          colors = ButtonDefaults.buttonColors(
            containerColor = if (isLocked) AccentOrange else PrimaryNavy,
            contentColor = PureWhite,
          ),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.testTag("lesson_view_btn_${lesson.id}"),
        ) {
          Text(if (isLocked) strings.unlockAccessBtn else strings.viewBtn)
        }
      }
    }
  }
}
