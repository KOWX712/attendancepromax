package io.github.kowx712.mmuautoqr.ui.navigation3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Simple navigation helper that owns a back stack and result channels.
 * Supports push/replace/pop/popUntil and result APIs: navigateForResult/setResult/observeResult/clearResult.
 */
class Navigator<T : NavKey>(
    initialKey: T
) {
    val backStack: SnapshotStateList<T> = mutableStateListOf(initialKey)

    private val resultBus = mutableMapOf<String, MutableSharedFlow<Any>>()

    /**
     * Push a key onto the back stack.
     */
    fun push(key: T) {
        backStack.add(key)
    }

    /**
     * Replace the top key, or push if the stack is empty.
     */
    fun replace(key: T) {
        if (backStack.isNotEmpty()) {
            backStack[backStack.lastIndex] = key
        } else {
            backStack.add(key)
        }
    }

    /**
     * Replace the backstack with a new list of keys if the stack is not empty.
     */
    fun replaceAll(keys: List<T>) {
        if (keys.isEmpty()) {
            return
        }
        if (backStack.isNotEmpty()) {
            backStack.clear()
            backStack.addAll(keys)
        }
    }


    /**
     * Pop the top key if present.
     */
    fun pop() {
        backStack.removeLastOrNull()
    }

    /**
     * Pop until predicate matches the top key.
     */
    fun popUntil(predicate: (T) -> Boolean) {
        while (backStack.isNotEmpty() && !predicate(backStack.last())) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    /**
     * Navigate expecting a result. Caller should subscribe via observeResult(requestKey).
     */
    fun navigateForResult(route: T, requestKey: String) {
        ensureChannel(requestKey)
        push(route)
    }

    /**
     * Set a result for the given request and then pop.
     */
    fun <R : Any> setResult(requestKey: String, value: R) {
        ensureChannel(requestKey).tryEmit(value)
        pop()
    }

    /**
     * Observe results for a given request key as a SharedFlow.
     */
    @Suppress("UNCHECKED_CAST")
    fun <R : Any> observeResult(requestKey: String): SharedFlow<R> {
        return ensureChannel(requestKey) as SharedFlow<R>
    }

    /**
     * Clear the last emitted result for the request key.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun clearResult(requestKey: String) {
        ensureChannel(requestKey).resetReplayCache()
    }

    /**
     * Get current NavKey on the back stack.
     */
    fun current(): T? {
        return backStack.lastOrNull()
    }

    /**
     * Get current size of back stack.
     */
    fun backStackSize(): Int {
        return backStack.size
    }

    private fun ensureChannel(key: String): MutableSharedFlow<Any> {
        return resultBus.getOrPut(key) { MutableSharedFlow(replay = 1, extraBufferCapacity = 0) }
    }

    companion object {
        fun <T : NavKey> Saver(): Saver<Navigator<T>, Any> = listSaver(save = { navigator ->
            navigator.backStack.toList()
        }, restore = { savedList ->
            @Suppress("UNCHECKED_CAST")
            val initialKey = savedList.firstOrNull() ?: Route.Home as T
            val navigator = Navigator(initialKey)
            navigator.backStack.clear()
            navigator.backStack.addAll(savedList)
            navigator
        })
    }
}


@Composable
fun <T : NavKey> rememberNavigator(startRoute: T): Navigator<T> {
    return rememberSaveable(startRoute, saver = Navigator.Saver()) {
        Navigator(startRoute)
    }
}

val LocalNavigator = staticCompositionLocalOf<Navigator<out NavKey>> {
    error("LocalNavigator not provided")
}
