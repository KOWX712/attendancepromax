package io.github.kowx712.mmuautoqr.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import io.github.kowx712.mmuautoqr.models.User;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserManager {
    private static final String PREFS_NAME = "AttendanceAppPrefs";
    private static final String KEY_USERS = "users";
    private static final String KEY_ENCRYPTION_KEY = "encryption_key";
    private static final String TAG = "UserManager";

    private final SharedPreferences prefs;
    private final Gson gson;

    public UserManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();

        // Get or generate encryption key
        String encryptionKey = prefs.getString(KEY_ENCRYPTION_KEY, null);
        if (encryptionKey == null) {
            encryptionKey = EncryptionUtils.generateKey();
            if (encryptionKey == null) {
                encryptionKey = "DefaultKey123";
            }
            prefs.edit().putString(KEY_ENCRYPTION_KEY, encryptionKey).apply();
        }
    }

    private void saveUsers(List<User> users) {
        try {
            List<User> usersToSave = new ArrayList<>();
            for (User user : users) {
                User userCopy = new User(user.getId(), user.getName(), user.getUserId(), user.getPassword());
                userCopy.setActive(user.isActive());

                String doubleEncrypted = EncryptionUtils.simpleEncrypt(
                        EncryptionUtils.simpleEncrypt(userCopy.getPassword())
                );
                userCopy.setPassword(doubleEncrypted);
                usersToSave.add(userCopy);
            }

            String json = gson.toJson(usersToSave);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_USERS, json);
            editor.apply();
            editor = null;
        } catch (Exception e) {
            Log.e(TAG, "Error saving users", e);
        }
    }

    public List<User> getUsers() {
        String json = prefs.getString(KEY_USERS, null);
        if (json != null) {
            try {
                Type type = new TypeToken<List<User>>(){}.getType();
                List<User> users = gson.fromJson(json, type);

                if (users == null) {
                    return new ArrayList<>();
                }

                for (User user : users) {
                    if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                        try {
                            String firstDecrypt = EncryptionUtils.simpleDecrypt(user.getPassword());
                            String finalDecrypt = EncryptionUtils.simpleDecrypt(firstDecrypt);
                            user.setPassword(finalDecrypt);
                        } catch (Exception e) {
                            try {
                                String singleDecrypt = EncryptionUtils.simpleDecrypt(user.getPassword());
                                user.setPassword(singleDecrypt);
                            } catch (Exception ex) {
                                Log.e(TAG, "Error decrypting password (inner catch)", ex);
                            }
                        }
                    }
                }
                return users;
            } catch (Exception e) {
                Log.e(TAG, "Error getting users", e);
                return new ArrayList<>();
            }
        }
        return new ArrayList<>();
    }

    public boolean addUser(String name, String userId, String password) {
        List<User> users = getUsers();

        for (User user : users) {
            if (user.getUserId().equals(userId)) {
                return false;
            }
        }

        User newUser = new User(UUID.randomUUID().toString(), name, userId, password);
        users.add(newUser);
        saveUsers(users);
        return true;
    }

    public boolean updateUser(String userId, String name, String password) {
        List<User> users = getUsers();

        for (User user : users) {
            if (user.getUserId().equals(userId)) {
                user.setName(name);
                user.setPassword(password);
                saveUsers(users);
                return true;
            }
        }
        return false;
    }

    public boolean deleteUser(String userId) {
        List<User> users = getUsers();

        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getUserId().equals(userId)) {
                users.remove(i);
                saveUsers(users);
                return true;
            }
        }
        return false;
    }

    public List<User> getActiveUsers() {
        List<User> allUsers = getUsers();
        List<User> activeUsers = new ArrayList<>();

        for (User user : allUsers) {
            if (user.isActive()) {
                activeUsers.add(user);
            }
        }
        return activeUsers;
    }

    public void toggleUserStatus(String userId) {
        List<User> users = getUsers();

        for (User user : users) {
            if (user.getUserId().equals(userId)) {
                user.setActive(!user.isActive());
                saveUsers(users);
                break;
            }
        }
    }

    public void clearAllUsers() {
        prefs.edit().remove(KEY_USERS).apply();
    }

    public int getUserCount() {
        return getUsers().size();
    }

    public int getActiveUserCount() {
        return getActiveUsers().size();
    }
}
