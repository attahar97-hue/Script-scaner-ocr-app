package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.OcrViewModel
import java.text.DecimalFormat
import kotlin.math.sqrt

data class CalcHistoryItem(val expression: String, val result: String)

data class ZakatCountryCurrency(
    val code: String,
    val name: String,
    val flag: String,
    val currencyName: String,
    val symbol: String,
    val defaultGoldPerGram: Double,
    val defaultSilverPerGram: Double
)

val WORLDWIDE_ZAKAT_CURRENCIES = listOf(
    ZakatCountryCurrency(code = "PK", name = "Pakistan (پاکستان)", flag = "🇵🇰", currencyName = "Pakistani Rupee", symbol = "PKR", defaultGoldPerGram = 24000.0, defaultSilverPerGram = 275.0),
    ZakatCountryCurrency(code = "UK", name = "United Kingdom (برطانیہ)", flag = "🇬🇧", currencyName = "British Pound", symbol = "GBP (£)", defaultGoldPerGram = 65.5, defaultSilverPerGram = 0.78),
    ZakatCountryCurrency(code = "US", name = "USA (امریکہ)", flag = "🇺🇸", currencyName = "US Dollar", symbol = "USD ($)", defaultGoldPerGram = 82.0, defaultSilverPerGram = 0.98),
    ZakatCountryCurrency(code = "CA", name = "Canada (کینیڈا)", flag = "🇨🇦", currencyName = "Canadian Dollar", symbol = "CAD (C$)", defaultGoldPerGram = 112.0, defaultSilverPerGram = 1.34),
    ZakatCountryCurrency(code = "DE", name = "Germany (جرمنی)", flag = "🇩🇪", currencyName = "Euro", symbol = "EUR (€)", defaultGoldPerGram = 77.0, defaultSilverPerGram = 0.92),
    ZakatCountryCurrency(code = "AE", name = "UAE (دبئی / امارات)", flag = "🇦🇪", currencyName = "UAE Dirham", symbol = "AED (د.إ)", defaultGoldPerGram = 301.0, defaultSilverPerGram = 3.60),
    ZakatCountryCurrency(code = "QA", name = "Qatar (قطر)", flag = "🇶🇦", currencyName = "Qatari Riyal", symbol = "QAR (ر.ق)", defaultGoldPerGram = 299.0, defaultSilverPerGram = 3.58),
    ZakatCountryCurrency(code = "IR", name = "Iran (ایران)", flag = "🇮🇷", currencyName = "Iranian Toman / Rial", symbol = "IRR / Toman", defaultGoldPerGram = 4800000.0, defaultSilverPerGram = 58000.0),
    ZakatCountryCurrency(code = "AF", name = "Afghanistan (افغانستان)", flag = "🇦🇫", currencyName = "Afghan Afghani", symbol = "AFN (؋)", defaultGoldPerGram = 5600.0, defaultSilverPerGram = 68.0),
    ZakatCountryCurrency(code = "BD", name = "Bangladesh (بنگلہ دیش)", flag = "🇧🇩", currencyName = "Bangladeshi Taka", symbol = "BDT (৳)", defaultGoldPerGram = 9800.0, defaultSilverPerGram = 120.0),
    ZakatCountryCurrency(code = "FR", name = "France (فرانس)", flag = "🇫🇷", currencyName = "Euro", symbol = "EUR (€)", defaultGoldPerGram = 77.0, defaultSilverPerGram = 0.92),
    ZakatCountryCurrency(code = "SA", name = "Saudi Arabia (سعودی عرب)", flag = "🇸🇦", currencyName = "Saudi Riyal", symbol = "SAR (ر.س)", defaultGoldPerGram = 308.0, defaultSilverPerGram = 3.70),
    ZakatCountryCurrency(code = "IN", name = "India (بھارت)", flag = "🇮🇳", currencyName = "Indian Rupee", symbol = "INR (₹)", defaultGoldPerGram = 6850.0, defaultSilverPerGram = 82.0),
    ZakatCountryCurrency(code = "TR", name = "Turkey (ترکیہ)", flag = "🇹🇷", currencyName = "Turkish Lira", symbol = "TRY (₺)", defaultGoldPerGram = 2800.0, defaultSilverPerGram = 33.5),
    ZakatCountryCurrency(code = "MY", name = "Malaysia (ملائیشیا)", flag = "🇲🇾", currencyName = "Malaysian Ringgit", symbol = "MYR (RM)", defaultGoldPerGram = 385.0, defaultSilverPerGram = 4.60),
    ZakatCountryCurrency(code = "ID", name = "Indonesia (انڈونیشیا)", flag = "🇮🇩", currencyName = "Indonesian Rupiah", symbol = "IDR (Rp)", defaultGoldPerGram = 1280000.0, defaultSilverPerGram = 15300.0),
    ZakatCountryCurrency(code = "OM", name = "Oman (عمان)", flag = "🇴🇲", currencyName = "Omani Rial", symbol = "OMR (ر.ع.)", defaultGoldPerGram = 31.6, defaultSilverPerGram = 0.38),
    ZakatCountryCurrency(code = "KW", name = "Kuwait (کویت)", flag = "🇰🇼", currencyName = "Kuwaiti Dinar", symbol = "KWD (د.ك)", defaultGoldPerGram = 25.2, defaultSilverPerGram = 0.30),
    ZakatCountryCurrency(code = "BH", name = "Bahrain (بحرین)", flag = "🇧🇭", currencyName = "Bahraini Dinar", symbol = "BHD (ب.د)", defaultGoldPerGram = 30.9, defaultSilverPerGram = 0.37),
    ZakatCountryCurrency(code = "AU", name = "Australia (آسٹریلیا)", flag = "🇦🇺", currencyName = "Australian Dollar", symbol = "AUD (A$)", defaultGoldPerGram = 126.0, defaultSilverPerGram = 1.50)
)

@Composable
fun CalculatorScreen(
    viewModel: OcrViewModel,
    modifier: Modifier = Modifier
) {
    val selectedSubTab by viewModel.selectedCalculatorSubTab.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("calculator_screen_container")
    ) {
        // Top Switcher Tabs: Professional Calculator & Zakat Calculator
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { viewModel.setCalculatorSubTab(0) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("کاروباری کیلکولیٹر (Business Calc)", fontWeight = FontWeight.Bold)
                    }
                },
                modifier = Modifier.testTag("tab_professional_calc")
            )

            Tab(
                selected = selectedSubTab == 1,
                onClick = { viewModel.setCalculatorSubTab(1) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Diamond, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF00C853))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("زکوٰۃ کیلکولیٹر (Zakat)", fontWeight = FontWeight.Bold)
                    }
                },
                modifier = Modifier.testTag("tab_zakat_calc")
            )
        }

        when (selectedSubTab) {
            0 -> ProfessionalCalculatorView(viewModel = viewModel)
            1 -> ZakatCalculatorView(viewModel = viewModel)
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 1. PROFESSIONAL CALCULATOR
// -------------------------------------------------------------------------------------------------

@Composable
fun ProfessionalCalculatorView(viewModel: OcrViewModel) {
    val context = LocalContext.current
    var expression by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("0") }
    val historyList = remember { mutableStateListOf<CalcHistoryItem>() }
    var showHistory by remember { mutableStateOf(false) }

    fun evaluate() {
        if (expression.isBlank()) return
        val calculated = evaluateMathExpression(expression)
        resultText = calculated
        historyList.add(0, CalcHistoryItem(expression, calculated))
        if (historyList.size > 25) historyList.removeLast()
    }

    fun handleKey(key: String) {
        when (key) {
            "C" -> {
                expression = ""
                resultText = "0"
            }
            "⌫" -> {
                if (expression.isNotEmpty()) {
                    expression = expression.dropLast(1)
                    resultText = if (expression.isNotBlank()) evaluateMathExpression(expression) else "0"
                }
            }
            "=" -> {
                evaluate()
            }
            "x²" -> {
                val num = resultText.toDoubleOrNull() ?: 0.0
                val sq = num * num
                val formatted = DecimalFormat("#,##0.######").format(sq)
                expression = "($expression)²"
                resultText = formatted
                historyList.add(0, CalcHistoryItem(expression, formatted))
            }
            "√" -> {
                val num = resultText.toDoubleOrNull() ?: 0.0
                if (num >= 0) {
                    val root = sqrt(num)
                    val formatted = DecimalFormat("#,##0.######").format(root)
                    expression = "√($expression)"
                    resultText = formatted
                    historyList.add(0, CalcHistoryItem(expression, formatted))
                } else {
                    resultText = "Error"
                }
            }
            "±" -> {
                if (expression.startsWith("-")) {
                    expression = expression.substring(1)
                } else if (expression.isNotEmpty()) {
                    expression = "-$expression"
                }
            }
            "π" -> {
                expression += "3.14159"
            }
            else -> {
                expression += key
                val live = evaluateMathExpression(expression)
                if (live != "Error" && live.isNotBlank()) {
                    resultText = live
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Display Screen Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.32f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                // Top Display Controls: History & Copy
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showHistory = !showHistory },
                        modifier = Modifier.testTag("btn_calc_history")
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = "History",
                            tint = if (showHistory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row {
                        // Insert into current OCR Document
                        IconButton(
                            onClick = {
                                viewModel.appendTextToDocument("\nCalculation: $expression = $resultText\n")
                                Toast.makeText(context, "Added calculation to OCR Note", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("btn_calc_insert_ocr")
                        ) {
                            Icon(Icons.Default.NoteAdd, contentDescription = "Insert into Document", tint = MaterialTheme.colorScheme.primary)
                        }

                        // Copy Result
                        IconButton(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Result", resultText))
                                Toast.makeText(context, "Copied: $resultText", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("btn_calc_copy")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Result", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // Expression & Result Output
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (expression.isBlank()) " " else expression,
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 2,
                        textAlign = TextAlign.End
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = resultText,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        textAlign = TextAlign.End
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // History Drawer
        AnimatedVisibility(visible = showHistory) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recent History", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        IconButton(
                            onClick = { historyList.clear() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear History", modifier = Modifier.size(16.dp))
                        }
                    }
                    LazyColumn {
                        items(historyList) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expression = item.result
                                        resultText = item.result
                                    }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(item.expression, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("= ${item.result}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }

        // Keypad Grid
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.68f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val rows = listOf(
                listOf("C", "⌫", "%", "÷"),
                listOf("√", "x²", "±", "×"),
                listOf("7", "8", "9", "-"),
                listOf("4", "5", "6", "+"),
                listOf("1", "2", "3", "π"),
                listOf("00", "0", ".", "=")
            )

            rows.forEach { rowKeys ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowKeys.forEach { key ->
                        val isOperator = key in listOf("÷", "×", "-", "+", "=")
                        val isSpecial = key in listOf("C", "⌫", "%", "√", "x²", "±", "π")
                        val isEquals = key == "="

                        val containerColor = when {
                            isEquals -> MaterialTheme.colorScheme.primary
                            isOperator -> MaterialTheme.colorScheme.primaryContainer
                            isSpecial -> MaterialTheme.colorScheme.surfaceVariant
                            else -> MaterialTheme.colorScheme.surface
                        }

                        val contentColor = when {
                            isEquals -> MaterialTheme.colorScheme.onPrimary
                            isOperator -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onSurface
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { handleKey(key) }
                                .testTag("btn_calc_$key"),
                            color = containerColor,
                            shape = RoundedCornerShape(14.dp),
                            shadowElevation = if (isEquals) 3.dp else 1.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = key,
                                    fontSize = if (key.length > 1 && !key.all { it.isDigit() }) 16.sp else 20.sp,
                                    fontWeight = if (isOperator || isEquals) FontWeight.Bold else FontWeight.SemiBold,
                                    color = contentColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 2. ISLAMIC ZAKAT CALCULATOR (زکوٰۃ کیلکولیٹر)
// -------------------------------------------------------------------------------------------------

@Composable
fun ZakatCalculatorView(viewModel: OcrViewModel) {
    val context = LocalContext.current

    // Selected Country & Currency (Default Pakistan PKR, with presets for UK, USA, Canada, Germany, UAE, Qatar, Iran, Afghanistan, Bangladesh, France, etc.)
    var selectedCountry by remember { mutableStateOf(WORLDWIDE_ZAKAT_CURRENCIES[0]) }
    var showCountryMenu by remember { mutableStateOf(false) }

    // Gold Details
    var goldUnit by remember { mutableStateOf("Tola") } // "Tola" or "Grams"
    var goldQuantity by remember { mutableStateOf("") }
    var goldRate by remember { mutableStateOf((selectedCountry.defaultGoldPerGram * 11.66).toLong().toString()) }

    // Silver Details
    var silverUnit by remember { mutableStateOf("Tola") }
    var silverQuantity by remember { mutableStateOf("") }
    var silverRate by remember { mutableStateOf((selectedCountry.defaultSilverPerGram * 11.66).toLong().toString()) }

    // Auto-update rates when user selects a different country
    LaunchedEffect(selectedCountry, goldUnit, silverUnit) {
        val gRate = if (goldUnit == "Grams") selectedCountry.defaultGoldPerGram else selectedCountry.defaultGoldPerGram * 11.66
        val sRate = if (silverUnit == "Grams") selectedCountry.defaultSilverPerGram else selectedCountry.defaultSilverPerGram * 11.66
        goldRate = if (gRate >= 100) gRate.toLong().toString() else String.format("%.2f", gRate)
        silverRate = if (sRate >= 100) sRate.toLong().toString() else String.format("%.2f", sRate)
    }

    // Cash & Assets
    var cashInHand by remember { mutableStateOf("") }
    var bankSavings by remember { mutableStateOf("") }
    var businessInventory by remember { mutableStateOf("") }
    var sharesInvestments by remember { mutableStateOf("") }

    // Liabilities / Debts
    var debtsPayable by remember { mutableStateOf("") }
    var unpaidBills by remember { mutableStateOf("") }

    // Computations
    val goldValue by remember {
        derivedStateOf {
            val q = goldQuantity.toDoubleOrNull() ?: 0.0
            val r = goldRate.toDoubleOrNull() ?: 0.0
            q * r
        }
    }

    val silverValue by remember {
        derivedStateOf {
            val q = silverQuantity.toDoubleOrNull() ?: 0.0
            val r = silverRate.toDoubleOrNull() ?: 0.0
            q * r
        }
    }

    val totalCashAndInvestments by remember {
        derivedStateOf {
            (cashInHand.toDoubleOrNull() ?: 0.0) +
            (bankSavings.toDoubleOrNull() ?: 0.0) +
            (businessInventory.toDoubleOrNull() ?: 0.0) +
            (sharesInvestments.toDoubleOrNull() ?: 0.0)
        }
    }

    val totalLiabilities by remember {
        derivedStateOf {
            (debtsPayable.toDoubleOrNull() ?: 0.0) +
            (unpaidBills.toDoubleOrNull() ?: 0.0)
        }
    }

    val netZakatableWealth by remember {
        derivedStateOf {
            maxOf(0.0, (goldValue + silverValue + totalCashAndInvestments) - totalLiabilities)
        }
    }

    // Silver Nisab (52.5 Tola or ~612.36 Grams of Silver)
    val silverNisabThreshold by remember {
        derivedStateOf {
            val r = silverRate.toDoubleOrNull() ?: 0.0
            if (silverUnit == "Grams") 612.36 * r else 52.5 * r
        }
    }

    val isSahibENisab by remember {
        derivedStateOf {
            netZakatableWealth >= silverNisabThreshold
        }
    }

    // Zakat is 2.5% (1/40th) of net wealth
    val zakatPayable by remember {
        derivedStateOf {
            if (isSahibENisab) netZakatableWealth * 0.025 else 0.0
        }
    }

    val df = remember { DecimalFormat("#,##0.00") }
    val currency = selectedCountry.symbol

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("zakat_calculator_column"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Country & Global Currency Selector Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ملک اور کرنسی کا انتخاب (Country & Currency)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Dropdown Trigger Box
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            onClick = { showCountryMenu = true },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_select_country_currency")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(selectedCountry.flag, fontSize = 22.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = selectedCountry.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${selectedCountry.currencyName} (${selectedCountry.symbol})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                            }
                        }

                        DropdownMenu(
                            expanded = showCountryMenu,
                            onDismissRequest = { showCountryMenu = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            WORLDWIDE_ZAKAT_CURRENCIES.forEach { country ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(country.flag, fontSize = 20.sp)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(country.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text("${country.currencyName} - ${country.symbol}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedCountry = country
                                        showCountryMenu = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Popular Country Chips
                    Text("فوری منتخب ممالک (Quick Select):", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            WORLDWIDE_ZAKAT_CURRENCIES[0], // PK
                            WORLDWIDE_ZAKAT_CURRENCIES[1], // UK
                            WORLDWIDE_ZAKAT_CURRENCIES[2], // US
                            WORLDWIDE_ZAKAT_CURRENCIES[5]  // UAE
                        ).forEach { quickCountry ->
                            FilterChip(
                                selected = selectedCountry.code == quickCountry.code,
                                onClick = { selectedCountry = quickCountry },
                                label = { Text("${quickCountry.flag} ${quickCountry.code}") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }
            }
        }

        // Hero Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Diamond, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "اسلامی حسابِ زکوٰۃ (${selectedCountry.symbol})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        // Nisab Status Badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSahibENisab) Color(0xFF00C853) else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = if (isSahibENisab) "زکوٰۃ واجب ہے (صاحبِ نصاب)" else "نصاب سے کم",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isSahibENisab) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "نصابِ چاندی: 52.5 تولہ / 612 گرام (تقریباً ${df.format(silverNisabThreshold)} $currency) - اس سے زائد سال بھر بچت پر 2.5% (چالیسواں حصہ) زکوٰۃ واجب ہوتی ہے۔",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // Section 1: Gold (سونا)
        item {
            ZakatSectionCard(
                title = "1. سونا (Gold Assets)",
                icon = Icons.Default.MonetizationOn
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = goldUnit == "Tola",
                        onClick = { goldUnit = "Tola" },
                        label = { Text("تولہ (Tola)") }
                    )
                    FilterChip(
                        selected = goldUnit == "Grams",
                        onClick = { goldUnit = "Grams" },
                        label = { Text("گرام (Grams)") }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = goldQuantity,
                        onValueChange = { goldQuantity = it },
                        label = { Text("وزن ($goldUnit)") },
                        placeholder = { Text("مثلاً 5") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_gold_qty")
                    )

                    OutlinedTextField(
                        value = goldRate,
                        onValueChange = { goldRate = it },
                        label = { Text("قیمت فی $goldUnit ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_gold_rate")
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "سونے کی کل مالیت: ${df.format(goldValue)} $currency",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Section 2: Silver (چاندی)
        item {
            ZakatSectionCard(
                title = "2. چاندی (Silver Assets)",
                icon = Icons.Default.Diamond
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = silverUnit == "Tola",
                        onClick = { silverUnit = "Tola" },
                        label = { Text("تولہ (Tola)") }
                    )
                    FilterChip(
                        selected = silverUnit == "Grams",
                        onClick = { silverUnit = "Grams" },
                        label = { Text("گرام (Grams)") }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = silverQuantity,
                        onValueChange = { silverQuantity = it },
                        label = { Text("وزن ($silverUnit)") },
                        placeholder = { Text("مثلاً 50") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_silver_qty")
                    )

                    OutlinedTextField(
                        value = silverRate,
                        onValueChange = { silverRate = it },
                        label = { Text("قیمت فی $silverUnit ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_silver_rate")
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "چاندی کی کل مالیت: ${df.format(silverValue)} $currency",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Section 3: Cash & Savings (نقدی اور بینک بیلنس)
        item {
            ZakatSectionCard(
                title = "3. نقدی و بینک ڈپازٹس (Cash & Bank)",
                icon = Icons.Default.AccountBalance
            ) {
                OutlinedTextField(
                    value = cashInHand,
                    onValueChange = { cashInHand = it },
                    label = { Text("گھر یا جیب میں موجود نقد رقم") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_cash_hand")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = bankSavings,
                    onValueChange = { bankSavings = it },
                    label = { Text("بینک اکاؤنٹس، سیونگز و پرائز بانڈز") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_bank_savings")
                )
            }
        }

        // Section 4: Business Stock (مالِ تجارت)
        item {
            ZakatSectionCard(
                title = "4. مالِ تجارت و حصص (Business Goods & Shares)",
                icon = Icons.Default.MonetizationOn
            ) {
                OutlinedTextField(
                    value = businessInventory,
                    onValueChange = { businessInventory = it },
                    label = { Text("برائے فروخت سامان کی موجودہ مالیت") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = sharesInvestments,
                    onValueChange = { sharesInvestments = it },
                    label = { Text("شئیرز و دیگر منافع بخش سرمایہ کاری") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Section 5: Deductions (قرضے و واجبات)
        item {
            ZakatSectionCard(
                title = "5. منہا ہونے والے قرضے و اخراجات (Debts)",
                icon = Icons.Default.Info
            ) {
                OutlinedTextField(
                    value = debtsPayable,
                    onValueChange = { debtsPayable = it },
                    label = { Text("فوری واجب الادا قرضے") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = unpaidBills,
                    onValueChange = { unpaidBills = it },
                    label = { Text("واجب الادا بلز یا ملازمین کی تنخواہیں") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Final Payable Zakat Card (Result)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_zakat_final_result"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "خلاصہ حسابِ زکوٰۃ (Zakat Summary)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("کل خالص مالیت (Net Wealth):", style = MaterialTheme.typography.bodyMedium)
                        Text("${df.format(netZakatableWealth)} $currency", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("نصاب کا معیار (Silver Nisab):", style = MaterialTheme.typography.bodyMedium)
                        Text("${df.format(silverNisabThreshold)} $currency", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "واجب الادا زکوٰۃ (2.5%)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "چالیسواں حصہ (1/40th of net wealth)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "${df.format(zakatPayable)} $currency",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = if (isSahibENisab) Color(0xFF00C853) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Copy & Reset Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val report = """
                                    --- اسلامی حسابِ زکوٰۃ رپورٹ (Zakat Report) ---
                                    ملک و کرنسی: ${selectedCountry.name} (${selectedCountry.currencyName} - $currency)
                                    سونے کی مالیت: ${df.format(goldValue)} $currency
                                    چاندی کی مالیت: ${df.format(silverValue)} $currency
                                    نقد و بینک بیلنس: ${df.format(totalCashAndInvestments)} $currency
                                    منہا قرضے: ${df.format(totalLiabilities)} $currency
                                    خالص مالیت: ${df.format(netZakatableWealth)} $currency
                                    نصاب حد: ${df.format(silverNisabThreshold)} $currency
                                    زکوٰۃ واجب الادا (2.5%): ${df.format(zakatPayable)} $currency
                                """.trimIndent()

                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Zakat Report", report))
                                Toast.makeText(context, "مکمل زکوٰۃ رپورٹ کاپی ہو گئی", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_copy_zakat_report"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("کاپی رپورٹ")
                        }

                        Button(
                            onClick = {
                                goldQuantity = ""
                                silverQuantity = ""
                                cashInHand = ""
                                bankSavings = ""
                                businessInventory = ""
                                sharesInvestments = ""
                                debtsPayable = ""
                                unpaidBills = ""
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ری سیٹ (Clear)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ZakatSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Mathematical Expression Parser
// -------------------------------------------------------------------------------------------------

private fun evaluateMathExpression(expr: String): String {
    return try {
        val sanitized = expr
            .replace("×", "*")
            .replace("÷", "/")
            .replace("%", "*0.01")
            .replace(" ", "")

        if (sanitized.isBlank()) return "0"

        val value = evalSimpleExpr(sanitized)
        val df = DecimalFormat("#,##0.######")
        df.format(value)
    } catch (e: Exception) {
        "Error"
    }
}

private fun evalSimpleExpr(str: String): Double {
    var pos = -1
    var ch = 0

    fun nextChar() {
        ch = if (++pos < str.length) str[pos].code else -1
    }

    fun eat(charToEat: Int): Boolean {
        while (ch == ' '.code) nextChar()
        if (ch == charToEat) {
            nextChar()
            return true
        }
        return false
    }

    fun parseFactor(): Double {
        if (eat('+'.code)) return parseFactor()
        if (eat('-'.code)) return -parseFactor()

        var x: Double
        val startPos = pos
        if (eat('('.code)) {
            x = parseFactor()
            eat(')'.code)
        } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
            while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
            x = str.substring(startPos, pos).toDouble()
        } else {
            throw RuntimeException("Unexpected: " + ch.toChar())
        }
        return x
    }

    fun parseTerm(): Double {
        var x = parseFactor()
        while (true) {
            when {
                eat('*'.code) -> x *= parseFactor()
                eat('/'.code) -> {
                    val d = parseFactor()
                    if (d == 0.0) throw ArithmeticException("Divide by zero")
                    x /= d
                }
                else -> return x
            }
        }
    }

    fun parseExpression(): Double {
        var x = parseTerm()
        while (true) {
            when {
                eat('+'.code) -> x += parseTerm()
                eat('-'.code) -> x -= parseTerm()
                else -> return x
            }
        }
    }

    nextChar()
    val result = parseExpression()
    if (pos < str.length) throw RuntimeException("Unexpected: " + ch.toChar())
    return result
}
