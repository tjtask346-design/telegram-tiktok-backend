package com.aim.earny.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aim.earny.ui.theme.*

private data class InboxItem(val name: String, val msg: String, val time: String, val unread: Boolean)

private val ITEMS = listOf(
    InboxItem("Mira", "Loved your last video 🔥", "2m", true),
    InboxItem("Arif", "Let's collab?", "1h", true),
    InboxItem("Earny", "You earned $2.40 today 💰", "3h", false),
    InboxItem("Skyline", "New follower", "1d", false),
    InboxItem("Nadia", "Check this trend", "2d", false)
)

@Composable
fun InboxScreen() {
    Column(Modifier.fillMaxSize().background(EarnyBlack).padding(top = 48.dp)) {
        Text(
            "Inbox",
            color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(12.dp))
        LazyColumn(Modifier.fillMaxSize()) {
            items(ITEMS) { item ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(48.dp).background(Gold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(item.name.first().toString(), color = EarnyBlack, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.name, color = TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(item.msg, color = TextWhite60, fontSize = 13.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(item.time, color = TextWhite40, fontSize = 11.sp)
                        if (item.unread) {
                            Spacer(Modifier.height(6.dp))
                            Box(Modifier.size(8.dp).background(HeartRed, CircleShape))
                        }
                    }
                }
            }
        }
    }
}
