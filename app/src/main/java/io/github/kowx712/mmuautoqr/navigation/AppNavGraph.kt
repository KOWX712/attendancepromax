package io.github.kowx712.mmuautoqr.navigation

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import io.github.kowx712.mmuautoqr.BottomNavItem
import io.github.kowx712.mmuautoqr.QRScannerActivity
import io.github.kowx712.mmuautoqr.R
import io.github.kowx712.mmuautoqr.ui.screens.MainScreen
import io.github.kowx712.mmuautoqr.ui.screens.UserScreen
import io.github.kowx712.mmuautoqr.viewmodel.HomeViewModel
import io.github.kowx712.mmuautoqr.viewmodel.HomeViewModelFactory
import io.github.kowx712.mmuautoqr.viewmodel.UserOperationFeedback
import io.github.kowx712.mmuautoqr.viewmodel.UserViewModel
import io.github.kowx712.mmuautoqr.viewmodel.UserViewModelFactory
import kotlinx.coroutines.flow.collectLatest

fun NavGraphBuilder.homeDestination(
    activity: Activity,
    navController: NavController,
    homeViewModelFactory: HomeViewModelFactory
) {
    composable(BottomNavItem.Home.route) {
        val homeViewModel: HomeViewModel = viewModel(factory = homeViewModelFactory)
        val totalUsers by homeViewModel.totalUsers
        val activeUsers by homeViewModel.activeUsers

        LaunchedEffect(Unit) {
            homeViewModel.refreshStats()
        }

        MainScreen(
            totalUsers = totalUsers,
            activeUsers = activeUsers,
            onRefreshStats = {
                homeViewModel.refreshStats()
            },
            onScanQr = {
                if (activeUsers > 0) {
                    if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        activity.startActivity(Intent(activity, QRScannerActivity::class.java))
                    } else {
                        Toast.makeText(activity, activity.getString(R.string.camera_permission_needed), Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(activity, activity.getString(R.string.add_users_first), Toast.LENGTH_LONG).show()
                    navController.navigate(BottomNavItem.Users.route)
                }
            }
        )
    }
}

fun NavGraphBuilder.usersDestination(
    context: Context,
    userViewModelFactory: UserViewModelFactory
) {
    composable(BottomNavItem.Users.route) {
        val userViewModel: UserViewModel = viewModel(factory = userViewModelFactory)
        val usersList by userViewModel.usersList

        LaunchedEffect(Unit) {
            userViewModel.operationFeedback.collectLatest { feedback ->
                val message = when (feedback) {
                    is UserOperationFeedback.Success -> context.getString(feedback.messageResId)
                    is UserOperationFeedback.Error -> context.getString(feedback.messageResId)
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }

        UserScreen(
            users = usersList,
            onAddUser = { name, userId, password ->
                userViewModel.addUser(name, userId, password)
            },
            onUpdateUser = { user, name, password ->
                userViewModel.updateUser(user, name, password)
            },
            onDeleteUser = { user ->
                userViewModel.deleteUser(user)
            },
            onToggleUserStatus = { userToToggle ->
                userViewModel.toggleUserStatus(userToToggle)
            }
        )
    }
}
