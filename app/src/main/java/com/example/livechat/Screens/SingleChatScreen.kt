package com.example.livechat.Screens

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.livechat.CommonDivider
import com.example.livechat.CommonImage
import com.example.livechat.LCViewModel
import com.example.livechat.data.Message

@Composable
fun SingleChatScreen(navController: NavController, vm: LCViewModel, chatId: String) {
    var reply by rememberSaveable {
        mutableStateOf("")
    }
    val myUser = vm.userData.value
    val currUser = vm.chats.value.first {
        it.chatId == chatId
    }
    val chatUser = if (myUser?.userId == currUser.user1.userId) {
        currUser.user2
    } else {
        currUser.user1
    }
    val onSendReply = {
        vm.onSendReply(chatId, reply)
        reply = ""
    }
    LaunchedEffect(key1 = Unit) {
        vm.populateMessage(chatId)
    }
    BackHandler {
        vm.depopulateMessage()
        navController.popBackStack()
    }
    Column {
        ChatHeader(name = chatUser.name ?: "", imageUrl = chatUser.imageUrl ?: "") {
            navController.popBackStack()
            vm.depopulateMessage()
        }
        MessageBox(
            modifier = Modifier.weight(1f),
            chatMessage = vm.chatMessage.value,
            currentUserId = myUser?.userId ?: ""
        )
        ReplyBox(reply = reply, onReplyChange = { reply = it }, onSendReply = onSendReply)
    }

}

@Composable
fun ReplyBox(reply: String, onReplyChange: (String) -> Unit, onSendReply: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        CommonDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextField(
                value = reply, onValueChange = onReplyChange, maxLines = 3, modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(end = 8.dp)
            )
            Button(onClick = onSendReply) {
                Text(text = "Send")
            }
        }
    }
}

@Composable
fun ChatHeader(
    name: String, imageUrl: String, onBackClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentSize(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.ArrowBack,
            contentDescription = null,
            modifier = Modifier
                .clickable { onBackClicked.invoke() }
                .padding(8.dp)
        )
        CommonImage(
            data = imageUrl,
            modifier = Modifier
                .padding(8.dp)
                .size(50.dp)
                .clip(CircleShape)
        )
        Text(text = name, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
fun MessageBox(modifier: Modifier, chatMessage: List<Message>, currentUserId: String) {

    val listState = rememberLazyListState()

    LaunchedEffect(chatMessage.size) {
        if(chatMessage.size>0){
            listState.animateScrollToItem(chatMessage.size - 1)
        }

    }
    LazyColumn(modifier = modifier,state = listState) {
        items(chatMessage) { msg ->
            val alignment = if (msg.sendBy == currentUserId) Alignment.End else Alignment.Start
            val color = if (msg.sendBy == currentUserId) Color(0xFF68C400) else Color(0xFFC0C0C0)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp), horizontalAlignment = alignment
            ) {
                Text(
                    text = msg.message ?: "",
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(color)
                        .padding(12.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}