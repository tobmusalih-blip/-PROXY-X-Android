package com.musalih.proxyx

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.google.zxing.integration.android.IntentIntegrator
import org.json.JSONArray
import org.json.JSONObject

private data class Profile(val id: Long, val title: String, val protocol: String, val uri: String)
private val Night = Color(0xFF05040B)
private val CardA = Color(0xFF100C20)
private val Purple = Color(0xFF9B32FF)
private val Purple2 = Color(0xFF5D19D8)
private val Cyan = Color(0xFF37DDF5)
private val Soft = Color(0xFFB8B1CC)
private val Muted = Color(0xFF8E879E)
private val Green = Color(0xFF00E58B)
private val Red = Color(0xFFFF4265)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ProxyXApp(this) }
    }
}

@Composable
private fun ProxyXApp(activity: MainActivity) {
    val prefs = remember { activity.getSharedPreferences("proxyx_profiles", Context.MODE_PRIVATE) }
    var profiles by remember {
        mutableStateOf(runCatching {
            val arr = JSONArray(prefs.getString("items", "[]"))
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Profile(o.getLong("id"), o.getString("title"), o.getString("protocol"), o.getString("uri"))
            }
        }.getOrDefault(emptyList()))
    }
    var input by remember { mutableStateOf("") }
    var activeTab by remember { mutableIntStateOf(0) }
    var selectedProtocol by remember { mutableStateOf("VLESS") }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var connected by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("اختر خادماً ثم اضغط اتصال") }
    var showAdvanced by remember { mutableStateOf(false) }
    var dns by remember { mutableStateOf("افتراضي") }
    var route by remember { mutableStateOf("كل الاتصالات") }
    var toast by remember { mutableStateOf("") }

    fun persist(list: List<Profile>) {
        val arr = JSONArray()
        list.forEach { p -> arr.put(JSONObject().put("id", p.id).put("title", p.title).put("protocol", p.protocol).put("uri", p.uri)) }
        prefs.edit().putString("items", arr.toString()).apply()
    }
    fun addProfile(value: String) {
        val clean = value.trim()
        val scheme = clean.substringBefore("://", "").lowercase()
        val map = mapOf("vless" to "VLESS", "vmess" to "VMess", "trojan" to "Trojan", "ss" to "Shadowsocks",
            "shadowsocks" to "Shadowsocks", "socks" to "SOCKS5", "socks5" to "SOCKS5", "socks4" to "SOCKS4",
            "http" to "HTTP", "https" to "HTTPS", "wireguard" to "WireGuard", "wg" to "WireGuard")
        val protocol = map[scheme]
        if (protocol == null || clean.length < 12) { toast = "صيغة الرابط غير معروفة أو غير مكتملة"; return }
        val item = Profile(System.currentTimeMillis(), "$protocol • ${profiles.size + 1}", protocol, clean)
        profiles = profiles + item
        selectedId = item.id
        persist(profiles)
        input = ""
        toast = "تم استيراد الخادم بنجاح"
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { runCatching { activity.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() } }
            .onSuccess { if (!it.isNullOrBlank()) addProfile(it!!) else toast = "الملف فارغ" }
            .onFailure { toast = "تعذر قراءة الملف" } }
    }
    val qrLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val scanned = IntentIntegrator.parseActivityResult(result.resultCode, result.data)?.contents
        if (!scanned.isNullOrBlank()) addProfile(scanned)
    }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(toast) { if (toast.isNotBlank()) { snackbar.showSnackbar(toast); toast = "" } }

    MaterialTheme(colorScheme = darkColorScheme(background = Night, surface = CardA, primary = Purple, secondary = Cyan)) {
        CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
            Scaffold(containerColor = Night, snackbarHost = { SnackbarHost(snackbar) },
                bottomBar = {
                    NavigationBar(containerColor = Color(0xFF090711), contentColor = Soft) {
                        val labels = listOf("الرئيسية", "الإحصائيات", "السجلات", "الحماية")
                        val icons = listOf(Icons.Default.Home, Icons.Default.Equalizer, Icons.Default.ReceiptLong, Icons.Default.Shield)
                        labels.forEachIndexed { i, label ->
                            NavigationBarItem(selected = activeTab == i, onClick = { activeTab = i },
                                icon = { Icon(icons[i], null, tint = if (activeTab == i) Purple else Soft) },
                                label = { Text(label, fontSize = 10.sp, color = if (activeTab == i) Color.White else Soft) },
                                colors = NavigationBarItemDefaults.colors(indicatorColor = Color(0xFF26103D)))
                        }
                    }
                }
            ) { padding ->
                LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    item {
                        Box(Modifier.fillMaxWidth().height(300.dp).background(
                            Brush.verticalGradient(listOf(Color(0xFF10051F), Night, Night)))) {
                            // Stylized purple haze and world-map-inspired hero backdrop.
                            Box(Modifier.align(Alignment.TopStart).offset(x = (-45).dp, y = 45.dp).size(250.dp)
                                .background(Brush.radialGradient(listOf(Color(0x995E19B8), Color.Transparent)), CircleShape))
                            Box(Modifier.align(Alignment.TopEnd).offset(x = 65.dp, y = 20.dp).size(260.dp)
                                .background(Brush.radialGradient(listOf(Color(0x665C20B9), Color.Transparent)), CircleShape))
                            Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { toast = "القائمة الجانبية قيد الإعداد" }) { Icon(Icons.Default.Menu, null, tint = Color.White) }
                                    Spacer(Modifier.weight(1f))
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("PROXY X", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                                        Text("PRIVATE CONNECTION MANAGER", color = Cyan, fontSize = 9.sp, letterSpacing = 2.sp)
                                    }
                                    Spacer(Modifier.weight(1f))
                                    Surface(color = Color(0xFF241508), border = BorderStroke(1.dp, Color(0xFFFFB52E)), shape = RoundedCornerShape(14.dp)) {
                                        Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.WorkspacePremium, null, tint = Color(0xFFFFC34D), modifier = Modifier.size(17.dp))
                                            Text(" PRO", color = Color(0xFFFFC34D), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
                                Row(Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
                                        Text("◢", color = Color(0x553F1688), fontSize = 112.sp, fontWeight = FontWeight.Black)
                                        Text("✕", color = Purple, fontSize = 66.sp, fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(start = 48.dp, top = 18.dp).shadow(20.dp, CircleShape))
                                    }
                                    Column(Modifier.weight(1.2f), horizontalAlignment = Alignment.End) {
                                        Text("تصفح بحرية وأمان", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                        Text("اتصال أسرع .. خصوصية أكبر .. بدون قيود", color = Soft, fontSize = 12.sp, textAlign = TextAlign.End)
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            Row(Modifier.fillMaxWidth().height(166.dp).clip(RoundedCornerShape(24.dp))
                                .background(Brush.horizontalGradient(listOf(Color(0xFF130C24), Color(0xFF090812), Color(0xFF130C24))))
                                .border(1.dp, Color(0x665D2D91), RoundedCornerShape(24.dp)).padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Bolt, null, tint = Purple, modifier = Modifier.size(27.dp))
                                    Text("السرعة الحالية", color = Soft, fontSize = 12.sp)
                                    Text(if (connected) "0.0 MB/s" else "-- MB/s", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                                Box(Modifier.weight(1.25f), contentAlignment = Alignment.Center) {
                                    Box(Modifier.size(138.dp).border(5.dp, Purple, CircleShape).padding(7.dp)
                                        .background(Brush.radialGradient(listOf(Color(0xFF271044), Color(0xFF090711))), CircleShape)
                                        .border(1.dp, Color(0xFFB76AFF), CircleShape), contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.PowerSettingsNew, null, tint = Color.White, modifier = Modifier.size(45.dp))
                                            Text(if (connected) "فصل" else "اتصال", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.SignalCellularAlt, null, tint = Green, modifier = Modifier.size(27.dp))
                                    Text("الحالة", color = Soft, fontSize = 12.sp)
                                    Text(if (connected) "متصل" else "غير متصل", color = if (connected) Green else Red, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            // No false connection action until the native tunnel service is fully wired.
                            Box(Modifier.matchParentSize().padding(start = 0.dp, end = 0.dp)) {
                                Box(Modifier.align(Alignment.Center).size(140.dp).clip(CircleShape).clickable {
                                    if (connected) { connected = false; status = "تم إيقاف الاتصال" }
                                    else if (profiles.isEmpty()) toast = "أضف خادماً أولاً"
                                    else { selectedId = selectedId ?: profiles.first().id; toast = "محرك VPN قيد الدمج والاختبار؛ لم يبدأ النفق بعد" }
                                })
                            }
                        }
                        Text(status, color = Muted, fontSize = 10.sp, modifier = Modifier.fillMaxWidth().padding(top = 3.dp), textAlign = TextAlign.Center)
                    }
                    item {
                        GlowPanel(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("اختر البروتوكول", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Icon(Icons.Default.Layers, null, tint = Purple)
                            }
                            Spacer(Modifier.height(12.dp))
                            val protocols = listOf("VLESS", "VMess", "Trojan", "Shadowsocks", "SOCKS5", "WireGuard", "HTTP", "HTTPS", "SOCKS4")
                            protocols.chunked(3).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    row.forEach { p ->
                                        val chosen = selectedProtocol == p
                                        Surface(Modifier.weight(1f).clickable { selectedProtocol = p },
                                            color = if (chosen) Color(0xFF291047) else Color(0xFF0A0812),
                                            shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, if (chosen) Purple else Color(0xFF29213C)) {
                                            Column(Modifier.padding(vertical = 10.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(protocolIcon(p), fontSize = 20.sp)
                                                Text(p, color = if (chosen) Color.White else Soft, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(7.dp))
                            }
                        }
                    }
                    item {
                        GlowPanel(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("إعدادات السيرفر", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Icon(Icons.Default.Settings, null, tint = Purple)
                            }
                            Spacer(Modifier.height(12.dp))
                            CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
                                OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("أدخل رابط السيرفر هنا...", color = Soft, fontSize = 13.sp) },
                                    minLines = 1, maxLines = 3, shape = RoundedCornerShape(15.dp),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Purple, unfocusedBorderColor = Color(0xFF3B2B56),
                                        focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Cyan))
                            }
                            Spacer(Modifier.height(10.dp))
                            Button(onClick = { addProfile(input) }, modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Purple), shape = RoundedCornerShape(13.dp)) {
                                Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("إضافة السيرفر", fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(9.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SmallAction("مسح QR", Icons.Default.QrCodeScanner, Modifier.weight(1f)) {
                                    qrLauncher.launch(IntentIntegrator(activity).setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
                                        .setPrompt("امسح QR الخاص بإعداد PROXY X").setBeepEnabled(false).createScanIntent())
                                }
                                SmallAction("استيراد ملف", Icons.Default.FolderOpen, Modifier.weight(1f)) { filePicker.launch(arrayOf("*/*")) }
                                SmallAction("لصق", Icons.Default.ContentPaste, Modifier.weight(1f)) {
                                    val cm = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = cm.primaryClip
                                    val text = clip?.getItemAt(0)?.coerceToText(activity)?.toString().orEmpty()
                                    if (text.isNotBlank()) input = text else toast = "الحافظة فارغة"
                                }
                            }
                        }
                    }
                    item {
                        GlowPanel(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("توجيه الاتصال", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Icon(Icons.Default.Public, null, tint = Purple)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("DNS", color = Soft, modifier = Modifier.weight(1f))
                                DropdownSetting(dns, listOf("افتراضي", "Cloudflare", "Google", "Quad9")) { dns = it }
                            }
                            Divider(color = Color(0x332F2350))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("نطاق التوجيه", color = Soft, modifier = Modifier.weight(1f))
                                DropdownSetting(route, listOf("كل الاتصالات", "تطبيقات محددة", "استثناء تطبيقات")) { route = it }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("الخيارات المتقدمة", color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                Switch(checked = showAdvanced, onCheckedChange = { showAdvanced = it }, colors = SwitchDefaults.colors(checkedThumbColor = Purple, checkedTrackColor = Color(0xFF47206D)))
                            }
                            if (showAdvanced) Text("إعدادات MTU وIPv6 وKill Switch ستظهر بعد اكتمال تكامل محرك النفق.", color = Soft, fontSize = 11.sp)
                        }
                    }
                    item {
                        GlowPanel(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("خوادمي", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Text("${profiles.size} خادم", color = Cyan, fontSize = 12.sp)
                            }
                            if (profiles.isEmpty()) Text("لا توجد خوادم مضافة بعد", color = Soft, fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
                            profiles.forEach { p ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 7.dp).clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF0A0812)).clickable { selectedId = p.id; selectedProtocol = p.protocol }.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(p.title, color = Color.White, fontWeight = FontWeight.Bold)
                                        Text(p.protocol, color = Cyan, fontSize = 10.sp)
                                    }
                                    if (selectedId == p.id) Icon(Icons.Default.CheckCircle, null, tint = Green)
                                    IconButton(onClick = { profiles = profiles.filterNot { it.id == p.id }; persist(profiles); if (selectedId == p.id) selectedId = null }) {
                                        Icon(Icons.Default.Delete, null, tint = Red)
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("متجر التطبيقات" to Icons.Default.ShoppingCart, "قناة المطور" to Icons.Default.Send,
                                "الإعدادات" to Icons.Default.Settings, "حول التطبيق" to Icons.Default.Info).forEach { (label, icon) ->
                                Surface(Modifier.weight(1f).clickable { toast = "$label — قيد الإعداد" },
                                    color = CardA, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Color(0x553F2B62))) {
                                    Column(Modifier.padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(icon, null, tint = Purple, modifier = Modifier.size(23.dp))
                                        Text(label, color = Soft, fontSize = 9.sp, textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        }
                    }
                    item { Text("PROXY X • PRIVATE CONNECTION MANAGER", color = Color(0xFF55466C), fontSize = 9.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                }
            }
        }
    }
}

@Composable
private fun GlowPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.clip(RoundedCornerShape(22.dp)).background(Brush.verticalGradient(listOf(Color(0xFF171027), Color(0xFF090711))))
        .border(1.dp, Color(0x664F2A80), RoundedCornerShape(22.dp)).padding(15.dp), content = content)
}

@Composable
private fun SmallAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier.clickable(onClick = onClick), color = Color(0xFF151023), shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0x553C2C5B))) {
        Column(Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = Soft, modifier = Modifier.size(19.dp))
            Text(label, color = Soft, fontSize = 10.sp)
        }
    }
}

@Composable
private fun DropdownSetting(value: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) { Text(value, color = Cyan, fontSize = 12.sp); Icon(Icons.Default.ExpandMore, null, tint = Cyan) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false }) }
        }
    }
}

private fun protocolIcon(p: String) = when (p) {
    "VLESS" -> "◈"; "VMess" -> "➤"; "Trojan" -> "♞"; "Shadowsocks" -> "➤"
    "SOCKS5", "SOCKS4" -> "⌘"; "WireGuard" -> "◎"; "HTTP", "HTTPS" -> "↗"; else -> "◇"
}
