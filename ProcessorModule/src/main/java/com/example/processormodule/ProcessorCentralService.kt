package com.example.processormodule

import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.example.componentframework.SAFService
import edu.stanford.nlp.classify.ColumnDataClassifier
import java.io.File

class ProcessorCentralService: SAFService(){

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.i("ProcessorCentralService","Data processing intent received")
        if(intent.action == ACTION_SAPPHIRE_TRAIN) {
            intent.setClassName(this,"com.example.processormodule.ProcessorTrainingService")
            // Send it to the training service
            startService(intent)
        }else if(intent.hasExtra(MESSAGE)){
            // Default to the purpose of the processor
            var text = intent.getStringExtra(MESSAGE)!!
            process(text)
        }

        return super.onStartCommand(intent, flags, startId)
    }

    fun process(text: String){
        var outgoingIntent = Intent()

        try{
            if(text != ""){
                var classifier = loadClassifier()
                // This is specific to how CoreNLP works
                var datumToClassify = classifier.makeDatumFromLine("none\t${text}")
                // Can these two be combined, or done at the same time?
                var classifiedDatum = classifier.classOf(datumToClassify)
                var classifiedScores = classifier.scoresOf(datumToClassify)
                Log.v("ProcessorCentralService","Datum classification: ${classifiedDatum}")
                // This is an arbitrary number, and should probably be a configurable variable
                if(classifiedScores.getCount(classifiedDatum) >= .04){
                    Log.i("ProcessorCentralService","Text matches class ${classifiedDatum}")
                    outgoingIntent.putExtra(POSTAGE,classifiedDatum)
                }else {
                    Log.i("ProcessorCentralService","Text does not match a class. Using default")
                    // This could generate an error
                    outgoingIntent.putExtra(POSTAGE,"DEFAULT")
                }

                /**
                 * I actually may not need to send out unformatted text. This filter is transforming it,
                 * so the next module probably doesn't need the unformatted text. I can just log a reference
                 * for text & binary sources, so that if a module needs it then a request can be made for
                 * the base data along the pipeline. This prevents overcomplicating the protocol
                  */
                outgoingIntent.putExtra(MESSAGE,text)
                startService(outgoingIntent)
            }
        }catch(exception: Exception){
            Log.e("ProcessorCentralService","There was an error trying to process the text")
        }
    }

    fun loadClassifier(): ColumnDataClassifier{
        var classifierFile = File(this.filesDir,"Intent.classifier")


        // I can't spontaneously train without requesting the data. Can this be overcome?
        if(classifierFile.exists() != true) {
            Log.e("ProcessorCentralService","There is no saved classifier. Requesting training data from core")
            var coreDataRequestIntent = Intent()
            coreDataRequestIntent.action = ACTION_SAPPHIRE_CORE_REQUEST_DATA
        }

        return ColumnDataClassifier.getClassifier(classifierFile.canonicalPath)
    }
}