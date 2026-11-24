package com.example

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.data.AcademyFileDownloadManager
import com.example.data.LocalPreferencesManager
import com.example.i18n.AppLanguage
import com.example.i18n.stringsFor
import com.example.model.AcademyStarterCatalog
import com.example.model.AccessEvaluator
import com.example.model.AppSettings
import com.example.model.ArchitectureCalculators
import com.example.model.ByLawsCatalog
import com.example.model.CadShortcutsCatalog
import com.example.model.EffectiveAccessState
import com.example.model.ResourceCategory
import com.example.model.ResourceItem
import com.example.model.UserProfile
import com.example.ui.components.RequestCustomMapDialog
import com.example.ui.components.WhatsAppFloatingButton
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OnboardingAndAuthScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.StudioToolsScreen
import com.example.ui.theme.ZahidAcademyTheme
import com.google.firebase.Timestamp
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.Date
import java.util.concurrent.TimeUnit

class AcademyAccessUnitTest {

  private val freeResource = ResourceItem(id = "r_free", title = "Free PDF", premium = false)
  private val proResource = ResourceItem(id = "r_pro", title = "Pro DWG", premium = true)

  @Test
  fun `trial lifecycle grants access during 7 days and blocks after expiry`() {
    val now = 1_700_000_000_000L
    val notStarted = UserProfile(uid = "u1", trialStatus = "NOT_STARTED", subscriptionStatus = "NONE")
    assertEquals(EffectiveAccessState.NOT_STARTED, AccessEvaluator.effectiveAccessState(notStarted, now))
    assertTrue(AccessEvaluator.canAccessResource(notStarted, freeResource, now))
    assertFalse(AccessEvaluator.canAccessResource(notStarted, proResource, now))

    val activeTrial = notStarted.copy(
      trialStatus = "ACTIVE",
      trialStartAt = Timestamp(Date(now - TimeUnit.DAYS.toMillis(2))),
      trialExpiresAt = Timestamp(Date(now + TimeUnit.DAYS.toMillis(5))),
    )
    assertEquals(EffectiveAccessState.TRIAL_ACTIVE, AccessEvaluator.effectiveAccessState(activeTrial, now))
    assertTrue(AccessEvaluator.canAccessResource(activeTrial, proResource, now))
    assertEquals(5, AccessEvaluator.trialDaysRemaining(activeTrial, now))

    val expiringSoon = activeTrial.copy(
      trialExpiresAt = Timestamp(Date(now + TimeUnit.HOURS.toMillis(20))),
    )
    assertEquals(EffectiveAccessState.TRIAL_EXPIRING_SOON, AccessEvaluator.effectiveAccessState(expiringSoon, now))
    assertTrue(AccessEvaluator.canAccessResource(expiringSoon, proResource, now))

    val expired = activeTrial.copy(
      trialExpiresAt = Timestamp(Date(now - TimeUnit.MINUTES.toMillis(5))),
    )
    assertEquals(EffectiveAccessState.TRIAL_EXPIRED, AccessEvaluator.effectiveAccessState(expired, now))
    assertFalse(AccessEvaluator.canAccessResource(expired, proResource, now))
    assertTrue(AccessEvaluator.canAccessResource(expired, freeResource, now))
  }

  @Test
  fun `pro subscription restores access even after trial expires`() {
    val now = 1_700_000_000_000L
    val proUser = UserProfile(
      uid = "u2",
      trialStatus = "EXPIRED",
      subscriptionStatus = "ACTIVE",
      subscriptionExpiresAt = Timestamp(Date(now + TimeUnit.DAYS.toMillis(25))),
    )
    assertEquals(EffectiveAccessState.PRO_ACTIVE, AccessEvaluator.effectiveAccessState(proUser, now))
    assertTrue(AccessEvaluator.canAccessResource(proUser, proResource, now))
  }

  @Test
  fun `suspended account blocks both free and premium resources`() {
    val now = 1_700_000_000_000L
    val suspendedPro = UserProfile(
      uid = "u3",
      accountStatus = "SUSPENDED",
      subscriptionStatus = "ACTIVE",
      subscriptionExpiresAt = Timestamp(Date(now + TimeUnit.DAYS.toMillis(10))),
    )
    assertEquals(EffectiveAccessState.SUSPENDED, AccessEvaluator.effectiveAccessState(suspendedPro, now))
    assertFalse(AccessEvaluator.canAccessResource(suspendedPro, freeResource, now))
    assertFalse(AccessEvaluator.canAccessResource(suspendedPro, proResource, now))
  }

  @Test
  fun `payment validation rejects short reference or invalid amount`() {
    assertNotNull(AccessEvaluator.validatePaymentInput("12", 1500L))
    assertNotNull(AccessEvaluator.validatePaymentInput("TID998877", 0L))
    assertNull(AccessEvaluator.validatePaymentInput("TID-99887766", 1500L))
  }

  @Test
  fun `multilingual strings and RTL flags match English Urdu Arabic`() {
    assertFalse(AppLanguage.EN.isRtl)
    assertTrue(AppLanguage.UR.isRtl)
    assertTrue(AppLanguage.AR.isRtl)
    assertEquals("Zahid Architecture Academy", stringsFor(AppLanguage.EN).appName)
    assertTrue(stringsFor(AppLanguage.UR).appName.isNotBlank())
    assertTrue(stringsFor(AppLanguage.AR).appName.isNotBlank())
  }

  @Test
  fun `starter catalog covers all home featured categories and 20 items per category and plot size`() {
    val featured = ResourceCategory.homeCategories()
    assertEquals(10, featured.size)
    val full = AcademyStarterCatalog.fullCatalogResources
    val starterCats = full.map { it.categoryId }.toSet()
    featured.forEach { cat ->
      assertTrue("Missing catalog resource for ${cat.id}", cat.id in starterCats)
      assertTrue(
        "Category ${cat.id} should have at least 20 items",
        full.count { it.categoryId == cat.id } >= 20,
      )
    }
    AcademyStarterCatalog.plotSizes.forEach { size ->
      assertEquals(20, full.count { it.plotSize == size })
    }
    assertEquals("+923467803931", AppSettings().whatsappNumber)
  }

  @Test
  fun `trial enforces 1 download per category and pro unlocks unlimited downloads`() {
    val now = 1_700_000_000_000L
    val activeTrial = UserProfile(
      uid = "u_trial",
      trialStatus = "ACTIVE",
      trialStartAt = Timestamp(Date(now - TimeUnit.DAYS.toMillis(1))),
      trialExpiresAt = Timestamp(Date(now + TimeUnit.DAYS.toMillis(6))),
    )
    val dwg1 = ResourceItem(id = "dwg_1", categoryId = "DWG", premium = true)
    val dwg2 = ResourceItem(id = "dwg_2", categoryId = "DWG", premium = true)
    val map3m = ResourceItem(id = "m3_1", categoryId = "NAQSHAY", plotSize = "3 Marla", premium = true)

    assertTrue(AccessEvaluator.canDownloadInCategory(activeTrial, dwg1, emptyMap(), nowMs = now))
    val afterDwg = mapOf("DWG" to 1)
    assertFalse(AccessEvaluator.canDownloadInCategory(activeTrial, dwg2, afterDwg, nowMs = now))
    assertTrue(AccessEvaluator.canDownloadInCategory(activeTrial, map3m, afterDwg, nowMs = now))

    val proUser = activeTrial.copy(
      subscriptionStatus = "ACTIVE",
      subscriptionExpiresAt = Timestamp(Date(now + TimeUnit.DAYS.toMillis(30))),
    )
    assertTrue(AccessEvaluator.canDownloadInCategory(proUser, dwg2, afterDwg, nowMs = now))
  }

  @Test
  fun `architecture calculators compute accurate plot, grey structure BOQ, and staircase dimensions`() {
    val conv = ArchitectureCalculators.convertFromMarla(5.0, 225.0)
    assertEquals(1125.0, conv.sqFeet, 0.01)
    assertEquals(0.25, conv.kanal, 0.001)

    val boq = ArchitectureCalculators.estimateGreyStructure(1000.0, 2)
    assertEquals(2000.0, boq.totalCoveredSqFt, 0.01)
    assertEquals(68000L, boq.bricksCount)
    assertEquals(840L, boq.cementBags)

    val stair = ArchitectureCalculators.calculateStaircase(11.0, 7.0)
    assertEquals(19, stair.riserCount)
    assertEquals(18, stair.treadCount)
    assertTrue(stair.exactRiserInches in 6.5..7.5)

    val cost = ArchitectureCalculators.estimateConstructionCost(1000.0, 2650L, 3150L)
    assertEquals(2_650_000L, cost.greyStructurePkr)
    assertEquals(3_150_000L, cost.finishingPkr)
    assertEquals(5_800_000L, cost.turnkeyTotalPkr)

    val scale = ArchitectureCalculators.convertDrawingScale(100.0, 100)
    assertEquals(30.48, scale.paperLengthCm, 0.01)
    assertEquals(12.0, scale.paperLengthInches, 0.01)

    assertEquals(4, ByLawsCatalog.standards.size)
    assertTrue(CadShortcutsCatalog.items.size >= 20)
  }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AcademyRobolectricTest {

  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun `bundled starter assets exist and preserve authentic binary and vector headers`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val pdfBytes = context.assets.open("academy/autocad_floor_plan_lesson.pdf").use { it.readBytes() }
    assertTrue(String(pdfBytes.take(5).toByteArray()).startsWith("%PDF-"))

    val dwgBytes = context.assets.open("academy/autocad_floor_plan.dwg").use { it.readBytes() }
    assertEquals("AC1027", String(dwgBytes.take(6).toByteArray()))

    val dxfText = context.assets.open("academy/house_map_5_marla.dxf").bufferedReader().use { it.readText() }
    assertTrue(dxfText.contains("SECTION") && dxfText.contains("LINE") && dxfText.contains("ENTITIES"))

    val maxBytes = context.assets.open("academy/3dmax_interior_scene.max").use { it.readBytes() }
    assertEquals(0xD0.toByte(), maxBytes[0])
    assertEquals(0xCF.toByte(), maxBytes[1])

    val mp4Bytes = context.assets.open("academy/cad_drafting_basics.mp4").use { it.readBytes() }
    assertTrue(mp4Bytes.size > 10_000)
    assertEquals("ftyp", String(mp4Bytes.copyOfRange(4, 8)))
  }

  @Test
  fun `downloadManager saves original PDF and DWG files preserving extensions and exact bytes`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = LocalPreferencesManager(context)
    val manager = AcademyFileDownloadManager(context, prefs)

    val pdfRes = AcademyStarterCatalog.starterResources.first { it.fileType == "PDF" }
    val dwgRes = AcademyStarterCatalog.starterResources.first { it.fileType == "DWG" }

    val pdfResult = manager.downloadResource(pdfRes, null)
    assertTrue(pdfResult.isSuccess)
    val savedPdf = pdfResult.getOrThrow()
    assertTrue(savedPdf.fileName.endsWith(".pdf"))
    assertTrue(manager.verifyFileExists(savedPdf))

    val expectedPdfBytes = context.assets.open("academy/autocad_floor_plan_lesson.pdf").use { it.readBytes() }
    if (!savedPdf.isTreeUri) {
      assertArrayEquals(expectedPdfBytes, File(savedPdf.savedPathOrUri).readBytes())
    }

    val dwgResult = manager.downloadResource(dwgRes, null)
    assertTrue(dwgResult.isSuccess)
    val savedDwg = dwgResult.getOrThrow()
    assertTrue(savedDwg.fileName.endsWith(".dwg"))
    assertTrue(manager.verifyFileExists(savedDwg))

    assertTrue(manager.deleteLocalCopy(savedPdf))
    assertTrue(manager.deleteLocalCopy(savedDwg))
  }

  @Test
  fun `home and profile UI show WhatsApp FAB and never display WhatsApp phone number`() {
    val strings = stringsFor(AppLanguage.EN)
    val sampleProfile = UserProfile(
      uid = "student_1",
      fullName = "Ali Raza",
      email = "ali@example.com",
      role = "USER",
      trialStatus = "ACTIVE",
      trialExpiresAt = Timestamp(Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(6))),
    )

    composeRule.setContent {
      ZahidAcademyTheme(isRtl = false) {
        Scaffold(
          floatingActionButton = {
            WhatsAppFloatingButton(
              whatsappNumber = "+923467803931",
              defaultMessage = "Hello",
              contentDescription = strings.whatsappSupportDesc,
            )
          }
        ) { inner ->
          Box(modifier = Modifier.fillMaxSize()) {
            HomeScreen(
              profile = sampleProfile,
              resources = AcademyStarterCatalog.fullCatalogResources,
              favorites = emptyList(),
              localDownloads = emptyList(),
              strings = strings,
              onStartTrial = {},
              onUpgradeClick = {},
              onCategoryClick = {},
              onSeeAllLibrary = {},
              onResourceClick = {},
              onViewResource = {},
              onDownloadResource = {},
              onToggleFavorite = {},
              onQuickJazzCash = {},
              onQuickEasypaisa = {},
              onWhatsAppClick = {},
            )
          }
        }
      }
    }

    composeRule.onNodeWithTag("whatsapp_support_fab").assertIsDisplayed()
    composeRule.onNodeWithText("7 Days FREE Trial").assertIsDisplayed()
    composeRule.onNodeWithTag("quick_jazzcash_btn").assertIsDisplayed()
    composeRule.onNodeWithTag("quick_easypaisa_btn").assertIsDisplayed()
    composeRule.onNodeWithTag("home_lazy_column").performScrollToNode(hasTestTag("plot_size_chip_3_Marla"))
    composeRule.onNodeWithTag("plot_size_chip_3_Marla").assertIsDisplayed()
    composeRule.onNodeWithTag("home_lazy_column").performScrollToNode(hasTestTag("category_card_SKETCHUP"))
    composeRule.onNodeWithTag("category_card_SKETCHUP").assertIsDisplayed()

    // Ensure the support phone number is NEVER shown anywhere in user-facing UI
    assertEquals(0, composeRule.onAllNodesWithText("0346", substring = true).fetchSemanticsNodes().size)
    assertEquals(0, composeRule.onAllNodesWithText("3467803931", substring = true).fetchSemanticsNodes().size)
    assertEquals(0, composeRule.onAllNodesWithText("7803931", substring = true).fetchSemanticsNodes().size)
  }

  @Test
  fun `onboarding screen renders Google Sign-In and 7 Days FREE Trial`() {
    val strings = stringsFor(AppLanguage.EN)
    composeRule.setContent {
      ZahidAcademyTheme(isRtl = false) {
        OnboardingAndAuthScreen(
          strings = strings,
          selectedLanguage = AppLanguage.EN,
          isSigningIn = false,
          authError = null,
          onSelectLanguage = {},
          onGoogleSignInClick = {},
        )
      }
    }
    composeRule.onAllNodesWithText("7 Days FREE Trial")[0].assertIsDisplayed()
    composeRule.onNodeWithTag("google_sign_in_button").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun `profile screen does not expose support number and displays user details`() {
    val strings = stringsFor(AppLanguage.EN)
    val sampleProfile = UserProfile(uid = "u1", fullName = "Sara Khan", email = "sara@academy.com")
    composeRule.setContent {
      ZahidAcademyTheme(isRtl = false) {
        ProfileScreen(
          profile = sampleProfile,
          fallbackEmail = "sara@academy.com",
          selectedLanguage = AppLanguage.EN,
          strings = strings,
          onUpdateName = {},
          onSelectLanguage = {},
          onOpenDownloads = {},
          onOpenSubscription = {},
          onOpenAdmin = {},
          onSignOut = {},
        )
      }
    }
    composeRule.onNodeWithText("Sara Khan").assertIsDisplayed()
    composeRule.onNodeWithTag("profile_tools_btn").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("logout_button").performScrollTo().assertIsDisplayed()
    assertEquals(0, composeRule.onAllNodesWithText("0346", substring = true).fetchSemanticsNodes().size)
    assertEquals(0, composeRule.onAllNodesWithText("3467803931", substring = true).fetchSemanticsNodes().size)
  }

  @Test
  fun `studio tools screen converts marla to sq ft and switches estimator tabs`() {
    val strings = stringsFor(AppLanguage.EN)
    composeRule.setContent {
      ZahidAcademyTheme(isRtl = false) {
        StudioToolsScreen(strings = strings)
      }
    }
    composeRule.onNodeWithText("1125.0 sq.ft").assertIsDisplayed()
    composeRule.onNodeWithTag("tools_tab_1").performClick()
    composeRule.onNodeWithText("Grey Structure Material Estimator (BOQ)").assertIsDisplayed()
    composeRule.onNodeWithTag("tools_tab_2").performClick()
    composeRule.onNodeWithText("Staircase Riser & Tread Calculator").assertIsDisplayed()
    composeRule.onNodeWithTag("tools_tab_3").performScrollTo().performClick()
    composeRule.onNodeWithTag("tools_bylaws_card").assertIsDisplayed()
    composeRule.onNodeWithTag("tools_tab_4").performScrollTo().performClick()
    composeRule.onNodeWithTag("tools_cost_card").assertIsDisplayed()
    composeRule.onNodeWithTag("tools_tab_5").performScrollTo().performClick()
    composeRule.onNodeWithTag("tools_shortcuts_card").assertIsDisplayed()
  }

  @Test
  fun `custom map request dialog builds WhatsApp brief without exposing support number`() {
    val strings = stringsFor(AppLanguage.EN)
    var sentBrief = ""
    composeRule.setContent {
      ZahidAcademyTheme(isRtl = false) {
        RequestCustomMapDialog(
          strings = strings,
          onDismiss = {},
          onSendWhatsAppBrief = { sentBrief = it },
        )
      }
    }
    composeRule.onNodeWithText("Request Custom House Map (Naqsha)").assertIsDisplayed()
    assertEquals(0, composeRule.onAllNodesWithText("0346", substring = true).fetchSemanticsNodes().size)
    assertEquals(0, composeRule.onAllNodesWithText("3467803931", substring = true).fetchSemanticsNodes().size)
    composeRule.onNodeWithTag("custom_map_whatsapp_send_btn").performClick()
    assertTrue(sentBrief.contains("5 Marla"))
    assertTrue(sentBrief.contains("Double Unit"))
  }
}
