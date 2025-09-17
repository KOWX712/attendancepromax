package io.github.kowx712.mmuautoqr.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.kowx712.mmuautoqr.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserManager(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private var _cachedUsers: MutableList<User>? = null

    private companion object {
        private const val TAG = "UserManager"
        private const val ENCRYPTION_KEY_PREFS_KEY = "user_manager_encryption_key_v1"
        private const val USERS_LIST_PREFS_KEY = "users_list_encrypted_v1"
    }

    private val encryptionKey: String

    init {
        val storedKey = sharedPreferences.getString(ENCRYPTION_KEY_PREFS_KEY, null)
        if (storedKey != null) {
            this.encryptionKey = storedKey
        } else {
            val generatedKey = EncryptionUtils.generateKey()
            if (generatedKey != null) {
                this.encryptionKey = generatedKey
                sharedPreferences.edit { putString(ENCRYPTION_KEY_PREFS_KEY, generatedKey) }
            } else {
                Log.e(TAG, "Failed to generate encryption key for UserManager. Data security cannot be ensured.")
                throw IllegalStateException("Failed to generate encryption key for UserManager. Data security cannot be ensured.")
            }
        }
    }

    suspend fun getUsers(): List<User> = withContext(Dispatchers.IO) {
        _cachedUsers?.toList()?.let { return@withContext it }

        val encryptedJson = sharedPreferences.getString(USERS_LIST_PREFS_KEY, null)
        val loadedUsers: MutableList<User> = if (encryptedJson != null && encryptedJson.isNotEmpty()) {
            val json = EncryptionUtils.decrypt(encryptedJson, encryptionKey)
            val type = object : TypeToken<MutableList<User>>() {}.type
            try {
                gson.fromJson(json, type) ?: mutableListOf()
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing users JSON from SharedPreferences. Clearing user data.", e)
                clearAllUsers()
                mutableListOf()
            }
        } else {
            mutableListOf()
        }
        _cachedUsers = loadedUsers
        loadedUsers.toList()
    }

    suspend fun getUserCount(): Int = getUsers().size

    suspend fun getActiveUserCount(): Int = getUsers().count { it.isActive }

    private suspend fun saveUsersToPrefs(usersToSave: List<User>) = withContext(Dispatchers.IO) {
        val json = gson.toJson(usersToSave)
        val encryptedJson = EncryptionUtils.encrypt(json, encryptionKey)
        sharedPreferences.edit { putString(USERS_LIST_PREFS_KEY, encryptedJson) }
    }

    suspend fun addUser(name: String, userId: String, password: String): Boolean {
        val currentUsers = _cachedUsers ?: getUsers().toMutableList()
        if (currentUsers.any { it.userId == userId }) {
            return false
        }
        val newUser = User(name = name, userId = userId, password = password)
        currentUsers.add(newUser)
        _cachedUsers = currentUsers
        saveUsersToPrefs(currentUsers)
        return true
    }

    suspend fun updateUser(userIdToUpdate: String, newName: String, newPassword: String): Boolean {
        val currentUsers = _cachedUsers ?: getUsers().toMutableList()
        val userIndex = currentUsers.indexOfFirst { it.userId == userIdToUpdate }
        if (userIndex != -1) {
            val oldUser = currentUsers[userIndex]
            currentUsers[userIndex] = oldUser.copy(name = newName, password = newPassword)
            _cachedUsers = currentUsers
            saveUsersToPrefs(currentUsers)
            return true
        }
        return false
    }

    suspend fun deleteUser(userIdToDelete: String): Boolean {
        val currentUsers = _cachedUsers ?: getUsers().toMutableList()
        val removed = currentUsers.removeAll { it.userId == userIdToDelete }
        if (removed) {
            _cachedUsers = currentUsers
            saveUsersToPrefs(currentUsers)
        }
        return removed
    }

    suspend fun toggleUserStatus(userIdToToggle: String): Boolean {
        val currentUsers = _cachedUsers ?: getUsers().toMutableList()
        val userIndex = currentUsers.indexOfFirst { it.userId == userIdToToggle }
        if (userIndex != -1) {
            val oldUser = currentUsers[userIndex]
            currentUsers[userIndex] = oldUser.copy(isActive = !oldUser.isActive)
            _cachedUsers = currentUsers
            saveUsersToPrefs(currentUsers)
            return true
        }
        return false
    }

    suspend fun clearAllUsers() {
        _cachedUsers = mutableListOf()
        saveUsersToPrefs(emptyList())
    }

    fun refreshCache() {
        _cachedUsers = null
    }
}
