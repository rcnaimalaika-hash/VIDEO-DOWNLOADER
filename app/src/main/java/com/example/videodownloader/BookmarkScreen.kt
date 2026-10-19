package com.example.videodownloader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.videodownloader.ui.theme.AppColors

data class BookmarkItem(
    val title: String,
    val quality: String,
    val format: String,
    val bookmarkedOn: String,
    val duration: String,
    val thumbColor: Color
)

private val sampleBookmarks = emptyList<BookmarkItem>()

@Composable
fun BookmarkScreen(
    onBack: () -> Unit = {},
    onItemClick: (BookmarkItem) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }

    val filtered = sampleBookmarks.filter {
        it.title.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background)
    ) {
        // ── Top bar ───────────────────────────────────────────────────────
        Column(modifier = Modifier.fillMaxWidth().background(AppColors.surface)) {
            Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBackIosNew, null, tint = AppColors.textPrimary, modifier = Modifier.size(20.dp))
                }
                Text(
                    text = stringResource(R.string.bookmark_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.textPrimary,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.size(48.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Search bar + sort ─────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f).height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AppColors.surface2)
                    .border(1.dp, AppColors.divider, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Search, null, tint = AppColors.textSecondary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(stringResource(R.string.bookmark_search_hint), color = AppColors.textSecondary, fontSize = 14.sp)
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor    = Color.Transparent,
                        unfocusedContainerColor  = Color.Transparent,
                        focusedIndicatorColor    = Color.Transparent,
                        unfocusedIndicatorColor  = Color.Transparent,
                        disabledIndicatorColor   = Color.Transparent,
                        focusedTextColor         = AppColors.textPrimary,
                        unfocusedTextColor       = AppColors.textPrimary,
                        cursorColor              = AppColors.blue
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                )
            }
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                    .background(AppColors.surface2)
                    .border(1.dp, AppColors.divider, RoundedCornerShape(14.dp))
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.SwapVert, null, tint = AppColors.textSecondary, modifier = Modifier.size(22.dp))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── List / Empty state ────────────────────────────────────
        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔖", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.bookmark_empty_title),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.bookmark_empty_subtitle),
                        fontSize = 13.sp,
                        color = AppColors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered) { item ->
                    BookmarkCard(item = item, bookmarkedOnLabel = stringResource(R.string.bookmark_on), onClick = { onItemClick(item) })
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun BookmarkCard(item: BookmarkItem, bookmarkedOnLabel: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(AppColors.surface)
            .border(1.dp, AppColors.cardBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(width = 110.dp, height = 76.dp)
                .clip(RoundedCornerShape(12.dp)).background(item.thumbColor),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.size(34.dp).clip(CircleShape).background(Color(0x88000000)),
                contentAlignment = Alignment.Center
            ) { Text("▶", fontSize = 14.sp, color = Color.White) }
            Box(
                modifier = Modifier.align(Alignment.BottomStart).padding(6.dp)
                    .clip(RoundedCornerShape(5.dp)).background(Color(0xBB000000))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) { Text(item.duration, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold) }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppColors.textPrimary, maxLines = 2)
            Spacer(modifier = Modifier.height(3.dp))
            Text("${item.quality}  •  ${item.format}", fontSize = 12.sp, color = AppColors.textSecondary)
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Bookmark, null, tint = AppColors.blue, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("$bookmarkedOnLabel\n${item.bookmarkedOn}", fontSize = 11.sp, color = AppColors.textSecondary, lineHeight = 16.sp)
            }
        }
        Spacer(modifier = Modifier.width(4.dp))
        Icon(Icons.Filled.MoreVert, null, tint = AppColors.textSecondary, modifier = Modifier.size(20.dp))
    }
}

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=390dp,height=844dp")
@Composable
fun BookmarkScreenPreview() { BookmarkScreen() }
