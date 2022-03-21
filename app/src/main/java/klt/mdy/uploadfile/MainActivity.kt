package klt.mdy.uploadfile

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class MainActivity : AppCompatActivity(
    R.layout.activity_main
) {
    private val btnCamera: Button by lazy { findViewById(R.id.btn_camera) }
    private val btnGallery: Button by lazy { findViewById(R.id.btn_gallery) }
    private val img: ImageView by lazy { findViewById(R.id.img) }
    private var tempUri: Uri? = null

    // (Camera)TakePicture
    private val fromCameraWithTakePicture = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) {
        if (it) {
            tempUri?.let { uri ->
                img.setImageURI(uri)
                val bitmap = uriToBitmap(uri = uri)
                //todo : if your server want
                // file => just convert your filePath to Multipart Body with Part
                // bytesArray => just convert your bitmap to bytesArray
                Log.d("noobcp",uri.path.toString())
                Log.d("noobcb",bitmap.toString())

            }
        }
    }
    private fun launchCameraWithTakePicture() {
        lifecycleScope.launchWhenCreated {
            getTmpFileUri().let {
                tempUri = it
                fromCameraWithTakePicture.launch(it)
            }
        }
    }
    // (Gallery)GetContent
    private val fromGalleryWithGetContent = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) {
        it?.let { uri ->
            img.setImageURI(uri)
            val bitmap = uriToBitmap(uri = uri)
            //todo : if your server want
            // file => just convert your filePath to Multipart Body with Part
            // bytesArray => just convert your bitmap to bytesArray

            Log.d("noobgp",uri.path.toString())
            Log.d("noobgb",bitmap.toString())
        }
    }
    private fun launchGalleryWithTakePicture() {
        lifecycleScope.launchWhenCreated {
            fromGalleryWithGetContent.launch("image/*")
            //    Any document "*/*"
            //    Images "image/*"
            //    Videos "video/*"
        }
    }

    // (Camera)StartActivityForResult
    private val fromCameraWithActivityResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){
        val bitmap = it.data?.extras?.let { bundle ->
            bundle.get("data") as Bitmap
        }
        bitmap?.let { bm ->
            img.setImageBitmap(bm)
        }
    }
    private fun launchCameraWithActivityResult() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.resolveActivity(packageManager)?.let {
            fromCameraWithActivityResult.launch(intent)
        }
    }
    // (Gallery)StartActivityForResult
    private val fromGalleryWithActivityResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){
        val bitmap = it.data?.extras?.let { bundle ->
            bundle.get("data") as Bitmap
        }
        bitmap?.let { bm ->
            img.setImageBitmap(bm)
        }
    }
    private fun launchGalleryWithActivityResult() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.resolveActivity(packageManager)?.let {
            fromGalleryWithActivityResult.launch(intent)
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        btnCamera.setOnClickListener {
            launchCameraWithTakePicture()
//            launchCameraWithActivityResult()
        }
        btnGallery.setOnClickListener {
            launchGalleryWithTakePicture()
//            launchGalleryWithActivityResult()
        }
    }

    private fun getTmpFileUri(): Uri {
        val tmpFile = File.createTempFile("tmp_image_file", ".png", cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }

        return FileProvider.getUriForFile(
            applicationContext,
            "${BuildConfig.APPLICATION_ID}.provider",
            tmpFile
        )
    }

    private fun uriToBitmap(uri: Uri): Bitmap {
        return when {

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {
                val source = ImageDecoder.createSource(this.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            }

            else -> {
                MediaStore.Images.Media.getBitmap(
                    this.contentResolver,
                    uri
                )
            }
        }
    }

    private fun bitmapToUri(context: Context, bitmap : Bitmap): Uri{
        val path = MediaStore.Images.Media.insertImage(
            context.contentResolver,
            bitmap,
            "title",
            "description"
        )
        return Uri.parse(path)
    }

}