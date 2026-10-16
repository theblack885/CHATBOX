package com.example.insta

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object Repo {
    private val auth get() = FirebaseAuth.getInstance()
    private val db get() = FirebaseFirestore.getInstance()

    val uid: String? get() = auth.currentUser?.uid

    fun authState(): Flow<String?> = callbackFlow {
        val l = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(l)
        awaitClose { auth.removeAuthStateListener(l) }
    }

    suspend fun signUp(email: String, pass: String, username: String) {
        val r = auth.createUserWithEmailAndPassword(email, pass).await()
        val id = r.user!!.uid
        db.collection("users").document(id)
            .set(UserProfile(uid = id, username = username.trim().lowercase())).await()
    }

    suspend fun login(email: String, pass: String) {
        auth.signInWithEmailAndPassword(email, pass).await()
    }

    fun logout() = auth.signOut()

    suspend fun currentUser(): UserProfile? =
        db.collection("users").document(uid ?: return null).get().await()
            .toObject(UserProfile::class.java)

    // ---------- Posts ----------
    fun feed(): Flow<List<Post>> = callbackFlow {
        val reg = db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING).limit(50)
            .addSnapshotListener { s, _ ->
                if (s != null) trySend(s.documents.mapNotNull { d ->
                    d.toObject(Post::class.java)?.copy(id = d.id)
                })
            }
        awaitClose { reg.remove() }
    }

    fun userPosts(userId: String): Flow<List<Post>> = callbackFlow {
        val reg = db.collection("posts").whereEqualTo("uid", userId)
            .addSnapshotListener { s, _ ->
                if (s != null) trySend(s.documents.mapNotNull { d ->
                    d.toObject(Post::class.java)?.copy(id = d.id)
                }.sortedByDescending { it.timestamp })
            }
        awaitClose { reg.remove() }
    }

    fun post(id: String): Flow<Post?> = callbackFlow {
        val reg = db.collection("posts").document(id).addSnapshotListener { d, _ ->
            trySend(d?.toObject(Post::class.java)?.copy(id = id))
        }
        awaitClose { reg.remove() }
    }

    suspend fun createPost(image: String, caption: String) {
        val me = currentUser() ?: return
        db.collection("posts").add(
            Post(
                uid = me.uid, username = me.username, userPhoto = me.photo, image = image,
                caption = caption.trim(), timestamp = System.currentTimeMillis()
            )
        ).await()
    }

    suspend fun toggleLike(p: Post) {
        val me = uid ?: return
        db.collection("posts").document(p.id).update(
            "likes",
            if (me in p.likes) FieldValue.arrayRemove(me) else FieldValue.arrayUnion(me)
        ).await()
    }

    // ---------- Comments ----------
    fun comments(postId: String): Flow<List<Comment>> = callbackFlow {
        val reg = db.collection("posts").document(postId).collection("comments")
            .orderBy("timestamp")
            .addSnapshotListener { s, _ ->
                if (s != null) trySend(s.documents.mapNotNull { d ->
                    d.toObject(Comment::class.java)?.copy(id = d.id)
                })
            }
        awaitClose { reg.remove() }
    }

    suspend fun addComment(postId: String, text: String) {
        val me = currentUser() ?: return
        val postRef = db.collection("posts").document(postId)
        postRef.collection("comments").add(
            Comment(uid = me.uid, username = me.username, text = text.trim(),
                timestamp = System.currentTimeMillis())
        ).await()
        postRef.update("commentCount", FieldValue.increment(1)).await()
    }

    // ---------- Users ----------
    fun user(userId: String): Flow<UserProfile?> = callbackFlow {
        val reg = db.collection("users").document(userId).addSnapshotListener { d, _ ->
            trySend(d?.toObject(UserProfile::class.java))
        }
        awaitClose { reg.remove() }
    }

    suspend fun searchUsers(q: String): List<UserProfile> {
        val s = q.trim().lowercase()
        if (s.isEmpty()) return emptyList()
        return db.collection("users").orderBy("username")
            .startAt(s).endAt(s + "\uf8ff").limit(20).get().await()
            .documents.mapNotNull { it.toObject(UserProfile::class.java) }
    }

    suspend fun toggleFollow(target: UserProfile) {
        val me = uid ?: return
        if (me == target.uid) return
        val myRef = db.collection("users").document(me)
        val theirRef = db.collection("users").document(target.uid)
        val following = me in target.followers
        val batch = db.batch()
        if (following) {
            batch.update(myRef, "following", FieldValue.arrayRemove(target.uid))
            batch.update(theirRef, "followers", FieldValue.arrayRemove(me))
        } else {
            batch.update(myRef, "following", FieldValue.arrayUnion(target.uid))
            batch.update(theirRef, "followers", FieldValue.arrayUnion(me))
        }
        batch.commit().await()
    }

    suspend fun updateProfile(bio: String, photo: String?) {
        val me = uid ?: return
        val map = mutableMapOf<String, Any>("bio" to bio.trim())
        if (photo != null) map["photo"] = photo
        db.collection("users").document(me).update(map).await()
    }
}
