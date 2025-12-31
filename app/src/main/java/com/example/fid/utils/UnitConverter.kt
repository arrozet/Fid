package com.example.fid.utils

import com.example.fid.data.database.entities.User

/**
 * Utility class for converting between metric and imperial units
 */
object UnitConverter {
    
    // Weight conversions
    const val KG_TO_LB = 2.20462f
    const val G_TO_OZ = 0.035274f
    
    // Height conversions
    const val CM_TO_INCHES = 0.393701f
    
    /**
     * Convert weight from kg to appropriate unit based on user preference
     * @param kg Weight in kilograms
     * @param unit User's measurement unit preference ("metric" or "imperial")
     * @return Converted weight value
     */
    fun convertWeight(kg: Float, unit: String): Float {
        return if (unit == "imperial") kg * KG_TO_LB else kg
    }
    
    /**
     * Convert grams to appropriate unit based on user preference
     * @param grams Weight in grams
     * @param unit User's measurement unit preference ("metric" or "imperial")
     * @return Converted weight value
     */
    fun convertGrams(grams: Float, unit: String): Float {
        return if (unit == "imperial") grams * G_TO_OZ else grams
    }
    
    /**
     * Convert height from cm to appropriate unit based on user preference
     * @param cm Height in centimeters
     * @param unit User's measurement unit preference ("metric" or "imperial")
     * @return Converted height value (in inches if imperial, cm if metric)
     */
    fun convertHeight(cm: Float, unit: String): Float {
        return if (unit == "imperial") cm * CM_TO_INCHES else cm
    }
    
    /**
     * Format weight with appropriate unit label
     * @param kg Weight in kilograms
     * @param unit User's measurement unit preference
     * @return Formatted string with unit (e.g., "70.0 kg" or "154.3 lb")
     */
    fun formatWeight(kg: Float, unit: String): String {
        val value = convertWeight(kg, unit)
        val unitLabel = if (unit == "imperial") "lb" else "kg"
        return "%.1f %s".format(value, unitLabel)
    }
    
    /**
     * Format grams with appropriate unit label
     * @param grams Weight in grams
     * @param unit User's measurement unit preference
     * @return Formatted string with unit (e.g., "100 g" or "3.5 oz")
     */
    fun formatGrams(grams: Float, unit: String): String {
        val value = convertGrams(grams, unit)
        val unitLabel = if (unit == "imperial") "oz" else "g"
        return "%.1f %s".format(value, unitLabel)
    }
    
    /**
     * Format height with appropriate unit label
     * @param cm Height in centimeters
     * @param unit User's measurement unit preference
     * @return Formatted string with unit (e.g., "170 cm" or "5'7\"")
     */
    fun formatHeight(cm: Float, unit: String): String {
        return if (unit == "imperial") {
            val totalInches = cm * CM_TO_INCHES
            val feet = (totalInches / 12).toInt()
            val inches = (totalInches % 12).toInt()
            "$feet'$inches\""
        } else {
            "%.0f cm".format(cm)
        }
    }
    
    /**
     * Get weight unit label
     */
    fun getWeightUnitLabel(unit: String): String {
        return if (unit == "imperial") "lb" else "kg"
    }
    
    /**
     * Get grams unit label
     */
    fun getGramsUnitLabel(unit: String): String {
        return if (unit == "imperial") "oz" else "g"
    }
    
    /**
     * Get height unit label
     */
    fun getHeightUnitLabel(unit: String): String {
        return if (unit == "imperial") "ft/in" else "cm"
    }
    
    /**
     * Convert from display unit back to metric (for saving)
     */
    fun convertWeightToMetric(value: Float, unit: String): Float {
        return if (unit == "imperial") value / KG_TO_LB else value
    }
    
    fun convertGramsToMetric(value: Float, unit: String): Float {
        return if (unit == "imperial") value / G_TO_OZ else value
    }
    
    fun convertHeightToMetric(value: Float, unit: String): Float {
        return if (unit == "imperial") value / CM_TO_INCHES else value
    }
}

