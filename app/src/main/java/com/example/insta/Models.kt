package com.example.insta

data class Post(
    val id: String = "",
    val uid: String = "",
    val username: String = "",
    val userPhoto: String = "",   // author ki profile photo (base64)
    val image: String = "",       // base64 JPEG
    val caption: String = "",
    val likes: List<String> = emptyList(),
    val commentCount: Long = 0,
    val timestamp: Long = 0
)

data class Comment(
    val id: String = "",
    val uid: String = "",
    val username: String = "",
    val text: String = "",
    val timestamp: Long = 0
)

data class UserProfile(
    val uid: String = "",
    val username: String = "",
    val bio: String = "",
    val photo: String = "",       // base64 JPEG
    val followers: List<String> = emptyList(),
    val following: List<String> = emptyList()
)
