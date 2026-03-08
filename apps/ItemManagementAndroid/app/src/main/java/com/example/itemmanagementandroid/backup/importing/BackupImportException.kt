package com.example.itemmanagementandroid.backup.importing

sealed class BackupImportException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {
    class InvalidParameter(
        message: String,
        cause: Throwable? = null
    ) : BackupImportException(message = message, cause = cause)

    class InvalidPackage(
        message: String,
        cause: Throwable? = null
    ) : BackupImportException(message = message, cause = cause)

    class RollbackSnapshotFailed(
        message: String,
        cause: Throwable
    ) : BackupImportException(message = message, cause = cause)

    class IoFailure(
        message: String,
        cause: Throwable
    ) : BackupImportException(message = message, cause = cause)
}
