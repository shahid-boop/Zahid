package com.example.ui.viewers

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.i18n.AcademyStrings
import com.example.model.LocalDownloadedFile
import com.example.model.ResourceItem
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.PureWhite
import com.example.viewmodel.ViewerTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

@Composable
fun ResourceViewerDialog(
  target: ViewerTarget,
  strings: AcademyStrings,
  onClose: () -> Unit,
  onDownloadResource: (ResourceItem) -> Unit,
  onOpenExternal: (LocalDownloadedFile) -> Unit,
) {
  Dialog(
    onDismissRequest = onClose,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    when (target) {
      is ViewerTarget.Pdf -> InAppPdfViewer(target.title, target.file, onClose)
      is ViewerTarget.Video -> InAppVideoPlayer(target.title, target.file, onClose)
      is ViewerTarget.ImageGallery -> InAppImageGalleryViewer(target.files, target.initialIndex, onClose)
      is ViewerTarget.Dxf -> InAppDxfViewer(target.title, target.file, onClose)
      is ViewerTarget.Unsupported -> UnsupportedCadFallbackDialog(
        resource = target.resource,
        localFile = target.localFile,
        strings = strings,
        onClose = onClose,
        onDownload = { onDownloadResource(target.resource) },
        onOpenExternal = onOpenExternal,
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InAppPdfViewer(
  title: String,
  pdfFile: File,
  onClose: () -> Unit,
) {
  var pageCount by remember { mutableIntStateOf(0) }
  var currentPage by remember { mutableIntStateOf(0) }
  var zoomScale by remember { mutableFloatStateOf(1f) }
  var isFullScreen by remember { mutableStateOf(false) }
  var bitmap by remember { mutableStateOf<Bitmap?>(null) }
  var errorText by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(pdfFile, currentPage, zoomScale) {
    withContext(Dispatchers.IO) {
      try {
        val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
        pfd.use { descriptor ->
          val renderer = PdfRenderer(descriptor)
          renderer.use { pdf ->
            pageCount = pdf.pageCount
            val safeIndex = currentPage.coerceIn(0, (pdf.pageCount - 1).coerceAtLeast(0))
            if (pdf.pageCount > 0) {
              pdf.openPage(safeIndex).use { page ->
                val targetWidth = (page.width * 2 * zoomScale.coerceIn(0.8f, 2.5f)).toInt().coerceAtLeast(200)
                val targetHeight = (page.height * 2 * zoomScale.coerceIn(0.8f, 2.5f)).toInt().coerceAtLeast(200)
                val bmp = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                bmp.eraseColor(android.graphics.Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap = bmp
              }
            }
          }
        }
      } catch (e: Exception) {
        errorText = e.localizedMessage ?: "Unable to render PDF"
      }
    }
  }

  Scaffold(
    containerColor = Color(0xFF1A2233),
    topBar = {
      if (!isFullScreen) {
        TopAppBar(
          colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryNavy, titleContentColor = PureWhite, actionIconContentColor = PureWhite, navigationIconContentColor = PureWhite),
          navigationIcon = {
            IconButton(onClick = onClose, modifier = Modifier.testTag("viewer_close_btn")) {
              Icon(Icons.Filled.Close, contentDescription = "Close")
            }
          },
          title = { Text(title, maxLines = 1) },
          actions = {
            IconButton(onClick = { zoomScale = (zoomScale - 0.25f).coerceAtLeast(0.8f) }) {
              Icon(Icons.Filled.ZoomOut, contentDescription = "Zoom Out")
            }
            IconButton(onClick = { zoomScale = (zoomScale + 0.25f).coerceAtMost(3.0f) }) {
              Icon(Icons.Filled.ZoomIn, contentDescription = "Zoom In")
            }
            IconButton(onClick = { isFullScreen = true }) {
              Icon(Icons.Filled.Fullscreen, contentDescription = "Fullscreen")
            }
          },
        )
      }
    },
    bottomBar = {
      if (!isFullScreen && pageCount > 0) {
        Surface(color = PrimaryNavy, modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            OutlinedButton(
              onClick = { if (currentPage > 0) currentPage-- },
              enabled = currentPage > 0,
              colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
            ) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous")
              Spacer(Modifier.width(4.dp))
              Text("Prev")
            }
            Text(
              text = "Page ${currentPage + 1} / $pageCount",
              color = PureWhite,
              style = MaterialTheme.typography.titleSmall,
            )
            OutlinedButton(
              onClick = { if (currentPage < pageCount - 1) currentPage++ },
              enabled = currentPage < pageCount - 1,
              colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
            ) {
              Text("Next")
              Spacer(Modifier.width(4.dp))
              Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
            }
          }
        }
      }
    },
  ) { inner ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(inner)
        .pointerInput(Unit) {
          detectTransformGestures { _, _, zoom, _ ->
            zoomScale = (zoomScale * zoom).coerceIn(0.8f, 3.0f)
          }
        },
      contentAlignment = Alignment.Center,
    ) {
      if (errorText != null) {
        Text(errorText ?: "", color = PureWhite)
      } else {
        bitmap?.let { bmp ->
          Column(
            modifier = Modifier
              .fillMaxSize()
              .verticalScroll(rememberScrollState())
              .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Image(
              bitmap = bmp.asImageBitmap(),
              contentDescription = "PDF Page ${currentPage + 1}",
              contentScale = ContentScale.FillWidth,
              modifier = Modifier.fillMaxWidth().graphicsLayer(scaleX = 1f, scaleY = 1f),
            )
          }
        }
      }

      if (isFullScreen) {
        IconButton(
          onClick = { isFullScreen = false },
          modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(PrimaryNavy.copy(alpha = 0.7f), RoundedCornerShape(50)),
        ) {
          Icon(Icons.Filled.FullscreenExit, contentDescription = "Exit Fullscreen", tint = PureWhite)
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InAppVideoPlayer(
  title: String,
  videoFile: File,
  onClose: () -> Unit,
) {
  val context = LocalContext.current
  val exoPlayer = remember(videoFile) {
    ExoPlayer.Builder(context).build().apply {
      setMediaItem(MediaItem.fromUri(Uri.fromFile(videoFile)))
      prepare()
      playWhenReady = true
    }
  }

  var isPlaying by remember { mutableStateOf(true) }
  var currentPosMs by remember { mutableLongStateOf(0L) }
  var durationMs by remember { mutableLongStateOf(0L) }
  var isFullScreen by remember { mutableStateOf(false) }

  DisposableEffect(exoPlayer) {
    onDispose {
      exoPlayer.release()
    }
  }

  LaunchedEffect(exoPlayer) {
    while (true) {
      isPlaying = exoPlayer.isPlaying
      currentPosMs = exoPlayer.currentPosition.coerceAtLeast(0L)
      durationMs = exoPlayer.duration.coerceAtLeast(0L)
      delay(250)
    }
  }

  Scaffold(
    containerColor = Color.Black,
    topBar = {
      if (!isFullScreen) {
        TopAppBar(
          colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryNavy, titleContentColor = PureWhite, actionIconContentColor = PureWhite, navigationIconContentColor = PureWhite),
          navigationIcon = {
            IconButton(onClick = onClose, modifier = Modifier.testTag("viewer_close_btn")) {
              Icon(Icons.Filled.Close, contentDescription = "Close")
            }
          },
          title = { Text(title, maxLines = 1) },
          actions = {
            IconButton(onClick = { isFullScreen = true }) {
              Icon(Icons.Filled.Fullscreen, contentDescription = "Fullscreen")
            }
          },
        )
      }
    },
  ) { inner ->
    Column(
      modifier = Modifier.fillMaxSize().padding(inner),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
        AndroidView(
          factory = { ctx ->
            PlayerView(ctx).apply {
              player = exoPlayer
              useController = false
            }
          },
          modifier = Modifier.fillMaxSize(),
        )
        if (isFullScreen) {
          IconButton(
            onClick = { isFullScreen = false },
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(PrimaryNavy.copy(alpha = 0.7f), RoundedCornerShape(50)),
          ) {
            Icon(Icons.Filled.FullscreenExit, contentDescription = "Exit Fullscreen", tint = PureWhite)
          }
        }
      }

      Surface(color = PrimaryNavy, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
          val maxVal = durationMs.coerceAtLeast(1L).toFloat()
          Slider(
            value = currentPosMs.toFloat().coerceIn(0f, maxVal),
            onValueChange = { pos -> exoPlayer.seekTo(pos.toLong()) },
            valueRange = 0f..maxVal,
            colors = SliderDefaults.colors(thumbColor = AccentOrange, activeTrackColor = AccentOrange),
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(formatMillis(currentPosMs), color = PureWhite, style = MaterialTheme.typography.labelSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
              IconButton(onClick = { exoPlayer.seekTo((exoPlayer.currentPosition - 10_000).coerceAtLeast(0L)) }) {
                Icon(Icons.Filled.Replay10, contentDescription = "Seek Backward", tint = PureWhite)
              }
              IconButton(
                onClick = {
                  if (exoPlayer.isPlaying) exoPlayer.pause() else {
                    if (exoPlayer.playbackState == androidx.media3.common.Player.STATE_ENDED) exoPlayer.seekTo(0L)
                    exoPlayer.play()
                  }
                },
                modifier = Modifier.size(48.dp).background(AccentOrange, RoundedCornerShape(50)),
              ) {
                Icon(
                  imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                  contentDescription = if (isPlaying) "Pause" else "Play",
                  tint = PureWhite,
                )
              }
              IconButton(onClick = { exoPlayer.seekTo((exoPlayer.currentPosition + 10_000).coerceAtMost(durationMs.coerceAtLeast(0L))) }) {
                Icon(Icons.Filled.Forward10, contentDescription = "Seek Forward", tint = PureWhite)
              }
            }
            Text(formatMillis(durationMs), color = PureWhite, style = MaterialTheme.typography.labelSmall)
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InAppImageGalleryViewer(
  images: List<Pair<String, File>>,
  initialIndex: Int,
  onClose: () -> Unit,
) {
  var index by remember { mutableIntStateOf(initialIndex.coerceIn(0, (images.size - 1).coerceAtLeast(0))) }
  var scale by remember { mutableFloatStateOf(1f) }
  var offset by remember { mutableStateOf(Offset.Zero) }
  var isFullScreen by remember { mutableStateOf(false) }

  val current = images.getOrNull(index)

  Scaffold(
    containerColor = Color.Black,
    topBar = {
      if (!isFullScreen) {
        TopAppBar(
          colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryNavy, titleContentColor = PureWhite, actionIconContentColor = PureWhite, navigationIconContentColor = PureWhite),
          navigationIcon = {
            IconButton(onClick = onClose, modifier = Modifier.testTag("viewer_close_btn")) {
              Icon(Icons.Filled.Close, contentDescription = "Close")
            }
          },
          title = { Text(current?.first ?: "Image", maxLines = 1) },
          actions = {
            IconButton(onClick = { scale = (scale - 0.3f).coerceAtLeast(1f); if (scale == 1f) offset = Offset.Zero }) {
              Icon(Icons.Filled.ZoomOut, contentDescription = "Zoom Out")
            }
            IconButton(onClick = { scale = (scale + 0.3f).coerceAtMost(4f) }) {
              Icon(Icons.Filled.ZoomIn, contentDescription = "Zoom In")
            }
            IconButton(onClick = { isFullScreen = true }) {
              Icon(Icons.Filled.Fullscreen, contentDescription = "Fullscreen")
            }
          },
        )
      }
    },
    bottomBar = {
      if (!isFullScreen && images.size > 1) {
        Surface(color = PrimaryNavy, modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            IconButton(onClick = { if (index > 0) { index--; scale = 1f; offset = Offset.Zero } }, enabled = index > 0) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev", tint = PureWhite)
            }
            Text("${index + 1} / ${images.size}", color = PureWhite)
            IconButton(onClick = { if (index < images.size - 1) { index++; scale = 1f; offset = Offset.Zero } }, enabled = index < images.size - 1) {
              Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", tint = PureWhite)
            }
          }
        }
      }
    },
  ) { inner ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(inner)
        .pointerInput(Unit) {
          detectTransformGestures { _, pan, zoom, _ ->
            scale = (scale * zoom).coerceIn(1f, 4f)
            if (scale > 1f) offset += pan else offset = Offset.Zero
          }
        },
      contentAlignment = Alignment.Center,
    ) {
      if (current != null) {
        AsyncImage(
          model = current.second,
          contentDescription = current.first,
          contentScale = ContentScale.Fit,
          modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(
              scaleX = scale,
              scaleY = scale,
              translationX = offset.x,
              translationY = offset.y,
            ),
        )
      }
      if (isFullScreen) {
        IconButton(
          onClick = { isFullScreen = false },
          modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(PrimaryNavy.copy(alpha = 0.7f), RoundedCornerShape(50)),
        ) {
          Icon(Icons.Filled.FullscreenExit, contentDescription = "Exit Fullscreen", tint = PureWhite)
        }
      }
    }
  }
}

private data class DxfLine(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val layer: String)
private data class DxfCircle(val cx: Float, val cy: Float, val r: Float, val layer: String)
private data class DxfText(val x: Float, val y: Float, val text: String)
private data class ParsedDxf(val lines: List<DxfLine>, val circles: List<DxfCircle>, val texts: List<DxfText>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InAppDxfViewer(
  title: String,
  dxfFile: File,
  onClose: () -> Unit,
) {
  val parsed = remember(dxfFile) { parseSimpleDxf(dxfFile) }
  var scale by remember { mutableFloatStateOf(1f) }
  var pan by remember { mutableStateOf(Offset.Zero) }
  val textMeasurer: TextMeasurer = rememberTextMeasurer()

  Scaffold(
    containerColor = Color(0xFF08244A),
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryNavy, titleContentColor = PureWhite, actionIconContentColor = PureWhite, navigationIconContentColor = PureWhite),
        navigationIcon = {
          IconButton(onClick = onClose, modifier = Modifier.testTag("viewer_close_btn")) {
            Icon(Icons.Filled.Close, contentDescription = "Close")
          }
        },
        title = { Text("$title (DXF CAD)", maxLines = 1) },
        actions = {
          IconButton(onClick = { scale = (scale - 0.3f).coerceAtLeast(0.6f) }) {
            Icon(Icons.Filled.ZoomOut, contentDescription = "Zoom Out")
          }
          IconButton(onClick = { scale = (scale + 0.3f).coerceAtMost(5f) }) {
            Icon(Icons.Filled.ZoomIn, contentDescription = "Zoom In")
          }
        },
      )
    },
  ) { inner ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(inner)
        .pointerInput(Unit) {
          detectTransformGestures { _, drag, zoom, _ ->
            scale = (scale * zoom).coerceIn(0.6f, 5f)
            pan += drag
          }
        },
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val allX = parsed.lines.flatMap { listOf(it.x1, it.x2) } + parsed.circles.map { it.cx }
        val allY = parsed.lines.flatMap { listOf(it.y1, it.y2) } + parsed.circles.map { it.cy }
        val minX = allX.minOrNull() ?: 0f
        val maxX = allX.maxOrNull() ?: 25f
        val minY = allY.minOrNull() ?: 0f
        val maxY = allY.maxOrNull() ?: 45f

        val spanX = (maxX - minX).coerceAtLeast(1f)
        val spanY = (maxY - minY).coerceAtLeast(1f)
        val pad = 48f
        val baseScale = minOf((size.width - pad * 2) / spanX, (size.height - pad * 2) / spanY) * scale
        val originX = (size.width - spanX * baseScale) / 2f + pan.x
        val originY = (size.height + spanY * baseScale) / 2f + pan.y

        fun mapPt(x: Float, y: Float) = Offset(originX + (x - minX) * baseScale, originY - (y - minY) * baseScale)

        // Subtle blueprint grid
        val gridStep = 48f
        var gx = 0f
        while (gx < size.width) {
          drawLine(Color.White.copy(alpha = 0.06f), Offset(gx, 0f), Offset(gx, size.height), strokeWidth = 1f)
          gx += gridStep
        }
        var gy = 0f
        while (gy < size.height) {
          drawLine(Color.White.copy(alpha = 0.06f), Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1f)
          gy += gridStep
        }

        parsed.lines.forEach { l ->
          val color = if (l.layer.contains("PORCH", true)) AccentOrange else Color.White
          drawLine(color = color, start = mapPt(l.x1, l.y1), end = mapPt(l.x2, l.y2), strokeWidth = 3f)
        }
        parsed.circles.forEach { c ->
          drawCircle(
            color = AccentOrange,
            radius = c.r * baseScale,
            center = mapPt(c.cx, c.cy),
            style = Stroke(width = 2.5f),
          )
        }
        parsed.texts.forEach { t ->
          val p = mapPt(t.x, t.y)
          drawText(
            textMeasurer = textMeasurer,
            text = t.text,
            topLeft = p,
            style = TextStyle(color = Color(0xFFFFD199), fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
          )
        }
      }

      Surface(
        color = PrimaryNavy.copy(alpha = 0.85f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
      ) {
        Text(
          text = "Entities: ${parsed.lines.size} lines • ${parsed.circles.size} arcs • ${parsed.texts.size} labels (Pinch to zoom/pan)",
          color = PureWhite,
          style = MaterialTheme.typography.labelSmall,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
      }
    }
  }
}

private fun parseSimpleDxf(file: File): ParsedDxf {
  val linesList = mutableListOf<DxfLine>()
  val circlesList = mutableListOf<DxfCircle>()
  val textsList = mutableListOf<DxfText>()
  try {
    val raw = file.readLines()
    var i = 0
    while (i + 1 < raw.size) {
      val code = raw[i].trim()
      val value = raw[i + 1].trim()
      if (code == "0" && value == "LINE") {
        var x1 = 0f; var y1 = 0f; var x2 = 0f; var y2 = 0f; var layer = "0"
        i += 2
        while (i + 1 < raw.size && raw[i].trim() != "0") {
          when (raw[i].trim()) {
            "8" -> layer = raw[i + 1].trim()
            "10" -> x1 = raw[i + 1].trim().toFloatOrNull() ?: 0f
            "20" -> y1 = raw[i + 1].trim().toFloatOrNull() ?: 0f
            "11" -> x2 = raw[i + 1].trim().toFloatOrNull() ?: 0f
            "21" -> y2 = raw[i + 1].trim().toFloatOrNull() ?: 0f
          }
          i += 2
        }
        linesList.add(DxfLine(x1, y1, x2, y2, layer))
        continue
      } else if (code == "0" && value == "CIRCLE") {
        var cx = 0f; var cy = 0f; var r = 1f; var layer = "0"
        i += 2
        while (i + 1 < raw.size && raw[i].trim() != "0") {
          when (raw[i].trim()) {
            "8" -> layer = raw[i + 1].trim()
            "10" -> cx = raw[i + 1].trim().toFloatOrNull() ?: 0f
            "20" -> cy = raw[i + 1].trim().toFloatOrNull() ?: 0f
            "40" -> r = raw[i + 1].trim().toFloatOrNull() ?: 1f
          }
          i += 2
        }
        circlesList.add(DxfCircle(cx, cy, r, layer))
        continue
      } else if (code == "0" && value == "TEXT") {
        var x = 0f; var y = 0f; var txt = ""
        i += 2
        while (i + 1 < raw.size && raw[i].trim() != "0") {
          when (raw[i].trim()) {
            "10" -> x = raw[i + 1].trim().toFloatOrNull() ?: 0f
            "20" -> y = raw[i + 1].trim().toFloatOrNull() ?: 0f
            "1" -> txt = raw[i + 1].trim()
          }
          i += 2
        }
        if (txt.isNotBlank()) textsList.add(DxfText(x, y, txt))
        continue
      }
      i += 2
    }
  } catch (_: Exception) {
  }
  return ParsedDxf(linesList, circlesList, textsList)
}

@Composable
private fun UnsupportedCadFallbackDialog(
  resource: ResourceItem,
  localFile: LocalDownloadedFile?,
  strings: AcademyStrings,
  onClose: () -> Unit,
  onDownload: () -> Unit,
  onOpenExternal: (LocalDownloadedFile) -> Unit,
) {
  Box(
    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).padding(20.dp),
    contentAlignment = Alignment.Center,
  ) {
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          Icon(Icons.Filled.Architecture, contentDescription = null, tint = PrimaryNavy, modifier = Modifier.size(28.dp))
          Text(strings.unsupportedPreviewTitle, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
          IconButton(onClick = onClose, modifier = Modifier.testTag("viewer_close_btn")) {
            Icon(Icons.Filled.Close, contentDescription = "Close")
          }
        }
        Text(
          text = "${resource.title} (${resource.fileType} • ${resource.fileSizeLabel})",
          style = MaterialTheme.typography.titleSmall,
          color = PrimaryNavy,
        )
        Text(
          text = strings.unsupportedPreviewDesc,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (localFile != null) {
          Button(
            onClick = { onOpenExternal(localFile) },
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy, contentColor = PureWhite),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(strings.openFileBtn)
          }
        } else {
          Button(
            onClick = {
              onClose()
              onDownload()
            },
            colors = ButtonDefaults.buttonColors(containerColor = AccentOrange, contentColor = PureWhite),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Icon(Icons.Filled.Download, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(strings.downloadBtn)
          }
        }
      }
    }
  }
}

private fun formatMillis(ms: Long): String {
  val totalSec = (ms / 1000L).coerceAtLeast(0L)
  val min = totalSec / 60L
  val sec = totalSec % 60L
  return String.format(Locale.US, "%02d:%02d", min, sec)
}
