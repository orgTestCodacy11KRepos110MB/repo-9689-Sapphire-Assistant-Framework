package com.example.calendarskill

import android.content.Intent
import android.net.Uri
import android.os.IBinder
import com.example.componentframework.SapphireFrameworkRegistrationService
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.URI

class CalendarModuleInstallServiceRefinedTwo: SapphireFrameworkRegistrationService(){
    val VERSION = "0.0.1"
    val CONFIG = "calendar.conf"
    val fileList = arrayListOf<String>("get.intent","set.intent")
    var ACTION_MANIPULATE_FILEDATA = "action.framework.module.MANIPULATE_FILE_DATA"
    var ACTION_REQUEST_FILEDATA = "action.framework.module.REQUEST_FILE_DATA" +
            "DATA"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(this.javaClass.name,"Calendar intent received")
        when(intent?.action){
            ACTION_SAPPHIRE_MODULE_REGISTER -> registerModule(intent)
            ACTION_MANIPULATE_FILEDATA -> coreTransferFile(intent)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun registerModule(intent: Intent) {
        super.registerModule(intent)
    }

    fun coreTransferFile(intent: Intent){
        try{
            when(intent.hasExtra(DATA_KEYS)){
                true -> processFiles(intent)
                false -> Log.d(this.javaClass.name, "There was some kind of DATA_KEY error")
            }
        }catch(exception: Exception){
            Log.d(this.javaClass.name, "Exception. There was some kind of DATA_KEY error")
            Log.d(this.javaClass.name, exception.toString())
        }
    }

    // This seems like unneeded modularity
    fun processFiles(intent: Intent){
        var clipData = intent.clipData!!
        var firstUri = intent.data!!

        writeToCore(firstUri)
        for(clipIndex in 0..clipData.itemCount){
            // This is how it has to be done w/ clipData it seems
            writeToCore(clipData.getItemAt(clipIndex).uri)
        }
    }

    fun writeToCore(uri: Uri){
        try {
            Log.i(this.javaClass.name,uri.toString()!!)
            //var testFile = uri.toFile()
            var somethingFD = contentResolver.openFileDescriptor(uri,"wa")!!
            var fd = somethingFD.fileDescriptor
            var outputStream = FileOutputStream(fd)
            outputStream.write(". This is appended".toByteArray())
            Log.i(this.javaClass.name,"Did it write?")

            // This is the essential part, when it comes to editing a file
            somethingFD = contentResolver.openFileDescriptor(uri,"rw")!!
            fd = somethingFD.fileDescriptor
            var inputStream = FileInputStream(fd)

            var testFile = File(cacheDir,"temp")
            var fileWriter = testFile.outputStream()

            var data = inputStream!!.read()
            while(data != -1){
                fileWriter.write(data)
                data = inputStream.read()
            }
            fileWriter.close()

            Log.i(this.javaClass.name, testFile.readText())

            Log.i(this.javaClass.name, "This seems like a valid way to edit the file")
        }catch (exception: Exception){
            Log.d(this.javaClass.name, "You cannot access the file this way")
            Log.i(this.javaClass.name, exception.toString())
        }
    }

    fun demoRequestFile(intent: Intent){
        var uri = intent.data!!
        try {
            var uri = intent.data!!
            Log.i(this.javaClass.name,uri.toString()!!)
            //var testFile = uri.toFile()
            var somethingFD = contentResolver.openFileDescriptor(uri,"wa")!!
            var fd = somethingFD.fileDescriptor
            var outputStream = FileOutputStream(fd)
            outputStream.write(". This is appended".toByteArray())
            Log.i(this.javaClass.name,"Did it write?")

            // This is the essential part, when it comes to editing a file
            somethingFD = contentResolver.openFileDescriptor(uri,"rw")!!
            fd = somethingFD.fileDescriptor
            var inputStream = FileInputStream(fd)

            var testFile = File(cacheDir,"temp")
            var fileWriter = testFile.outputStream()

            var data = inputStream!!.read()
            while(data != -1){
                fileWriter.write(data)
                data = inputStream.read()
            }
            fileWriter.close()

            Log.i(this.javaClass.name, testFile.readText())

            Log.i(this.javaClass.name, "This seems like a valid way to edit the file")
        }catch (exception: Exception){
            Log.d(this.javaClass.name, "You cannot access the file this way")
            Log.i(this.javaClass.name, exception.toString())
        }
    }

    fun p2pFile(): Uri?{
        var uri = null
        return uri
    }

    fun contentProvider(){
        // Implemented by developer
    }

    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }
}