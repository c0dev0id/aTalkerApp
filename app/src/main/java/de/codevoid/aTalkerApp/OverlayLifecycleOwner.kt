package de.codevoid.aTalkerApp

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner

/**
 * Provides the lifecycle/viewmodel/savedstate owners that ComposeView requires
 * when embedded in a WindowManager overlay (outside of an Activity context).
 */
class OverlayLifecycleOwner :
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val registry = LifecycleRegistry(this)
    private val vmStore = ViewModelStore()
    private val ssController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = registry
    override val viewModelStore: ViewModelStore get() = vmStore
    override val savedStateRegistry: SavedStateRegistry get() = ssController.savedStateRegistry

    fun onCreate() {
        ssController.performRestore(null)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    fun onStart() = registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    fun onResume() = registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onPause() = registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onStop() = registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)

    fun onDestroy() {
        registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        vmStore.clear()
    }
}
