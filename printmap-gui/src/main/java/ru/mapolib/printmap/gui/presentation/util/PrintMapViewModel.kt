package ru.mapolib.printmap.gui.presentation.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

abstract class PrintMapViewModel<Event : PrintMapEvent, Effect: PrintMapEffect>: ViewModel() {

    private val _eventChannel = Channel<Effect>(Channel.BUFFERED)
    val effect = _eventChannel.receiveAsFlow()

    fun onEffect(effect: Effect) {
        viewModelScope.launch {
            _eventChannel.send(effect)
        }
    }

    fun sendEvent(event: Event) {
        consumeEvent(event)
    }

    protected abstract fun consumeEvent(action: Event)
}