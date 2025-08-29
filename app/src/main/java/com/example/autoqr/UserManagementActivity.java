package com.example.autoqr;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.autoqr.adapters.UserAdapter;
import com.example.autoqr.models.User;
import com.example.autoqr.utils.UserManager;

import java.util.List;

public class UserManagementActivity extends AppCompatActivity {

    private ListView listViewUsers;
    private Button btnAddUser, btnClearAll;
    private UserAdapter userAdapter;
    private UserManager userManager;
    private List<User> userList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_management);

        initializeViews();
        initializeUserManager();
        setupClickListeners();
        loadUsers();
    }

    private void initializeViews() {
        listViewUsers = findViewById(R.id.listViewUsers);
        btnAddUser = findViewById(R.id.btnAddUser);
        btnClearAll = findViewById(R.id.btnClearAll);
    }

    private void initializeUserManager() {
        userManager = new UserManager(this);
    }

    private void setupClickListeners() {
        btnAddUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddUserDialog();
            }
        });

        btnClearAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showClearAllDialog();
            }
        });
    }

    private void loadUsers() {
        userList = userManager.getUsers();
        userAdapter = new UserAdapter(this, userList, new UserAdapter.UserActionListener() {
            @Override
            public void onEditUser(User user) {
                showEditUserDialog(user);
            }

            @Override
            public void onDeleteUser(User user) {
                showDeleteUserDialog(user);
            }

            @Override
            public void onToggleUserStatus(User user) {
                userManager.toggleUserStatus(user.getUserId());
                loadUsers();
                Toast.makeText(UserManagementActivity.this,
                        user.getName() + " status updated",
                        Toast.LENGTH_SHORT).show();
            }
        });
        listViewUsers.setAdapter(userAdapter);
    }

    private void showAddUserDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_user, null);

        EditText etName = dialogView.findViewById(R.id.etName);
        EditText etUserId = dialogView.findViewById(R.id.etUserId);
        EditText etPassword = dialogView.findViewById(R.id.etPassword);

        builder.setView(dialogView)
                .setTitle("Add New User")
                .setPositiveButton("Add", null)
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String userId = etUserId.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (validateInput(name, userId, password)) {
                if (userManager.addUser(name, userId, password)) {
                    Toast.makeText(this, "User added successfully", Toast.LENGTH_SHORT).show();
                    loadUsers();
                    dialog.dismiss();
                } else {
                    Toast.makeText(this, "User ID already exists", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showEditUserDialog(User user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_user, null);

        EditText etName = dialogView.findViewById(R.id.etName);
        EditText etUserId = dialogView.findViewById(R.id.etUserId);
        EditText etPassword = dialogView.findViewById(R.id.etPassword);

        etName.setText(user.getName());
        etUserId.setText(user.getUserId());
        etUserId.setEnabled(true);
        etPassword.setText(user.getPassword());

        builder.setView(dialogView)
                .setTitle("Edit User")
                .setPositiveButton("Update", null)
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (validateInput(name, user.getUserId(), password)) {
                if (userManager.updateUser(user.getUserId(), name, password)) {
                    Toast.makeText(this, "User updated successfully", Toast.LENGTH_SHORT).show();
                    loadUsers();
                    dialog.dismiss();
                } else {
                    Toast.makeText(this, "Failed to update user", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showDeleteUserDialog(User user) {
        new AlertDialog.Builder(this)
                .setTitle("Delete User")
                .setMessage("Are you sure you want to delete " + user.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (userManager.deleteUser(user.getUserId())) {
                        Toast.makeText(this, "User deleted successfully", Toast.LENGTH_SHORT).show();
                        loadUsers();
                    } else {
                        Toast.makeText(this, "Failed to delete user", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showClearAllDialog() {
        if (userList.isEmpty()) {
            Toast.makeText(this, "No users to clear", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Clear All Users")
                .setMessage("Are you sure you want to delete all users? This action cannot be undone.")
                .setPositiveButton("Clear All", (dialog, which) -> {
                    userManager.clearAllUsers();
                    Toast.makeText(this, "All users cleared", Toast.LENGTH_SHORT).show();
                    loadUsers();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private boolean validateInput(String name, String userId, String password) {
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter name", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (userId.isEmpty()) {
            Toast.makeText(this, "Please enter user ID", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "Please enter password", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (userId.length() < 3) {
            Toast.makeText(this, "User ID must be at least 3 characters", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (password.length() < 4) {
            Toast.makeText(this, "Password must be at least 4 characters", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}