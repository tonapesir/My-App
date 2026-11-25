package com.example.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.AppPage
import com.example.domain.model.Student
import com.example.domain.model.TeacherTab
import com.example.domain.model.UserRole
import com.example.ui.components.BannerView
import com.example.ui.components.BirthdaysModalDialog
import com.example.ui.components.CertificatePreviewDialog
import com.example.ui.components.ConfigSettingsDialog
import com.example.ui.components.HolidayCalendarDialog
import com.example.ui.components.LocalWhatsAppLauncher
import com.example.ui.components.MessageTemplatesDialog
import com.example.ui.components.PendingSavesModal
import com.example.ui.components.QrScannerDialog
import com.example.ui.components.QuickActionsHubDialog
import com.example.ui.components.WhatsAppComposeWithTemplateDialog
import com.example.ui.components.WhatsAppLaunchRequest
import com.example.ui.theme.ColorTokens
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.Gold
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldLight
import com.example.ui.theme.Navy
import com.example.ui.theme.Navy2
import com.example.ui.theme.NavySurface
import com.example.ui.theme.ScreenBg
import com.example.ui.theme.SoftRed
import com.example.ui.theme.TextCream
import com.example.ui.theme.TextMuted
import com.example.ui.viewmodel.ShellUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(
    shell: ShellUiState,
    onSelectPage: (AppPage) -> Unit,
    onSelectTeacherTab: (TeacherTab) -> Unit,
    onNavigateBack: () -> Boolean,
    onOpenConfig: () -> Unit,
    onCloseConfig: () -> Unit,
    onUrlChange: (String) -> Unit,
    onTestConnection: () -> Unit,
    onBulkLoad: () -> Unit,
    onClearCache: () -> Unit,
    onOpenPendingModal: () -> Unit,
    onClosePendingModal: () -> Unit,
    onFlushPending: () -> Unit,
    onExportPendingJson: () -> Unit,
    onRemovePending: (String) -> Unit,
    onClearAllPending: () -> Unit,
    onDismissToast: () -> Unit,
    onToggleTwoUp: (Boolean) -> Unit,
    onCloseCertPreview: () -> Unit,
    onRefreshNetwork: () -> Unit,
    onToggleTheme: () -> Unit = {},
    biometricSupported: Boolean = false,
    onToggleBiometric: (Boolean) -> Unit = {},
    onToggleReminders: (Boolean) -> Unit = {},
    onPromptBiometricUnlock: () -> Unit = {},
    onOpenQrScanner: () -> Unit = {},
    onDismissQrScanner: () -> Unit = {},
    onOpenQuickActions: () -> Unit = {},
    onDismissQuickActions: () -> Unit = {},
    onOpenHolidayDialog: () -> Unit = {},
    onDismissHolidayDialog: () -> Unit = {},
    onAddHoliday: (String, String) -> Unit = { _, _ -> },
    onDeleteHoliday: (String) -> Unit = {},
    onSeedDefaultHolidays: () -> Unit = {},
    onOpenBirthdaysDialog: () -> Unit = {},
    onDismissBirthdaysDialog: () -> Unit = {},
    onRefreshQuickBirthdays: () -> Unit = {},
    findStudentInCache: (String) -> Student? = { null },
    onQrOpenProfile: (String) -> Unit = {},
    onQrOpenEarlyLeave: (String) -> Unit = {},
    onQrOpenSearch: (String) -> Unit = {},
    onTogglePinQuickKey: (String) -> Unit = {},
    onAutoLockChange: (Int) -> Unit = {},
    onOpenTemplatesDialog: () -> Unit = {},
    onDismissTemplatesDialog: () -> Unit = {},
    onSaveMessageTemplate: (String, String, String?) -> Unit = { _, _, _ -> },
    onDeleteMessageTemplate: (String) -> Unit = {},
    onResetDefaultMessageTemplates: () -> Unit = {},
    onLogout: () -> Unit,
    content: @Composable (AppPage) -> Unit
) {
    val session = shell.session ?: return
    val allowedPages = AppPage.allowedFor(session.role)
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var lastBackPressMs by remember { mutableLongStateOf(0L) }
    var pendingWaRequest by remember { mutableStateOf<WhatsAppLaunchRequest?>(null) }

    BackHandler(enabled = true) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else if (!onNavigateBack()) {
            val now = System.currentTimeMillis()
            if (now - lastBackPressMs < 2000L) {
                (context as? Activity)?.finish()
            } else {
                lastBackPressMs = now
                Toast.makeText(context, "बाहेर पडण्यासाठी पुन्हा Back दाबा", Toast.LENGTH_SHORT).show()
            }
        }
    }

    CompositionLocalProvider(LocalWhatsAppLauncher provides { req -> pendingWaRequest = req }) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 840.dp

        val drawerMenuContent: @Composable () -> Unit = {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .background(NavySurface)
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                Surface(
                    color = Navy2,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Gold.copy(0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_school_emblem),
                                contentDescription = "SGS हायस्कूल पाचोरा",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(38.dp).clip(CircleShape)
                            )
                            Column {
                                Text("SGS हायस्कूल पाचोरा", color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                                Text("${session.label} (${session.username})", color = TextCream, fontSize = 12.sp)
                            }
                        }
                        if (session.assignedClass.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text("वर्ग: ${session.iyatta} ${session.tukdi}", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))

                if (session.role == UserRole.TEACHER) {
                    Text("वर्ग शिक्षक टॅब", color = GoldLight, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    TeacherTab.entries.forEach { tab ->
                        val active = shell.currentPage == AppPage.TEACHER && shell.teacherTab == tab
                        NavigationDrawerItem(
                            label = { Text("${tab.emoji} ${tab.titleMr}", fontSize = 13.5.sp) },
                            selected = active,
                            onClick = {
                                onSelectTeacherTab(tab)
                                scope.launch { drawerState.close() }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = GoldContainer,
                                selectedTextColor = GoldLight,
                                unselectedTextColor = TextCream
                            ),
                            modifier = Modifier.testTag("nav_teacher_${tab.key}")
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Navy2)
                }

                Text("मेनू", color = TextMuted, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                allowedPages.forEach { page ->
                    val active = shell.currentPage == page && !(page == AppPage.TEACHER && session.role == UserRole.TEACHER)
                    NavigationDrawerItem(
                        label = { Text("${page.emoji} ${page.titleMr}", fontSize = 13.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        selected = active,
                        onClick = {
                            onSelectPage(page)
                            scope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = GoldContainer,
                            selectedTextColor = GoldLight,
                            unselectedTextColor = TextCream
                        ),
                        modifier = Modifier.testTag("nav_${page.key}")
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Navy2)
                NavigationDrawerItem(
                    label = { Text("🚪 Logout", color = SoftRed, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = SoftRed) },
                    selected = false,
                    onClick = onLogout,
                    modifier = Modifier.testTag("drawer_logout_button")
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Software Developed by Tonape sir (SGS Highschool, Pachora)",
                    color = TextMuted,
                    fontSize = 10.5.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)
                )
            }
        }

        val mainScaffold: @Composable () -> Unit = {
            Scaffold(
                containerColor = ScreenBg,
                contentWindowInsets = WindowInsets.safeDrawing,
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy, titleContentColor = GoldLight),
                        navigationIcon = {
                            if (!isWideScreen) {
                                IconButton(
                                    onClick = { scope.launch { drawerState.open() } },
                                    modifier = Modifier.testTag("menu_drawer_button")
                                ) {
                                    Icon(Icons.Default.Menu, contentDescription = "मेनू उघडा", tint = GoldLight)
                                }
                            }
                        },
                        title = {
                            val titleTxt = if (shell.currentPage == AppPage.TEACHER && session.role == UserRole.TEACHER) {
                                "${shell.teacherTab.emoji} ${shell.teacherTab.titleMr}"
                            } else {
                                "${shell.currentPage.emoji} ${shell.currentPage.titleMr}"
                            }
                            Column {
                                Text(titleTxt, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(session.label, color = TextMuted, fontSize = 11.sp, maxLines = 1)
                            }
                        },
                        actions = {
                            if (shell.pendingSaves.isNotEmpty()) {
                                IconButton(
                                    onClick = onOpenPendingModal,
                                    modifier = Modifier.testTag("pending_queue_button")
                                ) {
                                    BadgedBox(badge = { Badge { Text("${shell.pendingSaves.size}") } }) {
                                        Icon(Icons.Default.CloudQueue, contentDescription = "Pending Saves", tint = ColorTokens.WarningText)
                                    }
                                }
                            }
                            IconButton(
                                onClick = onOpenQrScanner,
                                modifier = Modifier.testTag("open_qr_scanner_button")
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = "QR स्कॅनर", tint = GoldLight)
                            }
                            IconButton(
                                onClick = onOpenQuickActions,
                                modifier = Modifier.testTag("open_quick_actions_button")
                            ) {
                                Icon(Icons.Default.Apps, contentDescription = "सर्व जलद कृती", tint = GoldLight)
                            }
                            IconButton(
                                onClick = onToggleTheme,
                                modifier = Modifier.testTag("toggle_theme_button")
                            ) {
                                Icon(
                                    if (shell.isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "थीम बदला",
                                    tint = GoldLight
                                )
                            }
                            IconButton(
                                onClick = onRefreshNetwork,
                                modifier = Modifier.testTag("refresh_network_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "रिफ्रेश", tint = TextCream)
                            }
                            IconButton(
                                onClick = onOpenConfig,
                                modifier = Modifier.testTag("open_config_button")
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = "सेटिंग", tint = GoldLight)
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    if (shell.isOfflineBanner) {
                        Surface(
                            color = ColorTokens.WarningBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.CloudOff, contentDescription = null, tint = ColorTokens.WarningText, modifier = Modifier.size(16.dp))
                                Text(
                                    if (session.offline) "📴 ऑफलाइन Login सक्रिय — नवीन Save साठी नेट आल्यावर पुन्हा Login करा."
                                    else "📶 ऑफलाइन स्थिती — जतन केलेली माहिती दिसत आहे.",
                                    color = ColorTokens.WarningText,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // Persistent All-Menu Quick Actions Bar (for ALL users)
                    Surface(
                        color = DeepNavy,
                        border = BorderStroke(0.5.dp, Gold.copy(0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            item(key = "qa_hub_chip") {
                                Surface(
                                    color = Gold,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.clickable { onOpenQuickActions() }.testTag("bar_quick_hub_btn")
                                ) {
                                    Text(
                                        "⚡ सर्व मेनू",
                                        color = DeepNavy,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                            // Pinned favorite menus/tabs appear right after "⚡ सर्व मेनू"
                            val pinnedTabs = if (session.role == UserRole.TEACHER) {
                                TeacherTab.entries.filter { "tab_${it.key}" in shell.pinnedQuickKeys }
                            } else emptyList()
                            val pinnedPages = allowedPages.filter {
                                !(session.role == UserRole.TEACHER && it == AppPage.TEACHER) &&
                                    "page_${it.key}" in shell.pinnedQuickKeys
                            }
                            items(pinnedTabs, key = { "pin_ttab_${it.key}" }) { tab ->
                                val active = shell.currentPage == AppPage.TEACHER && shell.teacherTab == tab
                                Surface(
                                    color = if (active) GoldContainer else Navy2,
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.2.dp, Gold),
                                    modifier = Modifier.clickable { onSelectTeacherTab(tab) }.testTag("bar_pin_ttab_${tab.key}")
                                ) {
                                    Text(
                                        "⭐ ${tab.emoji} ${tab.titleMr}",
                                        color = GoldLight,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                            items(pinnedPages, key = { "pin_page_${it.key}" }) { pg ->
                                val active = shell.currentPage == pg
                                Surface(
                                    color = if (active) GoldContainer else Navy2,
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.2.dp, Gold),
                                    modifier = Modifier.clickable { onSelectPage(pg) }.testTag("bar_pin_page_${pg.key}")
                                ) {
                                    Text(
                                        "⭐ ${pg.emoji} ${pg.titleMr}",
                                        color = GoldLight,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                            item(key = "qa_qr_chip") {
                                Surface(
                                    color = Navy2,
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Gold.copy(0.45f)),
                                    modifier = Modifier.clickable { onOpenQrScanner() }.testTag("bar_quick_qr_btn")
                                ) {
                                    Text(
                                        "📷 QR स्कॅन",
                                        color = GoldLight,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.5.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                            item(key = "qa_holiday_chip") {
                                Surface(
                                    color = Navy2,
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Gold.copy(0.45f)),
                                    modifier = Modifier.clickable { onOpenHolidayDialog() }.testTag("bar_quick_holiday_btn")
                                ) {
                                    Text(
                                        "📅 सुट्ट्या",
                                        color = GoldLight,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.5.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                            item(key = "qa_bday_chip") {
                                Surface(
                                    color = Navy2,
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Gold.copy(0.45f)),
                                    modifier = Modifier.clickable { onOpenBirthdaysDialog() }.testTag("bar_quick_bday_btn")
                                ) {
                                    Text(
                                        "🎂 वाढदिवस",
                                        color = GoldLight,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.5.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                            item(key = "qa_templates_chip") {
                                Surface(
                                    color = Navy2,
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Gold.copy(0.45f)),
                                    modifier = Modifier.clickable { onOpenTemplatesDialog() }.testTag("bar_quick_templates_btn")
                                ) {
                                    Text(
                                        "💬 टेम्पलेट्स",
                                        color = GoldLight,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.5.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                            if (session.role == UserRole.TEACHER) {
                                items(
                                    items = TeacherTab.entries.filter { "tab_${it.key}" !in shell.pinnedQuickKeys },
                                    key = { "ttab_${it.key}" }
                                ) { tab ->
                                    val active = shell.currentPage == AppPage.TEACHER && shell.teacherTab == tab
                                    Surface(
                                        color = if (active) GoldContainer else Navy2,
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, if (active) Gold else Navy2),
                                        modifier = Modifier.clickable { onSelectTeacherTab(tab) }.testTag("bar_ttab_${tab.key}")
                                    ) {
                                        Text(
                                            "${tab.emoji} ${tab.titleMr}",
                                            color = if (active) GoldLight else TextCream,
                                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 11.5.sp,
                                            maxLines = 1,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                            items(
                                items = allowedPages.filter {
                                    !(session.role == UserRole.TEACHER && it == AppPage.TEACHER) &&
                                        "page_${it.key}" !in shell.pinnedQuickKeys
                                },
                                key = { "page_${it.key}" }
                            ) { pg ->
                                val active = shell.currentPage == pg
                                Surface(
                                    color = if (active) GoldContainer else Navy2,
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, if (active) Gold else Navy2),
                                    modifier = Modifier.clickable { onSelectPage(pg) }.testTag("bar_page_${pg.key}")
                                ) {
                                    Text(
                                        "${pg.emoji} ${pg.titleMr}",
                                        color = if (active) GoldLight else TextCream,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.5.sp,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (shell.toastMessage != null) {
                        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp).clickable { onDismissToast() }) {
                            BannerView(shell.toastMessage)
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        content(shell.currentPage)
                    }
                }
            }
        }

        if (isWideScreen) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.width(265.dp).fillMaxHeight()) {
                    drawerMenuContent()
                }
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    mainScaffold()
                }
            }
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(drawerContainerColor = NavySurface) {
                        drawerMenuContent()
                    }
                }
            ) {
                mainScaffold()
            }
        }
    }

    if (shell.showConfigDialog) {
        ConfigSettingsDialog(
            url = shell.sheetsUrl,
            cacheCount = shell.cacheCount,
            isBulkLoading = shell.isBulkLoading,
            bulkMessage = shell.bulkLoadMessage,
            testStatus = shell.testConnectionStatus,
            onUrlChange = onUrlChange,
            onTest = onTestConnection,
            onBulkLoad = onBulkLoad,
            onClearCache = onClearCache,
            onDismiss = onCloseConfig,
            isDarkTheme = shell.isDarkTheme,
            onToggleTheme = onToggleTheme,
            biometricEnabled = shell.biometricEnabled,
            biometricSupported = biometricSupported,
            onToggleBiometric = onToggleBiometric,
            remindersEnabled = shell.remindersEnabled,
            onToggleReminders = onToggleReminders,
            autoLockMinutes = shell.autoLockMinutes,
            onAutoLockChange = onAutoLockChange
        )
    }

    if (shell.isBiometricLocked || shell.isInactivityLocked) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ScreenBg)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = Navy,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.5.dp, Gold.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_school_emblem),
                        contentDescription = "SGS हायस्कूल",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(64.dp).clip(CircleShape)
                    )
                    Text(
                        if (shell.isInactivityLocked) "⏱️ निष्क्रियतेमुळे अ‍ॅप लॉक झाले आहे" else "🔒 अ‍ॅप बायोमेट्रिक लॉक आहे",
                        color = GoldLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        textAlign = TextAlign.Center
                    )
                    Text("${session.label} (${session.username})", color = TextCream, fontSize = 13.sp)
                    Button(
                        onClick = onPromptBiometricUnlock,
                        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = DeepNavy),
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("biometric_unlock_btn")
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(
                            if (biometricSupported) "फिंगरप्रिंट / स्क्रीन लॉकने उघडा" else "अ‍ॅप अनलॉक करा",
                            fontWeight = FontWeight.Bold
                        )
                    }
                    OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
                        Text("🚪 Logout करा", color = SoftRed)
                    }
                }
            }
        }
    }

    if (shell.showPendingModal) {
        PendingSavesModal(
            items = shell.pendingSaves,
            onRetryAll = onFlushPending,
            onExportJson = onExportPendingJson,
            onRemoveItem = onRemovePending,
            onClearAll = onClearAllPending,
            onDismiss = onClosePendingModal
        )
    }

    if (shell.certPreview != null) {
        CertificatePreviewDialog(
            payload = shell.certPreview,
            verifyBaseUrl = shell.sheetsUrl,
            twoUp = shell.twoUpPrint,
            onToggleTwoUp = onToggleTwoUp,
            onDismiss = onCloseCertPreview
        )
    }

    if (shell.showQuickActionsDialog) {
        QuickActionsHubDialog(
            role = session.role,
            pinnedKeys = shell.pinnedQuickKeys,
            onTogglePinKey = onTogglePinQuickKey,
            onSelectPage = onSelectPage,
            onSelectTeacherTab = onSelectTeacherTab,
            onOpenQrScanner = onOpenQrScanner,
            onOpenHolidayCalendar = onOpenHolidayDialog,
            onOpenBirthdays = onOpenBirthdaysDialog,
            onOpenTemplates = onOpenTemplatesDialog,
            onDismiss = onDismissQuickActions
        )
    }

    if (shell.showTemplatesDialog) {
        MessageTemplatesDialog(
            templates = shell.messageTemplates,
            onSaveTemplate = onSaveMessageTemplate,
            onDeleteTemplate = onDeleteMessageTemplate,
            onResetDefaults = onResetDefaultMessageTemplates,
            onDismiss = onDismissTemplatesDialog
        )
    }

    if (shell.showQrScanner) {
        QrScannerDialog(
            canOpenEarlyLeave = AppPage.EARLYLEAVE in allowedPages,
            findCachedStudent = findStudentInCache,
            onOpenProfile = onQrOpenProfile,
            onOpenEarlyLeave = onQrOpenEarlyLeave,
            onOpenSearch = onQrOpenSearch,
            onDismiss = onDismissQrScanner
        )
    }

    if (shell.showHolidayDialog) {
        HolidayCalendarDialog(
            holidays = shell.holidays,
            canManage = session.role == UserRole.SUPER || session.role == UserRole.MASTER,
            onAddHoliday = onAddHoliday,
            onDeleteHoliday = onDeleteHoliday,
            onSeedDefaults = onSeedDefaultHolidays,
            onDismiss = onDismissHolidayDialog
        )
    }

    if (shell.showBirthdaysDialog) {
        BirthdaysModalDialog(
            birthdays = shell.quickBirthdays,
            onRefresh = onRefreshQuickBirthdays,
            onDismiss = onDismissBirthdaysDialog
        )
    }

    pendingWaRequest?.let { waReq ->
        WhatsAppComposeWithTemplateDialog(
            request = waReq,
            templates = shell.messageTemplates,
            onOpenManageTemplates = {
                pendingWaRequest = null
                onOpenTemplatesDialog()
            },
            onDismiss = { pendingWaRequest = null }
        )
    }
    }
}
