package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.AcademyFileDownloadManager
import com.example.data.AcademyRepository
import com.example.data.AuthManager
import com.example.data.LocalPreferencesManager
import com.example.i18n.AppLanguage
import com.example.i18n.stringsFor
import com.example.model.UiState
import com.example.model.UserRole
import com.example.ui.components.AcademyBottomNavigation
import com.example.ui.components.AppHeader
import com.example.ui.components.DownloadProgressBanner
import com.example.ui.components.MainTab
import com.example.ui.components.QuickPaymentDialog
import com.example.ui.components.RequestCustomMapDialog
import com.example.ui.components.WhatsAppFloatingButton
import com.example.ui.components.openWhatsAppSupport
import com.example.ui.screens.AdminPanelScreen
import com.example.ui.screens.CourseDetailScreen
import com.example.ui.screens.CoursesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.MyDownloadsScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.OnboardingAndAuthScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ResourceDetailScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.StudioToolsScreen
import com.example.ui.screens.SubscriptionAndPaymentScreen
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.NavyTint
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.PureWhite
import com.example.ui.theme.ZahidAcademyTheme
import com.example.ui.viewers.ResourceViewerDialog
import com.example.viewmodel.AcademyViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      ZahidArchitectureAcademyRoot()
    }
  }
}

@Composable
fun ZahidArchitectureAcademyRoot() {
  val context = LocalContext.current
  val appContext = context.applicationContext
  val prefs = remember { LocalPreferencesManager(appContext) }
  val selectedLang by prefs.languageFlow.collectAsStateWithLifecycle(initialValue = AppLanguage.EN)
  val strings = remember(selectedLang) { stringsFor(selectedLang) }
  val scope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }

  var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }
  var isCheckingAuth by remember { mutableStateOf(Firebase.auth.currentUser == null) }
  var isSigningIn by remember { mutableStateOf(false) }
  var authError by remember { mutableStateOf<String?>(null) }

  DisposableEffect(Unit) {
    val listener = FirebaseAuth.AuthStateListener { auth ->
      currentUser = auth.currentUser
    }
    Firebase.auth.addAuthStateListener(listener)
    onDispose { Firebase.auth.removeAuthStateListener(listener) }
  }

  LaunchedEffect(Unit) {
    if (Firebase.auth.currentUser != null) {
      isCheckingAuth = false
    } else {
      AuthManager.attemptAutoSignIn(
        context = context,
        credentialManager = credentialManager,
        onAuthSuccess = {
          currentUser = Firebase.auth.currentUser
          isCheckingAuth = false
        },
        onUnauthenticated = {
          isCheckingAuth = false
        },
        scope = scope,
      )
    }
  }

  ZahidAcademyTheme(isRtl = selectedLang.isRtl) {
    when {
      isCheckingAuth -> {
        SplashScreen(strings = strings)
      }
      currentUser == null -> {
        OnboardingAndAuthScreen(
          strings = strings,
          selectedLanguage = selectedLang,
          isSigningIn = isSigningIn,
          authError = authError,
          onSelectLanguage = { lang ->
            scope.launch { prefs.setLanguage(lang) }
          },
          onGoogleSignInClick = {
            authError = null
            isSigningIn = true
            AuthManager.onGoogleSignInClicked(
              context = context,
              credentialManager = credentialManager,
              onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
                isSigningIn = false
              },
              onAuthError = { msg ->
                authError = msg
                isSigningIn = false
              },
              onAuthCancelled = {
                isSigningIn = false
              },
              scope = scope,
            )
          },
        )
      }
      else -> {
        val activeUser = currentUser!!
        key(activeUser.uid) {
          val dbId = remember { appContext.getString(R.string.firestore_database_id) }
          val repository = remember(dbId) { AcademyRepository(FirebaseFirestore.getInstance(dbId)) }
          val downloadManager = remember { AcademyFileDownloadManager(appContext, prefs) }
          val vm: AcademyViewModel = viewModel(
            key = activeUser.uid,
            factory = object : ViewModelProvider.Factory {
              @Suppress("UNCHECKED_CAST")
              override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AcademyViewModel(
                  repository = repository,
                  downloadManager = downloadManager,
                  prefs = prefs,
                  currentUserId = activeUser.uid,
                  firebaseUser = activeUser,
                ) as T
              }
            },
          )
          MainAppShell(
            viewModel = vm,
            activeUser = activeUser,
            onSignOut = {
              AuthManager.signOut(
                credentialManager = credentialManager,
                scope = scope,
                onSignOutComplete = { currentUser = null },
              )
            },
          )
        }
      }
    }
  }
}

@Composable
private fun MainAppShell(
  viewModel: AcademyViewModel,
  activeUser: FirebaseUser,
  onSignOut: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val navController = rememberNavController()
  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
  val snackbarHostState = remember { SnackbarHostState() }

  val selectedLang by viewModel.selectedLanguage.collectAsStateWithLifecycle()
  val strings = remember(selectedLang) { stringsFor(selectedLang) }
  val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
  val profile = (userProfileState as? UiState.Success)?.data
  val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
  val resources by viewModel.resources.collectAsStateWithLifecycle()
  val courses by viewModel.courses.collectAsStateWithLifecycle()
  val plans by viewModel.subscriptionPlans.collectAsStateWithLifecycle()
  val userPayments by viewModel.userPayments.collectAsStateWithLifecycle()
  val notifications by viewModel.notifications.collectAsStateWithLifecycle()
  val favorites by viewModel.favorites.collectAsStateWithLifecycle()
  val courseProgress by viewModel.courseProgress.collectAsStateWithLifecycle()
  val localDownloads by viewModel.localDownloads.collectAsStateWithLifecycle()
  val trialCategoryDownloads by viewModel.trialCategoryDownloads.collectAsStateWithLifecycle()
  val customFolderUri by viewModel.customFolderUri.collectAsStateWithLifecycle()
  val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
  val viewerTarget by viewModel.viewerTarget.collectAsStateWithLifecycle()
  val isPreparingViewer by viewModel.isPreparingViewer.collectAsStateWithLifecycle()
  val isSubmittingPayment by viewModel.isSubmittingPayment.collectAsStateWithLifecycle()
  val bannerMessage by viewModel.bannerMessage.collectAsStateWithLifecycle()
  var quickPayProvider by remember { mutableStateOf<String?>(null) }
  var showCustomMapDialog by remember { mutableStateOf(false) }

  val adminUsers by viewModel.adminUsers.collectAsStateWithLifecycle()
  val adminResources by viewModel.adminResources.collectAsStateWithLifecycle()
  val adminCourses by viewModel.adminCourses.collectAsStateWithLifecycle()
  val adminPayments by viewModel.adminPayments.collectAsStateWithLifecycle()
  val adminLogs by viewModel.adminLogs.collectAsStateWithLifecycle()

  val unreadCount = remember(notifications) { notifications.count { !it.read } }
  val isStaff = remember(profile) { UserRole.fromString(profile?.role).isStaff() }

  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route ?: MainTab.HOME.route

  LaunchedEffect(bannerMessage) {
    val msg = bannerMessage
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearBanner()
    }
  }

  val navigateTop: (String) -> Unit = { route ->
    navController.navigate(route) {
      popUpTo(navController.graph.startDestinationId) { saveState = true }
      launchSingleTop = true
      restoreState = true
    }
  }

  ModalNavigationDrawer(
    drawerState = drawerState,
    drawerContent = {
      ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = strings.appName,
            style = MaterialTheme.typography.titleLarge,
            color = PrimaryNavy,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = profile?.email ?: activeUser.email ?: "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        val drawerItems = buildList {
          add(Triple(MainTab.HOME.route, strings.navHome, Icons.Filled.Home))
          add(Triple("library", strings.navLibrary, Icons.AutoMirrored.Filled.LibraryBooks))
          add(Triple(MainTab.COURSES.route, strings.navCourses, Icons.Filled.School))
          add(Triple("tools", strings.navTools, Icons.Filled.Calculate))
          add(Triple(MainTab.DOWNLOADS.route, strings.navDownloads, Icons.Filled.DownloadDone))
          add(Triple("subscription", strings.navSubscription, Icons.Filled.Subscriptions))
          add(Triple("notifications", strings.navNotifications, Icons.Filled.Notifications))
          add(Triple(MainTab.PROFILE.route, strings.navProfile, Icons.Filled.Person))
          if (isStaff) {
            add(Triple("admin", strings.navAdmin, Icons.Filled.AdminPanelSettings))
          }
        }

        drawerItems.forEach { (route, label, icon) ->
          val selected = currentRoute.startsWith(route.substringBefore("?"))
          NavigationDrawerItem(
            selected = selected,
            onClick = {
              scope.launch { drawerState.close() }
              navigateTop(route)
            },
            icon = { Icon(icon, contentDescription = label) },
            label = { Text(label) },
            colors = NavigationDrawerItemDefaults.colors(
              selectedContainerColor = NavyTint,
              selectedIconColor = PrimaryNavy,
              selectedTextColor = PrimaryNavy,
            ),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
          )
        }

        Spacer(Modifier.weight(1f))
        HorizontalDivider()
        NavigationDrawerItem(
          selected = false,
          onClick = {
            scope.launch { drawerState.close() }
            onSignOut()
          },
          icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = strings.logoutBtn) },
          label = { Text(strings.logoutBtn) },
          modifier = Modifier.padding(12.dp),
        )
      }
    },
  ) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val showRail = maxWidth >= 840.dp

      Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
          AppHeader(
            title = strings.appName,
            unreadNotifications = unreadCount,
            photoUrl = profile?.photoUrl ?: activeUser.photoUrl?.toString(),
            userInitial = (profile?.fullName?.firstOrNull() ?: activeUser.displayName?.firstOrNull() ?: 'Z').toString(),
            onMenuClick = { scope.launch { drawerState.open() } },
            onNotificationsClick = { navigateTop("notifications") },
            onProfileClick = { navigateTop(MainTab.PROFILE.route) },
          )
        },
        bottomBar = {
          if (!showRail) {
            val baseTabRoute = when {
              currentRoute.startsWith("library") -> MainTab.LIBRARY.route
              currentRoute.startsWith("course") -> MainTab.COURSES.route
              else -> currentRoute
            }
            AcademyBottomNavigation(
              currentRoute = baseTabRoute,
              strings = strings,
              onSelectTab = { tab ->
                val targetRoute = if (tab == MainTab.LIBRARY) "library" else tab.route
                navigateTop(targetRoute)
              },
            )
          }
        },
        floatingActionButton = {
          WhatsAppFloatingButton(
            whatsappNumber = appSettings.whatsappNumber,
            defaultMessage = appSettings.whatsappDefaultMessage,
            contentDescription = strings.whatsappSupportDesc,
          )
        },
      ) { innerPadding ->
        Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
          if (showRail) {
            NavigationRail(
              containerColor = MaterialTheme.colorScheme.surface,
              modifier = Modifier.fillMaxHeight(),
            ) {
              val railTabs = listOf(
                Triple(MainTab.HOME, strings.navHome, Icons.Filled.Home),
                Triple(MainTab.LIBRARY, strings.navLibrary, Icons.AutoMirrored.Filled.LibraryBooks),
                Triple(MainTab.COURSES, strings.navCourses, Icons.Filled.School),
                Triple(MainTab.DOWNLOADS, strings.navDownloads, Icons.Filled.DownloadDone),
                Triple(MainTab.PROFILE, strings.navProfile, Icons.Filled.Person),
              )
              railTabs.forEach { (tab, label, icon) ->
                val selected = currentRoute.startsWith(tab.route)
                NavigationRailItem(
                  selected = selected,
                  onClick = {
                    navigateTop(if (tab == MainTab.LIBRARY) "library" else tab.route)
                  },
                  icon = { Icon(icon, contentDescription = label) },
                  label = { Text(label) },
                )
              }
            }
          }

          Column(modifier = Modifier.weight(1f).fillMaxSize()) {
            if (isPreparingViewer) {
              LinearProgressIndicator(
                color = AccentOrange,
                modifier = Modifier.fillMaxWidth().height(3.dp),
              )
            }

            DownloadProgressBanner(
              state = downloadState,
              strings = strings,
              onCancel = { viewModel.cancelDownload() },
              onOpenFile = { local ->
                if (!viewModel.downloadManager.openFileExternally(local)) {
                  viewModel.viewLocalDownloadedFile(local)
                }
              },
              onDismiss = { viewModel.downloadManager.resetState() },
            )

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
              NavHost(
                navController = navController,
                startDestination = MainTab.HOME.route,
              ) {
                composable(MainTab.HOME.route) {
                  HomeScreen(
                    profile = profile,
                    resources = resources,
                    favorites = favorites,
                    localDownloads = localDownloads,
                    strings = strings,
                    onStartTrial = { viewModel.activateFreeTrial() },
                    onUpgradeClick = { navigateTop("subscription") },
                    onCategoryClick = { cat -> navController.navigate("library?category=${cat.id}") },
                    onSeeAllLibrary = { navigateTop("library") },
                    onResourceClick = { res -> navController.navigate("resource_detail/${res.id}") },
                    onViewResource = { res -> viewModel.viewResource(res, profile) },
                    onDownloadResource = { res -> viewModel.downloadResource(res, profile) },
                    onToggleFavorite = { res -> viewModel.toggleFavorite(res) },
                    onOpenTools = { navigateTop("tools") },
                    onOpenDownloads = { navigateTop(MainTab.DOWNLOADS.route) },
                    trialCategoryDownloads = trialCategoryDownloads,
                    onQuickJazzCash = { quickPayProvider = "JAZZCASH" },
                    onQuickEasypaisa = { quickPayProvider = "EASYPAISA" },
                    onWhatsAppClick = {
                      openWhatsAppSupport(context, appSettings.whatsappNumber, appSettings.whatsappDefaultMessage)
                    },
                    onPlotSizeClick = { size ->
                      navController.navigate("library?plotSize=${android.net.Uri.encode(size)}")
                    },
                    onRequestCustomMap = { showCustomMapDialog = true },
                  )
                }

                composable("tools") {
                  StudioToolsScreen(
                    strings = strings,
                    onBrowsePlotMaps = { size ->
                      navController.navigate("library?plotSize=${android.net.Uri.encode(size)}")
                    },
                  )
                }

                composable(
                  route = "library?category={category}&plotSize={plotSize}",
                  arguments = listOf(
                    navArgument("category") {
                      type = NavType.StringType
                      nullable = true
                      defaultValue = null
                    },
                    navArgument("plotSize") {
                      type = NavType.StringType
                      nullable = true
                      defaultValue = null
                    },
                  ),
                ) { entry ->
                  val catArg = entry.arguments?.getString("category")
                  val plotArg = entry.arguments?.getString("plotSize")
                  LibraryScreen(
                    initialCategoryId = catArg,
                    profile = profile,
                    resources = resources,
                    favorites = favorites,
                    localDownloads = localDownloads,
                    strings = strings,
                    onResourceClick = { res -> navController.navigate("resource_detail/${res.id}") },
                    onViewResource = { res -> viewModel.viewResource(res, profile) },
                    onDownloadResource = { res -> viewModel.downloadResource(res, profile) },
                    onToggleFavorite = { res -> viewModel.toggleFavorite(res) },
                    initialPlotSize = plotArg,
                    trialCategoryDownloads = trialCategoryDownloads,
                  )
                }

                composable(
                  route = "resource_detail/{resourceId}",
                  arguments = listOf(navArgument("resourceId") { type = NavType.StringType }),
                ) { entry ->
                  val resId = entry.arguments?.getString("resourceId") ?: ""
                  val item = resources.firstOrNull { it.id == resId }
                  val isFav = favorites.any { it.resourceId == resId }
                  val local = localDownloads.firstOrNull { it.resourceId == resId }
                  ResourceDetailScreen(
                    resource = item,
                    profile = profile,
                    isFavorited = isFav,
                    localFile = local,
                    strings = strings,
                    onBack = { navController.popBackStack() },
                    onViewClick = { res -> viewModel.viewResource(res, profile) },
                    onDownloadClick = { res -> viewModel.downloadResource(res, profile) },
                    onOpenExternal = { lf ->
                      if (!viewModel.downloadManager.openFileExternally(lf)) {
                        viewModel.showBanner("No compatible external app found to open ${lf.fileName}.")
                      }
                    },
                    onToggleFavorite = { res -> viewModel.toggleFavorite(res) },
                    onUpgradeClick = { navigateTop("subscription") },
                  )
                }

                composable(MainTab.COURSES.route) {
                  CoursesScreen(
                    courses = courses,
                    courseProgress = courseProgress,
                    strings = strings,
                    onCourseClick = { course -> navController.navigate("course_detail/${course.id}") },
                  )
                }

                composable(
                  route = "course_detail/{courseId}",
                  arguments = listOf(navArgument("courseId") { type = NavType.StringType }),
                ) { entry ->
                  val courseId = entry.arguments?.getString("courseId") ?: ""
                  val course = courses.firstOrNull { it.id == courseId }
                  val modules by remember(courseId) { viewModel.observeModules(courseId) }
                    .collectAsStateWithLifecycle(initialValue = emptyList())
                  val lessons by remember(courseId) { viewModel.observeLessons(courseId) }
                    .collectAsStateWithLifecycle(initialValue = emptyList())
                  val prog = courseProgress.firstOrNull { it.courseId == courseId }

                  CourseDetailScreen(
                    course = course,
                    modules = modules,
                    lessons = lessons,
                    progress = prog,
                    profile = profile,
                    strings = strings,
                    onBack = { navController.popBackStack() },
                    onViewLesson = { lesson ->
                      viewModel.viewCourseLesson(
                        lesson = lesson,
                        profile = profile,
                        onLocked = { navigateTop("subscription") },
                      )
                    },
                    onToggleLessonComplete = { lesson, total ->
                      viewModel.toggleLessonComplete(courseId, lesson.id, total)
                    },
                    onUpgradeClick = { navigateTop("subscription") },
                  )
                }

                composable(MainTab.DOWNLOADS.route) {
                  MyDownloadsScreen(
                    localDownloads = localDownloads,
                    catalogResources = resources,
                    customFolderUri = customFolderUri,
                    strings = strings,
                    verifyExists = { lf -> viewModel.downloadManager.verifyFileExists(lf) },
                    onSelectCustomFolder = { uri -> viewModel.setCustomFolderUri(uri) },
                    onViewLocalFile = { lf -> viewModel.viewLocalDownloadedFile(lf) },
                    onOpenExternal = { lf ->
                      if (!viewModel.downloadManager.openFileExternally(lf)) {
                        viewModel.showBanner("No compatible app installed for ${lf.fileName}.")
                      }
                    },
                    onShareFile = { lf -> viewModel.downloadManager.shareFile(lf) },
                    onReDownload = { res -> viewModel.downloadResource(res, profile) },
                    onDeleteLocal = { lf -> viewModel.deleteLocalDownload(lf) },
                  )
                }

                composable("subscription") {
                  SubscriptionAndPaymentScreen(
                    profile = profile,
                    appSettings = appSettings,
                    plans = plans,
                    userPayments = userPayments,
                    isSubmitting = isSubmittingPayment,
                    strings = strings,
                    onActivateTrial = { viewModel.activateFreeTrial() },
                    onSubmitPayment = { plan, provider, ref, uri ->
                      profile?.let { p ->
                        viewModel.submitPayment(context, p, plan, provider, ref, uri)
                      }
                    },
                    onOpenWhatsApp = { customMsg ->
                      openWhatsAppSupport(context, appSettings.whatsappNumber, customMsg)
                    },
                  )
                }

                composable("notifications") {
                  NotificationsScreen(
                    notifications = notifications,
                    strings = strings,
                    onMarkRead = { id -> viewModel.markNotificationRead(id) },
                    onMarkAllRead = { viewModel.markAllNotificationsRead() },
                  )
                }

                composable(MainTab.PROFILE.route) {
                  ProfileScreen(
                    profile = profile,
                    fallbackEmail = activeUser.email ?: "",
                    selectedLanguage = selectedLang,
                    strings = strings,
                    onUpdateName = { newName -> viewModel.updateProfileName(newName) },
                    onSelectLanguage = { lang -> viewModel.setLanguage(lang) },
                    onOpenDownloads = { navigateTop(MainTab.DOWNLOADS.route) },
                    onOpenTools = { navigateTop("tools") },
                    onRequestCustomMap = { showCustomMapDialog = true },
                    onOpenSubscription = { navigateTop("subscription") },
                    onOpenAdmin = { navigateTop("admin") },
                    onSignOut = onSignOut,
                  )
                }

                composable("admin") {
                  AdminPanelScreen(
                    adminProfile = profile,
                    users = adminUsers,
                    resources = adminResources.ifEmpty { resources },
                    courses = adminCourses.ifEmpty { courses },
                    payments = adminPayments,
                    plans = plans,
                    appSettings = appSettings,
                    adminLogs = adminLogs,
                    strings = strings,
                    onApprovePayment = { req, note ->
                      profile?.let { viewModel.adminApprovePayment(it, req, note) }
                    },
                    onRejectPayment = { req, note ->
                      profile?.let { viewModel.adminRejectPayment(it, req, note) }
                    },
                    onUpdateUser = { target, status, role ->
                      profile?.let { viewModel.adminUpdateUser(it, target, status, role) }
                    },
                    onSaveResource = { res ->
                      profile?.let { viewModel.adminSaveResource(it, res) }
                    },
                    onSaveCourse = { course ->
                      profile?.let { viewModel.adminSaveCourse(it, course) }
                    },
                    onSaveLesson = { lesson ->
                      profile?.let { viewModel.adminSaveLesson(it, lesson) }
                    },
                    onSavePlan = { plan ->
                      profile?.let { viewModel.adminSavePlan(it, plan) }
                    },
                    onSaveSettings = { settings ->
                      profile?.let { viewModel.adminSaveSettings(it, settings) }
                    },
                    onSeedCatalog = { viewModel.adminSeedCatalog() },
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  viewerTarget?.let { target ->
    ResourceViewerDialog(
      target = target,
      strings = strings,
      onClose = { viewModel.closeViewer() },
      onDownloadResource = { res -> viewModel.downloadResource(res, profile) },
      onOpenExternal = { lf ->
        if (!viewModel.downloadManager.openFileExternally(lf)) {
          viewModel.showBanner("No compatible app installed for ${lf.fileName}.")
        }
      },
    )
  }

  quickPayProvider?.let { initialProv ->
    QuickPaymentDialog(
      initialProvider = initialProv,
      appSettings = appSettings,
      plans = plans,
      isSubmitting = isSubmittingPayment,
      strings = strings,
      onDismiss = { quickPayProvider = null },
      onSubmitPayment = { plan, provider, ref, uri ->
        profile?.let { p ->
          viewModel.submitPayment(context, p, plan, provider, ref, uri)
        }
      },
      onOpenWhatsApp = { customMsg ->
        openWhatsAppSupport(context, appSettings.whatsappNumber, customMsg)
      },
    )
  }

  if (showCustomMapDialog) {
    RequestCustomMapDialog(
      strings = strings,
      onDismiss = { showCustomMapDialog = false },
      onSendWhatsAppBrief = { brief ->
        openWhatsAppSupport(context, appSettings.whatsappNumber, brief)
      },
    )
  }
}
