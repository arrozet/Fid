package com.example.fid.ui.screens.goals

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.fid.R
import com.example.fid.data.database.FidDatabase
import com.example.fid.data.database.entities.User
import com.example.fid.data.repository.FidRepository
import com.example.fid.navigation.Screen
import com.example.fid.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun GoalSetupScreen(navController: NavController) {
    val context = LocalContext.current
    val repository = remember { FidRepository(FidDatabase.getDatabase(context)) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    var step by remember { mutableIntStateOf(1) }
    var selectedGoal by remember { mutableStateOf("maintain_weight") }
    var age by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf("male") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var selectedActivityLevel by remember { mutableStateOf("moderate") }
    var targetWeight by remember { mutableStateOf("") }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(scrollState)
        ) {
            // Progress indicator
            LinearProgressIndicator(
                progress = { step / 3f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = PrimaryGreen,
                trackColor = DarkCard,
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = stringResource(R.string.setup_goals),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            Text(
                text = "Paso $step de 3",
                fontSize = 14.sp,
                color = TextSecondary
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            when (step) {
                1 -> GoalSelectionStep(selectedGoal) { selectedGoal = it }
                2 -> PersonalInfoStep(
                    age, { age = it },
                    selectedGender, { selectedGender = it },
                    height, { height = it },
                    weight, { weight = it },
                    selectedActivityLevel, { selectedActivityLevel = it }
                )
                3 -> TargetWeightStep(targetWeight) { targetWeight = it }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (step > 1) {
                    OutlinedButton(
                        onClick = { step-- },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = PrimaryGreen
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(stringResource(R.string.back))
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
                
                Button(
                    onClick = {
                        if (step < 3) {
                            step++
                        } else {
                            // Save user data
                            scope.launch {
                                try {
                                    val ageInt = age.toIntOrNull() ?: 25
                                    val heightFloat = height.toFloatOrNull() ?: 170f
                                    val weightFloat = weight.toFloatOrNull() ?: 70f
                                    val targetWeightFloat = targetWeight.toFloatOrNull()
                                    
                                    val tdee = repository.calculateTDEE(
                                        selectedGender,
                                        weightFloat,
                                        heightFloat,
                                        ageInt,
                                        selectedActivityLevel
                                    )
                                    
                                    val (protein, fat, carb) = repository.calculateMacros(tdee, selectedGoal)
                                    
                                    val user = User(
                                        email = "user@fid.com", // This should come from auth
                                        name = "User",
                                        age = ageInt,
                                        gender = selectedGender,
                                        heightCm = heightFloat,
                                        currentWeightKg = weightFloat,
                                        targetWeightKg = targetWeightFloat,
                                        activityLevel = selectedActivityLevel,
                                        goal = selectedGoal,
                                        tdee = tdee,
                                        proteinGoalG = protein,
                                        fatGoalG = fat,
                                        carbGoalG = carb
                                    )
                                    
                                    repository.insertUser(user)
                                    
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.success_goals_set),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    
                                    navController.navigate(Screen.Dashboard.route) {
                                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryGreen,
                        contentColor = DarkBackground
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        text = if (step < 3) stringResource(R.string.next) 
                               else stringResource(R.string.confirm_goals),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun GoalSelectionStep(selectedGoal: String, onGoalSelected: (String) -> Unit) {
    Column {
        Text(
            text = stringResource(R.string.whats_your_goal),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        GoalCard("lose_weight", stringResource(R.string.lose_weight), selectedGoal, onGoalSelected)
        Spacer(modifier = Modifier.height(12.dp))
        GoalCard("maintain_weight", stringResource(R.string.maintain_weight), selectedGoal, onGoalSelected)
        Spacer(modifier = Modifier.height(12.dp))
        GoalCard("gain_muscle", stringResource(R.string.gain_muscle), selectedGoal, onGoalSelected)
    }
}

@Composable
fun GoalCard(goalId: String, label: String, selectedGoal: String, onGoalSelected: (String) -> Unit) {
    val isSelected = goalId == selectedGoal
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) PrimaryGreen.copy(alpha = 0.1f) else DarkCard,
                RoundedCornerShape(16.dp)
            )
            .border(
                width = 2.dp,
                color = if (isSelected) PrimaryGreen else DarkCard,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onGoalSelected(goalId) }
            .padding(20.dp)
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) PrimaryGreen else TextPrimary
        )
    }
}

@Composable
fun PersonalInfoStep(
    age: String, onAgeChange: (String) -> Unit,
    selectedGender: String, onGenderChange: (String) -> Unit,
    height: String, onHeightChange: (String) -> Unit,
    weight: String, onWeightChange: (String) -> Unit,
    selectedActivityLevel: String, onActivityLevelChange: (String) -> Unit
) {
    Column {
        Text(
            text = stringResource(R.string.tell_us_about_you),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(
            value = age,
            onValueChange = onAgeChange,
            label = { Text(stringResource(R.string.age)) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryGreen,
                unfocusedBorderColor = TextSecondary,
                focusedLabelColor = PrimaryGreen,
                cursorColor = PrimaryGreen
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.gender),
            fontSize = 14.sp,
            color = TextSecondary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GenderButton("male", stringResource(R.string.male), selectedGender, onGenderChange, Modifier.weight(1f))
            GenderButton("female", stringResource(R.string.female), selectedGender, onGenderChange, Modifier.weight(1f))
            GenderButton("other", stringResource(R.string.other), selectedGender, onGenderChange, Modifier.weight(1f))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = height,
            onValueChange = onHeightChange,
            label = { Text(stringResource(R.string.height_cm)) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryGreen,
                unfocusedBorderColor = TextSecondary,
                focusedLabelColor = PrimaryGreen,
                cursorColor = PrimaryGreen
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = weight,
            onValueChange = onWeightChange,
            label = { Text(stringResource(R.string.current_weight_kg)) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryGreen,
                unfocusedBorderColor = TextSecondary,
                focusedLabelColor = PrimaryGreen,
                cursorColor = PrimaryGreen
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.activity_level),
            fontSize = 14.sp,
            color = TextSecondary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ActivityLevelButton("sedentary", stringResource(R.string.sedentary), selectedActivityLevel, onActivityLevelChange)
            ActivityLevelButton("light", stringResource(R.string.light), selectedActivityLevel, onActivityLevelChange)
            ActivityLevelButton("moderate", stringResource(R.string.moderate), selectedActivityLevel, onActivityLevelChange)
            ActivityLevelButton("very_active", stringResource(R.string.very_active), selectedActivityLevel, onActivityLevelChange)
            ActivityLevelButton("athlete", stringResource(R.string.athlete), selectedActivityLevel, onActivityLevelChange)
        }
    }
}

@Composable
fun GenderButton(
    genderId: String,
    label: String,
    selectedGender: String,
    onGenderChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelected = genderId == selectedGender
    
    Button(
        onClick = { onGenderChange(genderId) },
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) PrimaryGreen else DarkCard,
            contentColor = if (isSelected) DarkBackground else TextSecondary
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun ActivityLevelButton(
    levelId: String,
    label: String,
    selectedLevel: String,
    onLevelChange: (String) -> Unit
) {
    val isSelected = levelId == selectedLevel
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) PrimaryGreen.copy(alpha = 0.1f) else DarkCard,
                RoundedCornerShape(12.dp)
            )
            .border(
                width = 2.dp,
                color = if (isSelected) PrimaryGreen else DarkCard,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onLevelChange(levelId) }
            .padding(16.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) PrimaryGreen else TextPrimary
        )
    }
}

@Composable
fun TargetWeightStep(targetWeight: String, onTargetWeightChange: (String) -> Unit) {
    Column {
        Text(
            text = stringResource(R.string.target_weight_kg),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Opcional: Establece un peso objetivo para ayudarnos a personalizar mejor tu plan.",
            fontSize = 14.sp,
            color = TextSecondary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(
            value = targetWeight,
            onValueChange = onTargetWeightChange,
            label = { Text(stringResource(R.string.target_weight_kg)) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryGreen,
                unfocusedBorderColor = TextSecondary,
                focusedLabelColor = PrimaryGreen,
                cursorColor = PrimaryGreen
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
    }
}

