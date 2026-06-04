package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.ChatMessage
import com.example.data.database.ChatSession
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ChatRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ChatRepository(database.chatDao())
    }

    val sessions: StateFlow<List<ChatSession>> = repository.allSessions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentMessages: StateFlow<List<ChatMessage>> = _currentSessionId
        .flatMapLatest { sessionId ->
            if (sessionId == null) {
                flowOf(emptyList())
            } else {
                repository.getMessagesForSession(sessionId)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    private val _typingText = MutableStateFlow("Roast Ready Kortesi...")
    val typingText: StateFlow<String> = _typingText.asStateFlow()

    private val funnyStatuses = listOf(
        "Muri khacche...",
        "Roast ready kortesi...",
        "Brain load hocche...",
        "Your stupidity level calculating...",
        "Dim bhaji kortesi...",
        "Formatting insult...",
        "Insult algorithm setup kortesi...",
        "Tea break nitest...",
        "Google thika churi kortesi..."
    )

    fun selectSession(sessionId: String) {
        _currentSessionId.value = sessionId
    }

    fun startNewSession() {
        _currentSessionId.value = null
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (_currentSessionId.value == sessionId) {
                _currentSessionId.value = null
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            _currentSessionId.value = null
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            // 1. Create session if we don't have one
            var sessionId = _currentSessionId.value
            if (sessionId == null) {
                val previewTitle = if (text.length > 25) text.take(22) + "..." else text
                val newSession = repository.createNewSession(previewTitle)
                sessionId = newSession.id
                _currentSessionId.value = sessionId
            }

            // 2. Save user message
            val userMsg = ChatMessage(sessionId = sessionId, sender = "user", text = text)
            repository.insertMessage(userMsg)

            // 3. Show dynamic typing status
            _isTyping.value = true
            updateTypingTextLoop()

            // 4. Send request to repository
            val history = currentMessages.value
            val response = repository.sendPromptToGemini(history, text)

            // 5. Save bot message
            val botMsg = ChatMessage(sessionId = sessionId, sender = "bot", text = response)
            repository.insertMessage(botMsg)

            // 6. Turn off typing
            _isTyping.value = false
        }
    }

    private fun updateTypingTextLoop() {
        viewModelScope.launch {
            while (_isTyping.value) {
                _typingText.value = funnyStatuses.random()
                delay(2000)
            }
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ChatViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
