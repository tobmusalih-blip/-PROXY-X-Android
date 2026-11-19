package com.musalih.proxyx

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.google.zxing.integration.android.IntentIntegrator
import org.json.JSONArray
import org.json.JSONObject

private data class Profile(val id: Long, val title: String, val protocol: String, val uri: String)
private val Ink = Color(0xFF050807)
private val Panel = Color(0xFF0C1210)
private val Panel2 = Color(0xFF101A16)
private val Lime = Color(0xFFC7FF35)
private val LimeDim = Color(0xFF83B91F)
private val Cyan = Color(0xFF91E9E1)
private val White = Color(0xFFF4F7F2)
private val Muted = Color(0xFF87918A)
private val Danger = Color(0xFFFF6677)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(5, 8, 7)
        window.navigationBarColor = android.graphics.Color.rgb(5, 8, 7)
        setContent { ProxyXApp(this) }
    }
}

@Composable
private fun ProxyXApp(activity: MainActivity) {
    val prefs = remember { activity.getSharedPreferences("proxyx_profiles", Context.MODE_PRIVATE) }
    var profiles by remember {
        mutableStateOf(runCatching {
            val a = JSONArray(prefs.getString("items", "[]"))
            (0 until a.length()).map { i ->
                val o = a.getJSONObject(i)
                Profile(o.getLong("id"), o.getString("title"), o.getString("protocol"), o.getString("uri"))
            }
        }.getOrDefault(emptyList()))
    }
    var input by remember { mutableStateOf("") }
    var selectedProtocol by remember { mutableStateOf("VLESS") }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var activeTab by remember { mutableIntStateOf(0) }
    var toast by remember { mutableStateOf("") }
    var dns by remember { mutableStateOf("Cloudflare") }
    var route by remember { mutableStateOf("كل الاتصالات") }
    val snackbar = remember { SnackbarHostState() }

    fun persist(list: List<Profile>) {
        val a = JSONArray()
        list.forEach { p -> a.put(JSONObject().put("id", p.id).put("title", p.title).put("protocol", p.protocol).put("uri", p.uri)) }
        prefs.edit().putString("items", a.toString()).apply()
    }
    fun addProfile(raw: String) {
        val value = raw.trim()
        val scheme = value.substringBefore("://", "").lowercase()
        val protocol = mapOf("vless" to "VLESS", "vmess" to "VMess", "trojan" to "Trojan",
            "ss" to "Shadowsocks", "shadowsocks" to "Shadowsocks", "socks" to "SOCKS5",
            "socks5" to "SOCKS5", "http" to "HTTP", "https" to "HTTPS", "wireguard" to "WireGuard", "wg" to "WireGuard")[scheme]
        if (protocol == null || value.length < 12) { toast = "صيغة الرابط غير معروفة أو غير مكتملة"; return }
        val p = Profile(System.currentTimeMillis(), "$protocol • ${profiles.size + 1}", protocol, value)
        profiles = profiles + p
        selectedId = p.id
        selectedProtocol = protocol
        persist(profiles)
        input = ""
        toast = "تم حفظ إعداد الخادم"
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { u ->
            runCatching { activity.contentResolver.openInputStream(u)?.bufferedReader()?.use { it.readText() } }
                .onSuccess { if (!it.isNullOrBlank()) addProfile(it) else toast = "الملف فارغ" }
                .onFailure { toast = "تعذر قراءة الملف" }
        }
    }
    val qr = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val scanned = IntentIntegrator.parseActivityResult(result.resultCode, result.data)?.contents
        if (!scanned.isNullOrBlank()) addProfile(scanned)
    }
    LaunchedEffect(toast) { if (toast.isNotBlank()) { snackbar.showSnackbar(toast); toast = "" } }

    MaterialTheme(colorScheme = darkColorScheme(background = Ink, surface = Panel, primary = Lime)) {
        CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
            Scaffold(
                containerColor = Ink,
                snackbarHost = { SnackbarHost(snackbar) },
                bottomBar = {
                    NavigationBar(containerColor = Color(0xFF080D0B), contentColor = Muted, tonalElevation = 0.dp) {
                        val names = listOf("الرئيسية", "الخوادم", "السجل", "الإعدادات")
                        val icons = listOf(Icons.Default.Home, Icons.Default.Dns, Icons.Default.ReceiptLong, Icons.Default.Settings)
                        names.forEachIndexed { i, name ->
                            NavigationBarItem(
                                selected = activeTab == i,
                                onClick = { activeTab = i },
                                icon = { Icon(icons[i], null, tint = if (activeTab == i) Lime else Muted) },
                                label = { Text(name, fontSize = 10.sp, color = if (activeTab == i) Lime else Muted) },
                                colors = NavigationBarItemDefaults.colors(indicatorColor = Color(0xFF202D14))
                            )
                        }
                    }
                }
            ) { pad ->
                LazyColumn(
                    Modifier.fillMaxSize().padding(pad),
                    contentPadding = PaddingValues(bottom = 26.dp),
                    verticalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    item {
                        Column(
                            Modifier.fillMaxWidth().background(Ink)
                                .drawBehind {
                                    // Subtle technical grid and lime atmospheric glow, matching the approved dark concept.
                                    val step = 34.dp.toPx()
                                    for (x in 0..(size.width / step).toInt()) {
                                        drawLine(Color(0x0DCAFF48), Offset(x * step, 0f), Offset(x * step, size.height), 1f)
                                    }
                                    for (y in 0..(size.height / step).toInt()) {
                                        drawLine(Color(0x0DCAFF48), Offset(0f, y * step), Offset(size.width, y * step), 1f)
                                    }
                                    drawCircle(Brush.radialGradient(listOf(Color(0x333D7020), Color.Transparent)),
                                        size.width * 0.55f, Offset(size.width * 0.82f, size.height * 0.18f))
                                }
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(Lime), contentAlignment = Alignment.Center) {
                                    Text("X", color = Ink, fontSize = 27.sp, fontWeight = FontWeight.Black)
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("PROXY X", color = White, fontSize = 23.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
                                    Text("PRIVATE • FAST • SECURE", color = Lime, fontSize = 8.sp, letterSpacing = 2.sp)
                                }
                                Spacer(Modifier.weight(1f))
                                Surface(color = Color(0xFF172016), shape = CircleShape, border = BorderStroke(1.dp, Color(0xFF536D2C))) {
                                    Row(Modifier.padding(horizontal = 11.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(6.dp).background(Lime, CircleShape))
                                        Spacer(Modifier.width(6.dp))
                                        Text("آمن", color = Lime, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(Modifier.height(22.dp))
                            Text("تحكّم باتصالك", color = White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                            Text("خصوصية أقوى. اتصال تحت سيطرتك.", color = Muted, fontSize = 12.sp)
                            Spacer(Modifier.height(17.dp))
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Box(Modifier.size(218.dp).background(
                                    Brush.radialGradient(listOf(Color(0x334B751B), Color(0x144B751B), Color.Transparent)), CircleShape))
                                Box(Modifier.size(174.dp).border(1.dp, Color(0x445B8C25), CircleShape).padding(9.dp)
                                    .border(2.dp, Lime, CircleShape).padding(8.dp)
                                    .clip(CircleShape)
                                    .background(Brush.radialGradient(listOf(Color(0xFF263719), Color(0xFF10180D), Color(0xFF080D09))))
                                    .border(1.dp, Color(0xFF8EBF39), CircleShape)
                                    .clickable { toast = if (profiles.isEmpty()) "أضف خادماً أولاً" else "محرك VPN قيد الدمج والاختبار؛ لم يبدأ النفق بعد" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.PowerSettingsNew, null, tint = Lime, modifier = Modifier.size(49.dp))
                                        Spacer(Modifier.height(4.dp))
                                        Text("اتصال", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("غير متصل", color = Muted, fontSize = 13.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                            Text("اختر خادماً ثم اضغط للاتصال", color = Muted, fontSize = 10.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            StatTile("PING", "--", "ms", Icons.Default.Speed, Modifier.weight(1f))
                            StatTile("تحميل", "--", "Mbps", Icons.Default.Download, Modifier.weight(1f))
                            StatTile("رفع", "--", "Mbps", Icons.Default.Upload, Modifier.weight(1f))
                        }
                    }
                    item {
                        ConceptPanel(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("الخادم المختار", color = White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text(profiles.firstOrNull { it.id == selectedId }?.title ?: "لم يتم اختيار خادم", color = Muted, fontSize = 12.sp)
                                }
                                Icon(Icons.Default.Dns, null, tint = Lime, modifier = Modifier.size(26.dp))
                            }
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { activeTab = 1 },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(13.dp),
                                border = BorderStroke(1.dp, LimeDim),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Lime)
                            ) { Icon(Icons.Default.Public, null); Spacer(Modifier.width(7.dp)); Text("اختيار خادم") }
                        }
                    }
                    item {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("البروتوكول", color = White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(Modifier.weight(1f))
                                Text("اختر نوع الاتصال", color = Muted, fontSize = 10.sp)
                            }
                            Spacer(Modifier.height(9.dp))
                            val protocols = listOf("VLESS", "VMess", "Shadowsocks", "Trojan", "WireGuard")
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                protocols.forEach { p ->
                                    val selected = selectedProtocol == p
                                    Surface(
                                        Modifier.weight(1f).clickable { selectedProtocol = p },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (selected) Color(0xFF243719) else Panel,
                                        border = BorderStroke(1.dp, if (selected) Lime else Color(0xFF26312A))
                                    ) {
                                        Text(p, color = if (selected) Lime else Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 12.dp, horizontal = 2.dp))
                                    }
                                }
                            }
                        }
                    }
                    item {
                        ConceptPanel(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("إضافة إعداد يدوي", color = White, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Icon(Icons.Default.AddLink, null, tint = Lime)
                            }
                            Spacer(Modifier.height(10.dp))
                            CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
                                OutlinedTextField(
                                    value = input, onValueChange = { input = it }, modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("Paste server link or config", color = Muted, fontSize = 12.sp) },
                                    minLines = 1, maxLines = 3, shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Lime, unfocusedBorderColor = Color(0xFF344132),
                                        focusedTextColor = White, unfocusedTextColor = White, cursorColor = Lime
                                    )
                                )
                            }
                            Spacer(Modifier.height(9.dp))
                            Button(onClick = { addProfile(input) }, modifier = Modifier.fillMaxWidth().height(47.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink),
                                shape = RoundedCornerShape(12.dp)) {
                                Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("إضافة إعداد يدوي", fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MiniAction("استيراد ملف", Icons.Default.FileOpen, Modifier.weight(1f)) { picker.launch(arrayOf("*/*")) }
                                MiniAction("مسح QR", Icons.Default.QrCodeScanner, Modifier.weight(1f)) {
                                    qr.launch(IntentIntegrator(activity).setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
                                        .setPrompt("امسح رمز إعداد الخادم").setBeepEnabled(false).createScanIntent())
                                }
                                MiniAction("لصق", Icons.Default.ContentPaste, Modifier.weight(1f)) {
                                    val cm = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val value = cm.primaryClip?.getItemAt(0)?.coerceToText(activity)?.toString().orEmpty()
                                    if (value.isNotBlank()) input = value else toast = "الحافظة فارغة"
                                }
                            }
                        }
                    }
                    item {
                        ConceptPanel(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            Text("الإعدادات", color = White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            SettingLine("DNS", dns) { dns = if (dns == "Cloudflare") "Google" else "Cloudflare" }
                            HorizontalDivider(color = Color(0xFF26312A))
                            SettingLine("توجيه الاتصال", route) { route = if (route == "كل الاتصالات") "تطبيقات محددة" else "كل الاتصالات" }
                        }
                    }
                    item {
                        ConceptPanel(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("خوادمي", color = White, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Text("${profiles.size} خادم", color = Lime, fontSize = 11.sp)
                            }
                            if (profiles.isEmpty()) Text("لا توجد خوادم محفوظة", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
                            profiles.forEach { p ->
                                Row(Modifier.fillMaxWidth().padding(top = 9.dp).clip(RoundedCornerShape(12.dp))
                                    .background(Panel2).clickable { selectedId = p.id; selectedProtocol = p.protocol }.padding(11.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(p.title, color = White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text(p.protocol, color = Lime, fontSize = 10.sp)
                                    }
                                    if (selectedId == p.id) Icon(Icons.Default.CheckCircle, null, tint = Lime)
                                    IconButton(onClick = {
                                        profiles = profiles.filterNot { it.id == p.id }; persist(profiles)
                                        if (selectedId == p.id) selectedId = null
                                    }) { Icon(Icons.Default.DeleteOutline, null, tint = Muted) }
                                }
                            }
                        }
                    }
                    item {
                        Text("PROXY X  •  PRIVATE CONNECTION", color = Color(0xFF58634F), fontSize = 9.sp,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, unit: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(15.dp)).background(Panel)
        .border(1.dp, Color(0xFF263329), RoundedCornerShape(15.dp)).padding(vertical = 13.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = Lime, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(5.dp))
        Text(label, color = Muted, fontSize = 10.sp)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, color = White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(3.dp))
            Text(unit, color = Muted, fontSize = 9.sp, modifier = Modifier.padding(bottom = 3.dp))
        }
    }
}

@Composable
private fun ConceptPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.clip(RoundedCornerShape(18.dp))
        .background(Brush.verticalGradient(listOf(Color(0xFF121A15), Color(0xFF0A100D))))
        .border(1.dp, Color(0xFF2A382D), RoundedCornerShape(18.dp)).padding(15.dp), content = content)
}

@Composable
private fun MiniAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier.clickable(onClick = onClick), color = Panel2, shape = RoundedCornerShape(11.dp),
        border = BorderStroke(1.dp, Color(0xFF2A382D))) {
        Column(Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = Lime, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, color = Muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun SettingLine(label: String, value: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(value, color = Lime, fontSize = 12.sp)
        Icon(Icons.Default.ChevronLeft, null, tint = Muted)
    }
}
