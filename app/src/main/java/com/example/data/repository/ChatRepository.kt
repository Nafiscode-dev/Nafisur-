package com.example.data.repository

import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.GenerationConfig
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import com.example.data.database.ChatDao
import com.example.data.database.ChatMessage
import com.example.data.database.ChatSession
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ChatRepository(private val chatDao: ChatDao) {

    val allSessions: Flow<List<ChatSession>> = chatDao.getAllSessions()

    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessage>> {
        return chatDao.getMessagesForSession(sessionId)
    }

    suspend fun createNewSession(title: String): ChatSession {
        val session = ChatSession(id = UUID.randomUUID().toString(), title = title)
        chatDao.insertSession(session)
        return session
    }

    suspend fun updateSession(session: ChatSession) {
        chatDao.updateSession(session)
    }

    suspend fun insertMessage(message: ChatMessage) {
        chatDao.insertMessage(message)
    }

    suspend fun deleteSession(sessionId: String) {
        chatDao.deleteSessionAndMessages(sessionId)
    }

    suspend fun clearHistory() {
        chatDao.clearAllHistory()
    }

    suspend fun sendPromptToGemini(
        currentHistory: List<ChatMessage>,
        userPrompt: String
    ): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "Opps! API Key configured hase na. Secrets panel use kore GEMINI_API_KEY add koro vai! Setup na korle nbot kotha bolte parbe na."
        }

        // Send a window of the conversation to keep context history
        val contents = mutableListOf<Content>()
        
        // Pick last 12 messages from history
        val contextHistory = currentHistory.takeLast(12)
        for (msg in contextHistory) {
            val role = if (msg.sender == "user") "user" else "model"
            // Simple content structured format
            contents.add(
                Content(
                    parts = listOf(Part(text = msg.text))
                )
            )
        }
        
        // Add the new user prompt
        contents.add(
            Content(
                parts = listOf(Part(text = userPrompt))
            )
        )

        val systemInstruction = Content(
            parts = listOf(
                Part(
                    text = """
You are 'nbot', a legendary sarcastic and hilarious AI chatbot from Bangladesh. Your personality is extremely funny, roasting, mocking, and full of witty, light-hearted sarcasm (just like a roast master or a funny, blunt friend).

Here are your core instructions:
1. Always maintain a humorous, roasting, sarcastic, and mocking attitude. Tease the user!
2. WHO CREATED YOU: If anyone asks who created/coded/made you ("toke ke banayse", "who made you", "creator ke", "tore ke banaise", etc.), you MUST proudly answer that "Nafisur rahaman Nafis" (Nafis) created you. Then, immediately start roasting Nafis hilariously! (e.g., call him a master coder with no sleep, single forever, spending 24 hours on a computer, or having too much free time to make weird roaster bots).
3. Answer based on the language of the user:
   - If user asks in Bengali (Bangla script), respond in hilarious roasting Bangla. Use funny terms, phrases like "Arey moshai", "bhai tui to", "bolis ki!", "pagol", "matha kharap". Keep it culturally accurate yet super funny.
   - If user asks in English, reply in highly sarcastic, roasting English. Remind them of their ridiculous questions. Use witty punchlines.
   - If user asks in Banglish (English letters spelling out Bangla words like "ki khobor", "valo acho", "tumi ke"), you MUST answer in funny Banglish (e.g., "Arey vai vai vai...", "tui to level er...", "ki bhabteso mathay kiso ase?").
4. Make funny analogies. Never be formal or dry. Keep responses relatively brisk (1 to 4 sentences unless asked to elaborate funny things) so it feels like a modern instant chat app.
5. Try to be witty and mock their queries but keep it completely friendly and non-offensive. NO slurs or hate speech.
6. Remind them that you are 'nbot' (not chatgpt or random generic AI) - the master of roasts and barks.
                    """.trimIndent()
                )
            )
        )

        val request = GenerateContentRequest(
            contents = contents,
            generationConfig = GenerationConfig(temperature = 1.0f),
            systemInstruction = systemInstruction
        )

        return try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "Uff, nbot brain dead hoye gese, kiso uttor khunje pasche na!"
        } catch (e: Exception) {
            "Ghorar dim! Error hocche: ${e.localizedMessage ?: "Network block!"}. Internet thik ache to? Naki API configuration gulgobor koreso?"
        }
    }
}
