package com.example

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appViewModel: AppViewModel = viewModel()
            val isDarkSystem = isSystemInDarkTheme()
            val isDark = when (appViewModel.themeOverride) {
                "dark" -> true
                "light" -> false
                else -> isDarkSystem
            }

            MyApplicationTheme(darkTheme = isDark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        OrbBackground(isDark = isDark)
                        AppNavigationContainer(viewModel = appViewModel, isDark = isDark)
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════
// THEME CANVAS ANIMATION
// ════════════════════════════════════
@Composable
fun OrbBackground(isDark: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbs")
    val driftX by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 60f,
        animationSpec = infiniteRepeatable(animation = tween(6000, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "driftX"
    )
    val driftY by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 50f,
        animationSpec = infiniteRepeatable(animation = tween(7000, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "driftY"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val rad1 = 300f
        val rad2 = 240f

        if (!isDark) {
            drawRect(color = Color(0xFFF0F5FF))
        } else {
            drawRect(color = Color(0xFF030712))
        }

        val primaryOrb = if (isDark) Color(0xFF2563C9) else Color(0xFF4A8FE0)
        val secondaryOrb = if (isDark) Color(0xFF0F3A80) else Color(0xFF7EC8FF)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primaryOrb.copy(alpha = if (isDark) 0.08f else 0.04f), Color.Transparent),
                center = Offset(w * 0.15f + driftX, h * 0.2f + driftY),
                radius = rad1
            ),
            center = Offset(w * 0.15f + driftX, h * 0.2f + driftY),
            radius = rad1
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(secondaryOrb.copy(alpha = if (isDark) 0.07f else 0.04f), Color.Transparent),
                center = Offset(w * 0.8f - driftX, h * 0.5f - driftY),
                radius = rad2
            ),
            center = Offset(w * 0.8f - driftX, h * 0.5f - driftY),
            radius = rad2
        )
    }
}

// ════════════════════════════════════
// INLINE DICTIONARY TRANSLATIONS
// ════════════════════════════════════
fun t(key: String, lang: String): String {
    val dict = mapOf(
        "en" to mapOf(
            "home_title" to "Tools.", "home_sub" to "Pick a tool below and get started instantly",
            "back_tools" to "All Tools", "brand_tag" to "AADITYA", "reset_btn" to "Reset", "go_btn" to "Go! →",
            "pdf_title" to "PDF Tools.", "pdf_sub" to "Compress, convert, edit or lock PDFs",
            "zip_title" to "Zip Maker.", "zip_sub" to "Select files, download zip instantly",
            "qr_title" to "QR Code Generator.", "qr_sub" to "Type text or link — generated live",
            "fun_title" to "Fun Tools.", "fun_sub" to "NASA daily picture, stats, jokes & words",
            "nav_title" to "Nepali Land.", "nav_sub" to "Nepali local area converter specs",
            "choose_files" to "📄 Choose Files"
        ),
        "ne" to mapOf(
            "home_title" to "टूलहरू।", "home_sub" to "तल टूल छान्नुहोस् र तुरुन्त सुरु गर्नुहोस्",
            "back_tools" to "सबै टूल", "brand_tag" to "आदित्य", "reset_btn" to "रिसेट", "go_btn" to "जाऊ! →",
            "pdf_title" to "PDF टूलहरू।", "pdf_sub" to "कम्प्रेस, एडिट र लक गर्नुहोस्",
            "zip_title" to "जिप मेकर।", "zip_sub" to "फाइल सिलेक्ट गरी जिप बनाउनुहोस्",
            "qr_title" to "QR जेनेरेटर।", "qr_sub" to "लिंक हाल्ने बित्तिकै तयार हुने",
            "fun_title" to "रमाइलो टूल।", "fun_sub" to "अन्तरिक्ष र दैनिक जानकारीहरू",
            "nav_title" to "नेपाली जग्गा।", "nav_sub" to "रोपनी र बिघा क्षेत्र क्यालकुलेटर",
            "choose_files" to "📄 फाइल चयन गर्नुहोस्"
        )
    )
    val fallback = dict["en"] ?: emptyMap()
    val active = dict[lang] ?: fallback
    return active[key] ?: fallback[key] ?: key
}

// ════════════════════════════════════
// BRAND HEADER MODULE
// ════════════════════════════════════
@Composable
fun TopBrandBar(viewModel: AppViewModel, isDark: Boolean) {
    var showLangMenu by remember { mutableStateOf(false) }
    var showThemeMenu by remember { mutableStateOf(false) }
    val selectedLangItem = viewModel.supportedLanguages.find { it.code == viewModel.currentLanguage }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo & Name
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { viewModel.showPage("home") }
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF4A8FE0), Color(0xFF1E5CBF)))),
                contentAlignment = Alignment.Center
            ) {
                Text("☁", color = Color.White, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Cloud",
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = if (isDark) Color.White else Color(0xFF0E1F50)
            )
        }

        // Actions
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // Theme selector Button
            Box {
                Button(
                    onClick = { showThemeMenu = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0x33FFFFFF) else Color(0x1F0E1F50),
                        contentColor = if (isDark) Color(0xFF7EC8FF) else Color(0xFF2A5090)
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    val icon = when (viewModel.themeOverride) {
                        "dark" -> "◑"
                        "light" -> "○"
                        else -> "⊙"
                    }
                    Text("$icon ▾", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                DropdownMenu(expanded = showThemeMenu, onDismissRequest = { showThemeMenu = false }) {
                    DropdownMenuItem(text = { Text("Device") }, onClick = { viewModel.setTheme("device"); showThemeMenu = false })
                    DropdownMenuItem(text = { Text("Dark") }, onClick = { viewModel.setTheme("dark"); showThemeMenu = false })
                    DropdownMenuItem(text = { Text("Light") }, onClick = { viewModel.setTheme("light"); showThemeMenu = false })
                }
            }

            // Language selector Button
            Box {
                Button(
                    onClick = { showLangMenu = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0x33FFFFFF) else Color(0x1F0E1F50),
                        contentColor = if (isDark) Color(0xFF7EC8FF) else Color(0xFF2A5090)
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("${selectedLangItem?.flag ?: "🇬🇧"} ${selectedLangItem?.code?.uppercase() ?: "EN"} ▾", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                DropdownMenu(expanded = showLangMenu, onDismissRequest = { showLangMenu = false }) {
                    viewModel.supportedLanguages.forEach { lang ->
                        DropdownMenuItem(
                            text = { Text("${lang.flag} ${lang.name}") },
                            onClick = { viewModel.setLang(lang.code); showLangMenu = false }
                        )
                    }
                }
            }

            // Admin configuration locking
            IconButton(
                onClick = { viewModel.dialogDevNotice = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Admin lock",
                    tint = if (isDark) Color(0xFF7EC8FF) else Color(0xFF2A5090),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Dynamic badge name
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) Color(0x1a4a8fe0) else Color(0x212563c9))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = t("brand_tag", viewModel.currentLanguage),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFF7EC8FF) else Color(0xFF1E5CBF)
                )
            }
        }
    }
}

// ════════════════════════════════════
// REUSABLE LAYOUT GLASS CARD
// ════════════════════════════════════
@Composable
fun GlassCard(
    isDark: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0x8C081232) else Color(0xCEFFFFFF)
        ),
        border = BorderStroke(
            1.dp,
            if (isDark) Color(0x2E3C6EDC) else Color(0x262563C9)
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            content = content
        )
    }
}

// ════════════════════════════════════
// MAIN CONTAINER COMPOSABLE
// ════════════════════════════════════
@Composable
fun AppNavigationContainer(viewModel: AppViewModel, isDark: Boolean) {
    val context = LocalContext.current
    val currentLang = viewModel.currentLanguage

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        TopBrandBar(viewModel = viewModel, isDark = isDark)

        Box(modifier = Modifier.weight(1f)) {
            when (viewModel.activePage) {
                "home" -> HomeScreen(viewModel = viewModel, isDark = isDark)
                "pdfhub" -> PdfHubScreen(viewModel = viewModel, isDark = isDark)
                "pdfcm" -> PdfcmScreen(viewModel = viewModel, isDark = isDark)
                "pdf2img" -> PdfToImageScreen(viewModel = viewModel, isDark = isDark)
                "img2pdf" -> ImagesToPdfScreen(viewModel = viewModel, isDark = isDark)
                "pdfe" -> PdfEditorScreen(viewModel = viewModel, isDark = isDark)
                "pai" -> AddImageToPdfScreen(viewModel = viewModel, isDark = isDark)
                "zip" -> ZipScreen(viewModel = viewModel, isDark = isDark)
                "qr" -> QrScreen(viewModel = viewModel, isDark = isDark)
                "funhub" -> FunHubScreen(viewModel = viewModel, isDark = isDark)
                "lifenums" -> LifeNumbersScreen(viewModel = viewModel, isDark = isDark)
                "cosmos" -> CosmosScreen(viewModel = viewModel, isDark = isDark)
                "jokes" -> JokesScreen(viewModel = viewModel, isDark = isDark)
                "wordday" -> WordDayScreen(viewModel = viewModel, isDark = isDark)
                "nav" -> NepaliLandScreen(viewModel = viewModel, isDark = isDark)
                "spacesim" -> SpaceSimScreen(viewModel = viewModel, isDark = isDark)
            }
        }
    }

    // ── Dialog Overlay: Silent Dev Notice
    if (viewModel.dialogDevNotice) {
        AlertDialog(
            onDismissRequest = { viewModel.dialogDevNotice = false },
            title = { Text("Feature in Development") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("This feature is currently in development and accessible only to authorized administrators.", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(20.dp))
                    // Secret Tap Indicator
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .clickable { viewModel.handleInvisibleDevTap() }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dialogDevNotice = false }) {
                    Text("Got it")
                }
            }
        )
    }

    // ── Dialog Overlay: Admin Unlock Password
    if (viewModel.dialogAdmin) {
        var passwordInput by remember { mutableStateOf("") }
        var isError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { viewModel.dialogAdmin = false },
            title = { Text("Admin Access") },
            text = {
                Column {
                    Text("Enter administrator pass to unlock dashboard:", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it; isError = false },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        isError = isError,
                        label = { Text("Password") }
                    )
                    if (isError) {
                        Text("Incorrect password supplied", color = Color.Red, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (viewModel.verifyAdminPassword(passwordInput)) {
                        viewModel.dialogAdmin = false
                        viewModel.dialogComments = true // Open panel directly
                    } else {
                        isError = true
                        passwordInput = ""
                    }
                }) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dialogAdmin = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ── Dialog Overlay: About Modal
    if (viewModel.dialogAbout) {
        val staticAboutHtml = "☁ **Cloud Tools**\n\nA fast, private, browser-based utility platform. No sign-up. No servers. No data collection. Every tool runs entirely on your device."
        AlertDialog(
            onDismissRequest = { viewModel.dialogAbout = false },
            title = { Text("About Platform") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    item {
                        Text(
                            text = viewModel.customAboutText.ifEmpty { staticAboutHtml },
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dialogAbout = false }) {
                    Text("Close")
                }
            }
        )
    }

    // ── Dialog Overlay: Comments & Custom Tools Admin Panel
    if (viewModel.dialogComments) {
        val commentsList by viewModel.commentsFlow.collectAsState(initial = emptyList())
        val customList by viewModel.customToolsFlow.collectAsState(initial = emptyList())
        val isHiddenList by viewModel.hiddenBuiltinsFlow.collectAsState(initial = emptyList())

        var userCommentName by remember { mutableStateOf("") }
        var userCommentText by remember { mutableStateOf("") }
        var commentStatus by remember { mutableStateOf("") }

        var customToolIcon by remember { mutableStateOf("🔮") }
        var customToolName by remember { mutableStateOf("") }
        var customToolDesc by remember { mutableStateOf("") }
        var customToolHref by remember { mutableStateOf("") }

        var editAbout by remember { mutableStateOf(viewModel.customAboutText) }

        AlertDialog(
            onDismissRequest = { viewModel.dialogComments = false },
            title = { Text("Platform Operations Control") },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(420.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Text("🔑 Unlocked Admin Status successfully.", color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    // Leave a Comment Box
                    item {
                        Text("💬 Submitting Feedback Card", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(value = userCommentName, onValueChange = { userCommentName = it }, label = { Text("Real name") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(value = userCommentText, onValueChange = { userCommentText = it }, label = { Text("Comment details") }, modifier = Modifier.fillMaxWidth())
                        if (commentStatus.isNotEmpty()) {
                            Text(commentStatus, color = Color.Red, fontSize = 11.sp)
                        }
                        Button(
                            onClick = {
                                val err = viewModel.submitUserComment(userCommentName, userCommentText)
                                if (err == null) {
                                    userCommentName = ""
                                    userCommentText = ""
                                    commentStatus = "Submitted successfully!"
                                } else {
                                    commentStatus = err
                                }
                            },
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            Text("Submit Comment")
                        }
                    }

                    // Saved Comments list
                    item {
                        Text("📋 Database Comments Count: ${commentsList.size}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    items(commentsList) { comment ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x0a000000))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(comment.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(comment.text, fontSize = 11.sp, color = Color.Gray)
                                Text(comment.timestamp, fontSize = 9.sp, color = Color.LightGray)
                            }
                            IconButton(onClick = { viewModel.deleteComment(comment.id) }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                            }
                        }
                    }
                    if (commentsList.isNotEmpty()) {
                        item {
                            Button(onClick = { viewModel.clearAllComments() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                                Text("Clear All Database Feedback")
                            }
                        }
                    }

                    // About section custom editing
                    item {
                        Text("ℹ Modify About Overlay Description", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = editAbout,
                            onValueChange = { editAbout = it },
                            label = { Text("About description text") },
                            modifier = Modifier.fillMaxWidth().height(100.dp)
                        )
                        Button(
                            onClick = {
                                viewModel.changeAboutText(editAbout)
                                Toast.makeText(context, "About updated!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text("Save About Metadata")
                        }
                    }

                    // Custom Link tool configuration
                    item {
                        Text("🔧 Inject Custom Link Tool Integration", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(value = customToolIcon, onValueChange = { customToolIcon = it }, label = { Text("Emoji symbol (e.g. 🔮)") })
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(value = customToolName, onValueChange = { customToolName = it }, label = { Text("Tool Label / Name") })
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(value = customToolDesc, onValueChange = { customToolDesc = it }, label = { Text("Short subtitle") })
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(value = customToolHref, onValueChange = { customToolHref = it }, label = { Text("HTTP target URL link") })
                        Button(
                            onClick = {
                                if (customToolName.isNotEmpty() && customToolHref.isNotEmpty()) {
                                    viewModel.addCustomTool(customToolIcon, customToolName, customToolDesc, customToolHref)
                                    customToolIcon = "🔮"
                                    customToolName = ""
                                    customToolDesc = ""
                                    customToolHref = ""
                                }
                            },
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            Text("Inject Tool Card")
                        }
                    }

                    // Added Custom tools list
                    items(customList) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x08000000))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${item.icon} ${item.name}", fontSize = 12.sp)
                            IconButton(onClick = { viewModel.removeCustomTool(item.id) }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red)
                            }
                        }
                    }

                    // Standard built-in visibility toggles
                    item {
                        Text("👁 Toggle Built-in core elements visibility", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    val defaultTools = listOf("frames", "pdfhub", "zip", "qr", "funhub", "calc", "nav")
                    items(defaultTools) { id ->
                        val isHidden = isHiddenList.any { it.id == id }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Built-in ID: $id", fontSize = 12.sp)
                            Button(
                                onClick = { viewModel.toggleBuiltInTool(id, !isHidden) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isHidden) Color.Red else Color(0xFF4ADE80)
                                )
                            ) {
                                Text(if (isHidden) "Hidden — Tap to Show" else "Visible — Tap to Hide", fontSize = 10.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dialogComments = false }) {
                    Text("Close Panel")
                }
            }
        )
    }
}

// ════════════════════════════════════
// 1. HOME SCREEN
// ════════════════════════════════════
@Composable
fun HomeScreen(viewModel: AppViewModel, isDark: Boolean) {
    val context = LocalContext.current
    val customToolsList by viewModel.customToolsFlow.collectAsState(initial = emptyList())
    val hiddenList by viewModel.hiddenBuiltinsFlow.collectAsState(initial = emptyList())

    val builtInTools = listOf(
        HomeToolItem("frames", "🎞️", "Video Frame Extractor", "Extract high quality frames from video file", "https://frameextract.netlify.app", true),
        HomeToolItem("pdfhub", "📄", "PDF Tools", "Compress, merge, lock, edit or convert PDFs", "pdfhub", false),
        HomeToolItem("zip", "🗜️", "Zip Maker", "Create zip archive with optional passwords", "zip", false),
        HomeToolItem("qr", "▦", "QR Generator", "Generate live precise vector QR codes", "qr", false),
        HomeToolItem("funhub", "🌌", "Fun Tools", "Interactive gravity, APOD, Word facts & jokes", "funhub", false),
        HomeToolItem("calc", "🧮", "Marks Calculator", "Calculate GPAs and marks aggregates", "https://yourlifebynumberscloud.netlify.app/", true),
        HomeToolItem("nav", "🧭", "Nepali Land", "Ropani, Aana, Bigha area converter", "nav", false)
    )

    val activeTools = builtInTools.filter { tool -> hiddenList.none { it.id == tool.id } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Hero Title card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF0A1F55), Color(0xFF1E5CBF), Color(0xFF2468D4))
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 32.dp)
            ) {
                Column {
                    Text(
                        text = t("home_title", viewModel.currentLanguage),
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Black,
                        fontSize = 44.sp,
                        lineHeight = 42.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = t("home_sub", viewModel.currentLanguage),
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Active tools list
        items(activeTools) { tool ->
            ToolGridItemCard(tool = tool, viewModel = viewModel, isDark = isDark)
        }

        // Added custom tools
        items(customToolsList) { custom ->
            HomeToolItem(
                id = custom.id,
                icon = custom.icon,
                title = custom.name,
                desc = custom.desc,
                target = custom.href,
                isExt = true
            ).let { ToolGridItemCard(tool = it, viewModel = viewModel, isDark = isDark) }
        }

        // About & Comments card strip
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { viewModel.dialogAbout = true }) {
                    Text("ℹ About", fontSize = 12.sp, color = if (isDark) Color(0xFF7EC8FF) else Color(0xFF1E5CBF))
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = { viewModel.dialogComments = true }) {
                    Text("💬 Comments / Admin", fontSize = 12.sp, color = if (isDark) Color(0xFF7EC8FF) else Color(0xFF1E5CBF))
                }
            }
        }

        // Fine polished copyright footer
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Divider(color = if (isDark) Color(0x334A8FE0) else Color(0x1F1E5CBF))
                Spacer(modifier = Modifier.height(12.dp))
                Text("Cloud", color = if (isDark) Color.White else Color(0xFF0E1F50), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Fast. Smooth. Efficient.", color = Color.Gray, fontSize = 10.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("© 2026 Cloud. All Rights Reserved.", color = Color.Gray, fontSize = 9.sp)
                Text("Intellectual property of Cloud. Made with Aaditya.", color = Color.Gray, fontSize = 8.sp)
            }
        }
    }
}

data class HomeToolItem(
    val id: String,
    val icon: String,
    val title: String,
    val desc: String,
    val target: String,
    val isExt: Boolean
)

@Composable
fun ToolGridItemCard(tool: HomeToolItem, viewModel: AppViewModel, isDark: Boolean) {
    val context = LocalContext.current
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0x1F2563C9) else Color(0x0F2563C9)
        ),
        border = BorderStroke(1.dp, if (isDark) Color(0x3D4A8FE0) else Color(0x1C2563C9)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (tool.isExt) {
                    Toast
                        .makeText(context, "Redirecting to external url...", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    viewModel.showPage(tool.target)
                }
            }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) Color(0x33FFFFFF) else Color(0x0f000000)),
                contentAlignment = Alignment.Center
            ) {
                Text(tool.icon, fontSize = 22.sp)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tool.title,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isDark) Color.White else Color(0xFF0E1F50)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tool.desc,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            Icon(
                imageVector = if (tool.isExt) Icons.Default.Share else Icons.Default.PlayArrow,
                contentDescription = "launch",
                tint = Color.Gray,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ════════════════════════════════════
// 2. PDF HUB CONTAINER PAGE
// ════════════════════════════════════
@Composable
fun PdfHubScreen(viewModel: AppViewModel, isDark: Boolean) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            IconButton(onClick = { viewModel.showPage("home") }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "back")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("All Tools", fontSize = 12.sp)
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF0A1F55), Color(0xFF1E5CBF)))
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Text("PDF\nTools.", fontWeight = FontWeight.Black, fontSize = 34.sp, color = Color.White, lineHeight = 34.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Choose a PDF operation below. Max 50MB.", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                }
            }
        }

        val pdfTools = listOf(
            Pair("pdfcm", "🗜️ Compress & Merge"),
            Pair("pdf2img", "🖼️ PDF to Image"),
            Pair("img2pdf", "📑 Images to PDF"),
            Pair("pdfe", "🖋️ PDF Editor"),
            Pair("pai", "🖼️ Add Image to PDF")
        )

        items(pdfTools) { (target, label) ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0x334A8FE0) else Color(0x1F2563C9)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.showPage(target) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (isDark) Color.White else Color(0xFF0E1F50))
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "right", tint = Color.Gray)
                }
            }
        }
    }
}

// ════════════════════════════════════
// 3. PDF COMPRESS SCREEN
// ════════════════════════════════════
@Composable
fun PdfcmScreen(viewModel: AppViewModel, isDark: Boolean) {
    val context = LocalContext.current
    val filesPicker = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetMultipleContents()) { uris ->
        viewModel.selectPdfFiles(uris)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TextButton(onClick = { viewModel.showPage("pdfhub") }) {
                Text("← PDF Tools")
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.radialGradient(listOf(Color(0xFF1E5CBF), Color(0xFF0A1F55))))
                    .padding(20.dp)
            ) {
                Column {
                    Text("Compress\n& Merge.", fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Color.White, lineHeight = 28.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(onClick = { filesPicker.launch("application/pdf") }) {
                        Text("📄 Choose PDF(s)")
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(onClick = { viewModel.pdfReset() }) {
                        Text("Reset", color = Color.White)
                    }
                }
            }
        }

        item {
            GlassCard(isDark = isDark) {
                Text("PDF Files Loaded", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                if (viewModel.pdfFiles.value.isEmpty()) {
                    Text("No files selected", color = Color.Gray, fontSize = 12.sp)
                } else {
                    viewModel.pdfFiles.value.forEach { f ->
                        Text("${f.name} - ${(f.size / 1024L)}KB", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("Compression Level", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val levels = listOf("screen", "ebook", "printer", "prepress")
                    levels.forEach { level ->
                        Button(
                            onClick = { viewModel.pdfCompLevel = level },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewModel.pdfCompLevel == level) Color(0xFF2563C9) else Color.Gray
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(level.uppercase(), fontSize = 10.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Merge Output Options", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = viewModel.pdfMerge == "yes", onClick = { viewModel.pdfMerge = "yes" })
                        Text("Yes, merge into one", fontSize = 11.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = viewModel.pdfMerge == "no", onClick = { viewModel.pdfMerge = "no" })
                        Text("No, process separately", fontSize = 11.sp)
                    }
                }

                if (viewModel.pdfStatus.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(viewModel.pdfStatus, color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                if (viewModel.pdfProgress > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(progress = viewModel.pdfProgress / 100f, modifier = Modifier.fillMaxWidth())
                }

                Spacer(modifier = Modifier.height(18.dp))
                Button(onClick = { viewModel.runPdfcm() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Process! →")
                }
            }
        }

        // Compiled results
        if (viewModel.pdfResults.value.isNotEmpty()) {
            item {
                Text("📄 Output", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            items(viewModel.pdfResults.value) { result ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(result.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("${(result.length() / 1024f).toInt()}KB", fontSize = 11.sp, color = Color.Gray)
                        }
                        Button(onClick = { FileSharingUtils.shareFile(context, result) }) {
                            Text("↓ Share")
                        }
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════
// 4. PDF TO IMAGE SCREEN
// ════════════════════════════════════
@Composable
fun PdfToImageScreen(viewModel: AppViewModel, isDark: Boolean) {
    val context = LocalContext.current
    val filePicker = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.selectP2iFile(uri)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            TextButton(onClick = { viewModel.showPage("pdfhub") }) {
                Text("← PDF Tools")
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.radialGradient(listOf(Color(0xFF1E5CBF), Color(0xFF0A1F55))))
                    .padding(20.dp)
            ) {
                Column {
                    Text("PDF to\nImage.", fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Color.White, lineHeight = 28.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(onClick = { filePicker.launch("application/pdf") }) {
                        Text("📄 Choose PDF File")
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(onClick = { viewModel.p2iReset() }) {
                        Text("Reset", color = Color.White)
                    }
                }
            }
        }

        item {
            GlassCard(isDark = isDark) {
                Text("PDF File", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(viewModel.p2iFile?.name ?: "No file selected", color = Color.Gray, fontSize = 12.sp)

                Spacer(modifier = Modifier.height(14.dp))
                Text("Output Format", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = viewModel.p2iFormat == "png", onClick = { viewModel.p2iFormat = "png" })
                        Text("PNG", fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = viewModel.p2iFormat == "jpeg", onClick = { viewModel.p2iFormat = "jpeg" })
                        Text("JPEG", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Export Resolution", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val scales = listOf("1", "2", "3")
                    scales.forEach { scale ->
                        Button(
                            onClick = { viewModel.p2iScale = scale },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewModel.p2iScale == scale) Color(0xFF2563C9) else Color.Gray
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("${scale}x", fontSize = 11.sp)
                        }
                    }
                }

                if (viewModel.p2iStatus.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(viewModel.p2iStatus, color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                if (viewModel.p2iProgress > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(progress = viewModel.p2iProgress / 100f, modifier = Modifier.fillMaxWidth())
                }

                Spacer(modifier = Modifier.height(18.dp))
                Button(onClick = { viewModel.runP2i() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Convert to Images! →")
                }
            }
        }

        // Extracted Images List
        if (viewModel.p2iResultsByFile.value.isNotEmpty()) {
            item {
                Text("📷 Pages as Images", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            items(viewModel.p2iResultsByFile.value) { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Button(onClick = { FileSharingUtils.shareFile(context, item, "image/*") }) {
                            Text("↓ Share")
                        }
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════
// 5. IMAGES TO PDF SCREEN
// ════════════════════════════════════
@Composable
fun ImagesToPdfScreen(viewModel: AppViewModel, isDark: Boolean) {
    val context = LocalContext.current
    val filesPicker = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetMultipleContents()) { uris ->
        viewModel.selectImg2pdfFiles(uris)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            TextButton(onClick = { viewModel.showPage("pdfhub") }) {
                Text("← PDF Tools")
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.radialGradient(listOf(Color(0xFF1E5CBF), Color(0xFF0A1F55))))
                    .padding(20.dp)
            ) {
                Column {
                    Text("Images\nto PDF.", fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Color.White, lineHeight = 28.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(onClick = { filesPicker.launch("image/*") }) {
                        Text("🖼 Choose Images")
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(onClick = { viewModel.img2pdfReset() }) {
                        Text("Reset", color = Color.White)
                    }
                }
            }
        }

        item {
            GlassCard(isDark = isDark) {
                Text("Image Files Layer Order", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                if (viewModel.img2pdfFiles.value.isEmpty()) {
                    Text("No images selected", color = Color.Gray, fontSize = 12.sp)
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        viewModel.img2pdfFiles.value.forEachIndexed { idx, it ->
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.DarkGray)
                            ) {
                                AsyncImage(
                                    model = it.uri,
                                    contentDescription = "thumb",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Text(
                                    "#${idx + 1}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(Color.Black.copy(alpha = 0.5f))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("Page Dimensions", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val sizes = listOf("a4", "letter", "fit")
                    sizes.forEach { s ->
                        Button(
                            onClick = { viewModel.img2pdfPagesize = s },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewModel.img2pdfPagesize == s) Color(0xFF2563C9) else Color.Gray
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(s.uppercase(), fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Orientation Mode", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val orients = listOf("portrait", "landscape", "auto")
                    orients.forEach { o ->
                        Button(
                            onClick = { viewModel.img2pdfOrientation = o },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewModel.img2pdfOrientation == o) Color(0xFF2563C9) else Color.Gray
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(o.substring(0, 1).uppercase() + o.substring(1), fontSize = 11.sp)
                        }
                    }
                }

                if (viewModel.img2pdfStatus.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(viewModel.img2pdfStatus, color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                if (viewModel.img2pdfProgress > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(progress = viewModel.img2pdfProgress / 100f, modifier = Modifier.fillMaxWidth())
                }

                Spacer(modifier = Modifier.height(18.dp))
                Button(onClick = { viewModel.runImg2Pdf() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Build PDF! →")
                }
            }
        }

        viewModel.img2pdfResultFile?.let { result ->
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(result.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("${(result.length() / 1024f).toInt()}KB", fontSize = 11.sp, color = Color.Gray)
                        }
                        Button(onClick = { FileSharingUtils.shareFile(context, result) }) {
                            Text("↓ Share PDF")
                        }
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════
// 6. PDF EDITOR (TEXT STAMP) SURFACE
// ════════════════════════════════════
@Composable
fun PdfEditorScreen(viewModel: AppViewModel, isDark: Boolean) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.selectPdfeFile(uri)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            TextButton(onClick = { viewModel.showPage("pdfhub") }) {
                Text("← PDF Tools")
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.radialGradient(listOf(Color(0xFF1E5CBF), Color(0xFF0A1F55))))
                    .padding(20.dp)
            ) {
                Column {
                    Text("PDF Editor.", fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(onClick = { picker.launch("application/pdf") }) {
                        Text("📄 Choose PDF File")
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(onClick = { viewModel.pdfeReset() }) {
                        Text("Reset", color = Color.White)
                    }
                }
            }
        }

        item {
            GlassCard(isDark = isDark) {
                Text("PDF File", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(viewModel.pdfeSelectedFile?.name ?: "No file loaded", color = Color.Gray, fontSize = 11.sp)

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = viewModel.pdfeText,
                    onValueChange = { viewModel.pdfeText = it },
                    label = { Text("Stamp Text Note") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = viewModel.pdfeFontSize,
                        onValueChange = { viewModel.pdfeFontSize = it },
                        label = { Text("Font Size") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = viewModel.pdfeRotate,
                        onValueChange = { viewModel.pdfeRotate = it },
                        label = { Text("Rotation") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Click inside canvas below to seal. Click Undo if wrong:", fontSize = 11.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(6.dp))
                Button(onClick = { viewModel.undoLastPdfeStamp() }) {
                    Text("↩ Undo Last Stamp")
                }

                if (viewModel.pdfeStatus.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(viewModel.pdfeStatus, color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))
                Button(onClick = { viewModel.buildPdfe() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Download Edited PDF →")
                }
            }
        }

        viewModel.pdfeSelectedFile?.let {
            item {
                Text("Preview & Placement Canvas", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                // Draw a simple interactive mock-stamping card that maps clicks proportionally!
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(Color.White)
                        .border(1.dp, Color.Gray)
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                change.consume()
                            }
                        }
                        .pointerInput(Unit) {
                            // Touch placement stamping
                            detectTapGestures { offset ->
                                val normX = offset.x / size.width.toFloat()
                                val normY = offset.y / size.height.toFloat()
                                viewModel.pdfeStamps.value = viewModel.pdfeStamps.value + PdfProcessingHelper.TextStamp(
                                    pageIndex = 0, // Page 1
                                    text = viewModel.pdfeText,
                                    size = viewModel.pdfeFontSize.toFloatOrNull() ?: 12f,
                                    colorHex = viewModel.pdfeColor,
                                    rotation = viewModel.pdfeRotate.toFloatOrNull() ?: 0f,
                                    normX = normX,
                                    normY = normY
                                )
                                viewModel.pdfeStatus = "Added stamp proportionally at info!"
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Draw stamps placed
                        viewModel.pdfeStamps.value.forEach { stamp ->
                            val x = stamp.normX * size.width
                            val y = stamp.normY * size.height
                            drawContext.canvas.nativeCanvas.save()
                            val paint = android.graphics.Paint().apply {
                                color = android.graphics.Color.BLACK
                                textSize = stamp.size * 2
                            }
                            drawContext.canvas.nativeCanvas.translate(x, y)
                            drawContext.canvas.nativeCanvas.rotate(stamp.rotation)
                            drawContext.canvas.nativeCanvas.drawText(stamp.text, 0f, 0f, paint)
                            drawContext.canvas.nativeCanvas.restore()
                        }
                    }
                    Text(
                        "PAGE 1 - Touch to place note stamp",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(8.dp).align(Alignment.BottomStart)
                    )
                }
            }
        }

        viewModel.pdfeResultFile?.let { result ->
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(result.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Button(onClick = { FileSharingUtils.shareFile(context, result) }) {
                            Text("↓ Share Edited")
                        }
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════
// 7. ADD IMAGE TO PDF SCREEN
// ════════════════════════════════════
@Composable
fun AddImageToPdfScreen(viewModel: AppViewModel, isDark: Boolean) {
    val context = LocalContext.current
    val pdfPicker = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.selectPaiPdf(uri)
    }
    val imgPicker = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.selectPaiImg(uri)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            TextButton(onClick = { viewModel.showPage("pdfhub") }) {
                Text("← PDF Tools")
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.radialGradient(listOf(Color(0xFF1E5CBF), Color(0xFF0A1F55))))
                    .padding(20.dp)
            ) {
                Column {
                    Text("Add Image\nto PDF.", fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(onClick = { pdfPicker.launch("application/pdf") }) {
                        Text("📄 Choose PDF File")
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(onClick = { imgPicker.launch("image/*") }) {
                        Text("🖼 Choose Image File")
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(onClick = { viewModel.paiReset() }) {
                        Text("Reset", color = Color.White)
                    }
                }
            }
        }

        item {
            GlassCard(isDark = isDark) {
                Text("PDF: ${viewModel.paiPdfFile?.name ?: "No PDF"}", fontSize = 12.sp)
                Text("Image: ${viewModel.paiImgFile?.name ?: "No Image"}", fontSize = 12.sp)

                Spacer(modifier = Modifier.height(10.dp))
                Text("Stamping Image Width", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val sizes = listOf(80, 160, 240, 320)
                    sizes.forEach { sz ->
                        Button(
                            onClick = { viewModel.paiWidthPill = sz },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewModel.paiWidthPill == sz) Color(0xFF2563C9) else Color.Gray
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(
                                when (sz) {
                                    80 -> "Small"
                                    160 -> "Medium"
                                    240 -> "Large"
                                    else -> "Full"
                                },
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                if (viewModel.paiStatus.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(viewModel.paiStatus, color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))
                Button(onClick = { viewModel.runPai() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Download PDF →")
                }
            }
        }

        viewModel.paiPdfFile?.let {
            item {
                Text("Preview Placement - Click on spot below to seal image stamp:", fontSize = 11.sp, color = Color.Gray)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(Color.White)
                        .border(1.dp, Color.LightGray)
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val normX = offset.x / size.width.toFloat()
                                val normY = offset.y / size.height.toFloat()
                                
                                val cacheImg = PdfProcessingHelper.copyUriToCache(context, viewModel.paiImgFile?.uri ?: return@detectTapGestures, ".png")
                                if (cacheImg != null) {
                                    viewModel.paiStamps.value = viewModel.paiStamps.value + PdfProcessingHelper.ImageStamp(
                                        pageIndex = 0,
                                        imageFile = cacheImg,
                                        width = viewModel.paiWidthPill.toFloat(),
                                        normX = normX,
                                        normY = normY
                                    )
                                    viewModel.paiStatus = "✓ Image signature stamped on coordinates!"
                                }
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        viewModel.paiStamps.value.forEach { stamp ->
                            // Draw mock indicator representing placement spot
                            drawCircle(
                                color = Color.Red.copy(alpha = 0.5f),
                                radius = 24f,
                                center = Offset(stamp.normX * size.width, stamp.normY * size.height)
                            )
                        }
                    }
                    Text("Preview Page 1 Stamping", color = Color.Gray, modifier = Modifier.padding(8.dp).align(Alignment.BottomStart))
                }
            }
        }

        viewModel.paiResultFile?.let { result ->
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(result.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Button(onClick = { FileSharingUtils.shareFile(context, result) }) {
                            Text("↓ Share PDF")
                        }
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════
// 8. ZIP MAKER SCREEN
// ════════════════════════════════════
@Composable
fun ZipScreen(viewModel: AppViewModel, isDark: Boolean) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetMultipleContents()) { uris ->
        viewModel.selectZipFiles(uris)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            TextButton(onClick = { viewModel.showPage("home") }) {
                Text("← Back Home")
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.radialGradient(listOf(Color(0xFF0F3A80), Color(0xFF1E5CBF))))
                    .padding(24.dp)
            ) {
                Column {
                    Text("Zip Tool.", fontWeight = FontWeight.Black, fontSize = 34.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(onClick = { picker.launch("*/*") }) {
                        Text("🗜️ Choose Files")
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(onClick = { viewModel.zipReset() }) {
                        Text("Reset", color = Color.White)
                    }
                }
            }
        }

        item {
            GlassCard(isDark = isDark) {
                Text("Files count: ${viewModel.zipFilesList.value.size}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                viewModel.zipFilesList.value.forEach { f ->
                    Text("${f.name} - ${(f.size / 1024f).toInt()}KB", fontSize = 12.sp, color = Color.Gray)
                }

                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = viewModel.zipName,
                    onValueChange = { viewModel.zipName = it },
                    label = { Text("Archive Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = viewModel.zipPass,
                    onValueChange = { viewModel.zipPass = it },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    label = { Text("Password (optional metadata)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Leave blank to compile standard open ZIP file.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 2.dp))

                if (viewModel.zipStatus.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(viewModel.zipStatus, color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                if (viewModel.zipProgress > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(progress = viewModel.zipProgress / 100f, modifier = Modifier.fillMaxWidth())
                }

                Spacer(modifier = Modifier.height(18.dp))
                Button(onClick = { viewModel.runZip() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Create Zip! →")
                }
            }
        }

        viewModel.zipResultFile?.let { result ->
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(result.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Button(onClick = { FileSharingUtils.shareFile(context, result, "application/zip") }) {
                            Text("↓ Share Zip")
                        }
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════
// 9. QR GENERATOR SCREEN
// ════════════════════════════════════
@Composable
fun QrScreen(viewModel: AppViewModel, isDark: Boolean) {
    val context = LocalContext.current
    val historyList by viewModel.qrHistoryFlow.collectAsState(initial = emptyList())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            TextButton(onClick = { viewModel.showPage("home") }) {
                Text("← Back Home")
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.radialGradient(listOf(Color(0xFF0F3A80), Color(0xFF1E5CBF))))
                    .padding(24.dp)
            ) {
                Column {
                    Text("QR Code\nGenerator.", fontWeight = FontWeight.Black, fontSize = 34.sp, color = Color.White, lineHeight = 34.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = viewModel.qrText,
                        onValueChange = {
                            viewModel.qrText = it
                            viewModel.triggerQrLivePreview()
                        },
                        placeholder = { Text("🔗 Enter URL or text…", color = Color.LightGray) },
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0x33FFFFFF),
                            focusedContainerColor = Color(0x4DFFFFFF)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Live Preview Row mapping
        viewModel.qrLiveResult?.let { bmp ->
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("LIVE PREVIEW", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .background(Color.White)
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "qr preview",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        item {
            GlassCard(isDark = isDark) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = viewModel.qrSize,
                        onValueChange = { viewModel.qrSize = it },
                        label = { Text("Size px") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = viewModel.qrEcc,
                        onValueChange = { viewModel.qrEcc = it },
                        label = { Text("ECC (L/M/Q/H)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = viewModel.qrFg,
                        onValueChange = { viewModel.qrFg = it; viewModel.triggerQrLivePreview() },
                        label = { Text("FG Color Hex") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = viewModel.qrBgColor,
                        onValueChange = { viewModel.qrBgColor = it; viewModel.triggerQrLivePreview() },
                        label = { Text("BG Color Hex") },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (viewModel.qrStatus.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(viewModel.qrStatus, color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))
                Button(onClick = { viewModel.generateQRFinal() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Generate & Share QR →")
                }
            }
        }

        // Final code to download/export share
        viewModel.qrFinalResult?.let { bmp ->
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("RESULT", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .background(Color.White)
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(bitmap = bmp.asImageBitmap(), contentDescription = "qr final", modifier = Modifier.fillMaxSize())
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(onClick = {
                        val file = File(context.cacheDir, "cloud-qr.png")
                        FileOutputStream(file).use { out ->
                            bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                        FileSharingUtils.shareFile(context, file, "image/png")
                    }) {
                        Text("↓ Export Compartment")
                    }
                }
            }
        }

        // QR creation scans history list
        if (historyList.isNotEmpty()) {
            item {
                Text("🕓 Recent QR Code Scans", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            items(historyList) { history ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.qrText = history.text
                            viewModel.qrFg = history.fg
                            viewModel.qrBgColor = history.bg
                            viewModel.triggerQrLivePreview()
                            viewModel.generateQRFinal()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(history.text, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(history.timestamp, fontSize = 11.sp, color = Color.Gray)
                        }
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Load")
                    }
                }
            }

            item {
                Button(onClick = { viewModel.clearQrHistory() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                    Text("🗑 Clear Scan History")
                }
            }
        }
    }
}

// ════════════════════════════════════
// 10. FUN TOOLS HUB SCREEN
// ════════════════════════════════════
@Composable
fun FunHubScreen(viewModel: AppViewModel, isDark: Boolean) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            TextButton(onClick = { viewModel.showPage("home") }) {
                Text("← Back Home")
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.radialGradient(listOf(Color(0xFF0F3A80), Color(0xFF1E5CBF))))
                    .padding(24.dp)
            ) {
                Column {
                    Text("Fun\nTools.", fontWeight = FontWeight.Black, fontSize = 34.sp, color = Color.White, lineHeight = 34.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Explore Cosmos facts & numbers", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                }
            }
        }

        val funTools = listOf(
            Triple("lifenums", "🔢 Life by Numbers", " heartbeats, breaths, blinks - lived stats"),
            // Redirect external life metric to space model
            Triple("spacesim", "🌌 Infinite Space Simulator", "Watch physics and visual orbit gravity unfold"),
            Triple("cosmos", "🌌 Today in Cosmos", "NASA photo detail, Subcontinent history events"),
            Triple("jokes", "😂 Random Jokes", "Refreshed general & coding joke cards"),
            Triple("wordday", "📖 Word of the Day", "Lexical vocabulary definitions")
        )

        items(funTools) { (target, name, subtitle) ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0x334A8FE0) else Color(0x1F2563C9)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.showPage(target) }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (isDark) Color.White else Color(0xFF0E1F50))
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "go", tint = Color.Gray)
                    }
                    Text(subtitle, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}

// ════════════════════════════════════
// 11. LIFE BY NUMBERS SCREEN
// ════════════════════════════════════
@Composable
fun LifeNumbersScreen(viewModel: AppViewModel, isDark: Boolean) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            TextButton(onClick = { viewModel.showPage("funhub") }) {
                Text("← Fun Tools")
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.radialGradient(listOf(Color(0xFF0F3A80), Color(0xFF1E5CBF))))
                    .padding(24.dp)
            ) {
                Column {
                    Text("Life by\nNumbers.", fontWeight = FontWeight.Black, fontSize = 34.sp, color = Color.White, lineHeight = 34.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = viewModel.lifeBirthday,
                        onValueChange = {
                            viewModel.lifeBirthday = it
                            viewModel.calcLifeMetrics()
                        },
                        placeholder = { Text("YYYY-MM-DD Birthday example...", color = Color.LightGray) },
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0x33FFFFFF),
                            focusedContainerColor = Color(0x4DFFFFFF)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (viewModel.lifeBirthday.isNotEmpty()) {
            item {
                Text(viewModel.lifeAgeText, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(vertical = 4.dp))
            }
            items(viewModel.lifeStats.value) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(item.num, fontWeight = FontWeight.Black, fontSize = 22.sp, color = if (isDark) Color(0xFF7EC8FF) else Color(0xFF1E5CBF))
                        Text(item.label.uppercase(), fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════
// 12. TODAY IN COSMOS SCREEN
// ════════════════════════════════════
@Composable
fun CosmosScreen(viewModel: AppViewModel, isDark: Boolean) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.loadCosmosData()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            TextButton(onClick = { viewModel.showPage("funhub") }) {
                Text("← Fun Tools")
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.radialGradient(listOf(Color(0xFF0F3A80), Color(0xFF1E5CBF))))
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = viewModel.cosmosDate,
                        onValueChange = { viewModel.cosmosDate = it },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0x33FFFFFF),
                            focusedContainerColor = Color(0x4DFFFFFF)
                        ),
                        label = { Text("Date (YYYY-MM-DD)", color = Color.White) }
                    )
                    Button(onClick = { viewModel.loadCosmosData() }) {
                        Text("↻ Load")
                    }
                }
            }
        }

        if (viewModel.cosmosLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else {
            // NASA APOD
            item {
                GlassCard(isDark = isDark) {
                    Text("🌌 NASA PICTURE OF THE DAY", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(viewModel.cosmosApodTitle, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (viewModel.cosmosApodUrl.isNotEmpty()) {
                        AsyncImage(
                            model = viewModel.cosmosApodUrl,
                            contentDescription = "apod detail",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(viewModel.cosmosApodDesc, fontSize = 12.sp, lineHeight = 16.sp)
                }
            }

            // Historical facts
            item {
                Text("📅 ON THIS DAY - Subcontinent Bias", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            items(viewModel.cosmosHistoryEvents.value) { event ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(event.year, fontWeight = FontWeight.Black, fontSize = 18.sp, color = if (isDark) Color(0xFF7EC8FF) else Color(0xFF1E5CBF))
                        Text(event.text, fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            // Daily advice quote
            item {
                GlassCard(isDark = isDark) {
                    Text("💡 COSMOS DAILY ADVICE slip", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("\"${viewModel.cosmosAdviceStr}\"", fontSize = 14.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                }
            }
        }
    }
}

// ════════════════════════════════════
// 13. JOKES SCREEN
// ════════════════════════════════════
@Composable
fun JokesScreen(viewModel: AppViewModel, isDark: Boolean) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadJokesData()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            TextButton(onClick = { viewModel.showPage("funhub") }) {
                Text("← Fun Tools")
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.radialGradient(listOf(Color(0xFF0F3A80), Color(0xFF1E5CBF))))
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Random\nJokes.", fontWeight = FontWeight.Black, fontSize = 28.sp, color = Color.White, lineHeight = 28.sp)
                    Button(onClick = { viewModel.loadJokesData() }) {
                        Text("↻ New")
                    }
                }
            }
        }

        if (viewModel.jokesLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else {
            items(viewModel.jokesLoadedList.value) { joke ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("${joke.emoji} ${joke.category} Joke", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(joke.text, fontSize = 14.sp, lineHeight = 18.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            TextButton(onClick = {
                                clipboardManager.setText(AnnotatedString(joke.text))
                                Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                            }) {
                                Text("📋 Copy")
                            }
                        }
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════
// 14. WORD OF THE DAY SCREEN
// ════════════════════════════════════
@Composable
fun WordDayScreen(viewModel: AppViewModel, isDark: Boolean) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadWordContent()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = { viewModel.showPage("funhub") }) {
            Text("← Fun Tools")
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.radialGradient(listOf(Color(0xFF0F3A80), Color(0xFF1E5CBF))))
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Word of\nthe Day.", fontWeight = FontWeight.Black, fontSize = 28.sp, color = Color.White, lineHeight = 28.sp)
                Button(onClick = { viewModel.loadWordContent() }) {
                    Text("↻ Refresh")
                }
            }
        }

        viewModel.wordOfToday?.let { w ->
            GlassCard(isDark = isDark) {
                Text("📖 WORD ENTRY", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(6.dp))
                Text(w.word, fontWeight = FontWeight.Black, fontSize = 34.sp, color = if (isDark) Color(0xFF7EC8FF) else Color(0xFF1E5CBF))
                Spacer(modifier = Modifier.height(12.dp))
                Text("📌 Definition", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Text(w.def, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("🏛 Origin", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Text(w.origin, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Text("📝 Example", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Text("\"${w.example}\"", fontSize = 13.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)

                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = {
                    clipboardManager.setText(AnnotatedString("${w.word}: ${w.def}"))
                    Toast.makeText(context, "Copied word details!", Toast.LENGTH_SHORT).show()
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("📋 Copy Word & Definition")
                }
            }
        }
    }
}

// ════════════════════════════════════
// 15. NEPALI LAND SCREEN
// ════════════════════════════════════
@Composable
fun NepaliLandScreen(viewModel: AppViewModel, isDark: Boolean) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            TextButton(onClick = { viewModel.showPage("home") }) {
                Text("← Back Home")
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.radialGradient(listOf(Color(0xFF0F3A80), Color(0xFF1E5CBF))))
                    .padding(20.dp)
            ) {
                Column {
                    Text("Nepali Land\nCalculator.", fontWeight = FontWeight.Black, fontSize = 28.sp, color = Color.White, lineHeight = 28.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Conversion breakdown for local units", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                }
            }
        }

        item {
            GlassCard(isDark = isDark) {
                Text("Calculation Input Mode", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    Button(
                        onClick = { viewModel.landMode = "single" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (viewModel.landMode == "single") Color(0xFF2563C9) else Color.Gray
                        )
                    ) {
                        Text("Single Value", fontSize = 11.sp)
                    }
                    Button(
                        onClick = { viewModel.landMode = "area" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (viewModel.landMode == "area") Color(0xFF2563C9) else Color.Gray
                        )
                    ) {
                        Text("Length × Breadth", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                if (viewModel.landMode == "single") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = viewModel.landSingleVal,
                            onValueChange = { viewModel.landSingleVal = it },
                            label = { Text("Value (e.g. 2.5)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = viewModel.landSingleFromUnit,
                            onValueChange = { viewModel.landSingleFromUnit = it },
                            label = { Text("From Unit") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = viewModel.landAreaLength,
                            onValueChange = { viewModel.landAreaLength = it },
                            label = { Text("Length") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = viewModel.landAreaBreadth,
                            onValueChange = { viewModel.landAreaBreadth = it },
                            label = { Text("Breadth") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (viewModel.landStatus.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(viewModel.landStatus, color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))
                Button(onClick = { viewModel.navConvertLand() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Convert Land Area! →")
                }
            }
        }

        if (viewModel.landResults.value.isNotEmpty()) {
            item {
                Text("Conversion Breakdowns", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            items(viewModel.landResults.value) { (unitName, value) ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(unitName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(String.format(Locale.US, "%.5f", value), fontWeight = FontWeight.Black, fontSize = 15.sp, color = if (isDark) Color(0xFF7EC8FF) else Color(0xFF1E5CBF))
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════
// 16. GRAVITY SPACE SIMULATOR SURFACE
// ════════════════════════════════════
@Composable
fun SpaceSimScreen(viewModel: AppViewModel, isDark: Boolean) {
    val context = LocalContext.current
    var simTime by remember { mutableStateOf(0f) }
    var scaleZoom by remember { mutableStateOf(1.0f) }

    LaunchedEffect(Unit) {
        while (true) {
            simTime += 0.05f
            delay(16)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TextButton(onClick = { viewModel.showPage("funhub") }) {
            Text("← Fun Tools")
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.radialGradient(listOf(Color(0xFF0F3A80), Color(0xFF1E5CBF))))
                .padding(20.dp)
        ) {
            Column {
                Text("Space\nSimulator.", fontWeight = FontWeight.Black, fontSize = 28.sp, color = Color.White)
                Text("Interactive gravity space preview model", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.Black)
                .border(2.dp, Color(0xFF4A8FE0), RoundedCornerShape(18.dp))
        ) {
            // Draw interactive space bodies on canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2
                val cy = size.height / 2

                // Starfield background
                for (i in 0..50) {
                    val angleRand = ((i * 37.3) % (2.0 * Math.PI)).toFloat()
                    val maxS = if (size.width > size.height) size.width else size.height
                    val rRand = (i * 91.7f) % maxS
                    val starX = cx + cos(angleRand) * rRand
                    val starY = cy + sin(angleRand) * rRand
                    drawCircle(Color.White.copy(alpha = 0.5f), radius = 1.5f, center = Offset(starX, starY))
                }

                // ── Sun star
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFD54F), Color(0xFFFF8F00), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = 42f * scaleZoom
                    ),
                    radius = 42f * scaleZoom,
                    center = Offset(cx, cy)
                )

                // ── Planet Mercury
                val mercAngle = simTime * 0.7f
                val mercX = cx + cos(mercAngle) * 60f * scaleZoom
                val mercY = cy + sin(mercAngle) * 60f * scaleZoom
                drawCircle(Color(0xFFB5B5B5), radius = 4f * scaleZoom, center = Offset(mercX, mercY))

                // ── Planet Venus
                val venAngle = simTime * 0.4f
                val venX = cx + cos(venAngle) * 90f * scaleZoom
                val venY = cy + sin(venAngle) * 90f * scaleZoom
                drawCircle(Color(0xFFE8CDA0), radius = 7f * scaleZoom, center = Offset(venX, venY))

                // ── Planet Earth + Orbiting Moon
                val earthAngle = simTime * 0.25f
                val earthX = cx + cos(earthAngle) * 130f * scaleZoom
                val earthY = cy + sin(earthAngle) * 130f * scaleZoom
                drawCircle(Color(0xFF4A9EFF), radius = 8f * scaleZoom, center = Offset(earthX, earthY))

                val moonAngle = simTime * 1.5f
                val moonX = earthX + cos(moonAngle) * 16f * scaleZoom
                val moonY = earthY + sin(moonAngle) * 16f * scaleZoom
                drawCircle(Color(0xFFE0E0E0), radius = 2f * scaleZoom, center = Offset(moonX, moonY))

                // ── Planet Mars
                val marsAngle = simTime * 0.15f
                val marsX = cx + cos(marsAngle) * 180f * scaleZoom
                val marsY = cy + sin(marsAngle) * 180f * scaleZoom
                drawCircle(Color(0xFFC1440E), radius = 6f * scaleZoom, center = Offset(marsX, marsY))

                // ── Planet Jupiter (Large)
                val jupAngle = simTime * 0.08f
                val jupX = cx + cos(jupAngle) * 230f * scaleZoom
                val jupY = cy + sin(jupAngle) * 230f * scaleZoom
                drawCircle(Color(0xFFFFB74D), radius = 16f * scaleZoom, center = Offset(jupX, jupY))
            }

            // Interactive controls inside simulator
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(onClick = { scaleZoom = if (scaleZoom - 0.1f < 0.5f) 0.5f else scaleZoom - 0.1f }) {
                    Text("Zoom -")
                }
                Button(onClick = { scaleZoom = scaleZoom + 0.1f }) {
                    Text("Zoom +")
                }
                Button(onClick = { scaleZoom = 1.0f }) {
                    Text("Reset")
                }
            }
        }
    }
}
