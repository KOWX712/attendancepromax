package io.github.kowx712.mmuautoqr.viewmodels

import android.annotation.SuppressLint
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.kowx712.mmuautoqr.R
import io.github.kowx712.mmuautoqr.models.User
import io.github.kowx712.mmuautoqr.utils.UserManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class UserOperationFeedback {
    data class Success(val messageResId: Int) : UserOperationFeedback()
    data class Error(val messageResId: Int) : UserOperationFeedback()
}

class UserViewModel(private val userManager: UserManager) : ViewModel() {
    private val _usersList = mutableStateOf<List<User>>(emptyList())
    val usersList: State<List<User>> = _usersList

    @SuppressLint("AutoboxingStateCreation")
    private val _totalUsers = mutableIntStateOf(0)
    private val _activeUsers = mutableIntStateOf(0)

    private val _operationFeedback = MutableSharedFlow<UserOperationFeedback>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val operationFeedback = _operationFeedback.asSharedFlow()


    init {
        viewModelScope.launch {
            loadUsersData()
        }
    }

    private suspend fun loadUsersData() {
        val currentUsers = withContext(Dispatchers.IO) { userManager.getUsers() }
        val currentTotal = withContext(Dispatchers.IO) { userManager.getUserCount() }
        val currentActive = withContext(Dispatchers.IO) { userManager.getActiveUserCount() }
        _usersList.value = currentUsers
        _totalUsers.intValue = currentTotal
        _activeUsers.intValue = currentActive
    }

    suspend fun addUser(name: String, userId: String, password: String): Boolean {
        val operationSuccess = withContext(Dispatchers.IO) {
            userManager.addUser(name, userId, password)
        }
        if (operationSuccess) {
            loadUsersData()
        } else {
            _operationFeedback.emit(UserOperationFeedback.Error(R.string.user_exists))
        }
        return operationSuccess
    }

    suspend fun updateUser(user: User, name: String, password: String): Boolean {
        val operationSuccess = withContext(Dispatchers.IO) {
            userManager.updateUser(user.userId, name, password)
        }
        if (operationSuccess) {
            loadUsersData()
        } else {
            _operationFeedback.emit(UserOperationFeedback.Error(R.string.failed_to_update_user))
        }
        return operationSuccess
    }

    suspend fun deleteUser(user: User): Boolean {
        val operationSuccess = withContext(Dispatchers.IO) {
            userManager.deleteUser(user.userId)
        }
        if (operationSuccess) {
            loadUsersData()
        } else {
            _operationFeedback.emit(UserOperationFeedback.Error(R.string.failed_to_delete_user))
        }
        return operationSuccess
    }

    fun toggleUserStatus(userToToggle: User) {
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                userManager.toggleUserStatus(userToToggle.userId)
            }
            if (success) {
                loadUsersData()
            }
        }
    }

    fun clearAllUsers() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                userManager.clearAllUsers()
            }
            loadUsersData()
        }
    }
}

class UserViewModelFactory(private val userManager: UserManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UserViewModel(userManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: \${modelClass.name}")
    }
}
