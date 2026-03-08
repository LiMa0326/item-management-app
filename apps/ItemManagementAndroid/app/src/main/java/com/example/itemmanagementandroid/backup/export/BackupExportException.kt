package com.example.itemmanagementandroid.backup.export

sealed class BackupExportException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {
    class InvalidParameter(
        message: String,
        cause: Throwable? = null
    ) : BackupExportException(message = message, cause = cause)

    class IoFailure(
        message: String,
        cause: Throwable
    ) : BackupExportException(message = message, cause = cause)

    class MissingPhotoFile(
        val photoId: String,
        val sourceUri: String?,
        val exportMode: ExportMode
    ) : BackupExportException(
        message = "Missing photo file for photoId=$photoId mode=${exportMode.wireValue} uri=${sourceUri ?: "null"}"
    )
}

