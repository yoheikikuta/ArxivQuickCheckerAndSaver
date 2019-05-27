package io.github.yoheikikuta.arxivquickcheckerandsaver

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.github.kittinunf.fuel.coroutines.awaitByteArrayResponseResult
import com.github.kittinunf.fuel.httpGet
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.*
import java.io.*

typealias DriveFile = com.google.api.services.drive.model.File

// This class refers to https://code.luasoftware.com/tutorials/android/setup-android-google-drive-rest-api/.
class GoogleDriveSaver : AppCompatActivity(), CoroutineScope by MainScope()  {

    companion object {
        private const val REQUEST_SIGN_IN = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestSignIn()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when(requestCode) {
            REQUEST_SIGN_IN -> {
                if (resultCode == RESULT_OK && data != null) {
                    val bundle = intent.extras
                    val title: String = bundle.getString("title")
                    val link: String = bundle.getString("link")
                    handleSignInResult(data, title, link)
                }
                else {
                    println("Sign-in request failed")
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun buildGoogleSignInClient(): GoogleSignInClient {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Scope(DriveScopes.DRIVE))
            .build()
        return GoogleSignIn.getClient(this, signInOptions)
    }

    private fun handleSignInResult(result: Intent, title: String, link: String) {
        GoogleSignIn.getSignedInAccountFromIntent(result)
            .addOnSuccessListener { googleAccount ->
                // Use the authenticated account to sign in to the Drive service.
                val credential = GoogleAccountCredential.usingOAuth2(
                    this, listOf(DriveScopes.DRIVE)
                )
                credential.selectedAccount = googleAccount.account
                val googleDriveService = Drive.Builder(
                    AndroidHttp.newCompatibleTransport(),
                    JacksonFactory.getDefaultInstance(),
                    credential)
                    .setApplicationName(getString(R.string.app_name))
                    .build()

                // GET pdf file.
                launch(Dispatchers.Default) {
                    val (_, _, result) = link.httpGet().awaitByteArrayResponseResult(scope = Dispatchers.IO)

                    // Uploading file refers to https://developers.google.com/drive/api/v3/manage-uploads.
                    // To make FileContent with file's URI, the pdf file is saved as a temp file.
                    val temp = File.createTempFile("temp", ".pdf")
                    val bos = BufferedOutputStream(FileOutputStream(temp))
                    bos.write(result.get())
                    bos.close()

                    val blobPdf = FileContent("application/pdf", File(temp.path))
                    val targetDriveFile = DriveFile()
                    targetDriveFile.name = makePaperFileName(title, link)
                    targetDriveFile.parents = arrayListOf("1NO62Pw__M2Em-Y77uMfPoDf1-VajlGCu")
                    googleDriveService.files().create(targetDriveFile, blobPdf)
                        .setFields("id")
                        .execute()
                }

            }
            .addOnFailureListener { e ->
                println("Sign-in error: $e")
            }
    }

    /**
     * Make a file name of given title and link.
     * @args title: "Some arXiv paper. some char", link: "https://arxiv.org/pdf/1905.09314.pdf"
     * @return "[1905.09314] Some arXiv paper.pdf"
     */
    private fun makePaperFileName(title: String, link: String) : String {
        val arXivID = "\\d+.\\d+".toRegex(RegexOption.IGNORE_CASE).find(link)?.value!!
        val titleText = ".+\\.".toRegex(RegexOption.IGNORE_CASE).find(title)?.value!!

        return "[$arXivID] ${titleText}pdf"
    }

    private fun requestSignIn() {
        val client = buildGoogleSignInClient()
        startActivityForResult(client.signInIntent, REQUEST_SIGN_IN)
    }
}