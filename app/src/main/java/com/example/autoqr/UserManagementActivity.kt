package com.example.autoqr

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import com.example.autoqr.models.User
import com.example.autoqr.ui.theme.AutoqrTheme
import com.example.autoqr.utils.UserManager

class UserManagementActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AutoqrTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val barColor = MaterialTheme.colorScheme.background.toArgb()
                    val dark = isSystemInDarkTheme()
                    SideEffect {
                        window.statusBarColor = barColor
                        window.navigationBarColor = barColor
                        val controller = WindowCompat.getInsetsController(window, window.decorView)
                        controller.isAppearanceLightStatusBars = !dark
                        controller.isAppearanceLightNavigationBars = !dark
                    }
                    UserManagementScreen(UserManager(this))
                }
            }
        }
    }
}

@SuppressLint("MutableCollectionMutableState")
@Composable
private fun UserManagementScreen(userManager: UserManager) {
    val context = LocalContext.current
    var users by remember { mutableStateOf(userManager.users) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showClearAllConfirm by remember { mutableStateOf(false) }
    var editUser by remember { mutableStateOf<User?>(null) }
    var userToDelete by remember { mutableStateOf<User?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = stringResource(R.string.user_management),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyColumn(modifier = Modifier.weight(1f).padding(bottom = 16.dp)) {
            items(users) { user ->
                UserRow(
                    user = user,
                    onEdit = { toEdit -> editUser = toEdit },
                    onRequestDelete = { toDelete -> userToDelete = toDelete },
                    onToggle = { toToggle ->
                        userManager.toggleUserStatus(toToggle.userId)
                        users = userManager.users
                    }
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { showAddDialog = true }, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.add_user))
            }
            OutlinedButton(onClick = {
                if (users.isEmpty()) {
                    Toast.makeText(context, R.string.no_users_to_clear, Toast.LENGTH_SHORT).show()
                } else {
                    showClearAllConfirm = true
                }
            }, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.clear_all))
            }
        }
    }

    if (showAddDialog) {
        showUserDialog(onDismiss = { showAddDialog = false }, onConfirmAdd = { name, userId, password ->
            val ok = userManager.addUser(name, userId, password)
            if (ok) {
                users = userManager.users
                showAddDialog = false
            } else {
                Toast.makeText(context, R.string.user_exists, Toast.LENGTH_SHORT).show()
            }
        })
    }

    editUser?.let { toEdit ->
        showUserDialog(
            initial = toEdit,
            onDismiss = { editUser = null },
            onConfirmEdit = { name, password ->
                if (userManager.updateUser(toEdit.userId, name, password)) {
                    users = userManager.users
                } else {
                    Toast.makeText(context, "Failed to update user", Toast.LENGTH_SHORT).show()
                }
                editUser = null
            }
        )
    }

    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            title = { Text(stringResource(R.string.clear_all_users_title)) },
            text = { Text(stringResource(R.string.clear_all_users_message)) },
            confirmButton = {
                FilledTonalButton(onClick = {
                    userManager.clearAllUsers()
                    users = userManager.users
                    showClearAllConfirm = false
                }) { Text(stringResource(R.string.clear_all_users_button)) }
            },
            dismissButton = { OutlinedButton(onClick = { showClearAllConfirm = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    userToDelete?.let { user ->
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text(stringResource(R.string.delete_user_title))},
            text = { Text(stringResource(R.string.delete_user_message, user.name)) },
            confirmButton = {
                FilledTonalButton(onClick = {
                    if (userManager.deleteUser(user.userId)) {
                        users = userManager.users
                    } else {
                        Toast.makeText(context, "Failed to delete user", Toast.LENGTH_SHORT).show()
                    }
                    userToDelete = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { OutlinedButton(onClick = { userToDelete = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
private fun UserRow(
    user: User,
    onEdit: (User) -> Unit,
    onRequestDelete: (User) -> Unit,
    onToggle: (User) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onEdit(user) },
                onLongClick = { onRequestDelete(user) }
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)) {
            Text(text = "${user.name} (${user.userId})", style = MaterialTheme.typography.titleMedium)
        }
        Switch(
            checked = user.isActive,
            onCheckedChange = { onToggle(user) }
        )
    }
}

@Composable
private fun showUserDialog(
    initial: User? = null,
    onDismiss: (() -> Unit)? = null,
    onConfirmAdd: ((String, String, String) -> Unit)? = null,
    onConfirmEdit: ((String, String) -> Unit)? = null,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var userId by remember { mutableStateOf(initial?.userId ?: "") }
    var password by remember { mutableStateOf(initial?.password ?: "") }

    AlertDialog(
        onDismissRequest = { onDismiss?.invoke() },
        title = { Text(if (initial == null) stringResource(R.string.add_new_user) else stringResource(R.string.edit_user)) },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.full_name)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = userId, onValueChange = { userId = it }, label = { Text(stringResource(R.string.user_id)) }, modifier = Modifier.fillMaxWidth(), enabled = initial == null)
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text(stringResource(R.string.password)) }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isBlank()) { return@Button }
                if (initial == null) {
                    if (userId.isBlank() || userId.length < 3) { return@Button }
                    if (password.isBlank() || password.length < 4) { return@Button }
                    onConfirmAdd?.invoke(name.trim(), userId.trim(), password.trim())
                } else {
                    if (password.isBlank() || password.length < 4) { return@Button }
                    onConfirmEdit?.invoke(name.trim(), password.trim())
                    onDismiss?.invoke()
                }
            }) { Text(if (initial == null) stringResource(R.string.add) else stringResource(R.string.update)) }
        },
        dismissButton = { OutlinedButton(onClick = { onDismiss?.invoke() }) { Text(stringResource(R.string.cancel)) } }
    )
}
