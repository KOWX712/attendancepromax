package io.github.kowx712.mmuautoqr.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.kowx712.mmuautoqr.utils.UserManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(private val userManager: UserManager) : ViewModel() {
    private val _totalUsers = mutableIntStateOf(0)
    val totalUsers: State<Int> = _totalUsers

    private val _activeUsers = mutableIntStateOf(0)
    val activeUsers: State<Int> = _activeUsers

    fun refreshStats() {
        viewModelScope.launch {
            val currentTotalUsers = withContext(Dispatchers.IO) {
                userManager.getUserCount()
            }
            val currentActiveUsers = withContext(Dispatchers.IO) {
                userManager.getActiveUserCount()
            }
            _totalUsers.intValue = currentTotalUsers
            _activeUsers.intValue = currentActiveUsers
        }
    }

    init {
        refreshStats()
    }
}

class HomeViewModelFactory(private val userManager: UserManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(userManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: \${modelClass.name}")
    }
}
