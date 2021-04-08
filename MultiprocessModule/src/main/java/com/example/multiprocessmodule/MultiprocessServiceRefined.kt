package com.example.multiprocessmodule

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.example.componentframework.SapphireFrameworkService
import org.json.JSONObject
import java.lang.Exception
import kotlin.math.absoluteValue
import kotlin.random.Random

/*
This also can be used to wait for a response for a single intent. Perhaps it should have a different name

The biggest difference between this, and how something like the CoreRegistrationService works is that
the CoreRegistrationService is idle until the last intent returns. This one is meant to to processing
in parallel, and as such has a different kind of complexity.

I believe this may *have* to bind CoreService, now that Uris are the only way to pass around information
I can't really record it to a JSON file the same way.
 */

class MultiprocessServiceRefined: SapphireFrameworkService(){

	// This ties the Multiprocess to Core much more than I wanted. Perhaps I should specify 'levels' of modules
	inner class Connection() : ServiceConnection {
		override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
			Log.i(this.javaClass.name, "Service connected")
		}

		override fun onServiceDisconnected(name: ComponentName?) {
			Log.i(this.javaClass.name, "Service disconnected")
		}
	}

	// This could be replaced w/ core ID
	val MULTIPROCESS_ID = "assistant.framework.multiprocess.protocol.ID"
	// This holds the value while waiting for all returns
	var storedIntents = mutableListOf<Intent>()
	// This holds the relavent indexes for each ID
	var intentLedger = JSONObject()
	// This holds specific information about each multiprocess intent
	var connection = Connection()

	override fun onCreate(){
		var coreIntent = Intent().setClassName(this.packageName,"com.example.sapphireassistantframework.CoreService")
		bindService(coreIntent,connection,0)
		super.onCreate()
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		when(intent?.hasExtra(MULTIPROCESS_ID)){
			true -> evaluateReturningIntent(intent)
			false -> handleNewMultiprocessIntent(intent)
			else -> Log.d(this.javaClass.name, "There was an intent error. Stopping Multiprocess Module...")
		}
		return super.onStartCommand(intent, flags, startId)
	}

	// Do what must be done
	fun handleNewMultiprocessIntent(intent: Intent?){
		// The record information for MULTIPROCESS_ID
		var intentRecord = JSONObject()
		var multiprocessIntent = prepareIntent(intent!!)

		// Add the initial intent and its data to storage, to await all results
		storedIntents.add(multiprocessIntent)
		// The size is the index of the related intent, so it can be pulled from StoredIntent
		intentRecord.put("ORIGINAL",storedIntents.size)
		// get the list of routes from the prepared intent
		for(route in multiprocessIntent.getStringArrayExtra("MULTIPROCESS_ROUTE_LIST")!!){
			var packageClass = route.split(";")
			// Make a placeholder record using the route as the key, and a zero which will be filled by an index from a returned intent with matching FROM
			intentRecord.put(route,0)
			multiprocessIntent.setClassName(packageClass[0],packageClass[1])
			// This is going to bind the service, and I need to be consious of that. I can probably tie the connection to the packageClass/FROM
			startSapphireService(multiprocessIntent)
		}
		// Store the record for this MULTIPROCESS_ID
		intentLedger.put(multiprocessIntent.getStringExtra(MULTIPROCESS_ID),intentRecord)
	}

	// This likely *only* works if the data is coming from the core, as that is the only time the permission applies to ALL uris
	fun sendUltimateResult(intentRecord: JSONObject){
		var resultIntent = Intent()
		var clipData = resultIntent.clipData

		for(value in intentRecord.keys()){
			when(value){
				"ORIGINAL" -> continue
			}
		}
	}

	fun evaluateReturningIntent(intent: Intent?){
		try{
			// Load the intent recod
			var intentRecord = intentLedger.getJSONObject(intent!!.getStringExtra(MULTIPROCESS_ID))
			storedIntents.add(intent)
			// the FROM is the unqiue ID for an intent from this MULTIPROCESS_ID. The size is the index, conveniently
			intentRecord.put(intent.getStringExtra(FROM),storedIntents.size)
			// Save the new information
			intentLedger.put(intent.getStringExtra(MULTIPROCESS_ID),intentRecord)
			// Check all the values for a zero. If there is one, keep waiting for inputs. Else, all subprocesses received
			for(key in intentRecord.keys()){
				if(intentRecord.getInt(key) == 0){
					// All finished. Wait fore more intents
					return
				}
			}
			// Ah! This multiprocess has finished. Time to ship it out
			sendUltimateResult(intentRecord)

		}catch(exception: Exception){
			Log.d(CLASS_NAME,exception.toString())
		}
	}

	// This is just a convenience method to help make things more readable
	fun prepareIntent(intent: Intent): Intent{
		// Give it an ID for tracking, and generate a mutable Intent
		var preparedIntent = generateId(intent)
		preparedIntent = regexRouteString(preparedIntent)
		preparedIntent = makeMultiprocessList(preparedIntent)
		// Make sure that every module knows what's going on
		preparedIntent.setAction(ACTION_REQUEST_FILE_DATA)
		return preparedIntent
	}

	// This is unique to the Multiprocess Module. I need it to look for the unique () syntax
	fun regexRouteString(intent: Intent): Intent{
		var route = intent.getStringExtra(ROUTE)!!
		// Break out the multiprocess syntax
		var start = route.indexOf("(")+1
		var end = route.indexOf(")",start)
		// Return the multiprocess block
		var multiprocessRoute = route.substring(start,end)
		// Return the rest of the information
		var remainingRoute = route.substring(end+1,)

		// I think it's just easier to pass around the intent right now
		intent.putExtra("MULTIPROCESS_ROUTE",multiprocessRoute)
		var returnModule = "${this.packageName};${this.javaClass.canonicalName},"
		// This is to have it return. I could probably move this to preparedIntent()
		intent.putExtra(ROUTE, returnModule+remainingRoute)
		return intent
	}

	// This simply takes the multiprocess route string and turns it in to a list
	fun makeMultiprocessList(intent: Intent): Intent{
		var preparedIntent = Intent(intent)
		var routeList: ArrayList<String> = preparedIntent.getStringExtra("MULTIPROCESS_ROUTE")!!.split(",") as ArrayList<String>
		preparedIntent.putExtra("MULTIPROCESS_ROUTE_LIST", routeList)
		return preparedIntent
	}

	// This should do some
	fun generateId(intent: Intent?): Intent{
		var id = -1
		do{
			id = Random.nextInt().absoluteValue
		}while((id == -1) and (intentLedger.isNull(id.toString()) == false))

		intent!!.putExtra(MULTIPROCESS_ID,id)
		return intent
	}

	override fun onBind(intent: Intent?): IBinder? {
		TODO("Not yet implemented")
	}
}