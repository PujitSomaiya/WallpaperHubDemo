package com.pujit.wallpaperdemo.view.main

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.Editable
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.pujit.wallpaperdemo.R
import com.pujit.wallpaperdemo.api.APIClient
import com.pujit.wallpaperdemo.api.APIInterface
import com.pujit.wallpaperdemo.api.response.HitsItem
import com.pujit.wallpaperdemo.api.response.PhotoResponse
import com.pujit.wallpaperdemo.interfaces.AllClickListeners
import com.pujit.wallpaperdemo.utilities.EndlessRecyclerViewScrollListener
import com.pujit.wallpaperdemo.utilities.ProgressDialog
import com.pujit.wallpaperdemo.view.fullscreen.view.FullScreenActivity
import com.pujit.wallpaperdemo.view.main.adapter.ImageAdapter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity(), AllClickListeners.OnImageClick, AllClickListeners.SetOnBottomDialogButtonClick {

    private lateinit var apiInterface: APIInterface
    private lateinit var photoResponse: PhotoResponse
    private lateinit var imageAdapter: ImageAdapter
    private lateinit var rvHome: RecyclerView
    private lateinit var scrollListener: EndlessRecyclerViewScrollListener
    private lateinit var gridLayoutManager: GridLayoutManager
    private var currentOffset = 2
    private var searchField = ""
    private var isLoadMore = false
    private var imageFilePath: String = ""
    private var imageType: String = "all"
    private var orientation: String = "all"
    private var isFromEditText = false
    private lateinit var progressbar: ProgressBar
    private lateinit var edSearchBox: EditText
    private lateinit var tvNoItem: TextView
    private lateinit var imgClear: ImageView
    private lateinit var imgFilter: ImageView
    private lateinit var dialog: BottomDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initControls()
        hideKeyboard(this)
        this.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
    }

    private fun initControls() {
        initViews()
        initRecyclerView()
        onClicks()
        callApi(searchField, currentOffset, isLoadMore,imageType,orientation)
    }

    private fun onClicks() {
        edSearchBox.doAfterTextChanged { text: Editable? ->
            if (text.toString().trim().length > 1) {
                imgClear.visibility = View.VISIBLE
                searchField = text.toString().trim()
                currentOffset = 1
                isLoadMore = false
                isFromEditText = true
                callApi(searchField, currentOffset, isLoadMore,imageType,orientation)
            } else {
                imgClear.visibility = View.INVISIBLE
                searchField = ""
                currentOffset = 1
                isLoadMore = false
                isFromEditText = true
                callApi(searchField, currentOffset, isLoadMore,imageType,orientation)
            }
        }

        imgClear.setOnClickListener {
            edSearchBox.clearFocus()
            edSearchBox.setText("")
            hideKeyboard(this@MainActivity)
        }

        imgFilter.setOnClickListener {
            if (!dialog.isHidden) {
                dialog.show(this.supportFragmentManager, "dialogFilter");
                dialog.isCancelable = false;
            } else {
                dialog.dismiss();
            }
        }
    }

    private fun initRecyclerView() {
        imageAdapter = ImageAdapter(this, this)
        gridLayoutManager = GridLayoutManager(this, 4, LinearLayoutManager.VERTICAL, false)
        rvHome.layoutManager = gridLayoutManager
        scrollListener = object : EndlessRecyclerViewScrollListener(gridLayoutManager) {
            override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView?) {
                println("Load more")
                isLoadMore = true
                currentOffset += 1
                callApi(searchField, currentOffset, isLoadMore,imageType,orientation)
            }

        }
        rvHome.addOnScrollListener(scrollListener)
        rvHome.adapter = imageAdapter
    }

    private fun initViews() {
        apiInterface = APIClient.getClient().create(APIInterface::class.java)
        dialog = BottomDialog(this);
        imgFilter = findViewById(R.id.imgFilter)
        imgClear = findViewById(R.id.imgClear)
        tvNoItem = findViewById(R.id.tvNoItem)
        edSearchBox = findViewById(R.id.edSearchBox)
        progressbar = findViewById(R.id.progressbar)
        rvHome = findViewById(R.id.rvHome)
    }

    private fun hideKeyboard(context: Activity) {
        val view = context.currentFocus
        if (view != null) {
            val imm: InputMethodManager =
                    context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun callApi(key: String, page: Int, loadMore: Boolean,imageType:String,orientation:String){

        if (!loadMore && !isFromEditText)
            ProgressDialog.showProgress(this@MainActivity)
        else
            progressbar.visibility = View.VISIBLE

        val call: Call<PhotoResponse> = apiInterface.getPhotos(
                "22823628-343c53ced661a5f7a387a4eb3",
                key,
                imageType,
                page,
                orientation,
                true
        )

        call.enqueue(object : Callback<PhotoResponse?> {
            override fun onResponse(
                    call: Call<PhotoResponse?>?,
                    response: Response<PhotoResponse?>
            ) {
                ProgressDialog.dismissProgress()
                progressbar.visibility = View.GONE


                photoResponse = PhotoResponse()
                photoResponse = response.body() as PhotoResponse
                if (isLoadMore) {
                    imageAdapter.add(photoResponse.hits)
                } else {
                    imageAdapter.refresh(photoResponse.hits)
                }

                if (!isLoadMore) {
                    if (photoResponse.hits.size == 0) {
                        rvHome.visibility = View.GONE
                        tvNoItem.visibility = View.VISIBLE
                    } else {
                        rvHome.visibility = View.VISIBLE
                        tvNoItem.visibility = View.GONE
                    }
                } else {
                    rvHome.visibility = View.VISIBLE
                    tvNoItem.visibility = View.GONE
                }

                println(response.body())
            }

            override fun onFailure(call: Call<PhotoResponse?>, t: Throwable?) {
                call.cancel()
                ProgressDialog.dismissProgress()
                progressbar.visibility = View.GONE
                println(t?.message)
            }
        })
    }


    override fun onImageClick(position: Int, item: HitsItem?) {
        dialogForDownload(position, item!!)
    }

    private fun dialogForDownload(position: Int, item: HitsItem) {
        if (!isFinishing) {
            AlertDialog.Builder(this@MainActivity)
                    .setCancelable(false)
                    .setTitle("Info!!")
                    .setMessage("Do you want to download??")
                    .setCancelable(false)
                    .setPositiveButton("Download"
                    ) { dialog, which ->
                        persmissionCheckAndDownload(item)
                    }
                    .setNegativeButton("No"
                    ) { dialog, which -> }
                    .setNeutralButton("View"
                    ) { dialog, which ->
                        val intent = Intent(this, FullScreenActivity::class.java)
                        intent.putExtra(
                            "item",
                            Gson().toJson(item)
                        )
                        startActivity(intent)

                    }.create().show()
        }
    }

    private fun persmissionCheckAndDownload(item: HitsItem) {
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
                this@MainActivity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
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
                AlertDialog.Builder(this@MainActivity)
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

    override fun setFilter(number: Int, rbImageType: RadioButton, rbOrientation: RadioButton) {

        if (number==1){
            searchField = edSearchBox.text.toString().trim()
            currentOffset = 1
            isLoadMore = false
            isFromEditText = true
            imageType = "all"
            orientation = "all"
            callApi(searchField, currentOffset, isLoadMore,imageType,orientation)
        }else if (number==2){
            searchField = edSearchBox.text.toString().trim()
            currentOffset = 1
            isLoadMore = false
            isFromEditText = true
            imageType = rbImageType.text.trim().toString()
            orientation = rbOrientation.text.trim().toString()
            callApi(searchField, currentOffset, isLoadMore,imageType,orientation)
        }

    }
}