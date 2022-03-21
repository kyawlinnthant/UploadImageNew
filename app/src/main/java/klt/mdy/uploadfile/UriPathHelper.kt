package klt.mdy.uploadfile

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore


object UriPathHelper {

    /* Check whether the current android os version is bigger than KitKat or not. */
    private fun isAboveKitKat() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

    //This method can parse out the real local file path from a file URI.
    fun getPath(
        context: Context,
        uri: Uri
    ): String? {
        return if (isAboveKitKat())
            getRealPathFromUriAboveKitKat(context, uri)
        else
            getImageRealPath(context.contentResolver, uri, null)

    }

    //This method will parse out the real local file path from the file content URI.
    //The method is only applied to the android SDK version number that is bigger than 19.
    private fun getRealPathFromUriAboveKitKat(
        context: Context?,
        uri: Uri?
    ): String? {
        var path: String? = ""
        if (context != null && uri != null) {
            if (isContentUri(uri)) {
                path = if (isGooglePhotoDocument(uri.authority)) {
                    uri.lastPathSegment
                } else {
                    getImageRealPath(context.contentResolver, uri, null)
                }
            } else if (isFileUri(uri)) {
                path = uri.path
            } else if (isDocumentUri(context, uri)) {

                // Get uri related document id.
                val documentId = DocumentsContract.getDocumentId(uri)

                // Get uri authority.
                val uriAuthority = uri.authority
                if (isMediaDocument(uriAuthority)) {
                    val idArr = documentId.split(":").toTypedArray()
                    if (idArr.size == 2) {
                        // First item is document type.
                        val docType = idArr[0]

                        // Second item is document real id.
                        val realDocId = idArr[1]

                        // Get content uri by document type.
                        var mediaContentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        if ("image" == docType) {
                            mediaContentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        } else if ("video" == docType) {
                            mediaContentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        } else if ("audio" == docType) {
                            mediaContentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                        }

                        // Get where clause with real document id.
                        val whereClause = MediaStore.Images.Media._ID + " = " + realDocId
                        path = getImageRealPath(context.contentResolver, mediaContentUri, whereClause)
                    }
                } else if (isDownloadDocument(uriAuthority)) {
                    // Build download URI.
                    val downloadUri = Uri.parse("content://downloads/public_downloads")

                    // Append download document id at URI end.
                    val downloadUriAppendId =
                        ContentUris.withAppendedId(downloadUri, java.lang.Long.valueOf(documentId))
                    path = getImageRealPath(context.contentResolver, downloadUriAppendId, null)
                } else if (isExternalStorageDocument(uriAuthority)) {
                    val idArr = documentId.split(":").toTypedArray()
                    if (idArr.size == 2) {
                        val type = idArr[0]
                        val realDocId = idArr[1]
                        if ("primary".equals(type, ignoreCase = true)) {
                            path = Environment.getExternalStorageDirectory()
                                .toString() + "/" + realDocId
                        }
                    }
                }
            }
        }
        return path
    }


    //Check whether this uri represent a document or not.
    private fun isDocumentUri(
        context: Context?,
        uri: Uri?
    ): Boolean {
        var isDocument = false
        if (context != null && uri != null) {
            isDocument = DocumentsContract.isDocumentUri(context, uri)
        }
        return isDocument
    }

    //Check whether this URI is a content URI or not.
    //content uri like, example => content://media/external/images/media/1302716
    private fun isContentUri(uri: Uri?): Boolean {
        var isContent = false
        if (uri != null) {
            val uriSchema = uri.scheme
            if ("content".equals(uriSchema, ignoreCase = true)) {
                isContent = true
            }
        }
        return isContent
    }

    //Check whether this URI is a file URI or not.
    //file URI like, example => file:///storage/41B7-12F1/DCIM/Camera/IMG_20180211_095139.jpg
    private fun isFileUri(uri: Uri?): Boolean {
        var isFile = false
        if (uri != null) {
            val uriSchema = uri.scheme
            if ("file".equals(uriSchema, ignoreCase = true)) {
                isFile = true
            }
        }
        return isFile
    }


    //Check whether this document is provided by ExternalStorageProvider.
    //Returns true means the file is saved in external storage.
    private fun isExternalStorageDocument(uriAuthority: String?): Boolean {
        var isExternalStorage = false
        if ("com.android.externalstorage.documents" == uriAuthority) {
            isExternalStorage = true
        }
        return isExternalStorage
    }

    //Check whether this document is provided by DownloadsProvider.
    //Returns true means this file is a downloaded file.
    private fun isDownloadDocument(uriAuthority: String?): Boolean {
        var isDownload = false
        if ("com.android.providers.downloads.documents" == uriAuthority) {
            isDownload = true
        }
        return isDownload
    }


    //Check if MediaProvider provides this document.
    //Returns true means this image is created in the android media app.
    private fun isMediaDocument(uriAuthority: String?): Boolean {
        var ret = false
        if ("com.android.providers.media.documents" == uriAuthority) {
            ret = true
        }
        return ret
    }

    //Check whether google photos provide this document.
    //Returns true means this image is created in the google photos app.
    private fun isGooglePhotoDocument(uriAuthority: String?): Boolean {
        var ret = false
        if ("com.google.android.apps.photos.content" == uriAuthority) {
            ret = true
        }
        return ret
    }

    //Return uri represented document file real local path.
    private fun getImageRealPath(
        contentResolver: ContentResolver,
        uri: Uri,
        whereClause: String?
    ): String? {
        var ret = ""
        // Query the URI with the condition.
        val cursor = contentResolver.query(uri, null, whereClause, null, null)
        if (cursor != null) {
            val moveToFirst = cursor.moveToFirst()
            if (moveToFirst) {

                // Get columns name by URI type.
                var columnName = MediaStore.Images.Media.DATA
                if (uri === MediaStore.Images.Media.EXTERNAL_CONTENT_URI) {
                    columnName = MediaStore.Images.Media.DATA
                } else if (uri === MediaStore.Audio.Media.EXTERNAL_CONTENT_URI) {
                    columnName = MediaStore.Audio.Media.DATA
                } else if (uri === MediaStore.Video.Media.EXTERNAL_CONTENT_URI) {
                    columnName = MediaStore.Video.Media.DATA
                }

                // Get column index.
                val imageColumnIndex = cursor.getColumnIndex(columnName)

                // Get column value which is the uri related file local path.
                ret = cursor.getString(imageColumnIndex)
            }
        }
        return ret
    }
}