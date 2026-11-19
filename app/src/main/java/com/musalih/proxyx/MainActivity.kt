package com.musalih.proxyx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class ProxyProfile(val name: String, val protocol: String, val address: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ProxyXScreen() }
    }
}

@Composable
private fun ProxyXScreen() {
    val bg = Color(0xFF080B14)
    val panel = Color(0xFF121A2A)
    val violet = Color(0xFF8B5CF6)
    val cyan = Color(0xFF22D3EE)
    var raw by remember { mutableStateOf("") }
    var profiles by remember { mutableStateOf(listOf<ProxyProfile>()) }
    var message by remember { mutableStateOf("أضف إعداداتك يدوياً للبدء") }

    MaterialTheme(colorScheme = darkColorScheme(background = bg, surface = panel, primary = violet, secondary = cyan, onBackground = Color.White, onSurface = Color.White)) {
        Column(
            Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF11152A), bg, Color(0xFF080B14))))
                .statusBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("PROXY", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                    Text("X  •  PRIVATE CONNECTION MANAGER", color = cyan, fontSize = 9.sp, letterSpacing = 1.5.sp)
                }
                Surface(color = Color(0xFF1D2440), shape = RoundedCornerShape(14.dp)) {
                    Text("BETA", color = violet, modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp), fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(28.dp))
            Surface(color = panel.copy(alpha = .92f), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("CONNECTION STATUS", color = Color(0xFF94A3B8), fontSize = 11.sp, letterSpacing = 2.sp)
                    Spacer(Modifier.height(14.dp))
                    Box(Modifier.size(104.dp).background(Color(0xFF20213D), RoundedCornerShape(60.dp)), contentAlignment = Alignment.Center) {
                        Text("X", color = violet, fontSize = 48.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("غير متصل", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(message, color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("محرك الاتصال غير مدمج بعد", color = Color(0xFFFBBF24), fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(22.dp))
            Text("إضافة إعداد يدوي", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("الصق رابط البروتوكول أو إعداد الخادم الذي تملكه", color = Color(0xFF94A3B8), fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = raw, onValueChange = { raw = it }, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("vless:// أو vmess:// أو ss:// أو socks5:// ...", fontSize = 12.sp) },
                minLines = 2, maxLines = 3, shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = violet, unfocusedBorderColor = Color(0xFF303B52)))
            Spacer(Modifier.height(10.dp))
            Button(onClick = {
                val value = raw.trim()
                val scheme = value.substringBefore("://", "").lowercase()
                val supported = setOf("http", "https", "socks4", "socks5", "vmess", "vless", "trojan", "ss", "shadowsocks", "wireguard", "wg")
                if (scheme !in supported) message = "رابط غير معروف؛ تحقق من نوع البروتوكول"
                else {
                    profiles = profiles + ProxyProfile("$scheme • ${profiles.size + 1}", scheme.uppercase(), value)
                    raw = ""
                    message = "تمت إضافة الإعداد محلياً"
                }
            }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = violet)) { Text("＋  إضافة الإعداد", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("خوادمي", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("${profiles.size} إعداد", color = cyan, fontSize = 12.sp)
            }
            Spacer(Modifier.height(8.dp))
            if (profiles.isEmpty()) {
                Text("لا توجد خوادم مضافة. لا يحتوي التطبيق على خوادم افتراضية.", color = Color(0xFF64748B), fontSize = 12.sp)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(profiles) { profile ->
                        Surface(color = panel, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(profile.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                                    Text(profile.protocol, color = cyan, fontSize = 10.sp)
                                }
                                TextButton(onClick = { profiles = profiles - profile }) { Text("حذف", color = Color(0xFFF87171)) }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Text("PROXY X  •  Manual configurations only", color = Color(0xFF475569), fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}
