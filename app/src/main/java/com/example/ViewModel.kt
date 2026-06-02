package com.example

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.floor
import kotlin.math.max

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = AppRepository(db.dao())

    // Language list reference
    val supportedLanguages = listOf(
        LangItem("en", "English", "🇬🇧"),
        LangItem("ne", "नेपाली", "🇳🇵"),
        LangItem("bh", "भोजपुरी", "🇮🇳🇳🇵"),
        LangItem("mai", "मैथिली", "🇮🇳🇳🇵"),
        LangItem("ar", "العربية", "🇸🇦"),
        LangItem("fr", "Français", "🇫🇷"),
        LangItem("de", "Deutsche", "🇩🇪"),
        LangItem("hi", "हिन्दी", "🇮🇳"),
        LangItem("it", "Italiano", "🇮🇹"),
        LangItem("pl", "Polski", "🇵🇱"),
        LangItem("zh_s", "简体中文", "🇨🇳"),
        LangItem("zh_t", "繁體中文", "🇹🇼"),
        LangItem("ja", "日本語", "🇯🇵"),
        LangItem("ko", "한국어", "🇰🇷")
    )

    // Lang & Theme configs
    var currentLanguage by mutableStateOf("en")
        private set
    var themeOverride by mutableStateOf("device") // "device", "dark", "light"
        private set

    // Admin Credentials & States
    var unlocked by mutableStateOf(false)
        private set
    var customAboutText by mutableStateOf("")
        private set
    var dialogDevNotice by mutableStateOf(false)
    var dialogAdmin by mutableStateOf(false)
    var dialogAbout by mutableStateOf(false)
    var dialogComments by mutableStateOf(false)

    // Comments & Custom Tools lists
    val commentsFlow = repository.allComments
    val customToolsFlow = repository.allCustomTools
    val hiddenBuiltinsFlow = repository.allHiddenBuiltins
    val qrHistoryFlow = repository.qrHistory

    init {
        viewModelScope.launch {
            repository.getSetting("about_text")?.let { customAboutText = it }
            repository.getSetting("lang")?.let { currentLanguage = it }
            repository.getSetting("theme")?.let { themeOverride = it }
        }
    }

    fun setLang(code: String) {
        currentLanguage = code
        viewModelScope.launch {
            repository.saveSetting("lang", code)
        }
    }

    fun setTheme(mode: String) {
        themeOverride = mode
        viewModelScope.launch {
            repository.saveSetting("theme", mode)
        }
    }

    // Secret trigger counter taps
    private var devTapCount = 0
    private var devTapTimer: Timer? = null

    fun handleInvisibleDevTap() {
        devTapCount++
        devTapTimer?.cancel()
        devTapTimer = Timer()
        devTapTimer?.schedule(object : TimerTask() {
            override fun run() {
                devTapCount = 0
            }
        }, 2000)

        if (devTapCount >= 7) {
            devTapCount = 0
            dialogDevNotice = false
            dialogAdmin = true
        }
    }

    fun verifyAdminPassword(p: String): Boolean {
        return if (p == "1111" || p == "aaditya") { // easy recovery or 1111 default
            unlocked = true
            true
        } else {
            false
        }
    }

    fun changeAboutText(t: String) {
        customAboutText = t
        viewModelScope.launch {
            repository.saveSetting("about_text", t)
        }
    }

    fun submitUserComment(name: String, text: String): String? {
        val bNames = listOf("cloud", "aaditya", "admin", "anonymous", "anon", "test", "user")
        val lowerName = name.lowercase().replace(" ", "")
        if (bNames.any { lowerName.contains(it) }) {
            return "That name is not allowed. Please use your real name."
        }
        if (name.length < 2) return "Name too short."
        if (text.length < 3) return "Comment too short."

        viewModelScope.launch {
            val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            repository.insertComment(name, text, date)
        }
        return null
    }

    fun deleteComment(id: Int) {
        viewModelScope.launch { repository.deleteComment(id) }
    }

    fun clearAllComments() {
        viewModelScope.launch { repository.deleteAllComments() }
    }

    fun addCustomTool(icon: String, name: String, desc: String, href: String) {
        viewModelScope.launch {
            val id = "custom_" + System.currentTimeMillis()
            repository.insertCustomTool(id, icon, name, desc, href)
        }
    }

    fun removeCustomTool(id: String) {
        viewModelScope.launch { repository.deleteCustomTool(id) }
    }

    fun toggleBuiltInTool(id: String, hidden: Boolean) {
        viewModelScope.launch {
            if (hidden) {
                repository.hideBuiltin(id)
            } else {
                repository.showBuiltin(id)
            }
        }
    }


    // ════════════════════════════════════
    // NAVIGATION
    // ════════════════════════════════════
    var activePage by mutableStateOf("home")
    fun showPage(id: String) {
        activePage = id
    }


    // ════════════════════════════════════
    // WORKSPACE 1: PDF COMPRESS & MERGE
    // ════════════════════════════════════
    var pdfFiles = mutableStateOf<List<SelectedFile>>(emptyList())
    var pdfCompLevel by mutableStateOf("screen")
    var pdfMerge by mutableStateOf("yes")
    var pdfStatus by mutableStateOf("")
    var pdfProgress by mutableStateOf(0)
    var pdfResults = mutableStateOf<List<File>>(emptyList())

    fun selectPdfFiles(uris: List<Uri>) {
        val files = uris.mapNotNull { uri ->
            resolveFileInfo(uri)?.let { info ->
                SelectedFile(uri, info.name, info.size)
            }
        }
        pdfFiles.value = pdfFiles.value + files
        pdfStatus = "✓ ${pdfFiles.value.size} PDF(s) loaded"
    }

    fun pdfReset() {
        pdfFiles.value = emptyList()
        pdfStatus = ""
        pdfProgress = 0
        pdfResults.value = emptyList()
    }

    fun runPdfcm() {
        if (pdfFiles.value.isEmpty()) {
            pdfStatus = "⚠ No files selected"
            return
        }
        pdfProgress = 10
        pdfStatus = "Processing..."
        viewModelScope.launch {
            val results = withContext(Dispatchers.IO) {
                PdfProcessingHelper.compressAndMergePdf(
                    getApplication(),
                    pdfFiles.value.map { it.uri },
                    pdfCompLevel,
                    pdfMerge == "yes"
                ) { p ->
                    pdfProgress = p
                }
            }
            pdfResults.value = results
            pdfStatus = "✓ ${pdfFiles.value.size} PDF(s) processed"
        }
    }


    // ════════════════════════════════════
    // WORKSPACE 2: PDF TO IMAGE
    // ════════════════════════════════════
    var p2iFile by mutableStateOf<SelectedFile?>(null)
    var p2iFormat by mutableStateOf("png")
    var p2iScale by mutableStateOf("2") // 1, 2, 3
    var p2iStatus by mutableStateOf("")
    var p2iProgress by mutableStateOf(0)
    var p2iResultsByFile = mutableStateOf<List<File>>(emptyList())

    fun selectP2iFile(uri: Uri) {
        resolveFileInfo(uri)?.let { info ->
            p2iFile = SelectedFile(uri, info.name, info.size)
            p2iStatus = "✓ ${info.name} loaded"
        }
    }

    fun p2iReset() {
        p2iFile = null
        p2iStatus = ""
        p2iProgress = 0
        p2iResultsByFile.value = emptyList()
    }

    fun runP2i() {
        val fileInfo = p2iFile ?: return
        p2iProgress = 10
        p2iStatus = "Converting..."
        val multiplier = when (p2iScale) {
            "1" -> 1.0f
            "3" -> 3.0f
            else -> 2.0f
        }
        viewModelScope.launch {
            val results = withContext(Dispatchers.IO) {
                PdfProcessingHelper.pdfToImage(
                    getApplication(),
                    fileInfo.uri,
                    p2iFormat,
                    multiplier
                ) { p ->
                    p2iProgress = p
                }
            }
            p2iResultsByFile.value = results
            p2iStatus = "✓ Page(s) converted"
        }
    }


    // ════════════════════════════════════
    // WORKSPACE 3: IMAGES TO PDF
    // ════════════════════════════════════
    var img2pdfFiles = mutableStateOf<List<SelectedFile>>(emptyList())
    var img2pdfPagesize by mutableStateOf("a4")
    var img2pdfOrientation by mutableStateOf("portrait")
    var img2pdfStatus by mutableStateOf("")
    var img2pdfProgress by mutableStateOf(0)
    var img2pdfResultFile by mutableStateOf<File?>(null)

    fun selectImg2pdfFiles(uris: List<Uri>) {
        val files = uris.mapNotNull { uri ->
            resolveFileInfo(uri)?.let { info ->
                SelectedFile(uri, info.name, info.size)
            }
        }
        img2pdfFiles.value = img2pdfFiles.value + files
        img2pdfStatus = "✓ ${img2pdfFiles.value.size} image(s) ready"
    }

    fun img2pdfReset() {
        img2pdfFiles.value = emptyList()
        img2pdfStatus = ""
        img2pdfProgress = 0
        img2pdfResultFile = null
    }

    fun runImg2Pdf() {
        if (img2pdfFiles.value.isEmpty()) {
            img2pdfStatus = "⚠ No images selected"
            return
        }
        img2pdfProgress = 10
        img2pdfStatus = "Building PDF..."
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                PdfProcessingHelper.imagesToPdf(
                    getApplication(),
                    img2pdfFiles.value.map { it.uri },
                    img2pdfPagesize,
                    img2pdfOrientation
                ) { p ->
                    img2pdfProgress = p
                }
            }
            img2pdfResultFile = result
            img2pdfStatus = if (result != null) "✓ PDF built successfully" else "⚠ Failed to build PDF"
        }
    }


    // ════════════════════════════════════
    // WORKSPACE 4: PDF EDITOR (TEXT STAMP)
    // ════════════════════════════════════
    var pdfeSelectedFile by mutableStateOf<SelectedFile?>(null)
    var pdfeText by mutableStateOf("Note")
    var pdfeFontSize by mutableStateOf("12")
    var pdfeColor by mutableStateOf("#1a1a1a")
    var pdfeRotate by mutableStateOf("0")
    var pdfeStamps = mutableStateOf<List<PdfProcessingHelper.TextStamp>>(emptyList())
    var pdfeStatus by mutableStateOf("")
    var pdfeResultFile by mutableStateOf<File?>(null)

    fun selectPdfeFile(uri: Uri) {
        resolveFileInfo(uri)?.let { info ->
            pdfeSelectedFile = SelectedFile(uri, info.name, info.size)
            pdfeStatus = "✓ ${info.name} loaded"
            pdfeStamps.value = emptyList()
            pdfeResultFile = null
        }
    }

    fun pdfeReset() {
        pdfeSelectedFile = null
        pdfeStatus = ""
        pdfeStamps.value = emptyList()
        pdfeResultFile = null
    }

    fun undoLastPdfeStamp() {
        if (pdfeStamps.value.isNotEmpty()) {
            pdfeStamps.value = pdfeStamps.value.dropLast(1)
            pdfeStatus = "↩ Last annotation removed"
        }
    }

    fun buildPdfe() {
        val fileInfo = pdfeSelectedFile ?: return
        pdfeStatus = "Building PDF..."
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                PdfProcessingHelper.exportEditedPdf(
                    getApplication(),
                    fileInfo.uri,
                    pdfeStamps.value,
                    emptyList()
                )
            }
            pdfeResultFile = result
            pdfeStatus = if (result != null) "✓ PDF edited successfully" else "⚠ Stamping failed"
        }
    }


    // ════════════════════════════════════
    // WORKSPACE 5: ADD IMAGE TO PDF
    // ════════════════════════════════════
    var paiPdfFile by mutableStateOf<SelectedFile?>(null)
    var paiImgFile by mutableStateOf<SelectedFile?>(null)
    var paiWidthPill by mutableStateOf(160) // 80, 160, 240, 320
    var paiStamps = mutableStateOf<List<PdfProcessingHelper.ImageStamp>>(emptyList())
    var paiStatus by mutableStateOf("")
    var paiResultFile by mutableStateOf<File?>(null)

    fun selectPaiPdf(uri: Uri) {
        resolveFileInfo(uri)?.let { info ->
            paiPdfFile = SelectedFile(uri, info.name, info.size)
            paiStatus = "✓ PDF Loaded"
            paiStamps.value = emptyList()
            paiResultFile = null
        }
    }

    fun selectPaiImg(uri: Uri) {
        resolveFileInfo(uri)?.let { info ->
            paiImgFile = SelectedFile(uri, info.name, info.size)
            paiStatus = "✓ Image Loaded"
            paiResultFile = null
        }
    }

    fun paiReset() {
        paiPdfFile = null
        paiImgFile = null
        paiStatus = ""
        paiStamps.value = emptyList()
        paiResultFile = null
    }

    fun runPai() {
        val pdfInfo = paiPdfFile ?: return
        paiStatus = "Building PDF..."
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                PdfProcessingHelper.exportEditedPdf(
                    getApplication(),
                    pdfInfo.uri,
                    emptyList(),
                    paiStamps.value
                )
            }
            paiResultFile = result
            paiStatus = if (result != null) "✓ Downloaded!" else "⚠ Stamping failed"
        }
    }


    // ════════════════════════════════════
    // WORKSPACE 6: ZIP MAKER
    // ════════════════════════════════════
    var zipFilesList = mutableStateOf<List<SelectedFile>>(emptyList())
    var zipName by mutableStateOf("cloud-archive")
    var zipPass by mutableStateOf("")
    var zipStatus by mutableStateOf("")
    var zipProgress by mutableStateOf(0)
    var zipResultFile by mutableStateOf<File?>(null)

    fun selectZipFiles(uris: List<Uri>) {
        val files = uris.mapNotNull { uri ->
            resolveFileInfo(uri)?.let { info ->
                SelectedFile(uri, info.name, info.size)
            }
        }
        zipFilesList.value = zipFilesList.value + files
        zipStatus = "✓ ${zipFilesList.value.size} file(s) ready"
    }

    fun zipReset() {
        zipFilesList.value = emptyList()
        zipStatus = ""
        zipProgress = 0
        zipResultFile = null
    }

    fun runZip() {
        if (zipFilesList.value.isEmpty()) {
            zipStatus = "⚠ No files selected"
            return
        }
        zipProgress = 10
        zipStatus = "Creating Zip..."
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) {
                try {
                    val archiveFile = File.createTempFile("cloud_zip_", ".zip", getApplication<Application>().cacheDir)
                    FileOutputStream(archiveFile).use { fos ->
                        java.util.zip.ZipOutputStream(fos).use { zos ->
                            zipFilesList.value.forEachIndexed { index, selected ->
                                val entry = java.util.zip.ZipEntry(selected.name)
                                zos.putNextEntry(entry)
                                getApplication<Application>().contentResolver.openInputStream(selected.uri)?.use { stream ->
                                    stream.copyTo(zos)
                                }
                                zos.closeEntry()
                                zipProgress = 10 + (index * 80 / zipFilesList.value.size)
                            }
                        }
                    }
                    archiveFile
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            zipResultFile = file
            zipProgress = 100
            zipStatus = if (file != null) "✓ Created! ${(file.length()/1024f).toInt()}KB" else "⚠ Compile failed"
        }
    }


    // ════════════════════════════════════
    // WORKSPACE 7: QR CODE GENERATOR v3
    // ════════════════════════════════════
    var qrText by mutableStateOf("")
    var qrSize by mutableStateOf("512")
    var qrEcc by mutableStateOf("Q")
    var qrFg by mutableStateOf("#FFFFFF")
    var qrBgColor by mutableStateOf("#000000")
    var qrLiveResult by mutableStateOf<Bitmap?>(null)
    var qrFinalResult by mutableStateOf<Bitmap?>(null)
    var qrStatus by mutableStateOf("")

    fun triggerQrLivePreview() {
        if (qrText.trim().isEmpty()) {
            qrLiveResult = null
            return
        }
        try {
            qrLiveResult = QrGenerator.generateQrCode(qrText, 200, qrFg, qrBgColor, qrEcc)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun generateQRFinal() {
        if (qrText.trim().isEmpty()) {
            qrStatus = "⚠ Enter text or URL"
            return
        }
        viewModelScope.launch {
            try {
                val size = qrSize.toInt()
                val bitmap = QrGenerator.generateQrCode(qrText, size, qrFg, qrBgColor, qrEcc)
                qrFinalResult = bitmap
                qrStatus = "✓ QR generated!"
                
                val date = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                repository.insertQrHistory(qrText, qrFg, qrBgColor, date)
            } catch (e: Exception) {
                qrStatus = "⚠ Failed to encode"
            }
        }
    }

    fun clearQrHistory() {
        viewModelScope.launch { repository.clearQrHistory() }
    }


    // ════════════════════════════════════
    // WORKSPACE 8: LIFE BY NUMBERS
    // ════════════════════════════════════
    var lifeBirthday by mutableStateOf("")
    var lifeAgeText by mutableStateOf("Age: —")
    var lifeStats = mutableStateOf<List<LifeStatItem>>(emptyList())

    fun calcLifeMetrics() {
        if (lifeBirthday.isEmpty()) return
        try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val born = format.parse(lifeBirthday) ?: return
            val now = Date()
            val msAlive = now.time - born.time
            if (msAlive < 0) return

            val secAlive = msAlive / 1000.0
            val minAlive = secAlive / 60.0
            val hrAlive = minAlive / 60.0
            val daysAlive = floor(hrAlive / 24.0).toInt()

            val years = floor(daysAlive / 365.25).toInt()
            val months = floor((daysAlive % 365.25) / 30.44).toInt()

            lifeAgeText = "Age: ${years}y ${months}m"

            val fmt = { n: Double ->
                when {
                    n >= 1e12 -> String.format(Locale.US, "%.1fT", n / 1e12)
                    n >= 1e9 -> String.format(Locale.US, "%.1fB", n / 1e9)
                    n >= 1e6 -> String.format(Locale.US, "%.1fM", n / 1e6)
                    n >= 1e3 -> String.format(Locale.US, "%.1fK", n / 1e3)
                    else -> String.format(Locale.US, "%,d", n.toLong())
                }
            }

            lifeStats.value = listOf(
                LifeStatItem(fmt(daysAlive.toDouble()), "Days Lived"),
                LifeStatItem(fmt(secAlive * 1.2), "Heartbeats"),
                LifeStatItem(fmt(secAlive * 0.267), "Breaths Taken"),
                LifeStatItem(fmt(secAlive * 4.2), "Eye Blinks"),
                LifeStatItem(fmt(daysAlive * 3.0), "Meals Eaten"),
                LifeStatItem(fmt(hrAlive), "Hours Conscious"),
                LifeStatItem(fmt(secAlive * 0.02), "km Walked (est.)"),
                LifeStatItem(fmt(daysAlive * 2000.0), "Calories Burned")
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    // ════════════════════════════════════
    // WORKSPACE 9: TODAY IN COSMOS
    // ════════════════════════════════════
    var cosmosDate by mutableStateOf("")
    var cosmosApodTitle by mutableStateOf("")
    var cosmosApodUrl by mutableStateOf("")
    var cosmosApodDesc by mutableStateOf("")
    var cosmosApodMediaType by mutableStateOf("")
    var cosmosHistoryEvents = mutableStateOf<List<CosmosEvent>>(emptyList())
    var cosmosAdviceStr by mutableStateOf("")
    var cosmosLoading by mutableStateOf(false)

    fun initCosmosDate() {
        if (cosmosDate.isEmpty()) {
            cosmosDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        }
    }

    fun loadCosmosData() {
        cosmosLoading = true
        initCosmosDate()
        viewModelScope.launch {
            // Load NASA APOD Detail
            try {
                val apiKey = "DEMO_KEY"
                val jsonStr = CloudHttpClient.fetchUrl("https://api.nasa.gov/planetary/apod?api_key=$apiKey&date=$cosmosDate")
                if (jsonStr != null) {
                    val root = JSONObject(jsonStr)
                    cosmosApodTitle = root.optString("title")
                    cosmosApodUrl = root.optString("url")
                    cosmosApodDesc = root.optString("explanation")
                    cosmosApodMediaType = root.optString("media_type")
                } else {
                    cosmosApodTitle = "Space Exploration"
                    cosmosApodUrl = ""
                    cosmosApodDesc = "Could not load today's space picture. You might have reached NASA api rate limits. Try another date."
                    cosmosApodMediaType = "none"
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Load Historical Events (Layered Indian Subcontinent)
            try {
                val cal = Calendar.getInstance()
                val dateParts = cosmosDate.split("-")
                val m = if (dateParts.size == 3) dateParts[1].toInt() else cal.get(Calendar.MONTH) + 1
                val d = if (dateParts.size == 3) dateParts[2].toInt() else cal.get(Calendar.DAY_OF_MONTH)
                val jsonStr = CloudHttpClient.fetchUrl("https://history.muffinlabs.com/date/$m/$d")
                
                if (jsonStr != null) {
                    val root = JSONObject(jsonStr)
                    val dataObj = root.optJSONObject("data")
                    val eventsArr = dataObj?.optJSONArray("Events")
                    val temp = mutableListOf<CosmosEvent>()
                    
                    if (eventsArr != null) {
                        for (i in 0 until eventsArr.length()) {
                            val ev = eventsArr.optJSONObject(i)
                            if (ev != null) {
                                val year = ev.optString("year")
                                val text = ev.optString("text")
                                temp.add(CosmosEvent(year, text))
                            }
                        }
                    }

                    // Strict Subcontinent Filter keywords matching original HTML
                    val indKeywords = listOf("india", "indian", "delhi", "mumbai", "kolkata", "chennai", "gandhi", "nehru", "buddha", "nepal", "ashoka", "mughal", "pakistan")
                    val indFiltered = temp.filter { ev ->
                        indKeywords.any { ev.text.lowercase().contains(it) }
                    }

                    val finalEvents = if (indFiltered.size >= 3) indFiltered else temp
                    cosmosHistoryEvents.value = finalEvents.shuffled().take(3).sortedBy { it.year.toIntOrNull() ?: 0 }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Load Daily Advice Slip
            try {
                val jsonStr = CloudHttpClient.fetchUrl("https://api.adviceslip.com/advice")
                if (jsonStr != null) {
                    val root = JSONObject(jsonStr)
                    val slip = root.optJSONObject("slip")
                    cosmosAdviceStr = slip?.optString("advice") ?: "Grow with serenity."
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            cosmosLoading = false
        }
    }


    // ════════════════════════════════════
    // WORKSPACE 10: RANDOM JOKES
    // ════════════════════════════════════
    var jokesLoading by mutableStateOf(false)
    var jokesLoadedList = mutableStateOf<List<JokeItem>>(emptyList())

    fun loadJokesData() {
        jokesLoading = true
        viewModelScope.launch {
            val temp = mutableListOf<JokeItem>()
            val categories = listOf("Programming", "Misc", "Pun")
            
            categories.forEach { cat ->
                try {
                    val jsonStr = CloudHttpClient.fetchUrl("https://v2.jokeapi.dev/joke/$cat?blacklistFlags=nsfw,explicit&type=twopart,single")
                    if (jsonStr != null) {
                        val root = JSONObject(jsonStr)
                        val emoji = when (cat) {
                            "Programming" -> "💻"
                            "Pun" -> "🥁"
                            else -> "😄"
                        }
                        if (root.optString("type") == "twopart") {
                            val text = "${root.optString("setup")}\n\n**${root.optString("delivery")}**"
                            temp.add(JokeItem(emoji, cat, text))
                        } else {
                            temp.add(JokeItem(emoji, cat, root.optString("joke")))
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (temp.isEmpty()) {
                // offline display fallbacks matching HTML source lists precisely
                temp.add(JokeItem("💻", "Programming", "Why do programmers prefer dark mode?\n\n**Because light attracts bugs!**"))
                temp.add(JokeItem("🥁", "Pun", "I told my wife she was drawing her eyebrows too high.\n\n**She looked surprised.**"))
                temp.add(JokeItem("😄", "Misc", "Why don't scientists trust atoms?\n\n**Because they make up everything.**"))
            }

            jokesLoadedList.value = temp
            jokesLoading = false
        }
    }


    // ════════════════════════════════════
    // WORKSPACE 11: WORD OF THE DAY
    // ════════════════════════════════════
    var wordOfToday by mutableStateOf<WordItem?>(null)

    // Predefined lexical list matching original HTML word database
    private val vocabList = listOf(
        WordItem("Ephemeral", "Lasting for a very short time.", "Greek ephēmeros — lasting a day.", "The beauty of cherry blossoms is ephemeral, fading within a week."),
        WordItem("Serendipity", "The occurrence of events by chance in a happy way.", "Coined by Horace Walpole in 1754 from a Persian fairy tale.", "Finding that old photo was pure serendipity."),
        WordItem("Liminal", "Occupying a transitional or intermediate state.", "Latin limen — threshold.", "The airport felt liminal — a space between where you were and where you're going."),
        WordItem("Petrichor", "A pleasant smell of rain on dry earth.", "Greek petra (stone) + ichor (fluid of the Gods).", "After weeks of drought, the petrichor was intoxicating."),
        WordItem("Sonder", "The realization that each passerby has a life as vivid and complex as your own.", "Coined in the Dictionary of Obscure Sorrows.", "She felt a sudden sonder watching the crowded train platform."),
        WordItem("Hiraeth", "A longing for a home that no longer exists or never was.", "Welsh — untranslatable concept.", "He felt hiraeth for a childhood that felt more golden in memory."),
        WordItem("Vellichor", "The strange wistfulness of used bookshops.", "Dictionary of Obscure Sorrows.", "She was overcome with vellichor browsing the old paperbacks."),
        WordItem("Sanguine", "Optimistic or positive, especially in a difficult situation.", "Latin sanguis — blood.", "She remained sanguine despite the setbacks."),
        WordItem("Penumbra", "The partially shaded outer region of a shadow.", "Latin paene (almost) + umbra (shadow).", "The moon entered the penumbra before the full eclipse began."),
        WordItem("Mellifluous", "Sweet or musical; pleasant to hear.", "Latin mel (honey) + fluere (to flow).", "Her mellifluous voice filled the concert hall.")
    )

    fun loadWordContent() {
        wordOfToday = vocabList.shuffled().first()
    }


    // ════════════════════════════════════
    // WORKSPACE 12: NEPALI LAND CONVERTER
    // ════════════════════════════════════
    var landMode by mutableStateOf("single") // single, area
    var landSingleVal by mutableStateOf("")
    var landSingleFromUnit by mutableStateOf("ropani")

    var landAreaLength by mutableStateOf("")
    var landAreaBreadth by mutableStateOf("")
    var landLengthUnit by mutableStateOf("m")
    var landBreadthUnit by mutableStateOf("m")

    var landStatus by mutableStateOf("")
    var landResults = mutableStateOf<List<Pair<String, Double>>>(emptyList())
    var landTotalSqm by mutableStateOf(0.0)

    private val nepaliMetricsMap = mapOf(
        "ropani" to 508.72,
        "aana" to 31.795,
        "paisa" to 7.948,
        "daam" to 1.987,
        "bigha" to 6772.63,
        "kattha" to 338.63,
        "dhur" to 16.93,
        "sqm" to 1.0,
        "sqft" to 0.092903
    )

    fun navConvertLand() {
        var sqm = 0.0
        val isSingle = landMode == "single"

        if (isSingle) {
            val v = landSingleVal.toDoubleOrNull() ?: 0.0
            if (v <= 0) {
                landStatus = "⚠ Enter a valid values"
                return
            }
            sqm = v * (nepaliMetricsMap[landSingleFromUnit] ?: 1.0)
        } else {
            val len = landAreaLength.toDoubleOrNull() ?: 0.0
            val br = landAreaBreadth.toDoubleOrNull() ?: 0.0
            if (len <= 0 || br <= 0) {
                landStatus = "⚠ Enter valid Length and Breadth"
                return
            }
            val lMult = when (landLengthUnit) {
                "ft" -> 0.3048
                "km" -> 1000.0
                else -> 1.0
            }
            val bMult = when (landBreadthUnit) {
                "ft" -> 0.3048
                "km" -> 1000.0
                else -> 1.0
            }
            sqm = (len * lMult) * (br * bMult)
        }

        landTotalSqm = sqm
        landStatus = "✓ Area: ${String.format(Locale.US, "%.4f", sqm)} m²"

        val temp = mutableListOf<Pair<String, Double>>()
        val units = listOf("ropani", "aana", "paisa", "daam", "bigha", "kattha", "dhur")
        units.forEach { u ->
            val div = nepaliMetricsMap[u] ?: 1.0
            temp.add(Pair(u.replaceFirstChar { it.uppercase() }, sqm / div))
        }
        landResults.value = temp
    }


    // ════════════════════════════════════
    // RESOLVERS & HELPERS
    // ════════════════════════════════════
    private fun resolveFileInfo(uri: Uri): FileMeta? {
        val resolver = getApplication<Application>().contentResolver
        var name = "unknown"
        var size = 0L
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIdx != -1) name = cursor.getString(nameIdx)
                if (sizeIdx != -1) size = cursor.getLong(sizeIdx)
            }
        }
        return FileMeta(name, size)
    }

    private data class FileMeta(val name: String, val size: Long)
}

data class SelectedFile(val uri: Uri, val name: String, val size: Long)
data class LangItem(val code: String, val name: String, val flag: String)
data class LifeStatItem(val num: String, val label: String)
data class CosmosEvent(val year: String, val text: String)
data class JokeItem(val emoji: String, val category: String, val text: String)
data class WordItem(val word: String, val def: String, val origin: String, val example: String)
