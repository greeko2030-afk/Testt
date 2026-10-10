package com.example.test

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TestTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RelayApp()
                }
            }
        }
    }

    private fun prepareVpnServer() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            startActivity(intent)
            return
        }
    }

    @Composable
    fun RelayApp() {
        var serverUrl by remember { mutableStateOf("http://10.0.2.2:3000") }
        var selectedRole by remember { mutableStateOf("source") }
        var status by remember { mutableStateOf("Disconnected") }
        var logText by remember { mutableStateOf("Ready.\n") }
        var socket by remember { mutableStateOf<Socket?>(null) }
        var connectedPeer by remember { mutableStateOf("") }

        fun appendLog(text: String) {
            logText += "[$selectedRole] $text\n"
        }

        fun startRelayVpn() {
            prepareVpnServer()
            val vpnIntent = Intent(this@MainActivity, VpnRelayService::class.java)
            vpnIntent.putExtra("server_url", serverUrl)
            vpnIntent.putExtra("role", selectedRole)
            startService(vpnIntent)
            appendLog("VPN service started")
        }

        fun stopRelayVpn() {
            stopService(Intent(this@MainActivity, VpnRelayService::class.java))
            appendLog("VPN service stopped")
            status = "VPN stopped"
        }

        fun connectSocket(role: String) {
            if (socket != null) {
                socket?.disconnect()
            }

            try {
                val options = IO.Options().apply {
                    transports = arrayOf("websocket")
                    reconnection = true
                    timeout = 20000
                }

                socket = IO.socket(serverUrl, options)

                socket?.on(Socket.EVENT_CONNECT) {
                    status = "Connected as $role"
                    appendLog("Connected to ${serverUrl}")
                    if (role == "source") {
                        socket?.emit("registerSource")
                    } else {
                        socket?.emit("registerClient")
                    }
                }

                socket?.on("status") { args ->
                    val payload = args.firstOrNull() as? JSONObject
                    val message = payload?.optString("message") ?: "status update"
                    status = message
                    appendLog(message)
                }

                socket?.on("connectedPeer") { args ->
                    val payload = args.firstOrNull() as? JSONObject
                    val peerId = payload?.optString("peerId") ?: "unknown"
                    val type = payload?.optString("type") ?: "unknown"
                    connectedPeer = "$peerId ($type)"
                    appendLog("Connected to peer: $peerId ($type)")
                }

                socket?.on("packet") { args ->
                    val payload = args.firstOrNull() as? JSONObject
                    val from = payload?.optString("from") ?: "unknown"
                    val data = payload?.optString("data") ?: ""
                    appendLog("Packet from $from: $data")
                }

                socket?.on(Socket.EVENT_DISCONNECT) {
                    appendLog("Disconnected from server")
                    status = "Disconnected"
                }

                socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                    val error = args.firstOrNull()?.toString() ?: "Unknown"
                    appendLog("Connect error: $error")
                    status = "Connection error"
                }

                socket?.connect()
            } catch (e: Exception) {
                status = "Failed to connect"
                appendLog("Error: ${e.message}")
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF2F5F9))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF101827))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Testt Relay VPN",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Production-style relay prototype",
                        color = Color(0xFFCBD5E1),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Text(text = "Server URL")
            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("http://10.0.2.2:3000") }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        selectedRole = "source"
                        startRelayVpn()
                        connectSocket("source")
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Source")
                }

                Button(
                    onClick = {
                        selectedRole = "client"
                        startRelayVpn()
                        connectSocket("client")
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Client")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { startRelayVpn() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Start VPN")
                }

                Button(
                    onClick = { stopRelayVpn() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Stop VPN")
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Role: $selectedRole", fontWeight = FontWeight.SemiBold)
                    Text("Status: $status")
                    if (connectedPeer.isNotEmpty()) {
                        Text("Peer: $connectedPeer")
                    }
                }
            }

            Button(
                onClick = {
                    val target = socket?.id()
                    if (target != null) {
                        socket?.emit(
                            "packet",
                            JSONObject().apply {
                                put("target", target)
                                put("data", "Test packet from $selectedRole")
                            }
                        )
                        appendLog("Sent test packet")
                    } else {
                        appendLog("No active socket")
                    }
                }
            ) {
                Text("Send Test Packet")
            }

            Text(
                text = "Logs",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .verticalScroll(rememberScrollState())
                    .background(Color(0xFF101828), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = logText,
                    color = Color(0xFFE2E8F0)
                )
            }
        }
    }
}

@Composable
fun TestTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
