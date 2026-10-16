@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.insta

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val NavSpace = 110.dp   // floating nav ke neeche jagah

// ============================ AUTH ============================
@Composable
fun AuthScreen() {
    var isLogin by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val bg = Brush.verticalGradient(
        if (dark) listOf(Color(0xFF2B1058), Color(0xFF0A0A0F))
        else listOf(Color(0xFFEADFFF), Color(0xFFF4F4FA))
    )

    Box(Modifier.fillMaxSize().background(bg).systemBarsPadding().imePadding(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Wordmark(64.sp)
            Text("Apne moments share karo", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(28.dp))
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shadowElevation = 12.dp
            ) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    OutlinedTextField(
                        email, { email = it }, placeholder = { Text("Email") }, singleLine = true,
                        shape = CircleShape, colors = fieldColors(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!isLogin) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            username, { username = it }, placeholder = { Text("Username") }, singleLine = true,
                            shape = CircleShape, colors = fieldColors(), modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        password, { password = it }, placeholder = { Text("Password (min 6)") }, singleLine = true,
                        shape = CircleShape, colors = fieldColors(),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    error?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(18.dp))
                    GradientButton(
                        text = if (loading) "Wait..." else if (isLogin) "Log in" else "Sign up",
                        enabled = !loading,
                        onClick = {
                            loading = true; error = null
                            scope.launch {
                                try {
                                    if (isLogin) Repo.login(email.trim(), password)
                                    else {
                                        require(username.isNotBlank()) { "Username daalo" }
                                        Repo.signUp(email.trim(), password, username)
                                    }
                                } catch (e: Exception) {
                                    error = e.message
                                } finally {
                                    loading = false
                                }
                            }
                        }
                    )
                }
            }
            TextButton(onClick = { isLogin = !isLogin; error = null }) {
                Text(if (isLogin) "Naya account banao" else "Pehle se account hai? Log in")
            }
        }
    }
}

// ============================ HOME + FLOATING NAV ============================
@Composable
fun HomeScreen(onComments: (String) -> Unit, onUser: (String) -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            when (tab) {
                0 -> Box(Modifier.statusBarsPadding()) { FeedScreen(onComments, onUser) }
                1 -> Box(Modifier.statusBarsPadding()) { SearchScreen(onUser) }
                2 -> Box(Modifier.statusBarsPadding()) { AddPostScreen(onDone = { tab = 0 }) }
                else -> ProfileScreen(Repo.uid ?: "", onBack = null, onOpenPost = onComments)
            }
        }
        FloatingNav(tab, { tab = it }, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
fun FloatingNav(tab: Int, onTab: (Int) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.navigationBarsPadding().padding(start = 28.dp, end = 28.dp, bottom = 14.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 6.dp,
        shadowElevation = 18.dp
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavIcon(tab == 0, { onTab(0) }, if (tab == 0) Icons.Filled.Home else Icons.Outlined.Home, "Home")
            NavIcon(tab == 1, { onTab(1) }, if (tab == 1) Icons.Filled.Search else Icons.Outlined.Search, "Search")
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(BrandBrush).clickable { onTab(2) },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.Add, "Add", tint = Color.White) }
            NavIcon(tab == 3, { onTab(3) }, if (tab == 3) Icons.Filled.Person else Icons.Outlined.Person, "Profile")
        }
    }
}

@Composable
private fun NavIcon(selected: Boolean, onClick: () -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String) {
    IconButton(onClick = onClick) {
        Icon(
            icon, desc,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(28.dp)
        )
    }
}

// ============================ FEED ============================
@Composable
fun FeedScreen(onComments: (String) -> Unit, onUser: (String) -> Unit) {
    val posts by remember { Repo.feed() }.collectAsState(initial = emptyList())
    val people = remember(posts) { posts.distinctBy { it.uid }.take(15) }

    LazyColumn(contentPadding = PaddingValues(bottom = NavSpace)) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) { Wordmark(36.sp) }
        }
        if (people.isNotEmpty()) {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(people, key = { it.uid }) { p ->
                        Column(
                            Modifier.clickable { onUser(p.uid) }.width(70.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            RingAvatar(p.userPhoto, p.username, 68.dp)
                            Spacer(Modifier.height(4.dp))
                            Text(p.username, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        if (posts.isEmpty()) {
            item {
                Box(Modifier.fillParentMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    Text("Abhi koi post nahi.\nPehli post tum daalo!", textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        items(posts, key = { it.id }) { p -> PostCard(p, onComments, onUser) }
    }
}

@Composable
fun PostCard(post: Post, onComments: (String) -> Unit, onUser: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val liked = Repo.uid in post.likes
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(bottom = 10.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().clickable { onUser(post.uid) }.padding(start = 14.dp, top = 12.dp, end = 14.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RingAvatar(post.userPhoto, post.username, 42.dp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(post.username, fontWeight = FontWeight.Bold)
                Text(timeAgo(post.timestamp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Base64Image(
            post.image,
            Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                .clip(RoundedCornerShape(24.dp)).aspectRatio(1f)
                .pointerInput(post.id, liked) {
                    detectTapGestures(onDoubleTap = {
                        if (!liked) scope.launch { runCatching { Repo.toggleLike(post) } }
                    })
                },
            ContentScale.Crop
        )
        Row(Modifier.padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { scope.launch { runCatching { Repo.toggleLike(post) } } }) {
                if (liked) Icon(Icons.Filled.Favorite, "Unlike", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                else Icon(Icons.Outlined.FavoriteBorder, "Like", modifier = Modifier.size(28.dp))
            }
            IconButton(onClick = { onComments(post.id) }) {
                Icon(Icons.Outlined.ChatBubbleOutline, "Comments", modifier = Modifier.size(26.dp))
            }
        }
        Text(
            "${post.likes.size} likes", fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        if (post.caption.isNotBlank()) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(post.username) }
                    append("  ${post.caption}")
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
        }
        if (post.commentCount > 0) {
            Text(
                "${post.commentCount} comments dekho",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { onComments(post.id) }.padding(horizontal = 16.dp, vertical = 2.dp)
            )
        }
    }
}

// ============================ SEARCH ============================
@Composable
fun SearchScreen(onUser: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    LaunchedEffect(query) {
        results = try { Repo.searchUsers(query) } catch (e: Exception) { emptyList() }
    }
    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text("Search", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            query, { query = it }, placeholder = { Text("Username search karo") },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            singleLine = true, shape = CircleShape, colors = fieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = NavSpace)
        ) {
            items(results, key = { it.uid }) { u ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { onUser(u.uid) }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Avatar(u.photo, u.username, 48.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(u.username, fontWeight = FontWeight.Bold)
                        if (u.bio.isNotBlank()) Text(u.bio, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
            }
        }
    }
}

// ============================ ADD POST ============================
@Composable
fun AddPostScreen(onDone: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var image by remember { mutableStateOf("") }
    var caption by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) scope.launch {
            image = withContext(Dispatchers.IO) { uriToBase64(ctx, uri, 1080, 65) } ?: ""
            if (image.isEmpty()) error = "Photo load nahi hui"
        }
    }
    val pick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = NavSpace)
    ) {
        Text("Nayi post", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        if (image.isNotEmpty()) {
            Base64Image(image, Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(28.dp)).clickable { pick() })
        } else {
            Column(
                Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant).clickable { pick() },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Outlined.AddPhotoAlternate, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Text("Photo chuno", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            caption, { caption = it }, placeholder = { Text("Caption likho...") },
            shape = RoundedCornerShape(22.dp), colors = fieldColors(), modifier = Modifier.fillMaxWidth()
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(16.dp))
        GradientButton(
            text = if (busy) "Upload ho raha hai..." else "Share",
            enabled = image.isNotEmpty() && !busy,
            onClick = {
                busy = true; error = null
                scope.launch {
                    try {
                        Repo.createPost(image, caption)
                        image = ""; caption = ""
                        onDone()
                    } catch (e: Exception) {
                        error = e.message
                    } finally {
                        busy = false
                    }
                }
            }
        )
    }
}

// ============================ PROFILE ============================
@Composable
fun ProfileScreen(uid: String, onBack: (() -> Unit)?, onOpenPost: (String) -> Unit) {
    val user by remember(uid) { Repo.user(uid) }.collectAsState(initial = null)
    val posts by remember(uid) { Repo.userPosts(uid) }.collectAsState(initial = emptyList())
    val isMe = uid == Repo.uid
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf(false) }
    val u = user

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {},
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            navigationIcon = {
                if (onBack != null) IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
            },
            actions = {
                if (isMe) IconButton(onClick = { Repo.logout() }) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, "Logout")
                }
            }
        )
        if (u == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = NavSpace),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(
                        Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        RingAvatar(u.photo, u.username, 104.dp)
                        Spacer(Modifier.height(10.dp))
                        Text(u.username, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        if (u.bio.isNotBlank()) {
                            Text(u.bio, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp))
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Stat("Posts", posts.size, Modifier.weight(1f))
                            Stat("Followers", u.followers.size, Modifier.weight(1f))
                            Stat("Following", u.following.size, Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(14.dp))
                        if (isMe) {
                            OutlinedButton({ editing = true }, Modifier.fillMaxWidth().height(48.dp), shape = CircleShape) {
                                Text("Profile edit karo")
                            }
                        } else {
                            val following = Repo.uid in u.followers
                            if (following) OutlinedButton(
                                { scope.launch { runCatching { Repo.toggleFollow(u) } } },
                                Modifier.fillMaxWidth().height(48.dp), shape = CircleShape
                            ) { Text("Following") }
                            else GradientButton("Follow", { scope.launch { runCatching { Repo.toggleFollow(u) } } })
                        }
                    }
                }
                items(posts, key = { it.id }) { p ->
                    Base64Image(
                        p.image,
                        Modifier.aspectRatio(1f).clip(RoundedCornerShape(16.dp)).clickable { onOpenPost(p.id) }
                    )
                }
            }
        }
    }

    if (editing && u != null) EditProfileDialog(u) { editing = false }
}

@Composable
fun Stat(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("$value", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun EditProfileDialog(user: UserProfile, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var bio by remember { mutableStateOf(user.bio) }
    var photo by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) scope.launch {
            photo = withContext(Dispatchers.IO) { uriToBase64(ctx, uri, 300, 70) }
        }
    }
    AlertDialog(
        onDismissRequest = onClose,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Profile edit") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar(photo ?: user.photo, user.username, 84.dp)
                TextButton(onClick = {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Text("Photo badlo") }
                OutlinedTextField(
                    bio, { bio = it }, placeholder = { Text("Bio") },
                    shape = RoundedCornerShape(20.dp), colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = {
                busy = true
                scope.launch {
                    runCatching { Repo.updateProfile(bio, photo) }
                    busy = false
                    onClose()
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancel") } }
    )
}

// ============================ COMMENTS ============================
@Composable
fun CommentsScreen(postId: String, onBack: () -> Unit, onUser: (String) -> Unit) {
    val post by remember(postId) { Repo.post(postId) }.collectAsState(initial = null)
    val comments by remember(postId) { Repo.comments(postId) }.collectAsState(initial = emptyList())
    var text by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Post", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
        },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    text, { text = it }, placeholder = { Text("Comment likho...") },
                    shape = CircleShape, colors = fieldColors(), modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.size(52.dp).clip(CircleShape).background(BrandBrush)
                        .clickable(enabled = text.isNotBlank()) {
                            val t = text
                            text = ""
                            scope.launch { runCatching { Repo.addComment(postId, t) } }
                        },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = Color.White) }
            }
        }
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize()) {
            post?.let { p -> item { PostCard(p, onComments = {}, onUser = onUser) } }
            items(comments, key = { it.id }) { c ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Avatar("", c.username, 36.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(
                        Modifier.clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(c.username, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                            modifier = Modifier.clickable { onUser(c.uid) })
                        Text(c.text)
                    }
                }
            }
        }
    }
}
