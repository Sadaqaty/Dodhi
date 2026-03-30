package com.dodhi.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.dodhi.ui.theme.GrassGreen
import com.dodhi.ui.theme.NatureGreen

@Composable
fun PremiumTextField(
    value: String, 
    onValueChange: (String) -> Unit, 
    label: String, 
    isNumber: Boolean = false,
    isError: Boolean = false,
    errorText: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        isError = isError,
        supportingText = errorText?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
        shape = MaterialTheme.shapes.medium,
        keyboardOptions = if (isNumber) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GrassGreen,
            unfocusedBorderColor = NatureGreen.copy(alpha = 0.5f),
            focusedLabelColor = NatureGreen
        )
    )
}
