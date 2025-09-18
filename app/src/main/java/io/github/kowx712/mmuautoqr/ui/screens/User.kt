package io.github.kowx712.mmuautoqr.ui.screens

import android.annotation.SuppressLint
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillManager
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.kowx712.mmuautoqr.R
import io.github.kowx712.mmuautoqr.models.User
import io.github.kowx712.mmuautoqr.ui.theme.AutoqrTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Preview(showBackground = true)
@Composable
fun UserScreenPreview() {
    AutoqrTheme {
        val usersData = listOf(
            User("111", "Alice", "111", "123123"),
            User("222", "Bob", "222", "1231231")
        )
        val usersState = remember { mutableStateOf(usersData) }

        UserScreen(
            users = usersState.value,
            onAddUser = { _, _, _ -> delay(1); true },
            onUpdateUser = { _, _, _ -> delay(1); true },
            onDeleteUser = { _ -> delay(1); true },
            onToggleUserStatus = {},
        )
    }
}

@SuppressLint("MutableCollectionMutableState")
@Composable
fun UserScreen(
    users: List<User>,
    onAddUser: suspend (String, String, String) -> Boolean,
    onUpdateUser: suspend (User, String, String) -> Boolean,
    onDeleteUser: suspend (User) -> Boolean,
    onToggleUserStatus: (User) -> Unit,
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var editUser by remember { mutableStateOf<User?>(null) }
    var userToDelete by remember { mutableStateOf<User?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 0.dp)
    ) {
        Text(
            text = stringResource(R.string.user_management),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 128.dp)) {
                items(users, key = { it.id }) { user ->
                    UserRow(
                        user = user,
                        onEdit = { toEdit -> editUser = toEdit },
                        onRequestDelete = { toDelete -> userToDelete = toDelete },
                        onToggle = { toToggle ->
                            onToggleUserStatus(toToggle)
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        }
                    )
                }
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.add_user),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        ShowUserDialog(
            onDismiss = { showAddDialog = false },
            onConfirmAction = { nameArg, userIdArg, passwordArg ->
                scope.launch {
                    if (onAddUser(nameArg, userIdArg, passwordArg)) {
                        showAddDialog = false
                    }
                }
            }
        )
    }

    editUser?.let { userToEdit ->
        ShowUserDialog(
            initial = userToEdit,
            onDismiss = { editUser = null },
            onConfirmAction = { nameArg, _, passwordArg ->
                scope.launch {
                    if (onUpdateUser(userToEdit, nameArg, passwordArg)) {
                        editUser = null
                    }
                }
            }
        )
    }

    userToDelete?.let { user ->
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text(stringResource(R.string.delete_user_title)) },
            text = { Text(stringResource(R.string.delete_user_message, user.name)) },
            confirmButton = {
                FilledTonalButton(onClick = {
                    scope.launch {
                        if (onDeleteUser(user)) {
                            userToDelete = null
                        }
                    }
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
            Text(text = user.name, style = MaterialTheme.typography.titleMedium)
            Text(text = user.userId, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = user.isActive,
            onCheckedChange = { onToggle(user) }
        )
    }
}

@Composable
private fun ShowUserDialog(
    initial: User? = null,
    onDismiss: () -> Unit,
    onConfirmAction: suspend (name: String, userIdForAddOrInitial: String, password: String) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var userIdTextState by remember { mutableStateOf(initial?.userId ?: "") }
    var password by remember { mutableStateOf(initial?.password ?: "") }
    var passwordVisible by remember { mutableStateOf(false) }
    val autofillManager = LocalContext.current.getSystemService(AutofillManager::class.java)
    val dialogScope = rememberCoroutineScope()

    val nameFocusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = {
            autofillManager?.cancel()
            onDismiss()
        },
        title = { Text(if (initial == null) stringResource(R.string.add_new_user) else stringResource(R.string.edit_user)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.full_name)) },
                    modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.PersonFullName }.focusRequester(nameFocusRequester),
                )
                OutlinedTextField(
                    value = userIdTextState,
                    onValueChange = { userIdTextState = it },
                    label = { Text(stringResource(R.string.user_id)) },
                    modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.Username },
                    enabled = initial == null
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.password)) },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.Password },
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        val description = if (passwordVisible) stringResource(R.string.hide_password) else stringResource(R.string.show_password)
                        IconButton(modifier = Modifier.pointerInput(Unit) {}, onClick = { passwordVisible = !passwordVisible }) { Icon(imageVector  = image, description) }
                    }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                autofillManager?.commit()
                if (name.isBlank()) { return@Button }

                dialogScope.launch {
                    val effectiveUserId = initial?.userId ?: userIdTextState.trim()
                    if (initial == null) {
                        if (userIdTextState.isBlank() || userIdTextState.length < 3) { return@launch }
                        if (password.isBlank() || password.length < 4) { return@launch }
                    } else {
                        if (password.isNotBlank() && password.length < 4) { return@launch }
                    }
                    onConfirmAction(name.trim(), effectiveUserId, password.trim())
                }
            }) { Text(if (initial == null) stringResource(R.string.add) else stringResource(R.string.update)) }
        },
        dismissButton = { OutlinedButton(onClick = {
            autofillManager?.cancel()
            onDismiss()
        }) { Text(stringResource(R.string.cancel)) } }
    )
    LaunchedEffect(Unit) {
        delay(100)
        nameFocusRequester.requestFocus()
    }
}
