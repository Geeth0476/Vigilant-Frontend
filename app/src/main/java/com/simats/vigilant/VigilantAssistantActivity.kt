package com.simats.vigilant

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.simats.vigilant.databinding.ActivityVigilantAssistantBinding
import java.util.Locale
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class VigilantAssistantActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVigilantAssistantBinding
    private val chatMessages = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter
    private val handler = Handler(Looper.getMainLooper())

    // Context Data
    private var appName: String = "this app"
    private var packageName: String = ""
    private var riskScore: Int = 0
    private var riskLevel: String = "Unknown"
    private var installAge: Long = 0
    private var riskFactors: List<String> = emptyList()

    data class ChatMessage(val text: String, val isBot: Boolean)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVigilantAssistantBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Parse Context
        appName = intent.getStringExtra("APP_NAME") ?: "this app"
        packageName = intent.getStringExtra("PACKAGE_NAME") ?: ""
        riskScore = intent.getIntExtra("RISK_SCORE", 0)
        riskLevel = intent.getStringExtra("RISK_LEVEL") ?: "Unknown"
        installAge = intent.getLongExtra("INSTALL_AGE", System.currentTimeMillis())
        riskFactors = intent.getStringArrayListExtra("RISK_FACTORS")?.toList() ?: emptyList()

        setupChatRecycler()
        setupListeners()
        
        startConversation()
    }

    private fun setupChatRecycler() {
        chatAdapter = ChatAdapter(chatMessages)
        binding.rvChat.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.rvChat.adapter = chatAdapter
    }

    private fun setupListeners() {
        binding.btnClose.setOnClickListener { finish() }
        
        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                addUserMessage(text)
                binding.etMessage.text.clear()
                // Process Query
                processUserQuery(text)
            }
        }
    }

    private fun startConversation() {
        // Rule 1: Auto-Greeting
        showTyping(true)
        handler.postDelayed({
            val greeting = "Hi, I’m Vigi \uD83D\uDC4B\nI’ve analyzed $appName and I can help explain why certain behaviors were detected and what you can safely do next."
            addBotMessage(greeting)
            showTyping(false)
            addSuggestionChips()
        }, 1000)
    }

    private fun addSuggestionChips() {
        val suggestions = mutableListOf<String>()
        
        if (riskScore > 0) {
            suggestions.add("Why is the risk score high?")
        } else {
            suggestions.add("Is this app safe?")
        }
        
        suggestions.add("Should I uninstall this?")
        
        // Add dynamic suggestions based on factors
        if (riskFactors.any { it.contains("Location", true) }) suggestions.add("Why does it need Location?")
        if (riskFactors.any { it.contains("Mic", true) || it.contains("Audio", true) }) suggestions.add("Is it listening to me?")
        if (riskFactors.any { it.contains("Accessibility", true) }) suggestions.add("What is Accessibility access?")
        
        suggestions.add("Data privacy info")

        binding.chipGroupSuggestions.removeAllViews()
        for (q in suggestions.take(4)) { // Limit to 4
            val chip = Chip(this).apply {
                text = q
                setChipBackgroundColorResource(R.color.vigilant_gray_bg)
                setChipStrokeColorResource(R.color.vigilant_blue)
                setChipStrokeWidth(2f)
                setOnClickListener { 
                    addUserMessage(q)
                    processUserQuery(q)
                }
            }
            binding.chipGroupSuggestions.addView(chip)
        }
    }
    
    // --- VIGI BRAIN ---
    private fun processUserQuery(query: String) {
        showTyping(true)
        
        // Use Coroutines for network call
        lifecycleScope.launch {
            try {
                // Prepare request
                val requestMap = mapOf(
                    "query" to query,
                    "package_name" to packageName,
                    "behaviors" to riskFactors
                )
                
                val api = com.simats.vigilant.data.api.ApiClient.getService(this@VigilantAssistantActivity)
                val response = api.chatMessage(requestMap)
                
                if (response.isSuccessful) {
                    val chatRes = response.body()
                    val chatData = chatRes?.data
                    val botText = chatData?.response ?: "I'm having trouble connecting to the security database."
                    
                    addBotMessage(botText)
                    
                    // Optional: Update chips if suggestions provided
                    if (!chatData?.suggestions.isNullOrEmpty()) {
                        updateSuggestionChips(chatData.suggestions!!)
                    }
                } else {
                   // Fallback to local logic if server fails
                   addBotMessage(fallbackLocalLogic(query))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                addBotMessage(fallbackLocalLogic(query))
            } finally {
                showTyping(false)
            }
        }
    }

    private fun updateSuggestionChips(suggestions: List<String>) {
        binding.chipGroupSuggestions.removeAllViews()
        for (q in suggestions) {
            val chip = Chip(this).apply {
                text = q
                setChipBackgroundColorResource(R.color.vigilant_gray_bg)
                setChipStrokeColorResource(R.color.vigilant_blue)
                setChipStrokeWidth(2f)
                setOnClickListener { 
                    addUserMessage(q)
                    processUserQuery(q)
                }
            }
            binding.chipGroupSuggestions.addView(chip)
        }
    }

    // Fallback if offline
    private fun fallbackLocalLogic(query: String): String {
         val q = query.lowercase(Locale.ROOT)
         if (q.contains("privacy")) return "Your data is processed locally for maximum privacy."
         if (q.contains("risk")) return "This app has a risk score of $riskScore."
         return "I'm currently creating a secure connection to the cloud... Please try again."
    }

    private fun addUserMessage(text: String) {
        chatMessages.add(ChatMessage(text, false))
        notifyAdapter()
    }

    private fun addBotMessage(text: String) {
        chatMessages.add(ChatMessage(text, true))
        notifyAdapter()
    }

    private fun showTyping(show: Boolean) {
        binding.tvTypingIndicator.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
             binding.rvChat.scrollToPosition(chatMessages.size - 1)
        }
    }

    private fun notifyAdapter() {
        chatAdapter.notifyItemInserted(chatMessages.size - 1)
        binding.rvChat.scrollToPosition(chatMessages.size - 1)
    }

    // --- Adapter ---
    inner class ChatAdapter(private val messages: List<ChatMessage>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val TYPE_BOT = 1
        private val TYPE_USER = 2

        override fun getItemViewType(position: Int): Int {
            return if (messages[position].isBot) TYPE_BOT else TYPE_USER
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == TYPE_BOT) {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message_bot, parent, false)
                BotViewHolder(view)
            } else {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message_user, parent, false)
                UserViewHolder(view)
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val msg = messages[position]
            if (holder is BotViewHolder) {
                holder.tvMessage.text = msg.text
            } else if (holder is UserViewHolder) {
                holder.tvMessage.text = msg.text
            }
        }

        override fun getItemCount() = messages.size

        inner class BotViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvMessage: TextView = view.findViewById(R.id.tvBotMessage)
        }

        inner class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvMessage: TextView = view.findViewById(R.id.tvUserMessage)
        }
    }
}
