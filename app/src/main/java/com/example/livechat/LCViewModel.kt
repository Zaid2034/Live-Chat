package com.example.livechat

import android.icu.util.Calendar
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.example.livechat.data.CHATS
import com.example.livechat.data.ChatData
import com.example.livechat.data.ChatUser
import com.example.livechat.data.MESSAGE
import com.example.livechat.data.Message
import com.example.livechat.data.USER_NODE
import com.example.livechat.data.UserData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Filter
import dagger.hilt.android.lifecycle.HiltViewModel
import java.lang.Exception
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.toObject
import com.google.firebase.firestore.toObjects
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class LCViewModel @Inject constructor(
    val auth: FirebaseAuth, var db: FirebaseFirestore, val storage: FirebaseStorage
) : ViewModel() {

    var inProgress = mutableStateOf(false)
    var inProgressChat = mutableStateOf(false)
    val chats = mutableStateOf<List<ChatData>>(listOf())
    val eventMutableState = mutableStateOf<Event<String>?>(null)
    var signIn = mutableStateOf(false)
    val userData = mutableStateOf<UserData?>(null)
    val chatMessage= mutableStateOf<List<Message>>(listOf())
    val inProgressChatMessage= mutableStateOf(false)
    var currentChatMessageListener:ListenerRegistration?=null

    init {
        val currentUser = auth.currentUser
        signIn.value = currentUser != null
        if (currentUser != null) {
            currentUser?.uid.let {
                Log.d("CUrrent User UID:", "${currentUser.uid}")
                getUserData(it!!)
            }
        }
    }

    fun populateMessage(chatId:String){
        inProgressChatMessage.value=true
        currentChatMessageListener=db.collection(CHATS).document(chatId).collection(MESSAGE).addSnapshotListener { value, error ->
            if(error!=null){
                handleException(error)
            }
            if(value!=null){
                chatMessage.value=value.documents.mapNotNull {
                    it.toObject<Message>()
                }.sortedBy {it.timestamp}
                inProgressChatMessage.value=false
            }
        }
    }
    fun depopulateMessage(){
        chatMessage.value= listOf()
        currentChatMessageListener=null
    }
    fun logIn(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            handleException(customMessage = "Please fill all the field")
        } else {
            inProgress.value = true
            auth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
                if (it.isSuccessful) {
                    signIn.value = true
                    inProgress.value = false
                    auth.currentUser?.uid.let {
                        getUserData(it!!)

                    }
                } else {
                    handleException(it.exception, "Login Failed")
                }
            }
        }
    }

    fun populateChats() {
        Log.d("Current User Id", "${userData.value?.userId}")
        inProgressChat.value = true
        db.collection(CHATS).where(
            Filter.or(
                Filter.equalTo("user1.userId", userData.value?.userId),
                Filter.equalTo("user2.userId", userData.value?.userId)
            )
        ).addSnapshotListener { value, error ->
            if (error != null) {
                handleException(error)
            }
            if (value != null) {
                chats.value = value.documents.mapNotNull {
                    //error is in this document is giving me null
                    Log.d("Map is not Null", "")
                    it.toObject<ChatData>()
                }
                if (value.isEmpty) {
                    Log.d("Value is not null but empty", "${value.documents}")
                } else {
                    Log.d("Value is not empty:", "${value.documents}")
                }

                Log.d("In populateChats2:", "${chats.value.size}")
                inProgressChat.value = false
            }
        }
    }

    fun signUp(name: String, number: String, email: String, password: String) {

        inProgress.value = true
        if (name.isEmpty() or number.isEmpty() or email.isEmpty() or password.isEmpty()) {
            Log.d("Name:", name)
            Log.d("Number:", number)
            Log.d("Email", email)
            Log.d("password", password)
            handleException(customMessage = "Please fill all fields")
            Log.d("returning condition", name)
            return
        }
        inProgress.value = true
        db.collection(USER_NODE).whereEqualTo("number", number).get().addOnSuccessListener {
            Log.d("Before if with the user name Hello Zaid:", name)
            if (it.isEmpty) {
                Log.d("It is empty", name)
                auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener {
                    Log.d("In create User", name)
                    if (it.isSuccessful) {
                        signIn.value = true
                        createOrUpdateProfile(name, number)
                        Log.d("TAG", "Sign up User In")
                    } else {
                        handleException(it.exception, "Sign up Failed")
                    }
                }
            } else {
                Log.d("It is not empty", name)
                handleException(customMessage = "Number already exist")
            }
        }.addOnFailureListener {
            Log.d("In failure Listener", name)

        }

    }

    fun handleException(exception: Exception? = null, customMessage: String = "") {
        Log.e("LiveChatApp", "Live Chat Exception: ", exception)
        exception?.printStackTrace()
        val errorMsg = exception?.localizedMessage ?: ""
        val message = if (customMessage.isNullOrEmpty()) errorMsg else customMessage
        eventMutableState.value = Event(message)
        inProgress.value = false
    }

    fun createOrUpdateProfile(
        name: String? = null,
        number: String? = null,
        imageUrl: String? = null
    ) {
        var uid = auth.currentUser?.uid
        val userData = UserData(
            userId = uid,
            userName = name ?: userData.value?.userName,
            userNumber = number ?: userData.value?.userNumber,
            userImageUrl = imageUrl ?: userData.value?.userImageUrl
        )
        uid?.let {
            inProgress.value = true
            db.collection(USER_NODE).document(uid).get().addOnSuccessListener {
                if (it.exists()) {
                    //update user data
                    inProgress.value = false
                    db.collection(USER_NODE).document(uid).set(userData)
                    getUserData(uid)
                } else {
                    Log.d("User created:", "YES")
                    db.collection(USER_NODE).document(uid).set(userData)
                    inProgress.value = false
                    getUserData(uid)
                }
            }.addOnFailureListener {
                handleException(it, "Cannot Retrive user")
            }
        }
    }

    private fun getUserData(uid: String) {
        inProgress.value = true
        db.collection(USER_NODE).document(uid).addSnapshotListener { value, error ->
            if (error != null) {
                handleException(error, "Cannot Retrive User")
            }
            if (value != null) {
                if (value.exists()) {
                    Log.d("Value exist:", "${value.id}")
                }
                var user = value.toObject<UserData>()
                userData.value = user
                Log.d("User really exist", "${userData?.value?.userId}")
                inProgress.value = false
                populateChats()
            }
        }

    }

    fun uploadProfileImage(uri: Uri) {
        uploadImage(uri) {
            createOrUpdateProfile(imageUrl = it.toString())
        }
    }

    fun uploadImage(uri: Uri, onSuccess: (Uri) -> Unit) {
        inProgress.value = true
        val storageRef = storage.reference
        val uuid = UUID.randomUUID()
        val imageRef = storageRef.child("Images/$uuid")
        val uploadTask = imageRef.putFile(uri)
        uploadTask.addOnSuccessListener {
            val result = it.metadata?.reference?.downloadUrl
            result?.addOnSuccessListener(onSuccess)
            inProgress.value = false
        }.addOnFailureListener {
            handleException(it)
        }
    }

    fun logOut() {
        auth.signOut()
        signIn.value = false
        userData.value = null
        depopulateMessage()
        currentChatMessageListener=null
        eventMutableState.value = Event("Logged Out")

    }

    fun onAddChatVm(number: String) {
        Log.d("In add chat:", number)
        if (number.isEmpty() || !number.isDigitsOnly()) {
            Log.d("Number is empty ", number)
            handleException(customMessage = "Number must be contain digits only")

        } else {
            Log.d("Number is not empty ", number)
            db.collection(CHATS).where(
                Filter.or(
                    Filter.and(
                        Filter.equalTo("user1.number", number),
                        Filter.equalTo("user2.number", userData.value?.userNumber)
                    ),
                    Filter.and(
                        Filter.equalTo("user1.number", userData.value?.userNumber),
                        Filter.equalTo("user2.number", number)
                    )
                )
            ).get().addOnSuccessListener {
                Log.d("In Success ", number)
                if (it.isEmpty) {
                    Log.d("It is empty ", number)
                    db.collection(USER_NODE).whereEqualTo("userNumber", number).get()
                        .addOnSuccessListener {
                            if (it.isEmpty) {
                                Log.d("Number does not exists in USER NODE", number)
                                handleException(customMessage = "Number Not found")
                            } else {
                                Log.d("Number exists in USER NODE", number)
                                val chatPartner = it.toObjects<UserData>()[0]
                                val id = db.collection(CHATS).document().id
                                val chat = ChatData(
                                    chatId = id,
                                    ChatUser(
                                        userData.value?.userId,
                                        name = userData.value?.userName,
                                        imageUrl = userData.value?.userImageUrl,
                                        number = userData.value?.userNumber
                                    ),
                                    ChatUser(
                                        chatPartner.userId,
                                        chatPartner.userName,
                                        chatPartner.userImageUrl,
                                        chatPartner.userNumber

                                    )
                                )
                                db.collection(CHATS).document(id).set(chat)
                            }
                        }.addOnFailureListener {
                            handleException(it)
                        }
                } else {
                    Log.d("It is not empty:", number)
                    handleException(customMessage = "Chat already exists")
                }
            }.addOnFailureListener {
                Log.d("In failure", number)
            }
        }
    }

    fun onSendReply(chatId: String, message: String) {
        val time = Calendar.getInstance().time.toString()
        val msg = Message(userData.value?.userId, message, time)
        db.collection(CHATS).document(chatId).collection(MESSAGE).document().set(msg)

    }
}



