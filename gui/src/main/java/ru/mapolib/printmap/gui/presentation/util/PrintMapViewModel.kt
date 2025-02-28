package ru.mapolib.printmap.gui.presentation.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

abstract class PrintMapViewModel<Event : PrintMapEvent, Effect : PrintMapEffect> : ViewModel() {

    private val _eventChannel = Channel<Effect>(Channel.BUFFERED)
    val effect = _eventChannel.receiveAsFlow()

    /*Счетчик активных запросов в сеть*/
    private val _activeRequestCount = MutableStateFlow(0)
    val activeRequestCount = _activeRequestCount.asStateFlow()


    fun onEffect(effect: Effect) {
        viewModelScope.launch {
            _eventChannel.send(effect)
        }
    }

    override fun onCleared() {
        super.onCleared()
        println("onCleared ${this.javaClass.simpleName}")
    }
    fun sendEvent(event: Event) {
        consumeEvent(event)
    }

    private fun updateActiveRequest(isLaunch: IsLaunch) {
        _activeRequestCount.value =
            if (isLaunch) _activeRequestCount.value.inc() else _activeRequestCount.value.dec()
    }
    protected suspend fun <P> doWork(onProgress: (Boolean) -> Unit, doOnAsyncBlock: suspend () -> P) {
        onProgress(true)
        doCoroutineWork(doOnAsyncBlock)
        onProgress(false)
    }
    protected suspend fun <P> doWork(doOnAsyncBlock: suspend () -> P) {
        doCoroutineWork(doOnAsyncBlock)
    }
    private suspend inline fun <P> doCoroutineWork(crossinline doOnAsyncBlock: suspend () -> P) {
        updateActiveRequest(isLaunch = true)
        doOnAsyncBlock()
        updateActiveRequest(isLaunch = false)
    }

    protected abstract fun consumeEvent(action: Event)
}
typealias IsLaunch = Boolean
