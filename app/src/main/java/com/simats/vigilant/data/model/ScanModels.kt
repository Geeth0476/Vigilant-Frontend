package com.simats.vigilant.data.model

// Add Scan Start Response
data class ScanStartResponse(
    val success: Boolean,
    val data: ScanStartData?
)

data class ScanStartData(
    val scan_id: String, // Or Int, assume String safe
    val status: String
)
