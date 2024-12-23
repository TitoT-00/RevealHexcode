package com.ttdeveloper.hexcode

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.ttdeveloper.hexcode.ui.theme.RevealHexcodeTheme
import android.Manifest
import android.annotation.SuppressLint
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.core.graphics.scale
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RevealHexcodeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HexcodeRevealerApp()
                }
            }
        }
    }
}

@SuppressLint("NewApi")
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HexcodeRevealerApp() {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isCameraMode by remember { mutableStateOf(true) }
    var currentHexcode by remember { mutableStateOf<String?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageWidth by remember { mutableStateOf(0) }
    var imageHeight by remember { mutableStateOf(0) }

    val context = LocalContext.current
    val permissionsState = rememberMultiplePermissionsState(
        permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            listOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_MEDIA_IMAGES
            )
        } else {
            listOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
    )

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            val source = ImageDecoder.createSource(context.contentResolver, it)
            val originalBitmap = ImageDecoder.decodeBitmap(source)
            selectedBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
            isCameraMode = false
        }
    }

    LaunchedEffect(Unit) {
        permissionsState.launchMultiplePermissionRequest()
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { 
                    Column {
                        Text(
                            "Hexcode Revealer",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            if (isCameraMode) "Camera Mode" else "Gallery Mode",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.shadow(8.dp)
            )
        },
        bottomBar = {
            Column(modifier = Modifier.navigationBarsPadding()) {
                // Color info card
                currentHexcode?.let { hexcode ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium,
                        tonalElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(40.dp),
                                    color = Color(android.graphics.Color.parseColor(hexcode)),
                                    shape = MaterialTheme.shapes.small,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {}
                                Text(
                                    text = hexcode,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            FilledTonalIconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Color Hexcode", hexcode)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Hexcode copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copy hexcode",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                
                // Navigation bar
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = isCameraMode,
                        onClick = { isCameraMode = true },
                        icon = {
                            Icon(
                                Icons.Default.Camera,
                                contentDescription = "Camera"
                            )
                        },
                        label = { Text("Camera") }
                    )
                    NavigationBarItem(
                        selected = !isCameraMode,
                        onClick = { galleryLauncher.launch("image/*") },
                        icon = {
                            Icon(
                                Icons.Default.PhotoLibrary,
                                contentDescription = "Gallery"
                            )
                        },
                        label = { Text("Gallery") }
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (permissionsState.allPermissionsGranted) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (isCameraMode) {
                        CameraPreview(
                            onColorPicked = { hexcode ->
                                currentHexcode = hexcode
                            }
                        )
                    } else {
                        selectedImageUri?.let { uri ->
                            AsyncImage(
                                model = uri,
                                contentDescription = "Selected image",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .onSizeChanged { size ->
                                        imageWidth = size.width
                                        imageHeight = size.height
                                    }
                                    .pointerInput(Unit) {
                                        detectTapGestures { offset ->
                                            selectedBitmap?.let { bitmap ->
                                                val scaledBitmap = bitmap.scale(
                                                    imageWidth,
                                                    imageHeight,
                                                    true
                                                )
                                                val x = offset.x.roundToInt()
                                                val y = offset.y.roundToInt()
                                                if (x in 0 until scaledBitmap.width && y in 0 until scaledBitmap.height) {
                                                    val pixel = scaledBitmap.getPixel(x, y)
                                                    currentHexcode = String.format("#%06X", 0xFFFFFF and pixel)
                                                }
                                            }
                                        }
                                    }
                            )
                        } ?: run {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Icon(
                                        Icons.Default.PhotoLibrary,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        "Tap the gallery icon to select an image",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Camera,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            "Camera and Storage Permissions Required",
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Please grant the required permissions to use this app",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Button(
                            onClick = { permissionsState.launchMultiplePermissionRequest() },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Grant Permissions")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CameraPreview(onColorPicked: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    var lastTapTime by remember { mutableStateOf(0L) }
    
    LaunchedEffect(previewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val cameraProvider = cameraProviderFuture.get()
        
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    AndroidView(
        factory = { previewView },
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastTapTime > 500) { // Debounce taps
                        lastTapTime = currentTime
                        val bitmap = previewView.bitmap
                        bitmap?.let {
                            val x = (offset.x * (it.width.toFloat() / size.width)).roundToInt()
                            val y = (offset.y * (it.height.toFloat() / size.height)).roundToInt()
                            if (x in 0 until it.width && y in 0 until it.height) {
                                val pixel = it.getPixel(x, y)
                                onColorPicked(String.format("#%06X", 0xFFFFFF and pixel))
                            }
                        }
                    }
                }
            }
    )
}