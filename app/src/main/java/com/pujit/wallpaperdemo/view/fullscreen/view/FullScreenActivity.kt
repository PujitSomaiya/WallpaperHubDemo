package com.pujit.wallpaperdemo.view.fullscreen.view

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DownloadManager
import android.app.WallpaperManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bogdwellers.pinchtozoom.ImageMatrixTouchHandler
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.pujit.wallpaperdemo.R
import com.pujit.wallpaperdemo.api.response.HitsItem
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class FullScreenActivity : AppCompatActivity() {

    private lateinit var imgClose: ImageView
    private lateinit var imgSetWallpaper: ImageView
    private lateinit var imgDownload: ImageView
    private lateinit var imgWallpaper: ImageView
    private lateinit var item: HitsItem
    private var imageFilePath: String = ""
    lateinit var wallpaperManager: WallpaperManager
    var options = RequestOptions()
        .centerCrop()
        .placeholder(R.drawable.loader_animation)
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .priority(Priority.HIGH)
        .dontAnimate()
        .dontTransform()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen)

        initControls()
    }

    private fun initControls() {
        initViews()
        onClick()
        getBundleData()
        loadImage()
    }

    private fun onClick() {
        imgClose.setOnClickListener { finish() }
        imgDownload.setOnClickListener { permissionCheckAndDownload(item) }
        imgSetWallpaper.setOnClickListener { checkPersmissionAndSetWallpaper() }
    }

    @SuppressLint("StaticFieldLeak")
    private fun checkPersmissionAndSetWallpaper() {
        Dexter.withActivity(this)
            .withPermissions(
                Manifest.permission.SET_WALLPAPER,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    // check if all permissions are granted
                    if (report.areAllPermissionsGranted()) {
                        setWallpaper()
                    }

                    // check for permanent denial of any permission
                    if (report.isAnyPermissionPermanentlyDenied) {
                        showSettingsDialog()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token!!.continuePermissionRequest()
                }


            })
            .onSameThread()
            .check()

    }

    @SuppressLint("StaticFieldLeak")
    private fun setWallpaper() {
        var image: File? = null
        object : AsyncTask<Void, Void, Boolean>() {

            override fun doInBackground(vararg params: Void?): Boolean {
                return try {
                    image = Glide.with(this@FullScreenActivity)
                        .downloadOnly()
                        .diskCacheStrategy(DiskCacheStrategy.DATA) // Cache resource before it's decoded
                        .load(item.largeImageURL)
                        .submit(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                        .get()
                    true
                } catch (ex: java.lang.Exception) {
                    false
                }
            }

            override fun onPostExecute(result: Boolean?) {
                try {
                    if (image != null)
                        wallpaperManager.setBitmap(BitmapFactory.decodeFile(image!!.path))
                    Toast.makeText(
                        this@FullScreenActivity, "Wallpaper set",
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: Exception) {
                    Toast.makeText(
                        this@FullScreenActivity, "Failed!!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }.execute()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun loadImage() {
        imgWallpaper.setOnTouchListener(ImageMatrixTouchHandler(imgWallpaper.context));
        Glide.with(this@FullScreenActivity).load(item.largeImageURL).apply(options)
            .into(imgWallpaper)
    }

    private fun getBundleData() {
        if (intent.extras != null) {
            item = Gson().fromJson<HitsItem>(
                intent.getStringExtra("item"),
                object : TypeToken<HitsItem>() {}.type
            )
        }
    }

    private fun initViews() {
        wallpaperManager = WallpaperManager.getInstance(applicationContext)
        imgWallpaper = findViewById(R.id.imgWallpaper)
        imgClose = findViewById(R.id.imgClose)
        imgSetWallpaper = findViewById(R.id.imgSetWallpaper)
        imgDownload = findViewById(R.id.imgDownload)
    }

    private fun permissionCheckAndDownload(item: HitsItem) {
        Dexter.withActivity(this)
            .withPermissions(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    // check if all permissions are granted
                    if (report.areAllPermissionsGranted()) {
                        downloadFile(item.largeImageURL)
                    }

                    // check for permanent denial of any permission
                    if (report.isAnyPermissionPermanentlyDenied) {
                        showSettingsDialog()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token!!.continuePermissionRequest()
                }


            })
            .onSameThread()
            .check()
    }

    fun downloadFile(uRl: String?) {
        val direct = createImageFile()
        if (!direct?.exists()!!) {
            direct.mkdirs()
        }

        val mgr =
            this@FullScreenActivity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadUri: Uri = Uri.parse(uRl)
        val request = DownloadManager.Request(
            downloadUri
        )
        request.setAllowedNetworkTypes(
            DownloadManager.Request.NETWORK_WIFI
                    or DownloadManager.Request.NETWORK_MOBILE
        )
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverRoaming(true).setTitle("Image")
            .setDescription("Downloading!!!")

        mgr.enqueue(request)
    }

    @Throws(IOException::class)
    private fun createImageFile(): File? {

        val timeStamp: String = SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            Locale.getDefault()
        ).format(Date())
        val imageFileName = "IMG_" + timeStamp + "_"

//        New-way to store image in gallery (not secured) ******
        val storagePath =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                .toString() + "/WallpaperHub"
        val NewStorageDir = File(storagePath)
        if (!NewStorageDir.exists()) {
            val wallpaperDirectory = File(storagePath)
            wallpaperDirectory.mkdirs()
        }


//        *****************

//        old-way to store images in app's package name pictures folder (secured)
//        -------------------
//        File storageDir =
//                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
//        -------------------
        val image = File.createTempFile(
            imageFileName,  /* prefix */
            ".jpg",  /* suffix */
            NewStorageDir /* directory */
        )
        imageFilePath = image.absolutePath
        return image
    }

    private fun showSettingsDialog() {
        val builder =
            AlertDialog.Builder(this@FullScreenActivity)
        builder.setTitle("Need Permissions")
        builder.setMessage("This app needs permission to use this feature. You can grant them in app settings.")
        builder.setPositiveButton("GOTO SETTINGS", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface, which: Int) {
                dialog.cancel()
                openSettings()
            }
        })
        builder.setNegativeButton("Cancel", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface, which: Int) {
                dialog.cancel()
            }
        })
        builder.show()
    }

    // navigating user to app settings
    private fun openSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivityForResult(intent, 101)
    }
}