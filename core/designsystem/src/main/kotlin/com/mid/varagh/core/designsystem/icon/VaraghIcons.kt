package com.mid.varagh.core.designsystem.icon

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings

/** Central icon set so screens stay consistent. `AutoMirrored` icons flip in RTL. */
object VaraghIcons {
    val Library = Icons.AutoMirrored.Filled.MenuBook
    val LibraryOutlined = Icons.AutoMirrored.Outlined.MenuBook
    val History = Icons.Filled.History
    val HistoryOutlined = Icons.Outlined.History
    val Profile = Icons.Filled.Person
    val ProfileOutlined = Icons.Outlined.Person
    val Social = Icons.Filled.People
    val SocialOutlined = Icons.Outlined.People
    val Settings = Icons.Filled.Settings
    val SettingsOutlined = Icons.Outlined.Settings
    val Add = Icons.Filled.Add
    val Back = Icons.AutoMirrored.Filled.ArrowBack
    val Bookmark = Icons.Filled.Bookmark
    val BookmarkBorder = Icons.Outlined.BookmarkBorder
}
