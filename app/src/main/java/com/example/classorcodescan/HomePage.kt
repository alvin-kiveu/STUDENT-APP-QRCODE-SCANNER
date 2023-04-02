package com.example.classorcodescan

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class HomePage : AppCompatActivity() {
    private val sharedPrefFile = "classorcodescan"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_page)
        val sharedPreferences: SharedPreferences = this.getSharedPreferences(
            sharedPrefFile,
            Context.MODE_PRIVATE
        )

        val loginUser = sharedPreferences?.getString("loginRegNo","")

        val logoutBtn = findViewById<Button>(R.id.logoutBtn)
        val qrCodeBtn = findViewById<Button>(R.id.viewCourse)
        val textViewName = findViewById<TextView>(R.id.textViewName)
        val textViewEmail = findViewById<TextView>(R.id.textViewEmail)
        val textViewRegNo = findViewById<TextView>(R.id.textViewRegNo)
        val textViewCourse = findViewById<TextView>(R.id.textViewCourse)

        val queue = Volley.newRequestQueue(this)
        val dataUrl = "https://class.kariukijames.com/api/getstudentinfo"

        val builder = AlertDialog.Builder(this)

        val params = HashMap<String, String>()
        params["studentRegNo"] = loginUser.toString()

        val jsonObject = JSONObject(params as Map<*, *>?)
        val request = JsonObjectRequest(
            Request.Method.POST, dataUrl, jsonObject,
            { stringResponse ->
                val resultCode = stringResponse.getString("ResultCode")
                if (resultCode.toString() == "200") {
                    val userStudentNo = "Reg No: ${stringResponse.getString("studentid")}"
                    val userStudentName = "Name: ${stringResponse.getString("studentname")}"
                    val userStudentEmail = "Email: ${stringResponse.getString("studentemail")}"
                    val userStudentCourse = "Course: ${stringResponse.getString("coursename")}"

                    textViewRegNo.setText(userStudentNo).toString()
                    textViewEmail.setText(userStudentName).toString()
                    textViewName.setText(userStudentEmail).toString()
                    textViewCourse.setText(userStudentCourse).toString()

                }
            },
            { resError ->
                // handle error
                builder.setMessage("Network Error failed. Check your internet connection")
                builder.setPositiveButton("Retry") { dialogInterface: DialogInterface, i: Int ->
                }
                builder.show()
            }
        )
        queue.add(request)


        logoutBtn!!.setOnClickListener {
            val editor: SharedPreferences.Editor = sharedPreferences.edit()
            editor.putString("loginRegNo", "")
            editor.apply()
            editor.commit()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        qrCodeBtn!!.setOnClickListener{
            val intent = Intent(this, QRcodeScaner::class.java)
            startActivity(intent)
            finish()
        }
    }
}
