definition(
    name: "Shabbat sunset",
    namespace: "jmutnick",
    author: "Jonathan Mutnick",
    description: "Notifies of sunrise and sunset.",
    category: "My Apps",
    iconUrl: "https://s3.us-east-2.amazonaws.com/jmutnick-icons/shabbat-shalom.png",
    iconX2Url: "https://s3.us-east-2.amazonaws.com/jmutnick-icons/shabbat-shalom%402x.png",
    iconX3Url: "https://s3.us-east-2.amazonaws.com/jmutnick-icons/shabbat-shalom%403x.png"
)	

preferences {
    section("Send Push Notification On Sunset?") {
        input "sendPushSet", "bool", required: false, title: "Send Push Notification On Sunset?"
    }
    section("Phrases...") {
        input name: "SunsetPhrase", type: "text", title: "Sunset Phrase", description: "Enter Text", required: true
	}
    section("Device to speak...") {
    	input name: "SpeakingDevice", type: "capability.speechSynthesis", required: true
    }
   	section("On Which Days") {
        input "days", "enum", title: "Select Days of the Week", required: true, multiple: true, options: ["Sunday":"Sunday", "Monday": "Monday", "Tuesday": "Tuesday", "Wednesday": "Wednesday", "Thursday": "Thursday", "Friday": "Friday", "Saturday": "Saturday"]
    }
    section("Sunset offset (optional)...") {
        input "sunsetOffsetValue", "text", title: "HH:MM", required: false
    }
    section("Zip code") {
        input "zipCode", "text", required: false
    }
}

def install() {
	log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated() {
	log.debug "Installed with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() {
	scheduleNextSunset()
    }

def scheduleNextSunset(date = null) {
    def s = getSunriseAndSunset(zipCode: zipCode, sunsetOffset: getSunsetOffset(), date: date)
    //def s = getSunriseAndSunset()
    def now = new Date()
    def setTime = s.sunset
    log.debug "setTime: $setTime"

    // use state to keep track of sunset times between executions
    // if sunset time has changed, unschedule and reschedule handler with updated time
    if(state.setTime != setTime.time) {
        unschedule("sunsetHandler")

        if(setTime.before(now)) {
            setTime = setTime.next()
        }

        state.setTime = setTime.time

        log.info "scheduling sunset handler for $setTime"
        schedule(setTime, sunsetHandler)
    }
}
def sunsetHandler(evt) {
    log.info "Sunset has been detected by my app"
    log.info "Moving on to day check..."
    def df = new java.text.SimpleDateFormat("EEEE")
    def day = df.format(new Date())
    def dayCheck = days.contains(day)
    if (dayCheck) {
    	log.info "Day check has passed"
        log.info "Moving on to push notifications" 
    	if (sendPushSet) {
        	log.info "Push notification set and sending"
        	sendPush(settings.SunsetPhrase)
    	}
        log.info "About to speak"
        try {
        	log.info("Talk")
            def customevent = [displayName: 'MyApp:TalkNow', name: 'TalkNow', value: 'TalkNow']
            Talk(settings.SunsetPhrase, settings.SpeakingDevice, customevent)            
            }
        catch(ex) {
        	log.debug("Failed to speak", ex)
            }
    }
    // schedule for tomorrow
    scheduleNextSunset(new Date() + 1)
}	


private getSunsetOffset() {
    //if there is an offset, make negative since we only care about before
    sunsetOffsetValue ? "-$sunsetOffsetValue" : null
}

def processPhraseVariables(phrase, evt){
    def zipCode = location.zipCode
    if (phrase.toLowerCase().contains(" percent ")) { phrase = phrase.replace(" percent ","%") }
    if (phrase.toLowerCase().contains("%devicename%")) {
    	try {
        	phrase = phrase.toLowerCase().replace('%devicename%', evt.displayName)  //User given name of the device triggering the event
        }
        catch (ex) { 
        	LOGDEBUG("evt.displayName failed; trying evt.device.displayName")
        	try {
                phrase = phrase.toLowerCase().replace('%devicename%', evt.device.displayName) //User given name of the device triggering the event
            }
            catch (ex2) {
            	LOGDEBUG("evt.device.displayName filed; trying evt.device.name")
                try {
                	phrase = phrase.toLowerCase().replace('%devicename%', evt.device.name) //SmartThings name for the device triggering the event
                }
                catch (ex3) {
                	LOGDEBUG("evt.device.name filed; Giving up")
                    phrase = phrase.toLowerCase().replace('%devicename%', "Device Name Unknown")
                }
            }
       }
    }
    if (phrase.toLowerCase().contains("%devicetype%")) {phrase = phrase.toLowerCase().replace('%devicetype%', evt.name)}  //Device type: motion, switch, etc...
    if (phrase.toLowerCase().contains("%devicechange%")) {phrase = phrase.toLowerCase().replace('%devicechange%', evt.value)}  //State change that occurred: on/off, active/inactive, etc...
    if (phrase.toLowerCase().contains("%description%")) {phrase = phrase.toLowerCase().replace('%description%', evt.descriptionText)}  //Description of the event which occurred via device-specific text`
    if (phrase.toLowerCase().contains("%locationname%")) {phrase = phrase.toLowerCase().replace('%locationname%', location.name)}
    if (phrase.toLowerCase().contains("%lastmode%")) {phrase = phrase.toLowerCase().replace('%lastmode%', state.lastMode)}
    if (phrase.toLowerCase().contains("%mode%")) {phrase = phrase.toLowerCase().replace('%mode%', location.mode)}
    if (phrase.toLowerCase().contains("%time%")) {phrase = phrase.toLowerCase().replace('%time%', getTimeFromCalendar(false,true))}
    if (phrase.toLowerCase().contains("%weathercurrent%")) {phrase = phrase.toLowerCase().replace('%weathercurrent%', getWeather("current", zipCode)); phrase = adjustWeatherPhrase(phrase)}
    if (phrase.toLowerCase().contains("%weathertoday%")) {phrase = phrase.toLowerCase().replace('%weathertoday%', getWeather("today", zipCode)); phrase = adjustWeatherPhrase(phrase)}
    if (phrase.toLowerCase().contains("%weathertonight%")) {phrase = phrase.toLowerCase().replace('%weathertonight%', getWeather("tonight", zipCode));phrase = adjustWeatherPhrase(phrase)}
    if (phrase.toLowerCase().contains("%weathertomorrow%")) {phrase = phrase.toLowerCase().replace('%weathertomorrow%', getWeather("tomorrow", zipCode));phrase = adjustWeatherPhrase(phrase)}
    if (phrase.toLowerCase().contains("%weathercurrent(")) {
        if (phrase.toLowerCase().contains(")%")) {
            def phraseZipStart = (phrase.toLowerCase().indexOf("%weathercurrent(") + 16)
            def phraseZipEnd = (phrase.toLowerCase().indexOf(")%"))
            zipCode = phrase.substring(phraseZipStart, phraseZipEnd)
            LOGDEBUG("Custom zipCode: ${zipCode}")
            phrase = phrase.toLowerCase().replace("%weathercurrent(${zipCode})%", getWeather("current", zipCode))
            phrase = adjustWeatherPhrase(phrase.toLowerCase())
        } else {
            phrase = "Custom Zip Code format error in request for current weather"
        }
    }
    if (phrase.toLowerCase().contains("%weathertoday(")) {
        if (phrase.contains(")%")) {
            def phraseZipStart = (phrase.toLowerCase().indexOf("%weathertoday(") + 14)
            def phraseZipEnd = (phrase.toLowerCase().indexOf(")%"))
            zipCode = phrase.substring(phraseZipStart, phraseZipEnd)
            LOGDEBUG("Custom zipCode: ${zipCode}")
            phrase = phrase.toLowerCase().replace("%weathertoday(${zipCode})%", getWeather("today", zipCode))
            phrase = adjustWeatherPhrase(phrase.toLowerCase())
        } else {
            phrase = "Custom Zip Code format error in request for today's weather"
        }
    }
    if (phrase.toLowerCase().contains("%weathertonight(")) {
        if (phrase.contains(")%")) {
            def phraseZipStart = (phrase.toLowerCase().indexOf("%weathertonight(") + 16)
            def phraseZipEnd = (phrase.toLowerCase().indexOf(")%"))
            zipCode = phrase.substring(phraseZipStart, phraseZipEnd)
            LOGDEBUG("Custom zipCode: ${zipCode}")
            phrase = phrase.toLowerCase().replace("%weathertonight(${zipCode})%", getWeather("tonight", zipCode))
            phrase = adjustWeatherPhrase(phrase)
        } else {
            phrase = "Custom Zip Code format error in request for tonight's weather"
        }
    }
    if (phrase.toLowerCase().contains("%weathertomorrow(")) {
        if (phrase.contains(")%")) {
            def phraseZipStart = (phrase.toLowerCase().indexOf("%weathertomorrow(") + 17)
            def phraseZipEnd = (phrase.toLowerCase().indexOf(")%"))
            zipCode = phrase.substring(phraseZipStart, phraseZipEnd)
            LOGDEBUG("Custom zipCode: ${zipCode}")
            phrase = phrase.toLowerCase().replace("%weathertomorrow(${zipCode})%", getWeather("tomorrow", zipCode))
            phrase = adjustWeatherPhrase(phrase)
        } else {
            phrase = "Custom ZipCode format error in request for tomorrow's weather"
        }
    }
    if (state.speechDeviceType == "capability.speechSynthesis"){
        //ST TTS Engine pronunces "Dash", so only convert for speechSynthesis devices (LANnouncer)
        if (phrase.contains(",")) { phrase = phrase.replace(","," - ") }
        //if (phrase.contains(".")) { phrase = phrase.replace("."," - ") }
    }
    if (phrase.toLowerCase().contains("%shmstatus%")) {
    	def shmstatus = location.currentState("alarmSystemStatus")?.value
        LOGDEBUG("SHMSTATUS=${shmstatus}")
		def shmmessage = [off : "Disarmed", away: "Armed, away", stay: "Armed, stay"][shmstatus] ?: shmstatus
        LOGDEBUG("SHMMESSAGE=${shmmessage}")
        phrase = phrase.replace("%shmstatus%", shmmessage)
    }
    if (phrase.contains('"')) { phrase = phrase.replace('"',"") }
    if (phrase.contains("'")) { phrase = phrase.replace("'","") }
    if (phrase.contains("10S")) { phrase = phrase.replace("10S","tens") }
    if (phrase.contains("20S")) { phrase = phrase.replace("20S","twenties") }
    if (phrase.contains("30S")) { phrase = phrase.replace("30S","thirties") }
    if (phrase.contains("40S")) { phrase = phrase.replace("40S","fourties") }
    if (phrase.contains("50S")) { phrase = phrase.replace("50S","fifties") }
    if (phrase.contains("60S")) { phrase = phrase.replace("60S","sixties") }
    if (phrase.contains("70S")) { phrase = phrase.replace("70S","seventies") }
    if (phrase.contains("80S")) { phrase = phrase.replace("80S","eighties") }
    if (phrase.contains("90S")) { phrase = phrase.replace("90S","nineties") }
    if (phrase.contains("100S")) { phrase = phrase.replace("100S","one hundreds") }
    if (phrase.contains("%")) { phrase = phrase.replace("%"," percent ") }
    return phrase
}


def Talk(phrase, customSpeechDevice, evt){
    def currentSpeechDevices = []
    if (state.speechDeviceType == "capability.musicPlayer"){
        state.sound = ""
        state.ableToTalk = false
        if (!(phrase == null)) {
            phrase = processPhraseVariables(phrase, evt)
            LOGTRACE("TALK(${evt.name}) |mP| >> ${phrase}")
            try {
                state.sound = textToSpeech(phrase instanceof List ? phrase[0] : phrase) 
                state.ableToTalk = true
            } catch(e) {
                LOGERROR("ST Platform issue (textToSpeech)? ${e}")
                //Try Again
                try {
                    LOGTRACE("Trying textToSpeech function again...")
                    state.sound = textToSpeech(phrase instanceof List ? phrase[0] : phrase)
                    state.ableToTalk = true
                } catch(ex) {
                    LOGERROR("ST Platform issue (textToSpeech)? I tried textToSpeech() twice, SmartThings wouldn't convert/process.  I give up, Sorry..")
                    sendNotificationEvent("ST Platform issue? textToSpeech() failed.")
                    sendNotification("BigTalker couldn't announce: ${phrase}")
                }
            }
            unschedule("poll")
            LOGDEBUG("Delaying polling for 120 seconds")
            myRunIn(120, poll)
            if (state.ableToTalk){
                state.sound.duration = (state.sound.duration.toInteger() + 5).toString()  //Try to prevent cutting out, add seconds to the duration
                if (!(customSpeechDevice == null)) {
                    currentSpeechDevices = customSpeechDevice
                } else {
                    //Use Default Speech Device
                    currentSpeechDevices = settings.speechDeviceDefault
                }
                LOGTRACE("Last poll: ${state.lastPoll}")
                //Iterate Speech Devices and talk
		        def attrs = currentSpeechDevices.supportedAttributes
                currentSpeechDevices.each(){
            	    //if (state.speechDeviceType == "capability.musicPlayer"){
                	    LOGDEBUG("attrs=${attrs}")
                	    def currentStatus = it.latestValue('status')
                	    def currentTrack = it.latestState("trackData")?.jsonValue
                	    def currentVolume = it.latestState("level")?.integerValue ? it.currentState("level")?.integerValue : 0
                	    LOGDEBUG("currentStatus:${currentStatus}")
                	    LOGDEBUG("currentTrack:${currentTrack}")
                	    LOGDEBUG("currentVolume:${currentVolume}")
                        LOGDEBUG("Sound: ${state.sound.uri} , ${state.sound.duration}")
                	    if (settings.speechVolume) { LOGTRACE("${it.displayName} | Volume: ${currentVolume}, Desired Volume: ${settings.speechVolume}") }
                	    if (!(settings.speechVolume)) { LOGTRACE("${it.displayName} | Volume: ${currentVolume}") }
                	    if (!(currentTrack == null)){
                    	    //currentTrack has data
                            if (!(currentTrack?.status == null)) { LOGTRACE("mP | ${it.displayName} | Current Status: ${currentStatus}, CurrentTrack: ${currentTrack}, CurrentTrack.Status: ${currentTrack.status}.") }
                    	    if (currentTrack?.status == null) { LOGTRACE("mP | ${it.displayName} | Current Status: ${currentStatus}, CurrentTrack: ${currentTrack}.") }
                    	    if (currentStatus == 'playing' || currentTrack?.status == 'playing') {
    	                        LOGTRACE("${it.displayName} | cT<>null | cS/cT=playing | Sending playTrackAndResume().")
        	                    if (settings.speechVolume) { 
                	                if (settings.speechVolume == currentVolume){it.playTrackAndResume(state.sound.uri, state.sound.duration)}
                                    if (!(settings.speechVolume == currentVolume)){it.playTrackAndResume(state.sound.uri, state.sound.duration, settings.speechVolume)}
                    	        } else { 
                            	    if (currentVolume >= 50) { it.playTrackAndResume(state.sound.uri, state.sound.duration) }
                            	    if (currentVolume < 50) { it.playTrackAndResume(state.sound.uri, state.sound.duration, 50) }
                        	    }
                    	    } else
                    	    {
                        	    LOGTRACE("mP | ${it.displayName} | cT<>null | cS/cT<>playing | Sending playTrackAndRestore().")
                        	    if (settings.speechVolume) { 
	                                if (settings.speechVolume == currentVolume){it.playTrackAndRestore(state.sound.uri, state.sound.duration)}
                                    if (!(settings.speechVolume == currentVolume)){it.playTrackAndRestore(state.sound.uri, state.sound.duration, settings.speechVolume)}
	                            } else { 
            	                    if (currentVolume >= 50) { it.playTrackAndRestore(state.sound.uri, state.sound.duration) }
                	                if (currentVolume < 50) { it.playTrackAndRestore(state.sound.uri, state.sound.duration, 50) }
                    	        }
                    	    }
                	    } else {
                    	    //currentTrack doesn't have data or is not supported on this device
                            if (!(currentStatus == null)) {
                    	        LOGTRACE("mP | ${it.displayName} | (2) Current Status: ${currentStatus}.")
                                if (currentStatus == "disconnected") {
	                                //VLCThing?
    	                            LOGTRACE("mP | ${it.displayName} | cT=null | cS=disconnected | Sending playTrackAndResume().")
	                                if (settings.speechVolume) { 
                    	                if (settings.speechVolume == currentVolume){it.playTrackAndResume(state.sound.uri, state.sound.duration)}
                                        if (!(settings.speechVolume == currentVolume)){it.playTrackAndResume(state.sound.uri, state.sound.duration, settings.speechVolume)}
                        	        } else { 
                                        if (currentVolume >= 50) { it.playTrackAndResume(state.sound.uri, state.sound.duration) }
                	                    if (currentVolume < 50) { it.playTrackAndResume(state.sound.uri, state.sound.duration, 50) }
                        	        }
                    	        } else {
    	                            if (currentStatus == "playing") {
            	                        LOGTRACE("mP | ${it.displayName} | cT=null | cS=playing | Sending playTrackAndResume().")
                	                    if (settings.speechVolume) { 
                        	                if (settings.speechVolume == currentVolume){it.playTrackAndResume(state.sound.uri, state.sound.duration)}
                                            if (!(settings.speechVolume == currentVolume)){it.playTrackAndResume(state.sound.uri, state.sound.duration, settings.speechVolume)}
                            	        } else { 
        	                                if (currentVolume >= 50) { it.playTrackAndResume(state.sound.uri, state.sound.duration) }
            	                            if (currentVolume < 50) { it.playTrackAndResume(state.sound.uri, state.sound.duration, 50) }
                	                    }
                    	            } else {
                            	        LOGTRACE("mP | ${it.displayName} | cT=null | cS<>playing | Sending playTrackAndRestore().")
                            	        if (settings.speechVolume) { 
                                	        if (settings.speechVolume == currentVolume){it.playTrackAndRestore(state.sound.uri, state.sound.duration)}
                                            if (!(settings.speechVolume == currentVolume)){it.playTrackAndRestore(state.sound.uri, state.sound.duration, settings.speechVolume)}
                            	        } else { 
	                                        if (currentVolume >= 50) { it.playTrackAndRestore(state.sound.uri, state.sound.duration) }
    	                                    if (currentVolume < 50) { it.playTrackAndRestore(state.sound.uri, state.sound.duration, 50) }
        	                            }
            	                    }
                	            }
                            } else {
                                //currentTrack and currentStatus are both null
                                LOGTRACE("mP | ${it.displayName} | (3) cT=null | cS=null | Sending playTrackAndRestore().")
                                if (settings.speechVolume) { 
                                    if (settings.speechVolume == currentVolume){it.playTrackAndRestore(state.sound.uri, state.sound.duration)}
                                    if (!(settings.speechVolume == currentVolume)){it.playTrackAndRestore(state.sound.uri, state.sound.duration, settings.speechVolume)}
                                } else { 
	                                if (currentVolume >= 50) { it.playTrackAndRestore(state.sound.uri, state.sound.duration) }
    	                            if (currentVolume < 50) { it.playTrackAndRestore(state.sound.uri, state.sound.duration, 50) }
        	                    }
                            }
                	    }
                    } //currentSpeechDevices.each()
            	} //state.ableToTalk
            } //!phrase == null
        } else {
            //capability.speechSynthesis is in use
            if (!(phrase == null)) {
                phrase = processPhraseVariables(phrase, evt)
                LOGTRACE("TALK(${evt.name}) |sS| >> ${phrase}")
                if (!(customSpeechDevice == null)) {
                    currentSpeechDevices = customSpeechDevice
                } else {
                    //Use Default Speech Device
                    currentSpeechDevices = settings.speechDeviceDefault
                }
                //Iterate Speech Devices and talk
		        def attrs = currentSpeechDevices.supportedAttributes
                currentSpeechDevices.each(){
	                try {
                    	LOGTRACE("sS | ${it.displayName} | Sending speak().")
                    }
                    catch (ex) {
                    	LOGDEBUG("LOGTRACE it.displayName failed, trying it.device.displayName")
                    	try {
                    		LOGTRACE("sS | ${it.device.displayName} | Sending speak().")
                        }
                        catch (ex2) {
                        	LOGDEBUG("LOGTRACE it.device.displayName failed, trying it.device.name")
                        	LOGTRACE("sS | ${it.device.name} | Sending speak().")
                        }
                    }
	                it.speak(phrase)
                }
    	    } //!phrase == null
        } //state.speechDeviceType
}//Talk()


def LOGDEBUG(txt){
    if (settings.debugmode) { log.debug("${app.label.replace(" ","").toUpperCase()}(${state.appversion}) || ${txt}") }
}
def LOGTRACE(txt){
    log.trace("${app.label.replace(" ","").toUpperCase()}(${state.appversion}) || ${txt}")
}
def LOGERROR(txt){
    log.error("${app.label.replace(" ","").toUpperCase()}(${state.appversion}) || ERROR: ${txt}")
}