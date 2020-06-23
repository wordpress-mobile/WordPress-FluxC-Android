package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.EncryptedLog
import org.wordpress.android.fluxc.network.rest.wpcom.encryptedlog.EncryptedLogRestClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptedLogStore @Inject constructor(
    private val encryptedLogRestClient: EncryptedLogRestClient
) {
    /**
     * Create and provide a log file - save the details of it to DB
     * Add sqlutils to retrieve logs from DB - check that the file still exists, if it doesn't delete the row
     * Add a way to start new uploads
     * Find the file that should be uploaded first
     * If upload succeeds delete the row
     * If upload fails update the DB
     * When upload finishes check if there are any new uploads
     * Have a way to set a file to be uploaded (which should also trigger uploading new files)
     */
    private suspend fun uploadLog(log: EncryptedLog) {
        encryptedLogRestClient.uploadLog(log.uuid, log.file)
    }
}
