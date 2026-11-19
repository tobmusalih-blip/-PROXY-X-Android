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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.google.zxing.integration.android.IntentIntegrator
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.*

private data class Profile(val id: Long, val title: String, val protocol: String, val uri: String)
private val Bg = Color(0xFF05030B)
private val Card = Color(0xFF100B1B)
private val Card2 = Color(0xFF171025)
private val Purple = Color(0xFFB52BFF)
private val Violet = Color(0xFF7420D8)
private val Pink = Color(0xFFE04CFF)
private val White = Color(0xFFF8F4FF)
private val Muted = Color(0xFFAAA0B9)
private val Red = Color(0xFFFF405F)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(5, 3, 11)
        window.navigationBarColor = android.graphics.Color.rgb(5, 3, 11)
        window.decorView.systemUiVisibility = 0
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
    var message by remember { mutableStateOf("") }
    var dns by remember { mutableStateOf("Cloudflare") }
    var route by remember { mutableStateOf("افتراضي") }
    var advanced by remember { mutableStateOf(false) }
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
            "socks5" to "SOCKS5", "wireguard" to "WireGuard", "wg" to "WireGuard")[scheme]
        if (protocol == null || value.length < 12) { message = "الرابط غير معروف أو غير مكتمل"; return }
        val p = Profile(System.currentTimeMillis(), "$protocol • ${profiles.size + 1}", protocol, value)
        profiles = profiles + p
        selectedId = p.id
        selectedProtocol = protocol
        persist(profiles)
        input = ""
        message = "تم حفظ إعداد الخادم"
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { u ->
            runCatching { activity.contentResolver.openInputStream(u)?.bufferedReader()?.use { it.readText() } }
                .onSuccess { if (!it.isNullOrBlank()) addProfile(it) else message = "الملف فارغ" }
                .onFailure { message = "تعذرت قراءة الملف" }
        }
    }
    val qr = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val scanned = IntentIntegrator.parseActivityResult(result.resultCode, result.data)?.contents
        if (!scanned.isNullOrBlank()) addProfile(scanned)
    }
    LaunchedEffect(message) { if (message.isNotBlank()) { snackbar.showSnackbar(message); message = "" } }

    MaterialTheme(colorScheme = darkColorScheme(background = Bg, surface = Card, primary = Purple)) {
        CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
            Scaffold(
                containerColor = Bg,
                snackbarHost = { SnackbarHost(snackbar) },
                bottomBar = {
                    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
                    NavigationBar(containerColor = Color(0xFF080510), tonalElevation = 0.dp) {
                        val names = listOf("الرئيسية", "الإحصائيات", "السجلات", "الحماية")
                        val icons = listOf(Icons.Default.Home, Icons.Default.BarChart, Icons.Default.ReceiptLong, Icons.Default.Security)
                        names.forEachIndexed { i, name ->
                            NavigationBarItem(selected = activeTab == i, onClick = { activeTab = i },
                                icon = { Icon(icons[i], null, tint = if (activeTab == i) Purple else Muted, modifier = Modifier.size(24.dp)) },
                                label = { Text(name, fontSize = 10.sp, color = if (activeTab == i) Purple else Muted) },
                                colors = NavigationBarItemDefaults.colors(indicatorColor = Color(0xFF29133B)))
                        }
                    }
                    }
                }
            ) { pad ->
                LazyColumn(
                    Modifier.fillMaxSize().padding(pad),
                    contentPadding = PaddingValues(bottom = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Column(Modifier.fillMaxWidth().background(Bg)) {
                            CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 17.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { activeTab = 3 }, modifier = Modifier.size(44.dp).clip(RoundedCornerShape(13.dp))
                                    .background(Card).border(1.dp, Color(0xFF38234F), RoundedCornerShape(13.dp))) {
                                    Icon(Icons.Default.Menu, null, tint = White)
                                }
                                Spacer(Modifier.weight(1f))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("PROXY", color = White, fontSize = 27.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                        Text(" X", color = Purple, fontSize = 30.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                    }
                                    Text("X • PRIVATE CONNECTION MANAGER", color = Muted, fontSize = 8.sp, letterSpacing = 1.4.sp)
                                }
                                Spacer(Modifier.weight(1f))
                                Surface(color = Color(0xFF211329), shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFF9C641E))) {
                                    Row(Modifier.padding(horizontal = 10.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.WorkspacePremium, null, tint = Color(0xFFFFC64B), modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("PRO", color = Color(0xFFFFD16A), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                            }
                            Box(Modifier.fillMaxWidth().height(278.dp)) {
                                HackerHeroArt(Modifier.fillMaxSize())
                                CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
                                Column(Modifier.align(Alignment.TopEnd).padding(end = 20.dp, top = 70.dp),
                                    horizontalAlignment = Alignment.End) {
                                    Text("تصفح بحرية وأمان", color = White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                                    Text("اتصال أسرع .. خصوصية أكبر .. بدون قيود", color = Color(0xFFD5C9E3), fontSize = 12.sp)
                                }
                                }
                            }
                            Box(Modifier.fillMaxWidth().height(132.dp), contentAlignment = Alignment.TopCenter) {
                                CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
                                Row(Modifier.fillMaxWidth().padding(horizontal = 17.dp).offset(y = 20.dp),
                                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                    MiniMetric("السرعة الحالية", "-- MB/s", Icons.Default.Bolt, Modifier.weight(1f))
                                    Spacer(Modifier.width(122.dp))
                                    MiniMetric("الحالة", "● غير متصل", Icons.Default.SignalCellularAlt, Modifier.weight(1f), true)
                                }
                                }
                                NeonPowerButton(onClick = {
                                    message = if (profiles.none { it.id == selectedId }) "اختر خادماً أولاً" else
                                        "خدمة VPN لم تُدمج بعد؛ لم يبدأ اتصال حقيقي"
                                }, modifier = Modifier.offset(y = (-18).dp))
                            }
                        }
                    }
                    item {
                        GlowPanel(Modifier.fillMaxWidth().padding(horizontal = 15.dp)) {
                            SectionTitle("اختر البروتوكول", Icons.Default.Layers)
                            Spacer(Modifier.height(12.dp))
                            val protocols = listOf("VLESS", "VMess", "Trojan", "Shadowsocks", "SOCKS5", "WireGuard")
                            val protocolIcons = listOf(Icons.Default.VpnKey, Icons.Default.Send, Icons.Default.Security,
                                Icons.Default.FlightTakeoff, Icons.Default.Hub, Icons.Default.Lan)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                protocols.forEachIndexed { i, p ->
                                    val chosen = selectedProtocol == p
                                    Column(Modifier.weight(1f).height(104.dp).clip(RoundedCornerShape(16.dp))
                                        .background(if (chosen) Brush.verticalGradient(listOf(Color(0xFF47205F), Color(0xFF180D25))) else Brush.verticalGradient(listOf(Color(0xFF100C19), Color(0xFF080610))))
                                        .border(if (chosen) 1.8.dp else 1.dp, if (chosen) Purple else Color(0xFF30233D), RoundedCornerShape(16.dp))
                                        .clickable { selectedProtocol = p }.padding(vertical = 9.dp, horizontal = 1.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                        Icon(protocolIcons[i], null, tint = if (chosen) Color(0xFFD16BFF) else Color(0xFFB7A9CC),
                                            modifier = Modifier.size(29.dp))
                                        Spacer(Modifier.height(9.dp))
                                        Text(p, color = if (chosen) White else Color(0xFFD2C9DE), fontSize = 9.sp,
                                            fontWeight = if (chosen) FontWeight.Bold else FontWeight.Normal, textAlign = TextAlign.Center, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                    item {
                        GlowPanel(Modifier.fillMaxWidth().padding(horizontal = 15.dp)) {
                            SectionTitle("إعدادات السيرفر", Icons.Default.Settings)
                            Spacer(Modifier.height(12.dp))
                            CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
                                OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("أدخل رابط السيرفر هنا...", color = Muted, fontSize = 13.sp) },
                                    leadingIcon = { Icon(Icons.Default.Link, null, tint = Muted) }, minLines = 1, maxLines = 2,
                                    shape = RoundedCornerShape(15.dp),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Purple, unfocusedBorderColor = Color(0xFF382A49),
                                        focusedTextColor = White, unfocusedTextColor = White, cursorColor = Purple))
                            }
                            Spacer(Modifier.height(9.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ActionButton("مسح الكل", Icons.Default.Delete, Modifier.weight(1f)) {
                                    profiles = emptyList(); selectedId = null; persist(profiles); message = "تم مسح الخوادم"
                                }
                                ActionButton("استيراد ملف", Icons.Default.FolderOpen, Modifier.weight(1f)) { picker.launch(arrayOf("*/*")) }
                                ActionButton("مسح QR", Icons.Default.QrCodeScanner, Modifier.weight(1f)) {
                                    qr.launch(IntentIntegrator(activity).setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
                                        .setPrompt("امسح رمز إعداد الخادم").setBeepEnabled(false).createScanIntent())
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { addProfile(input) }, modifier = Modifier.fillMaxWidth().height(42.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF291638), contentColor = White),
                                border = BorderStroke(1.dp, Violet), shape = RoundedCornerShape(13.dp)) {
                                Icon(Icons.Default.AddLink, null, tint = Purple); Spacer(Modifier.width(7.dp)); Text("إضافة السيرفر", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    item {
                        GlowPanel(Modifier.fillMaxWidth().padding(horizontal = 15.dp)) {
                            Row(Modifier.fillMaxWidth().clickable { advanced = !advanced }, verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Public, null, tint = Purple, modifier = Modifier.size(25.dp))
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("توجيه الاتصال", color = White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(route, color = Muted, fontSize = 11.sp)
                                }
                                Text("الخيارات المتقدمة", color = White, fontSize = 12.sp)
                                Icon(if (advanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = Muted)
                            }
                            if (advanced) {
                                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Color(0xFF382A49))
                                SettingLine("DNS", dns) { dns = if (dns == "Cloudflare") "Google" else "Cloudflare" }
                                SettingLine("توجيه التطبيقات", route) { route = if (route == "افتراضي") "كل التطبيقات" else "افتراضي" }
                            }
                        }
                    }
                    item {
                        GlowPanel(Modifier.fillMaxWidth().padding(horizontal = 15.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("رفع", color = Muted, fontSize = 11.sp)
                                    Text("↑  0 KB/s", color = White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("تحميل", color = Muted, fontSize = 11.sp)
                                    Text("↓  0 KB/s", color = White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Spacer(Modifier.width(12.dp))
                                MiniWave(Modifier.weight(1.4f).height(54.dp))
                            }
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 15.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Shortcut("حول التطبيق", Icons.Default.Info, Modifier.weight(1f)) { message = "PROXY X • Private Connection Manager" }
                            Shortcut("الإعدادات", Icons.Default.Settings, Modifier.weight(1f)) { activeTab = 3 }
                            Shortcut("قناة المطور", Icons.Default.Send, Modifier.weight(1f)) { message = "لم يتم إعداد رابط قناة المطور" }
                            Shortcut("متجر التطبيقات", Icons.Default.ShoppingCart, Modifier.weight(1f)) { message = "المتجر غير متاح حالياً" }
                        }
                    }
                    item {
                        GlowPanel(Modifier.fillMaxWidth().padding(horizontal = 15.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.WorkspacePremium, null, tint = Color(0xFFFFC64B))
                                Spacer(Modifier.width(8.dp))
                                Text("اتصال آمن • PROXY X • خصوصيتك أولاً", color = Muted, fontSize = 11.sp)
                            }
                        }
                    }
                    if (activeTab == 1 || activeTab == 2 || activeTab == 3) {
                        item {
                            GlowPanel(Modifier.fillMaxWidth().padding(horizontal = 15.dp)) {
                                Text(when(activeTab) { 1 -> "الإحصائيات"; 2 -> "السجلات"; else -> "إعدادات التطبيق" },
                                    color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                Text(if (activeTab == 1) "ستظهر إحصائيات الاتصال بعد دمج محرك VPN." else if (activeTab == 2) "لا توجد سجلات اتصال بعد." else "إعدادات DNS والتوجيه متاحة من الخيارات المتقدمة.", color = Muted, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HackerHeroArt(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(Brush.verticalGradient(listOf(Color(0xFF0D0318), Color(0xFF05030B), Color(0xFF07030D))))
        drawCircle(Brush.radialGradient(listOf(Color(0xAA8616D2), Color(0x557016B5), Color.Transparent),
            center = Offset(w * .18f, h * .48f), radius = w * .62f), w * .62f, Offset(w * .18f, h * .48f))
        // Dotted world map on the upper-right.
        for (row in 0..19) for (col in 0..45) {
            val nx = col / 45f
            val ny = row / 19f
            val land = (nx < .18f && ny > .22f && ny < .78f) ||
                (nx in .22f.. .39f && ny > .12f && ny < .70f) ||
                (nx in .47f.. .98f && ny > .18f && ny < .72f) ||
                (nx in .38f.. .56f && ny > .68f && ny < .90f)
            if (land && (col * 13 + row * 7) % 4 != 0)
                drawCircle(Color(0x887E2ACD), 1.dp.toPx(), Offset(w * (.43f + nx * .56f), h * (.05f + ny * .56f)))
        }
        // Cloak and broad shoulders.
        val cloak = Path().apply {
            moveTo(-w * .08f, h * 1.02f)
            cubicTo(-w * .03f, h * .74f, w * .02f, h * .51f, w * .12f, h * .40f)
            cubicTo(w * .13f, h * .20f, w * .19f, h * .08f, w * .34f, h * .07f)
            cubicTo(w * .50f, h * .06f, w * .58f, h * .22f, w * .58f, h * .43f)
            cubicTo(w * .72f, h * .56f, w * .75f, h * .78f, w * .78f, h * 1.02f)
            close()
        }
        drawPath(cloak, Brush.linearGradient(listOf(Color(0xFF32104E), Color(0xFF100817), Color(0xFF07040D)),
            start = Offset(0f, h * .10f), end = Offset(w * .72f, h)))
        drawPath(cloak, Color(0xFF8C2BD2).copy(alpha = .72f), style = Stroke(width = 2.1.dp.toPx()))
        // Rounded hood.
        val hood = Path().apply {
            moveTo(w * .13f, h * .46f)
            cubicTo(w * .08f, h * .17f, w * .19f, h * .025f, w * .36f, h * .025f)
            cubicTo(w * .55f, h * .025f, w * .64f, h * .18f, w * .59f, h * .47f)
            cubicTo(w * .55f, h * .65f, w * .46f, h * .75f, w * .36f, h * .79f)
            cubicTo(w * .25f, h * .73f, w * .17f, h * .62f, w * .13f, h * .46f)
            close()
        }
        drawPath(hood, Brush.linearGradient(listOf(Color(0xFF42115F), Color(0xFF100718), Color(0xFF260B3A)),
            start = Offset(w * .12f, h * .08f), end = Offset(w * .62f, h * .72f)))
        drawPath(hood, Color(0xFFB12BFF).copy(alpha = .88f), style = Stroke(width = 3.dp.toPx()))
        drawOval(Brush.radialGradient(listOf(Color(0xFF020106), Color(0xFF08030E), Color(0xFF1B0829)),
            center = Offset(w * .36f, h * .39f), radius = w * .23f),
            topLeft = Offset(w * .19f, h * .19f), size = androidx.compose.ui.geometry.Size(w * .34f, h * .43f))
        // Purple fabric folds and visor.
        drawLine(Color(0xFF8F2BD2).copy(alpha = .65f), Offset(w * .12f, h * .52f), Offset(w * .035f, h * .94f), 2.dp.toPx())
        drawLine(Color(0xFF6F1DAA).copy(alpha = .75f), Offset(w * .52f, h * .52f), Offset(w * .68f, h * .96f), 2.dp.toPx())
        val cx = w * .36f
        val cy = h * .42f
        drawCircle(Color(0x557D12C4), w * .13f, Offset(cx, cy))
        drawLine(Color(0xFFE13BFF), Offset(cx - w * .075f, cy - h * .055f), Offset(cx + w * .075f, cy + h * .055f), 5.dp.toPx(), cap = StrokeCap.Round)
        drawLine(Color(0xFFE13BFF), Offset(cx + w * .075f, cy - h * .055f), Offset(cx - w * .075f, cy + h * .055f), 5.dp.toPx(), cap = StrokeCap.Round)
        drawLine(Brush.horizontalGradient(listOf(Color.Transparent, Violet, Pink, Color.Transparent)),
            Offset(0f, h * .97f), Offset(w, h * .97f), 2.dp.toPx())
    }
}

@Composable
private fun NeonPowerButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.size(178.dp).shadow(22.dp, CircleShape, ambientColor = Purple, spotColor = Purple)
        .background(Brush.radialGradient(listOf(Color(0xFF29113C), Color(0xFF10091B), Bg)), CircleShape)
        .border(5.dp, Brush.sweepGradient(listOf(Purple, Pink, Violet, Purple)), CircleShape)
        .padding(8.dp).border(1.dp, Color(0xFF9D4CE0), CircleShape).clip(CircleShape)
        .clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.PowerSettingsNew, null, tint = White, modifier = Modifier.size(57.dp))
            Text("اتصال", color = White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MiniMetric(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, right: Boolean = false) {
    Column(modifier.height(92.dp).clip(RoundedCornerShape(20.dp))
        .background(Brush.verticalGradient(listOf(Color(0xFF171025), Color(0xFF0B0712))))
        .border(1.dp, Color(0xFF49305F), RoundedCornerShape(20.dp)).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(icon, null, tint = if (right) Color(0xFF28D58A) else Purple, modifier = Modifier.size(22.dp))
        Text(title, color = White, fontSize = 11.sp)
        Text(value, color = if (right) Red else Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun GlowPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.clip(RoundedCornerShape(21.dp))
        .background(Brush.verticalGradient(listOf(Color(0xFF171023), Color(0xFF0B0712))))
        .border(1.dp, Color(0xFF4A315F), RoundedCornerShape(21.dp))
        .drawBehind { drawCircle(Brush.radialGradient(listOf(Color(0x337D22B7), Color.Transparent)), size.width*.25f, Offset(0f,0f)) }
        .padding(14.dp), content = content)
}

@Composable
private fun SectionTitle(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = White, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Icon(icon, null, tint = Purple, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun ActionButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier.clickable(onClick = onClick), color = Color(0xFF1B1427), shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFF49315E))) {
        Column(Modifier.padding(vertical = 11.dp, horizontal = 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = Color(0xFFC59AFF), modifier = Modifier.size(21.dp))
            Spacer(Modifier.height(5.dp))
            Text(label, color = White, fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

@Composable
private fun Shortcut(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(17.dp)).background(Card)
        .border(1.dp, Color(0xFF3C2850), RoundedCornerShape(17.dp)).clickable(onClick=onClick)
        .padding(vertical=13.dp, horizontal=3.dp), horizontalAlignment=Alignment.CenterHorizontally) {
        Icon(icon, null, tint=Purple, modifier=Modifier.size(27.dp))
        Spacer(Modifier.height(7.dp))
        Text(label, color=White, fontSize=9.sp, textAlign=TextAlign.Center, maxLines=1)
    }
}

@Composable
private fun MiniWave(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val p = Path()
        val points = listOf(0f to .78f, .08f to .55f, .14f to .75f, .22f to .48f, .3f to .68f, .39f to .15f,
            .47f to .72f, .56f to .4f, .65f to .7f, .74f to .3f, .82f to .78f, .91f to .62f, 1f to .8f)
        points.forEachIndexed { i, pt ->
            val x=pt.first*size.width; val y=pt.second*size.height
            if (i==0) p.moveTo(x,y) else p.lineTo(x,y)
        }
        drawPath(p, Color(0xFFB52BFF), style=Stroke(2.dp.toPx(), cap=StrokeCap.Round, join=StrokeJoin.Round))
        drawPath(p.apply { lineTo(size.width,size.height); lineTo(0f,size.height); close() },
            Brush.verticalGradient(listOf(Color(0x557D20C5), Color.Transparent)))
    }
}

@Composable
private fun SettingLine(label: String, value: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick=onClick).padding(vertical=11.dp), verticalAlignment=Alignment.CenterVertically) {
        Text(label, color=Muted, fontSize=12.sp, modifier=Modifier.weight(1f))
        Text(value, color=Color(0xFFD0A5FF), fontSize=12.sp)
        Icon(Icons.Default.ChevronLeft, null, tint=Muted)
    }
}
