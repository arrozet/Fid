package com.example.fid.ui.screens.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fid.R
import com.example.fid.data.database.entities.DailySummary
import com.example.fid.ui.theme.*
import com.example.fid.utils.UnitConverter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Tarjeta cualitativa con título y mensaje
 */
@Composable
fun QualitativeCard(
    title: String,
    message: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkCard, RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = title,
                fontSize = 14.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                fontSize = 16.sp,
                color = color,
                lineHeight = 24.sp
            )
        }
    }
}

/**
 * Tarjeta simple de tendencia
 */
@Composable
fun SimpleTrendCard(
    title: String,
    trend: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkCard, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                color = TextPrimary
            )
            Text(
                text = trend,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

/**
 * Fila de estadística con etiqueta y valor
 */
@Composable
fun StatRow(
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            color = TextSecondary
        )
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

/**
 * Tarjeta de distribución de macros
 */
@Composable
fun MacroDistributionCard(
    label: String,
    percentage: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(100.dp)
            .background(DarkCard, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = percentage,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Botón de selección de período
 */
@Composable
fun PeriodButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) PrimaryGreen else DarkCard,
            contentColor = if (selected) DarkBackground else TextSecondary
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * Tarjeta de resumen diario
 */
@Composable
fun DailySummaryCard(
    summary: DailySummary,
    measurementUnit: String = "metric",
    onClick: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("EEEE, d MMM yyyy", Locale.getDefault()) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkCard, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dateFormatter.format(Date(summary.date)),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.meals_registered_count, summary.mealsCount),
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${summary.totalCalories.toInt()} kcal",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )
                    val caloriePercentage = if (summary.calorieGoal > 0) {
                        ((summary.totalCalories / summary.calorieGoal) * 100).toInt()
                    } else 0
                    
                    Text(
                        text = stringResource(R.string.percentage_of_goal, caloriePercentage),
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Mini progress bars for macros
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiniMacroBar(
                    label = "P",
                    current = summary.totalProteinG,
                    goal = summary.proteinGoal,
                    color = ProteinColor,
                    measurementUnit = measurementUnit,
                    modifier = Modifier.weight(1f)
                )
                MiniMacroBar(
                    label = "G",
                    current = summary.totalFatG,
                    goal = summary.fatGoal,
                    color = FatColor,
                    measurementUnit = measurementUnit,
                    modifier = Modifier.weight(1f)
                )
                MiniMacroBar(
                    label = "C",
                    current = summary.totalCarbG,
                    goal = summary.carbGoal,
                    color = CarbColor,
                    measurementUnit = measurementUnit,
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Wellness info (sleep and water)
            if (summary.sleepHours > 0f || summary.waterIntakeMl > 0f) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (summary.sleepHours > 0f) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "😴", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${summary.sleepHours.toInt()}h",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                    if (summary.waterIntakeMl > 0f) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(text = "💧", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = String.format(Locale.getDefault(), "%.1fL", summary.waterIntakeMl / 1000f),
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Barra mini de macro
 */
@Composable
fun MiniMacroBar(
    label: String,
    current: Float,
    goal: Float,
    color: Color,
    measurementUnit: String = "metric",
    modifier: Modifier = Modifier
) {
    val displayValue = UnitConverter.convertGrams(current, measurementUnit)
    val unitLabel = UnitConverter.getGramsUnitLabel(measurementUnit)
    
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${displayValue.toInt()}$unitLabel",
                fontSize = 10.sp,
                color = TextPrimary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        val progress = if (goal > 0) (current / goal).coerceIn(0f, 1f) else 0f
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = color,
            trackColor = DarkSurface,
        )
    }
}
