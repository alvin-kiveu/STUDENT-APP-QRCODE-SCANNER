package com.example.classorcodescan

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.classorcodescan.databinding.ActivityMainBinding
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private val sharedPrefFile = "classorcodescan"
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPreferences: SharedPreferences = this.getSharedPreferences(
            sharedPrefFile,
            Context.MODE_PRIVATE
        )


        val loginUser = sharedPreferences.getString("loginRegNo", "")
        val userData = loginUser.toString()
        if (userData.isBlank()) {

        val loginBtn = findViewById<Button>(R.id.login_button)
        val studentRegNo = findViewById<EditText>(R.id.studentregno)
        val password = findViewById<EditText>(R.id.password)
        val builder = AlertDialog.Builder(this)

        loginBtn!!.setOnClickListener {
            //CONVERT INPUT TO STRING
            val userStudentRegNo = studentRegNo.text
            val userPassword = password.text
            if (userStudentRegNo.isBlank() || userPassword.isBlank()) {
                builder.setTitle("LOGIN FAILED")
                builder.setMessage("Please all fill the required field!!")
                builder.setPositiveButton("Try Again",
                    { dialogInterface: DialogInterface, i: Int -> })
                builder.show()
            } else {

                val queue = Volley.newRequestQueue(this)
                val dataUrl = "https://class.kariukijames.com/api/studentlogin"

                val builder = AlertDialog.Builder(this)

                val params = HashMap<String, String>()
                params["studentRegNo"] = userStudentRegNo.toString()
                params["userPass"] = userPassword.toString()
                val jsonObject = JSONObject(params as Map<*, *>?)
                val request = JsonObjectRequest(
                    Request.Method.POST, dataUrl, jsonObject,
                    { stringResponse ->
                        val resultCode = stringResponse.getString("ResultCode")
                        if (resultCode.toString() == "200") {
                            val successMessage = stringResponse.getString("successMessage")
                            val editor: SharedPreferences.Editor = sharedPreferences.edit()
                            editor.putString("loginRegNo", userStudentRegNo.toString())
                            editor.apply()
                            editor.commit()
                            builder.setTitle("LOGIN SUCCESS ✔")
                            builder.setMessage(successMessage)
                            builder.setPositiveButton("Okay") { dialogInterface: DialogInterface, i: Int ->
                                val intent = Intent(this, HomePage::class.java)
                                startActivity(intent)
                                finish()
                            }
                            builder.show()
                        } else {
                            val errorMessage = stringResponse.getString("errorMessage")
                            builder.setTitle("LOGIN FAILED!!!")
                            builder.setMessage(errorMessage)
                            builder.setPositiveButton("Try Again") { dialogInterface: DialogInterface, i: Int ->
                            }

                            builder.show()
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

            }
        }
    }else{
            val intent = Intent(this, HomePage::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}