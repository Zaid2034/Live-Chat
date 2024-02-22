package com.example.livechat

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage


@Module
@InstallIn(ViewModelComponent::class)
class HiltModule {
    @Provides
    fun provideAuthentication():FirebaseAuth=Firebase.auth

    @Provides
    fun provideFirestore():FirebaseFirestore=Firebase.firestore

    @Provides
    fun provideFirestorage():FirebaseStorage=Firebase.storage


}