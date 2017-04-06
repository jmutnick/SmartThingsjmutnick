definition(
    name: "Test App",
    namespace: "jmutnick",
    author: "Jonathan Mutnick",
    description: "Will perform various tests on things when needed.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)	

preferences {
	section("Choose your preferences...") {
        input "myCamera","capability.imageCapture", title: "Select Camera", submitOnChange: true
        input "myMotion","capability.motionSensor", title: "Select Motion Sensor", submitOnChange: true
        input "sendPushSet", "bool", required: false, title: "Send Push Notifications?"
        //input "signal", "capability.signalStrength", require:false, title: "Signal Strength on Device"
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() {
    subscribe(myMotion, "motion.active", motionHandler)
}

def motionHandler(evt) {
	try {
    	log.debug "motionHandler called: $evt"
        myCamera.take()
        log.debug "Picture taken"
        }
    catch(ex) {
    	log.debug "Problem"
            }
}

