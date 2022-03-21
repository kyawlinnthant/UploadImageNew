package klt.mdy.uploadfile

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore


object UriPathUtil {

    private const val FILE = "file"
    private const val CONTENT = "content"
    private const val IMAGE_TYPE = "image"
    private const val AUDIO_TYPE = "audio"
    private const val VIDEO_TYPE = "video"
    private const val URI_EXTERNAL_STORAGE = "com.android.externalstorage.documents"
    private const val URI_DOWNLOAD_STORAGE = "com.android.providers.downloads.documents"
    private const val URI_MEDIA_DOCUMENT = "com.android.providers.media.documents"
    private const val URI_GOOGLE_PHOTO = "com.google.android.apps.photos.content"
    private const val PUBLIC_DOWNLOAD_URI = "content://downloads/public_downloads"

    //This method can parse out the real local file path from a file URI.
    fun getPath(
        context: Context,
        uri: Uri?
    ): String? {
        return getRealUriPath(context, uri)
    }

    //This method will parse out the real local file path from the file content URI.
    //The method is only applied to the android SDK version number that is bigger than 19.
    private fun getRealUriPath(
        context: Context,
        uri: Uri?
    ): String? {
        var path: String? = ""
        uri?.let {
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
                        when (docType) {
                            IMAGE_TYPE -> {
                                mediaContentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            }
                            VIDEO_TYPE -> {
                                mediaContentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            }
                            AUDIO_TYPE -> {
                                mediaContentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                            }
                        }

                        // Get where clause with real document id.
                        val whereClause = MediaStore.Images.Media._ID + " = " + realDocId
                        path =
                            getImageRealPath(context.contentResolver, mediaContentUri, whereClause)
                    }
                } else if (isDownloadDocument(uriAuthority)) {
                    // Build download URI.
                    val downloadUri = Uri.parse(PUBLIC_DOWNLOAD_URI)

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

    //Return uri represented document file real local path.
    private fun getImageRealPath(
        contentResolver: ContentResolver,
        uri: Uri?,
        whereClause: String?
    ): String {
        var path = ""
        uri?.let {
            // Query the URI with the condition.
            val cursor = contentResolver.query(uri, null, whereClause, null, null)
            if (cursor != null) {
                val moveToFirst = cursor.moveToFirst()
                if (moveToFirst) {
                    // Get columns name by URI type.
                    var columnName = MediaStore.Images.Media.DATA
                    when {
                        uri === MediaStore.Images.Media.EXTERNAL_CONTENT_URI -> {
                            columnName = MediaStore.Images.Media.DATA
                        }
                        uri === MediaStore.Audio.Media.EXTERNAL_CONTENT_URI -> {
                            columnName = MediaStore.Audio.Media.DATA
                        }
                        uri === MediaStore.Video.Media.EXTERNAL_CONTENT_URI -> {
                            columnName = MediaStore.Video.Media.DATA
                        }
                    }

                    // Get column index.
                    val imageColumnIndex = cursor.getColumnIndex(columnName)

                    // Get column value which is the uri related file local path.
                    path = cursor.getString(imageColumnIndex)
                }
            }
        }
        return path
    }


    //Check whether this uri represent a document or not.
    private fun isDocumentUri(
        context: Context,
        uri: Uri?
    ): Boolean {
        return DocumentsContract.isDocumentUri(context, uri)
    }

    //Check whether this URI is a content URI or not.
    //content uri like, example => content://media/external/images/media/1302716
    private fun isContentUri(uri: Uri?): Boolean {
        return uri?.let {
            (CONTENT.equals(it.scheme, ignoreCase = true))
        } ?: kotlin.run { false }
    }

    //Check whether this URI is a file URI or not.
    //file URI like, example => file:///storage/41B7-12F1/DCIM/Camera/IMG_20180211_095139.jpg
    private fun isFileUri(uri: Uri?): Boolean {
        return uri?.let {
            (FILE.equals(it.scheme, ignoreCase = true))
        } ?: kotlin.run { false }
    }


    //Check whether this document is provided by ExternalStorageProvider.
    //Returns true means the file is saved in external storage.
    private fun isExternalStorageDocument(uriAuthority: String?): Boolean {
        return URI_EXTERNAL_STORAGE == uriAuthority
    }

    //Check whether this document is provided by DownloadsProvider.
    //Returns true means this file is a downloaded file.
    private fun isDownloadDocument(uriAuthority: String?): Boolean {
        return URI_DOWNLOAD_STORAGE == uriAuthority
    }


    //Check if MediaProvider provides this document.
    //Returns true means this image is created in the android media app.
    private fun isMediaDocument(uriAuthority: String?): Boolean {
        return URI_MEDIA_DOCUMENT == uriAuthority
    }

    //Check whether google photos provide this document.
    //Returns true means this image is created in the google photos app.
    private fun isGooglePhotoDocument(uriAuthority: String?): Boolean {
        return URI_GOOGLE_PHOTO == uriAuthority
    }

}