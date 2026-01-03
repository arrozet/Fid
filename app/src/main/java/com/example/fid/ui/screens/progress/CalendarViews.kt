package com.example.fid.ui.screens.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
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
import java.text.SimpleDateFormat
import java.util.*

/**
 * Leyenda del calendario
 */
@Composable
fun CalendarLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Objetivo cumplido
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(PrimaryGreen, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.goal_reached_legend),
            fontSize = 12.sp,
            color = TextSecondary
        )
        
        Spacer(modifier = Modifier.width(24.dp))
        
        // Sin datos / no cumplido
        Box(
            modifier = Modifier
                .size(16.dp)
                .border(2.dp, TextSecondary.copy(alpha = 0.5f), CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.no_data_legend),
            fontSize = 12.sp,
            color = TextSecondary
        )
    }
}

/**
 * Vista semanal - 7 días como círculos horizontales
 */
@Composable
fun WeeklyCalendarView(
    summariesByDate: Map<Long, DailySummary>,
    onDayClick: (Long) -> Unit
) {
    val today = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    
    // Obtener los últimos 7 días
    val weekDays = remember {
        (6 downTo 0).map { daysAgo ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }
    }
    
    val dayOfWeekFormatter = remember { SimpleDateFormat("EEE", Locale.getDefault()) }
    val dayFormatter = remember { SimpleDateFormat("d", Locale.getDefault()) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkCard, RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Text(
            text = stringResource(R.string.last_7_days),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            weekDays.forEach { dayTimestamp ->
                val summary = summariesByDate[dayTimestamp]
                val isToday = dayTimestamp == today
                
                DayCircle(
                    dayOfWeek = dayOfWeekFormatter.format(Date(dayTimestamp)),
                    dayNumber = dayFormatter.format(Date(dayTimestamp)),
                    summary = summary,
                    isToday = isToday,
                    onClick = { onDayClick(dayTimestamp) }
                )
            }
        }
    }
}

/**
 * Vista mensual - Calendario completo del mes
 */
@Composable
fun MonthlyCalendarView(
    currentDate: Calendar,
    summariesByDate: Map<Long, DailySummary>,
    onDayClick: (Long) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val monthYearFormatter = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    
    val today = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    
    // Obtener días del mes
    val daysInMonth = remember(currentDate) {
        val cal = currentDate.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        
        val firstDayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Lunes = 0
        val totalDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        val days = mutableListOf<Long?>()
        
        // Espacios vacíos antes del primer día
        repeat(firstDayOfWeek) { days.add(null) }
        
        // Días del mes
        for (day in 1..totalDays) {
            cal.set(Calendar.DAY_OF_MONTH, day)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            days.add(cal.timeInMillis)
        }
        
        days
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkCard, RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        // Header con navegación
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = stringResource(R.string.previous_month),
                    tint = TextPrimary
                )
            }
            
            Text(
                text = monthYearFormatter.format(currentDate.time).replaceFirstChar { it.uppercase() },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            IconButton(onClick = onNextMonth) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = stringResource(R.string.next_month),
                    tint = TextPrimary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Días de la semana
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("L", "M", "X", "J", "V", "S", "D").forEach { day ->
                Text(
                    text = day,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(36.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Grid de días
        val rows = daysInMonth.chunked(7)
        rows.forEach { week ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                week.forEach { dayTimestamp ->
                    if (dayTimestamp != null) {
                        val summary = summariesByDate[dayTimestamp]
                        val isToday = dayTimestamp == today
                        val isFuture = dayTimestamp > today
                        
                        MonthDayCircle(
                            dayNumber = SimpleDateFormat("d", Locale.getDefault()).format(Date(dayTimestamp)),
                            summary = summary,
                            isToday = isToday,
                            isFuture = isFuture,
                            onClick = { if (!isFuture) onDayClick(dayTimestamp) }
                        )
                    } else {
                        // Espacio vacío
                        Box(modifier = Modifier.size(36.dp))
                    }
                }
                // Rellenar fila incompleta
                repeat(7 - week.size) {
                    Box(modifier = Modifier.size(36.dp))
                }
            }
        }
    }
}

/**
 * Vista anual - 12 meses mini
 * Al pulsar en un mes, navega a la vista mensual de ese mes
 */
@Composable
fun YearlyCalendarView(
    currentDate: Calendar,
    summariesByDate: Map<Long, DailySummary>,
    onMonthClick: (Int, Int) -> Unit, // (month, year) -> cambia a vista mensual
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit
) {
    val year = currentDate.get(Calendar.YEAR)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkCard, RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        // Header con navegación
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousYear) {
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = stringResource(R.string.previous_year),
                    tint = TextPrimary
                )
            }
            
            Text(
                text = year.toString(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            IconButton(onClick = onNextYear) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = stringResource(R.string.next_year),
                    tint = TextPrimary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Grid de meses (4 filas x 3 columnas)
        val monthNames = remember {
            val symbols = java.text.DateFormatSymbols(Locale.getDefault())
            symbols.shortMonths.filter { it.isNotEmpty() }
        }
        
        val today = remember {
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
        
        (0..3).forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                (0..2).forEach { col ->
                    val month = row * 3 + col
                    if (month < 12) {
                        MiniMonthView(
                            monthName = monthNames[month].replaceFirstChar { it.uppercase() },
                            month = month,
                            year = year,
                            summariesByDate = summariesByDate,
                            today = today,
                            onMonthClick = { onMonthClick(month, year) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Mini vista de un mes para la vista anual
 * Al pulsar, navega a la vista mensual de ese mes
 */
@Composable
fun MiniMonthView(
    monthName: String,
    month: Int,
    year: Int,
    summariesByDate: Map<Long, DailySummary>,
    today: Long,
    onMonthClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val daysInMonth = remember(month, year) {
        val cal = Calendar.getInstance()
        cal.set(year, month, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        
        val totalDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        (1..totalDays).map { day ->
            cal.set(Calendar.DAY_OF_MONTH, day)
            cal.timeInMillis
        }
    }
    
    Column(
        modifier = modifier
            .padding(4.dp)
            .clickable(onClick = onMonthClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = monthName,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Mini grid de días (puntos pequeños) - solo visual, no clickable
        val rows = daysInMonth.chunked(7)
        rows.forEach { week ->
            Row(
                horizontalArrangement = Arrangement.Center
            ) {
                week.forEach { dayTimestamp ->
                    val summary = summariesByDate[dayTimestamp]
                    val isFuture = dayTimestamp > today
                    
                    // Simple: verde si cumplió objetivo, vacío si no
                    val goalMet = summary != null && 
                        summary.calorieGoal > 0 && 
                        (summary.totalCalories / summary.calorieGoal) in 0.9f..1.1f
                    
                    val color = when {
                        isFuture -> DarkSurface
                        goalMet -> PrimaryGreen
                        else -> TextSecondary.copy(alpha = 0.3f)
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .padding(1.dp)
                            .background(color, CircleShape)
                    )
                }
            }
        }
    }
}

/**
 * Círculo de un día para la vista semanal
 */
@Composable
fun DayCircle(
    dayOfWeek: String,
    dayNumber: String,
    summary: DailySummary?,
    isToday: Boolean,
    onClick: () -> Unit
) {
    // Simple: verde si cumplió objetivo (90%-110%), vacío si no
    val goalMet = summary != null && 
        summary.calorieGoal > 0 && 
        (summary.totalCalories / summary.calorieGoal) in 0.9f..1.1f
    
    val backgroundColor = if (goalMet) PrimaryGreen else Color.Transparent
    val borderColor = if (isToday) PrimaryGreen else TextSecondary.copy(alpha = 0.5f)
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = dayOfWeek.take(3).replaceFirstChar { it.uppercase() },
            fontSize = 10.sp,
            color = TextSecondary
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Box(
            modifier = Modifier
                .size(40.dp)
                .then(
                    if (backgroundColor != Color.Transparent) {
                        Modifier.background(backgroundColor, CircleShape)
                    } else {
                        Modifier.border(2.dp, borderColor, CircleShape)
                    }
                )
                .then(
                    if (isToday && backgroundColor != Color.Transparent) {
                        Modifier.border(3.dp, PrimaryGreen, CircleShape)
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = dayNumber,
                fontSize = 14.sp,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (backgroundColor != Color.Transparent) DarkBackground else TextPrimary
            )
        }
    }
}

/**
 * Círculo de un día para la vista mensual
 */
@Composable
fun MonthDayCircle(
    dayNumber: String,
    summary: DailySummary?,
    isToday: Boolean,
    isFuture: Boolean,
    onClick: () -> Unit
) {
    // Simple: verde si cumplió objetivo (90%-110%), vacío si no
    val goalMet = summary != null && 
        summary.calorieGoal > 0 && 
        (summary.totalCalories / summary.calorieGoal) in 0.9f..1.1f
    
    val backgroundColor = when {
        isFuture -> DarkSurface
        goalMet -> PrimaryGreen
        else -> Color.Transparent
    }
    
    val borderColor = when {
        isToday -> PrimaryGreen
        isFuture -> TextSecondary.copy(alpha = 0.2f)
        else -> TextSecondary.copy(alpha = 0.5f)
    }
    
    Box(
        modifier = Modifier
            .size(36.dp)
            .then(
                if (backgroundColor != Color.Transparent && !isFuture) {
                    Modifier.background(backgroundColor, CircleShape)
                } else {
                    Modifier.border(1.5.dp, borderColor, CircleShape)
                }
            )
            .then(
                if (isToday) {
                    Modifier.border(2.dp, PrimaryGreen, CircleShape)
                } else Modifier
            )
            .clickable(enabled = !isFuture, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = dayNumber,
            fontSize = 12.sp,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isFuture -> TextSecondary.copy(alpha = 0.4f)
                backgroundColor != Color.Transparent -> DarkBackground
                else -> TextPrimary
            }
        )
    }
}
