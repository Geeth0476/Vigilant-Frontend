package com.simats.vigilant

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

class SecureAccessActivity : AppCompatActivity() {

    private val currentPin = StringBuilder()
    private val pinDots = arrayOfNulls<View>(4)
    private lateinit var btnConfirmPin: MaterialButton
    private lateinit var tvPinTitle: TextView
    private lateinit var tvPinDesc: TextView
    
    // State
    private var isConfirming = false
    private var firstPin = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_secure_access)

        setupToolbar()
        setupViews()
        setupKeypad()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        toolbar.setNavigationOnClickListener {
             if (isConfirming) {
                 // Back during confirm goes back to create
                 resetToCreate()
             } else {
                 finish()
             }
        }
    }

    private fun setupViews() {
        tvPinTitle = findViewById(R.id.tvPinTitle)
        tvPinDesc = findViewById(R.id.tvPinDesc)
        
        pinDots[0] = findViewById(R.id.pinDot1)
        pinDots[1] = findViewById(R.id.pinDot2)
        pinDots[2] = findViewById(R.id.pinDot3)
        pinDots[3] = findViewById(R.id.pinDot4)

        btnConfirmPin = findViewById(R.id.btnConfirmPin)
        btnConfirmPin.setOnClickListener {
             handlePinSubmit()
        }
    }
    
    private fun handlePinSubmit() {
        val enteredPin = currentPin.toString()
        
        if (!isConfirming) {
            // Step 1: Create
            firstPin = enteredPin
            isConfirming = true
            
            // Clear inputs for next step
            currentPin.clear()
            updatePinDots()
            
            // Update UI
            tvPinTitle.text = "Confirm App PIN"
            tvPinDesc.text = "Re-enter your PIN to confirm"
            btnConfirmPin.text = "Confirm PIN"
            btnConfirmPin.isEnabled = false
            btnConfirmPin.alpha = 0.5f
            
        } else {
            // Step 2: Confirm
            if (enteredPin == firstPin) {
                // Success
                savePin(enteredPin)
                Toast.makeText(this, "PIN Successfully Set", Toast.LENGTH_SHORT).show()
                
                if (intent.getBooleanExtra("IS_ONBOARDING", false)) {
                    val bioIntent = android.content.Intent(this, BiometricSetupActivity::class.java)
                    bioIntent.putExtra("IS_ONBOARDING", true)
                    startActivity(bioIntent)
                }
                finish()
            } else {
                // Mismatch
                Toast.makeText(this, "PINs do not match. Please try again.", Toast.LENGTH_SHORT).show()
                resetToCreate()
            }
        }
    }
    
    private fun resetToCreate() {
        isConfirming = false
        firstPin = ""
        currentPin.clear()
        updatePinDots()
        
        tvPinTitle.setText(R.string.create_pin_heading) // Make sure string resource is available or use hardcode
        tvPinTitle.text = "Create App PIN"
        tvPinDesc.text = "Enter a 4-digit PIN for your account"
        btnConfirmPin.text = "Continue"
    }

    private fun savePin(pin: String) {
        getSharedPreferences("vigilant_prefs", MODE_PRIVATE)
            .edit()
            .putString("app_pin", pin)
            .apply()
    }

    private fun setupKeypad() {
        val listener = View.OnClickListener { v ->
            if (v is TextView) {
                val digit = v.text.toString()
                appendDigit(digit)
            }
        }

        findViewById<TextView>(R.id.btnKey1).setOnClickListener(listener)
        findViewById<TextView>(R.id.btnKey2).setOnClickListener(listener)
        findViewById<TextView>(R.id.btnKey3).setOnClickListener(listener)
        findViewById<TextView>(R.id.btnKey4).setOnClickListener(listener)
        findViewById<TextView>(R.id.btnKey5).setOnClickListener(listener)
        findViewById<TextView>(R.id.btnKey6).setOnClickListener(listener)
        findViewById<TextView>(R.id.btnKey7).setOnClickListener(listener)
        findViewById<TextView>(R.id.btnKey8).setOnClickListener(listener)
        findViewById<TextView>(R.id.btnKey9).setOnClickListener(listener)
        findViewById<TextView>(R.id.btnKey0).setOnClickListener(listener)

        findViewById<ImageView>(R.id.btnKeyBackspace).setOnClickListener {
            removeDigit()
        }
    }

    private fun appendDigit(digit: String) {
        if (currentPin.length < 4) {
            currentPin.append(digit)
            updatePinDots()
        }
    }

    private fun removeDigit() {
        if (currentPin.isNotEmpty()) {
            currentPin.deleteCharAt(currentPin.length - 1)
            updatePinDots()
        }
    }

    private fun updatePinDots() {
        val length = currentPin.length
        for (i in 0 until 4) {
            if (i < length) {
                pinDots[i]?.setBackgroundResource(R.drawable.bg_pin_indicator_filled)
            } else {
                pinDots[i]?.setBackgroundResource(R.drawable.bg_pin_indicator_empty)
            }
        }

        btnConfirmPin.isEnabled = length == 4
        btnConfirmPin.alpha = if (length == 4) 1.0f else 0.5f
    }
}
