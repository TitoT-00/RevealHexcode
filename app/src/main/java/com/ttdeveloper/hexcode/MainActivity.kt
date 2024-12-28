package com.ttdeveloper.hexcode

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.ContentCopy
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.core.graphics.scale
import androidx.lifecycle.lifecycleScope
import com.ttdeveloper.hexcode.data.PreferencesManager
import com.ttdeveloper.hexcode.ui.WelcomeScreen
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private val preferencesManager by lazy { PreferencesManager(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RevealHexcodeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showWelcome by remember { mutableStateOf(true) }
                    var hasCheckedWelcome by remember { mutableStateOf(false) }

                    LaunchedEffect(Unit) {
                        preferencesManager.hasSeenWelcome.collect { hasSeenWelcome ->
                            showWelcome = !hasSeenWelcome
                            hasCheckedWelcome = true
                        }
                    }

                    if (hasCheckedWelcome) {
                        AnimatedVisibility(
                            visible = showWelcome,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            WelcomeScreen(
                                onGetStarted = {
                                    showWelcome = false
                                    lifecycleScope.launch {
                                        preferencesManager.setHasSeenWelcome()
                                    }
                                }
                            )
                        }

                        AnimatedVisibility(
                            visible = !showWelcome,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            HexcodeRevealerApp()
                        }
                    }
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
            val originalBitmap = ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
            selectedBitmap = originalBitmap
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Mode Toggle
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .shadow(4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ElevatedButton(
                    onClick = { isCameraMode = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = if (isCameraMode) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.surface
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Camera,
                        contentDescription = "Camera",
                        tint = if (isCameraMode) 
                            MaterialTheme.colorScheme.onPrimary 
                        else 
                            MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Camera",
                        color = if (isCameraMode) 
                            MaterialTheme.colorScheme.onPrimary 
                        else 
                            MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                ElevatedButton(
                    onClick = { 
                        isCameraMode = false
                        galleryLauncher.launch("image/*")
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = if (!isCameraMode) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.surface
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Gallery",
                        tint = if (!isCameraMode) 
                            MaterialTheme.colorScheme.onPrimary 
                        else 
                            MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Gallery",
                        color = if (!isCameraMode) 
                            MaterialTheme.colorScheme.onPrimary 
                        else 
                            MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Main Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (permissionsState.allPermissionsGranted) {
                if (isCameraMode) {
                    Card(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .shadow(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        CameraPreview { hexcode ->
                            currentHexcode = hexcode
                        }
                    }
                } else {
                    selectedImageUri?.let { uri ->
                        Card(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                                .shadow(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Selected image",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .pointerInput(Unit) {
                                            detectTapGestures { offset ->
                                                selectedBitmap?.let { bitmap ->
                                                    val scaledX = (offset.x * (bitmap.width.toFloat() / size.width)).roundToInt()
                                                    val scaledY = (offset.y * (bitmap.height.toFloat() / size.height)).roundToInt()
                                                    
                                                    if (scaledX in 0 until bitmap.width && scaledY in 0 until bitmap.height) {
                                                        val pixel = bitmap.getPixel(scaledX, scaledY)
                                                        currentHexcode = String.format("#%06X", 0xFFFFFF and pixel)
                                                    }
                                                }
                                            }
                                        }
                                )
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Camera and Storage permissions are required",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                    Button(
                        onClick = { permissionsState.launchMultiplePermissionRequest() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Grant Permissions")
                    }
                }
            }
        }

        // Hexcode Display
        AnimatedVisibility(
            visible = currentHexcode != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .shadow(4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    currentHexcode?.let { hexcode ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Color preview
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .shadow(4.dp, RoundedCornerShape(8.dp))
                                    .background(
                                        Color(android.graphics.Color.parseColor(hexcode)),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                                        RoundedCornerShape(8.dp)
                                    )
                            )
                            // Hexcode text
                            Text(
                                text = hexcode,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Hexcode", hexcode)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Hexcode copied!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy hexcode",
                                tint = MaterialTheme.colorScheme.primary
                            )
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
    var size by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
    var lastTapTime by remember { mutableStateOf(0L) }
    
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }
    
    LaunchedEffect(previewView) {
        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
        
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
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
                .onSizeChanged {
                    size = androidx.compose.ui.geometry.Size(it.width.toFloat(), it.height.toFloat())
                }
        )
        
        // Capture button
        IconButton(
            onClick = {
                val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                    .format(System.currentTimeMillis())
                
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "$name.jpg")
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/HexcodeRevealer")
                }

                val outputOptions = ImageCapture.OutputFileOptions.Builder(
                    context.contentResolver,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                ).build()

                imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            Toast.makeText(
                                context,
                                "Photo saved to gallery",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Toast.makeText(
                                context,
                                "Failed to save photo: ${exception.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .size(72.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Camera,
                contentDescription = "Take photo",
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}