package com.example.fid.ui.screens.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.fid.R
import com.example.fid.data.database.entities.DailySummary
import com.example.fid.data.repository.PeriodStats
import com.example.fid.navigation.Screen
import com.example.fid.ui.theme.*
import com.example.fid.utils.UnitConverter
import java.util.*

/**
 * Vista de progreso detallada con calendario y estadísticas
 */
@Composable
fun DetailedProgressView(
    modifier: Modifier = Modifier,
    selectedPeriod: TimePeriod,
    onPeriodChange: (TimePeriod) -> Unit,
    periodStats: PeriodStats?,
    dailySummaries: List<DailySummary>,
    isLoading: Boolean,
    navController: NavController,
    measurementUnit: String = "metric"
) {
    val scrollState = rememberScrollState()
    val unitLabel = UnitConverter.getGramsUnitLabel(measurementUnit)
    
    // Estado para navegación del calendario
    var currentCalendarDate by remember { mutableStateOf(Calendar.getInstance()) }
    
    // Crear mapa de resúmenes por fecha para búsqueda rápida
    val summariesByDate = remember(dailySummaries) {
        dailySummaries.associateBy { summary ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = summary.date
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp)
    ) {
        // Period selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PeriodButton(
                text = stringResource(R.string.week),
                selected = selectedPeriod == TimePeriod.WEEK,
                onClick = { onPeriodChange(TimePeriod.WEEK) },
                modifier = Modifier.weight(1f)
            )
            PeriodButton(
                text = stringResource(R.string.month),
                selected = selectedPeriod == TimePeriod.MONTH,
                onClick = { onPeriodChange(TimePeriod.MONTH) },
                modifier = Modifier.weight(1f)
            )
            PeriodButton(
                text = stringResource(R.string.year),
                selected = selectedPeriod == TimePeriod.YEAR,
                onClick = { onPeriodChange(TimePeriod.YEAR) },
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
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
            // Calendario visual según el período
            when (selectedPeriod) {
                TimePeriod.WEEK -> WeeklyCalendarView(
                    summariesByDate = summariesByDate,
                    onDayClick = { date ->
                        navController.navigate(Screen.DailyDetail.createRoute(date))
                    }
                )
                TimePeriod.MONTH -> MonthlyCalendarView(
                    currentDate = currentCalendarDate,
                    summariesByDate = summariesByDate,
                    onDayClick = { date ->
                        navController.navigate(Screen.DailyDetail.createRoute(date))
                    },
                    onPreviousMonth = {
                        currentCalendarDate = (currentCalendarDate.clone() as Calendar).apply {
                            add(Calendar.MONTH, -1)
                        }
                    },
                    onNextMonth = {
                        currentCalendarDate = (currentCalendarDate.clone() as Calendar).apply {
                            add(Calendar.MONTH, 1)
                        }
                    }
                )
                TimePeriod.YEAR -> YearlyCalendarView(
                    currentDate = currentCalendarDate,
                    summariesByDate = summariesByDate,
                    onMonthClick = { month, year ->
                        // Al pulsar un mes, cambiar a vista mensual con ese mes
                        currentCalendarDate = Calendar.getInstance().apply {
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, month)
                            set(Calendar.DAY_OF_MONTH, 1)
                        }
                        onPeriodChange(TimePeriod.MONTH)
                    },
                    onPreviousYear = {
                        currentCalendarDate = (currentCalendarDate.clone() as Calendar).apply {
                            add(Calendar.YEAR, -1)
                        }
                    },
                    onNextYear = {
                        currentCalendarDate = (currentCalendarDate.clone() as Calendar).apply {
                            add(Calendar.YEAR, 1)
                        }
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Leyenda
            CalendarLegend()
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Period stats (si hay datos)
            if (periodStats != null && dailySummaries.isNotEmpty()) {
                val periodNameWeekly = stringResource(R.string.period_weekly)
                val periodNameMonthly = stringResource(R.string.period_monthly)
                val periodNameYearly = stringResource(R.string.period_yearly)
                val periodName = when (selectedPeriod) {
                    TimePeriod.WEEK -> periodNameWeekly
                    TimePeriod.MONTH -> periodNameMonthly
                    TimePeriod.YEAR -> periodNameYearly
                }
                
                Text(
                    text = stringResource(R.string.summary_period, periodName),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkCard, RoundedCornerShape(16.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        StatRow(
                            label = stringResource(R.string.daily_average),
                            value = "${periodStats.avgCalories.toInt()} kcal",
                            color = PrimaryGreen
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        StatRow(
                            label = stringResource(R.string.days_on_target),
                            value = "${periodStats.daysOnTarget} / ${periodStats.totalDays}",
                            color = ProteinColor
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        StatRow(
                            label = stringResource(R.string.average_protein),
                            value = "${UnitConverter.convertGrams(periodStats.avgProteinG, measurementUnit).toInt()}$unitLabel",
                            color = ProteinColor
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Macro distribution
                Text(
                    text = stringResource(R.string.macro_distribution_period, periodName),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MacroDistributionCard(
                        label = stringResource(R.string.proteins),
                        percentage = "${periodStats.proteinPercentage}%",
                        color = ProteinColor,
                        modifier = Modifier.weight(1f)
                    )
                    MacroDistributionCard(
                        label = stringResource(R.string.fats),
                        percentage = "${periodStats.fatPercentage}%",
                        color = FatColor,
                        modifier = Modifier.weight(1f)
                    )
                    MacroDistributionCard(
                        label = stringResource(R.string.carbs_short),
                        percentage = "${periodStats.carbPercentage}%",
                        color = CarbColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                // No data
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(DarkCard, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_data_for_period),
                        color = TextSecondary,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
