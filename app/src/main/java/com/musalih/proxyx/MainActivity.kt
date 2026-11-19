package com.musalih.proxyx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection

private data class ProxyProfile(val name: String, val protocol: String, val address: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ProxyXScreen() }
    }
}

private val Bg = Color(0xFF080B12)
private val Panel = Color(0xFF111827)
private val Panel2 = Color(0xFF172235)
private val Accent = Color(0xFF7C5CFC)
private val Mint = Color(0xFF38D6C5)
private val Muted = Color(0xFF91A0B7)

@Composable
private fun ProxyXScreen() {
    var raw by remember { mutableStateOf("") }
    var profiles by remember { mutableStateOf(listOf<ProxyProfile>()) }
    var message by remember { mutableStateOf("أضف خادمك للبدء") }
    var error by remember { mutableStateOf(false) }

    MaterialTheme(colorScheme = darkColorScheme(
        background = Bg, surface = Panel, primary = Accent,
        secondary = Mint, onBackground = Color.White, onSurface = Color.White
    )) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Box(
                Modifier.fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color(0xFF12182B), Bg, Bg)))
                    .statusBarsPadding()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("PROXY X", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp)
                                Text("اتصالك، بإعداداتك", color = Muted, fontSize = 12.sp)
                            }
                            Surface(color = Color(0xFF201B3C), shape = RoundedCornerShape(50)) {
                                Text("BETA", color = Color(0xFFB5A3FF), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                            }
                        }
                    }
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Panel)
                        ) {
                            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = Mint, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("حالة الاتصال", color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Spacer(Modifier.weight(1f))
                                    Surface(color = Color(0xFF30202A), shape = RoundedCornerShape(50)) {
                                        Text("غير متصل", color = Color(0xFFFF8C9B), fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp))
                                    }
                                }
                                Spacer(Modifier.height(22.dp))
                                Box(
                                    Modifier.size(76.dp).background(Color(0xFF242044), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.WifiOff, contentDescription = null, tint = Color(0xFFAD9AFF), modifier = Modifier.size(34.dp))
                                }
                                Spacer(Modifier.height(14.dp))
                                Text("لا يوجد اتصال نشط", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(5.dp))
                                Text(message, color = if (error) Color(0xFFFF9AA8) else Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
                                Spacer(Modifier.height(14.dp))
                                Surface(color = Color(0xFF30291B), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                                    Text("محرك الاتصال قيد التطوير — هذه النسخة لا تنشئ نفق VPN بعد",
                                        color = Color(0xFFFFD27A), fontSize = 11.sp, textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp))
                                }
                            }
                        }
                    }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("إضافة خادم", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                            Text("أدخل رابط إعداد الخادم يدوياً", color = Muted, fontSize = 12.sp)
                        }
                    }
                    item {
                        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    OutlinedTextField(
                                        value = raw,
                                        onValueChange = { raw = it; error = false },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text("vless://  vmess://  ss://  socks5://", fontSize = 12.sp, color = Muted) },
                                        minLines = 2,
                                        maxLines = 4,
                                        shape = RoundedCornerShape(14.dp),
                                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Accent,
                                            unfocusedBorderColor = Color(0xFF344158),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            cursorColor = Mint
                                        )
                                    )
                                }
                                Button(
                                    onClick = {
                                        val value = raw.trim()
                                        val scheme = value.substringBefore("://", "").lowercase()
                                        val supported = setOf("http", "https", "socks4", "socks5", "vmess", "vless", "trojan", "ss", "shadowsocks", "wireguard", "wg")
                                        if (scheme !in supported || value.substringAfter("://", "").isBlank()) {
                                            message = "الرابط غير صالح أو البروتوكول غير مدعوم"
                                            error = true
                                        } else {
                                            profiles = profiles + ProxyProfile("خادم ${profiles.size + 1}", scheme.uppercase(), value)
                                            raw = ""
                                            message = "تمت إضافة الإعداد إلى قائمتك"
                                            error = false
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Accent)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("إضافة إلى خوادمي", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("خوادمي", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                                Text("إعداداتك المستوردة يدوياً", color = Muted, fontSize = 11.sp)
                            }
                            Surface(color = Panel2, shape = RoundedCornerShape(50)) {
                                Text("${profiles.size} خادم", color = Mint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp))
                            }
                        }
                    }
                    if (profiles.isEmpty()) {
                        item {
                            Surface(color = Panel.copy(alpha = .7f), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Router, contentDescription = null, tint = Color(0xFF65748C), modifier = Modifier.size(30.dp))
                                    Spacer(Modifier.height(9.dp))
                                    Text("قائمتك فارغة حالياً", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("أضف رابط خادم ليظهر هنا", color = Muted, fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        items(profiles) { profile ->
                            Surface(color = Panel, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(40.dp).background(Color(0xFF242044), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Router, contentDescription = null, tint = Color(0xFFB5A3FF))
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(profile.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Text(profile.protocol, color = Mint, fontSize = 11.sp)
                                    }
                                    IconButton(onClick = { profiles = profiles - profile }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = Color(0xFFFF8292))
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Text("PROXY X  •  إعدادات يدوية فقط", color = Color(0xFF59677D), fontSize = 10.sp,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}
