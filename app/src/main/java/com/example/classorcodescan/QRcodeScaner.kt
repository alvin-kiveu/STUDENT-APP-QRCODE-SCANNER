package com.example.classorcodescan

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.util.isNotEmpty
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.MultiDetector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import org.json.JSONObject
import java.io.IOException
private var loginUser: String = ""


class QRcodeScaner : AppCompatActivity() {
    private val sharedPrefFile = "classorcodescan"
    private val TAG = "QRcodeScaner"
    private lateinit var surfaceView: SurfaceView
    private lateinit var qrcodeText: TextView
    private lateinit var cameraSource: CameraSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qrcode_scaner)
        surfaceView = findViewById(R.id.surface_view)
        qrcodeText = findViewById(R.id.qrcode_text)

        val sharedPreferences: SharedPreferences = this.getSharedPreferences(
            sharedPrefFile,
            Context.MODE_PRIVATE
        )

        loginUser = sharedPreferences.getString("loginRegNo","")!!

        val backBtn = findViewById<Button>(R.id.back_button)
        backBtn!!.setOnClickListener{
            val intent = Intent(this, HomePage::class.java)
            startActivity(intent)
            finish()
        }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        } else {
            startCamera()
        }


    }

    override fun onResume() {
        super.onResume()
        startCamera()
    }

    override fun onPause() {
        super.onPause()
        cameraSource.stop()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Camera permission is required to scan QR code",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }



    private fun startCamera() {
        val barcodeDetector = BarcodeDetector.Builder(this)
            .setBarcodeFormats(Barcode.QR_CODE)
            .build()

        val multiDetector = MultiDetector.Builder()
            .add(barcodeDetector)
            .build()

        val cameraSourceBuilder = CameraSource.Builder(this, multiDetector)
            .setFacing(CameraSource.CAMERA_FACING_BACK)
            .setAutoFocusEnabled(true)
            .setRequestedFps(30.0f)

        cameraSource = cameraSourceBuilder.build()
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            @SuppressLint("MissingPermission")
            override fun surfaceCreated(holder: SurfaceHolder) {
                try {
                    cameraSource.start(holder)
                } catch (e: IOException) {
                    Log.e(TAG, "Error starting camera source: ${e.message}")
                }
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                cameraSource.stop()
            }
        })

        barcodeDetector.setProcessor(object : Detector.Processor<Barcode> {
            override fun release() {}

            override fun receiveDetections(detections: Detector.Detections<Barcode>) {
                detections.let { detection ->



                    val qrCodes = detection.detectedItems

                    if (qrCodes.isNotEmpty()) {
                        val qrCode = qrCodes.valueAt(0)
                        val lesson = qrCode.rawValue

                        //SENDING THE DATA TO THE API
                        val queue = Volley.newRequestQueue(this@QRcodeScaner)
                        val dataUrl = "https://class.kariukijames.com/api/studentsign"

                        val builder = AlertDialog.Builder(this@QRcodeScaner)

                        val params = HashMap<String, String>()
                        params["studentRegNo"] = loginUser
                        params["lessonId"] = lesson
                        val jsonObject = JSONObject((params as Map<*, *>?)!!)
                        val request = JsonObjectRequest(
                            Request.Method.POST, dataUrl, jsonObject,
                            { stringResponse ->
                                val resultCode = stringResponse.getString("ResultCode")
                                if (resultCode == "200") {
                                    cameraSource.stop()
                                    val userStudentNo = stringResponse.getString("studentid")
                                    val userStudentName = stringResponse.getString("studentname")
                                    val userStudentCourse = stringResponse.getString("coursename")
                                    val userStudentLesson = stringResponse.getString("lessonname")

                                    val message = "Student Name: $userStudentName\n" +
                                            "Student Reg No: $userStudentNo\n" +
                                            "Course : $userStudentCourse\n" +
                                            "Lesson : $userStudentLesson"
                                    builder.setTitle("Attendance Sign Success ✔")
                                    builder.setMessage(message)

                                    builder.setPositiveButton("Okay") { dialogInterface: DialogInterface, i: Int ->
                                        val intent = Intent(this@QRcodeScaner, HomePage::class.java)
                                        startActivity(intent)
                                        finish()
                                    }

                                    builder.show()
                                }else{
                                    val errorMessage = stringResponse.getString("errorMessage")
                                    builder.setTitle("Attendance Sign Failed!!")
                                    builder.setMessage(errorMessage)
                                    builder.setPositiveButton("Try Again") { dialogInterface: DialogInterface, i: Int ->
                                        val intent = Intent(this@QRcodeScaner, QRcodeScaner::class.java)
                                        startActivity(intent)
                                        finish()
                                    }
                                    builder.show()
                                }
                            },
                            { resError ->
                                // handle error
                                builder.setMessage("Network Error failed. Check your internet connection $lesson")
                                builder.setPositiveButton("Retry") { dialogInterface: DialogInterface, i: Int ->
                                }
                                builder.show()
                            }
                        )
                        queue.add(request)


                    }
                }
            }
        })
    }


    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
    }

}