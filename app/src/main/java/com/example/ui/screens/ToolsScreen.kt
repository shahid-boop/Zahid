package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Stairs
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.i18n.AcademyStrings
import com.example.model.ArchitectureCalculators
import com.example.model.ByLawsCatalog
import com.example.model.CadShortcutsCatalog
import com.example.ui.components.AcademySearchBar
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.NavyTint
import com.example.ui.theme.OrangeTint
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SuccessGreen
import java.text.NumberFormat
import java.util.Locale

@Composable
fun StudioToolsScreen(
  strings: AcademyStrings,
  onBrowsePlotMaps: (String) -> Unit = {},
) {
  var selectedTab by remember { mutableIntStateOf(0) }
  val tabs = listOf(
    strings.plotConverterTab,
    strings.materialEstimatorTab,
    strings.stairCalculatorTab,
    strings.byLawsTab,
    strings.costAndScaleTab,
    strings.shortcutsTab,
  )

  Box(modifier = Modifier.fillMaxSize().testTag("tools_screen"), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 840.dp).fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 96.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          Icon(Icons.Filled.Calculate, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(28.dp))
          Text(
            text = strings.toolsTitle,
            style = MaterialTheme.typography.headlineSmall,
            color = PrimaryNavy,
            fontWeight = FontWeight.Bold,
          )
        }
      }

      item {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(tabs.indices.toList()) { idx ->
            FilterChip(
              selected = selectedTab == idx,
              onClick = { selectedTab = idx },
              label = { Text(tabs[idx]) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = PrimaryNavy,
                selectedLabelColor = PureWhite,
              ),
              modifier = Modifier.testTag("tools_tab_$idx"),
            )
          }
        }
      }

      item {
        when (selectedTab) {
          0 -> PlotConverterCard()
          1 -> MaterialEstimatorCard()
          2 -> StaircaseCalculatorCard()
          3 -> ByLawsReferenceCard(onBrowsePlotMaps = onBrowsePlotMaps)
          4 -> CostAndScaleCard()
          else -> CadShortcutsCard()
        }
      }
    }
  }
}

@Composable
private fun PlotConverterCard() {
  var marlaInput by remember { mutableStateOf("5") }
  var sqFtPerMarla by remember { mutableStateOf(225.0) }
  val marlaValue = marlaInput.toDoubleOrNull() ?: 0.0
  val result = remember(marlaValue, sqFtPerMarla) {
    ArchitectureCalculators.convertFromMarla(marlaValue, sqFtPerMarla)
  }

  val presets = listOf(
    Triple("3 Marla", "3", "20' × 33.75'"),
    Triple("5 Marla", "5", "25' × 45'"),
    Triple("7 Marla", "7", "30' × 52.5'"),
    Triple("10 Marla", "10", "35' × 65'"),
    Triple("1 Kanal", "20", "50' × 90'"),
  )

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, BorderSubtle),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Filled.Architecture, contentDescription = null, tint = PrimaryNavy)
        Text("Plot Area & Marla Converter", style = MaterialTheme.typography.titleMedium, color = PrimaryNavy)
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        FilterChip(
          selected = sqFtPerMarla == 225.0,
          onClick = { sqFtPerMarla = 225.0 },
          label = { Text("225 sq.ft / Marla (Modern)") },
          colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AccentOrange, selectedLabelColor = PureWhite),
          modifier = Modifier.weight(1f),
        )
        FilterChip(
          selected = sqFtPerMarla == 272.25,
          onClick = { sqFtPerMarla = 272.25 },
          label = { Text("272.25 sq.ft (Standard)") },
          colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AccentOrange, selectedLabelColor = PureWhite),
          modifier = Modifier.weight(1f),
        )
      }

      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(presets) { (label, valStr, dims) ->
          FilterChip(
            selected = marlaInput == valStr,
            onClick = { marlaInput = valStr },
            label = { Text("$label ($dims)") },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryNavy, selectedLabelColor = PureWhite),
          )
        }
      }

      OutlinedTextField(
        value = marlaInput,
        onValueChange = { marlaInput = it },
        label = { Text("Plot Size in Marla") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().testTag("plot_marla_input"),
      )

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        MetricBox(
          label = "Square Feet",
          value = String.format(Locale.US, "%.1f sq.ft", result.sqFeet),
          modifier = Modifier.weight(1f),
        )
        MetricBox(
          label = "Square Meters",
          value = String.format(Locale.US, "%.2f m²", result.sqMeters),
          modifier = Modifier.weight(1f),
        )
      }
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        MetricBox(
          label = "Marla",
          value = String.format(Locale.US, "%.2f", result.marla),
          modifier = Modifier.weight(1f),
        )
        MetricBox(
          label = "Kanal",
          value = String.format(Locale.US, "%.3f", result.kanal),
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

@Composable
private fun MaterialEstimatorCard() {
  var coveredAreaInput by remember { mutableStateOf("1850") }
  var floors by remember { mutableIntStateOf(1) }
  val area = coveredAreaInput.toDoubleOrNull() ?: 0.0
  val est = remember(area, floors) { ArchitectureCalculators.estimateGreyStructure(area, floors) }

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, BorderSubtle),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text("Grey Structure Material Estimator (BOQ)", style = MaterialTheme.typography.titleMedium, color = PrimaryNavy)
      OutlinedTextField(
        value = coveredAreaInput,
        onValueChange = { coveredAreaInput = it },
        label = { Text("Covered Area per Floor (sq.ft)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().testTag("boq_area_input"),
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Floors:", style = MaterialTheme.typography.labelLarge)
        (1..4).forEach { count ->
          FilterChip(
            selected = floors == count,
            onClick = { floors = count },
            label = { Text("$count") },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryNavy, selectedLabelColor = PureWhite),
          )
        }
      }
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        MetricBox("Bricks (Approx.)", "${est.bricksCount}", Modifier.weight(1f))
        MetricBox("Cement Bags", "${est.cementBags}", Modifier.weight(1f))
      }
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        MetricBox("Sand (cu.ft)", "${est.sandCuFt}", Modifier.weight(1f))
        MetricBox("Crush (cu.ft)", "${est.crushCuFt}", Modifier.weight(1f))
      }
      MetricBox("Steel Rebar (Approx. kg)", "${est.steelKg} kg (${String.format(Locale.US, "%.2f", est.steelKg / 1000.0)} Tons)", Modifier.fillMaxWidth())
      Text(
        text = "Educational architectural thumb-rule estimate for ${est.totalCoveredSqFt.toInt()} sq.ft total covered area. Verify final quantities with your structural engineer.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun StaircaseCalculatorCard() {
  var floorHeightInput by remember { mutableStateOf("11.0") }
  var riserInput by remember { mutableStateOf("7.0") }
  val height = floorHeightInput.toDoubleOrNull() ?: 11.0
  val targetRiser = riserInput.toDoubleOrNull() ?: 7.0
  val stair = remember(height, targetRiser) {
    ArchitectureCalculators.calculateStaircase(height, targetRiser)
  }

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, BorderSubtle),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Filled.Stairs, contentDescription = null, tint = PrimaryNavy)
        Text("Staircase Riser & Tread Calculator", style = MaterialTheme.typography.titleMedium, color = PrimaryNavy)
      }
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
          value = floorHeightInput,
          onValueChange = { floorHeightInput = it },
          label = { Text("Floor Height (ft)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
          value = riserInput,
          onValueChange = { riserInput = it },
          label = { Text("Target Riser (in)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.weight(1f),
        )
      }

      Surface(color = NavyTint, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(120.dp)) {
        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
          val steps = stair.riserCount.coerceIn(4, 18)
          val dx = size.width / steps
          val dy = size.height / steps
          val path = Path().apply {
            moveTo(0f, size.height)
            for (i in 0 until steps) {
              val x = i * dx
              val y = size.height - i * dy
              lineTo(x, y - dy)
              lineTo(x + dx, y - dy)
            }
          }
          drawPath(path, color = PrimaryNavy, style = Stroke(width = 5f, cap = StrokeCap.Round))
          drawLine(
            color = AccentOrange,
            start = Offset(0f, size.height),
            end = Offset(size.width, 0f),
            strokeWidth = 3f,
          )
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        MetricBox("Risers / Treads", "${stair.riserCount} R / ${stair.treadCount} T", Modifier.weight(1f))
        MetricBox("Exact Riser", String.format(Locale.US, "%.2f\"", stair.exactRiserInches), Modifier.weight(1f))
      }
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        MetricBox("Recommended Tread", String.format(Locale.US, "%.2f\"", stair.recommendedTreadInches), Modifier.weight(1f))
        MetricBox("Total Run", String.format(Locale.US, "%.2f ft", stair.totalRunFeet), Modifier.weight(1f))
      }
    }
  }
}

@Composable
private fun ByLawsReferenceCard(
  onBrowsePlotMaps: (String) -> Unit,
) {
  var selectedPlot by remember { mutableStateOf("5 Marla") }
  val bylaw = remember(selectedPlot) {
    ByLawsCatalog.standards.firstOrNull { it.plotSize == selectedPlot } ?: ByLawsCatalog.standards.first()
  }

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, BorderSubtle),
    modifier = Modifier.fillMaxWidth().testTag("tools_bylaws_card"),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Filled.Gavel, contentDescription = null, tint = PrimaryNavy)
        Text("Residential Planning By-Laws & Setbacks", style = MaterialTheme.typography.titleMedium, color = PrimaryNavy)
      }

      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(ByLawsCatalog.standards) { item ->
          FilterChip(
            selected = selectedPlot == item.plotSize,
            onClick = { selectedPlot = item.plotSize },
            label = { Text(item.plotSize) },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryNavy, selectedLabelColor = PureWhite),
            modifier = Modifier.testTag("bylaw_chip_${item.plotSize.replace(" ", "_")}"),
          )
        }
      }

      Surface(color = NavyTint, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text("Standard Dimensions", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(bylaw.dimensions, style = MaterialTheme.typography.titleMedium, color = PrimaryNavy, fontWeight = FontWeight.Bold)
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        MetricBox("Front Setback", bylaw.frontSetback, Modifier.weight(1f))
        MetricBox("Rear Setback", bylaw.rearSetback, Modifier.weight(1f))
      }
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        MetricBox("Side Setback", bylaw.sideSetback, Modifier.weight(1f))
        MetricBox("Max Coverage", bylaw.maxCoverage, Modifier.weight(1f))
      }
      MetricBox("Max Building Height", bylaw.maxHeight, Modifier.fillMaxWidth())

      Surface(color = OrangeTint.copy(alpha = 0.65f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text("Recommended Room Clearances", style = MaterialTheme.typography.labelLarge, color = AccentOrange)
          Text(bylaw.minRoomSizes, style = MaterialTheme.typography.bodySmall, color = PrimaryNavy)
        }
      }

      Button(
        onClick = { onBrowsePlotMaps(bylaw.plotSize) },
        colors = ButtonDefaults.buttonColors(containerColor = AccentOrange, contentColor = PureWhite),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().testTag("bylaws_browse_maps_btn"),
      ) {
        Icon(Icons.Filled.Map, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Browse ${bylaw.plotSize} Maps (20 Designs)")
      }
    }
  }
}

@Composable
private fun CostAndScaleCard() {
  var areaInput by remember { mutableStateOf("1850") }
  var finishTier by remember { mutableStateOf("STANDARD") }
  var greyRate by remember { mutableLongStateOf(2650L) }
  var finishRate by remember { mutableLongStateOf(3150L) }

  val area = areaInput.toDoubleOrNull() ?: 0.0
  val cost = remember(area, greyRate, finishRate) {
    ArchitectureCalculators.estimateConstructionCost(area, greyRate, finishRate)
  }

  var realFtInput by remember { mutableStateOf("25.0") }
  var selectedScale by remember { mutableIntStateOf(100) }
  val scaleResult = remember(realFtInput, selectedScale) {
    ArchitectureCalculators.convertDrawingScale(realFtInput.toDoubleOrNull() ?: 0.0, selectedScale)
  }
  val nf = remember { NumberFormat.getNumberInstance(Locale.US) }

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, BorderSubtle),
      modifier = Modifier.fillMaxWidth().testTag("tools_cost_card"),
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(Icons.Filled.Payments, contentDescription = null, tint = PrimaryNavy)
          Text("House Construction Cost Estimator (PKR)", style = MaterialTheme.typography.titleMedium, color = PrimaryNavy)
        }

        OutlinedTextField(
          value = areaInput,
          onValueChange = { areaInput = it },
          label = { Text("Total Covered Area (sq.ft)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().testTag("cost_area_input"),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
          listOf(
            Triple("STANDARD", "Standard", 2650L to 3150L),
            Triple("DELUXE", "Deluxe", 2950L to 4150L),
            Triple("LUXURY", "Luxury", 3400L to 5600L),
          ).forEach { (key, label, rates) ->
            FilterChip(
              selected = finishTier == key,
              onClick = {
                finishTier = key
                greyRate = rates.first
                finishRate = rates.second
              },
              label = { Text(label) },
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryNavy, selectedLabelColor = PureWhite),
              modifier = Modifier.weight(1f),
            )
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
          MetricBox("Grey Structure", "PKR ${nf.format(cost.greyStructurePkr)}", Modifier.weight(1f))
          MetricBox("Finishing", "PKR ${nf.format(cost.finishingPkr)}", Modifier.weight(1f))
        }
        MetricBox("Estimated Turnkey Total", "PKR ${nf.format(cost.turnkeyTotalPkr)}", Modifier.fillMaxWidth())

        // Split bar
        val greyFrac = if (cost.turnkeyTotalPkr > 0L) (cost.greyStructurePkr.toFloat() / cost.turnkeyTotalPkr.toFloat()).coerceIn(0.1f, 0.9f) else 0.45f
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Row(modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(6.dp))) {
            Box(modifier = Modifier.weight(greyFrac).height(10.dp).background(PrimaryNavy))
            Box(modifier = Modifier.weight(1f - greyFrac).height(10.dp).background(AccentOrange))
          }
          Text(
            text = "Grey ${(greyFrac * 100).toInt()}% (PKR $greyRate/sq.ft) • Finishing ${((1f - greyFrac) * 100).toInt()}% (PKR $finishRate/sq.ft)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, BorderSubtle),
      modifier = Modifier.fillMaxWidth().testTag("tools_scale_card"),
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(Icons.Filled.Straighten, contentDescription = null, tint = AccentOrange)
          Text("Architectural Drawing Scale Converter", style = MaterialTheme.typography.titleMedium, color = PrimaryNavy)
        }
        OutlinedTextField(
          value = realFtInput,
          onValueChange = { realFtInput = it },
          label = { Text("Real Building Dimension (feet)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(listOf(25, 50, 100, 200, 500)) { scale ->
            FilterChip(
              selected = selectedScale == scale,
              onClick = { selectedScale = scale },
              label = { Text("1:$scale") },
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryNavy, selectedLabelColor = PureWhite),
            )
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
          MetricBox("Paper Length (cm)", String.format(Locale.US, "%.2f cm", scaleResult.paperLengthCm), Modifier.weight(1f))
          MetricBox("Paper Length (in)", String.format(Locale.US, "%.2f in", scaleResult.paperLengthInches), Modifier.weight(1f))
        }
      }
    }
  }
}

@Composable
private fun CadShortcutsCard() {
  var softwareFilter by remember { mutableStateOf("ALL") }
  var searchQuery by remember { mutableStateOf("") }

  val filtered = remember(softwareFilter, searchQuery) {
    val q = searchQuery.trim().lowercase()
    CadShortcutsCatalog.items.filter { sc ->
      val matchSw = softwareFilter == "ALL" || sc.software == softwareFilter
      val matchQ = q.isEmpty() || sc.key.lowercase().contains(q) || sc.command.lowercase().contains(q) || sc.purpose.lowercase().contains(q)
      matchSw && matchQ
    }
  }

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, BorderSubtle),
    modifier = Modifier.fillMaxWidth().testTag("tools_shortcuts_card"),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Filled.Keyboard, contentDescription = null, tint = PrimaryNavy)
        Text("CAD & 3D Studio Shortcuts Cheat-Sheet", style = MaterialTheme.typography.titleMedium, color = PrimaryNavy)
      }

      AcademySearchBar(
        query = searchQuery,
        hint = "Search command (TRIM, OFFSET, V-Ray, Push...)",
        onQueryChange = { searchQuery = it },
      )

      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(listOf("ALL" to "All Apps", "AutoCAD" to "AutoCAD", "3ds Max" to "3ds Max", "SketchUp" to "SketchUp")) { (key, label) ->
          FilterChip(
            selected = softwareFilter == key,
            onClick = { softwareFilter = key },
            label = { Text(label) },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryNavy, selectedLabelColor = PureWhite),
          )
        }
      }

      filtered.forEach { sc ->
        Surface(
          color = NavyTint.copy(alpha = 0.55f),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Surface(color = PrimaryNavy, shape = RoundedCornerShape(8.dp)) {
              Text(
                text = sc.key,
                style = MaterialTheme.typography.labelLarge,
                color = PureWhite,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = "${sc.command} • ${sc.software}",
                style = MaterialTheme.typography.titleSmall,
                color = PrimaryNavy,
                fontWeight = FontWeight.SemiBold,
              )
              Text(
                text = sc.purpose,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun MetricBox(label: String, value: String, modifier: Modifier = Modifier) {
  Surface(
    color = OrangeTint.copy(alpha = 0.65f),
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, AccentOrange.copy(alpha = 0.25f)),
    modifier = modifier,
  ) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
      Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Text(value, style = MaterialTheme.typography.titleMedium, color = PrimaryNavy, fontWeight = FontWeight.Bold)
    }
  }
}
