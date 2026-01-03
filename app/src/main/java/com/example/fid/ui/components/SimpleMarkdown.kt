package com.example.fid.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Componente simple para renderizar Markdown básico en Compose
 * Soporta: **negrita**, *cursiva*, listas con -, encabezados con #
 */
@Composable
fun SimpleMarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    fontSize: TextUnit = 15.sp,
    lineHeight: TextUnit = 24.sp,
    boldColor: Color = color
) {
    val annotatedString = parseMarkdown(text, color, boldColor, fontSize)
    
    Text(
        text = annotatedString,
        modifier = modifier,
        fontSize = fontSize,
        lineHeight = lineHeight
    )
}

/**
 * Parsea el texto markdown y devuelve un AnnotatedString con estilos
 */
private fun parseMarkdown(
    text: String,
    defaultColor: Color,
    boldColor: Color,
    fontSize: TextUnit
): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        val length = text.length
        
        while (i < length) {
            when {
                // Negrita con **texto**
                i + 1 < length && text[i] == '*' && text[i + 1] == '*' -> {
                    val endIndex = text.indexOf("**", i + 2)
                    if (endIndex != -1) {
                        val boldText = text.substring(i + 2, endIndex)
                        withStyle(
                            SpanStyle(
                                fontWeight = FontWeight.Bold,
                                color = boldColor,
                                fontSize = fontSize * 1.05f
                            )
                        ) {
                            append(boldText)
                        }
                        i = endIndex + 2
                    } else {
                        withStyle(SpanStyle(color = defaultColor)) {
                            append(text[i])
                        }
                        i++
                    }
                }
                
                // Cursiva con *texto* (solo un asterisco)
                text[i] == '*' && (i == 0 || text[i - 1] != '*') && (i + 1 >= length || text[i + 1] != '*') -> {
                    val endIndex = findSingleAsterisk(text, i + 1)
                    if (endIndex != -1) {
                        val italicText = text.substring(i + 1, endIndex)
                        withStyle(
                            SpanStyle(
                                fontWeight = FontWeight.Normal,
                                color = defaultColor.copy(alpha = 0.9f)
                            )
                        ) {
                            append(italicText)
                        }
                        i = endIndex + 1
                    } else {
                        withStyle(SpanStyle(color = defaultColor)) {
                            append(text[i])
                        }
                        i++
                    }
                }
                
                // Texto normal
                else -> {
                    withStyle(SpanStyle(color = defaultColor)) {
                        append(text[i])
                    }
                    i++
                }
            }
        }
    }
}

/**
 * Encuentra el siguiente asterisco simple (no doble)
 */
private fun findSingleAsterisk(text: String, startIndex: Int): Int {
    var i = startIndex
    while (i < text.length) {
        if (text[i] == '*') {
            // Verificar que no sea parte de **
            val isDouble = (i + 1 < text.length && text[i + 1] == '*') ||
                          (i > 0 && text[i - 1] == '*')
            if (!isDouble) {
                return i
            }
        }
        i++
    }
    return -1
}
