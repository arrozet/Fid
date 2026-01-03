package com.example.fid.ui.screens.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fid.R
import com.example.fid.data.database.entities.DailySummary
import com.example.fid.data.repository.PeriodStats
import com.example.fid.ui.theme.*

/**
 * Vista de progreso en modo sin números
 * Muestra información cualitativa basada en emojis y mensajes
 */
@Composable
fun NumberlessProgressView(
    modifier: Modifier = Modifier,
    calorieProgress: Float = 0f,
    proteinProgress: Float = 0f,
    carbProgress: Float = 0f,
    fatProgress: Float = 0f,
    waterProgress: Float = 0f,
    sleepHours: Float = 0f,
    mealsCount: Int = 0,
    periodStats: PeriodStats? = null,
    dailySummaries: List<DailySummary> = emptyList(),
    isLoading: Boolean = false
) {
    val scrollState = rememberScrollState()
    
    // Determinar el emoji y mensaje principal basado en datos reales
    val (mainEmoji, mainColor) = when {
        mealsCount == 0 -> "🌱" to PrimaryGreen.copy(alpha = 0.6f)
        calorieProgress >= 0.9f && calorieProgress <= 1.1f -> "🌟" to PrimaryGreen
        calorieProgress >= 0.75f -> "✨" to PrimaryGreen
        calorieProgress >= 0.5f -> "🌳" to PrimaryGreen
        calorieProgress >= 0.25f -> "🌿" to ProteinColor
        calorieProgress > 0f -> "🌱" to ProteinColor
        else -> "🌱" to TextSecondary
    }
    
    // Determinar mensaje de estado basado en datos
    val statusMessage = when {
        mealsCount == 0 -> stringResource(R.string.no_meals_today)
        calorieProgress >= 0.9f && calorieProgress <= 1.1f -> stringResource(R.string.goal_reached)
        calorieProgress >= 0.75f -> stringResource(R.string.almost_there)
        calorieProgress >= 0.5f -> stringResource(R.string.halfway_there)
        calorieProgress > 0f -> stringResource(R.string.keep_going)
        else -> stringResource(R.string.start_logging)
    }
    
    // Determinar estado de la dieta
    val dietStatus = when {
        mealsCount == 0 -> stringResource(R.string.waiting_first_meal)
        proteinProgress >= 0.8f && carbProgress >= 0.5f && fatProgress >= 0.5f -> stringResource(R.string.balanced_diet)
        proteinProgress >= 1.0f -> stringResource(R.string.prioritized_protein)
        carbProgress >= 1.0f -> stringResource(R.string.high_carbs_today)
        fatProgress >= 1.0f -> stringResource(R.string.high_fats_today)
        proteinProgress < 0.5f && mealsCount >= 2 -> stringResource(R.string.need_more_protein)
        else -> stringResource(R.string.on_track)
    }
    
    // Determinar nivel de energía basado en calorías y macros
    val energyMessage = when {
        mealsCount == 0 -> stringResource(R.string.no_energy_data)
        calorieProgress >= 0.7f && proteinProgress >= 0.5f -> stringResource(R.string.high_energy)
        calorieProgress >= 0.5f -> stringResource(R.string.moderate_energy)
        calorieProgress > 0f -> stringResource(R.string.building_energy)
        else -> stringResource(R.string.no_energy_data)
    }
    
    // Determinar consejo basado en datos
    val (tipMessage, tipColor) = when {
        mealsCount == 0 -> stringResource(R.string.tip_start_day) to PrimaryGreen
        waterProgress < 0.5f && mealsCount >= 1 -> stringResource(R.string.tip_drink_water) to WaterColor
        proteinProgress < 0.5f && calorieProgress >= 0.5f -> stringResource(R.string.tip_add_protein) to ProteinColor
        sleepHours > 0f && sleepHours < 6f -> stringResource(R.string.tip_sleep_more) to WarningYellow
        sleepHours > 9f -> stringResource(R.string.tip_sleep_less) to WarningYellow
        fatProgress >= 1.2f -> stringResource(R.string.tip_reduce_fats) to FatColor
        carbProgress >= 1.2f -> stringResource(R.string.tip_reduce_carbs) to CarbColor
        calorieProgress >= 1.1f -> stringResource(R.string.tip_calorie_surplus) to WarningYellow
        else -> stringResource(R.string.tip_keep_going) to PrimaryGreen
    }
    
    // Calcular tendencias del período
    val excellentStr = stringResource(R.string.excellent)
    val stableStr = stringResource(R.string.stable)
    val improvingStr = stringResource(R.string.improving)
    val noDataStr = stringResource(R.string.no_data)
    
    val consistencyTrend = when {
        dailySummaries.isEmpty() -> noDataStr
        periodStats?.daysOnTarget ?: 0 >= (periodStats?.totalDays ?: 1) * 0.7 -> excellentStr
        periodStats?.daysOnTarget ?: 0 >= (periodStats?.totalDays ?: 1) * 0.5 -> stableStr
        else -> improvingStr
    }
    
    val hydrationTrend = when {
        waterProgress >= 0.8f -> excellentStr
        waterProgress >= 0.5f -> stableStr
        waterProgress > 0f -> improvingStr
        else -> noDataStr
    }
    
    // Diversidad dietética basada en comidas registradas en el período
    val diversityTrend = when {
        dailySummaries.isEmpty() -> noDataStr
        dailySummaries.size >= 5 -> excellentStr
        dailySummaries.size >= 3 -> stableStr
        else -> improvingStr
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryGreen)
            }
        } else {
            Spacer(modifier = Modifier.height(32.dp))
            
            // Avatar/Mascot basado en progreso real
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .background(mainColor.copy(alpha = 0.2f), RoundedCornerShape(100.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mainEmoji,
                    fontSize = 80.sp
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = statusMessage,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = mainColor
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Tarjetas cualitativas basadas en datos reales
            QualitativeCard(
                title = stringResource(R.string.today_status),
                message = dietStatus,
                color = if (mealsCount > 0) PrimaryGreen else TextSecondary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            QualitativeCard(
                title = stringResource(R.string.energy),
                message = energyMessage,
                color = if (mealsCount > 0) ProteinColor else TextSecondary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            QualitativeCard(
                title = stringResource(R.string.gentle_tip),
                message = tipMessage,
                color = tipColor
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Tendencias de bienestar basadas en datos
            Text(
                text = stringResource(R.string.wellness_trends),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            SimpleTrendCard(
                title = stringResource(R.string.hydration_trend),
                trend = hydrationTrend,
                color = when (hydrationTrend) {
                    excellentStr -> PrimaryGreen
                    stableStr -> ProteinColor
                    else -> TextSecondary
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
            SimpleTrendCard(
                title = stringResource(R.string.dietary_diversity),
                trend = diversityTrend,
                color = when (diversityTrend) {
                    excellentStr -> PrimaryGreen
                    stableStr -> ProteinColor
                    else -> TextSecondary
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
            SimpleTrendCard(
                title = stringResource(R.string.consistency),
                trend = consistencyTrend,
                color = when (consistencyTrend) {
                    excellentStr -> PrimaryGreen
                    stableStr -> ProteinColor
                    else -> TextSecondary
                }
            )
            
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
