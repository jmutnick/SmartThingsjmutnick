definition(
    name: "Exhaust on High Humidity",
    namespace: "jmutnick",
    author: "Jonathan Mutnick; jmutnick@umich.edu",
    description: "Turn on exhaust fan on high humidity",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Choose humidity sensor and exhaust fan:") {
        input "humidity", "capability.relativeHumidityMeasurement", title: "Humidity sensor"
        input "exhaust", "capability.switch", title: "Exhaust Fan"
        input "HumidityValue", "text", title: "Humidity to control switch"
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
    subscribe(humidity, "humidity", humidityEvent)
}

def humidityEvent(evt){
    log.trace "Current Humidity is ${evt.value} as of ${evt.date}"

	def currentValue = exhaust.currentValue("switch")
    log.debug "the current value of the exhaust fan is $currentValue"

    if (evt.value >= HumidityValue) { 
        if (currentValue == "off") {
        	exhaust.on()
        	log.debug "Turning on Exhaust Fan"
        }
    }
    
    if (evt.value < HumidityValue) {
    	if (currentValue == "on") {
        	exhaust.off()
        	log.debug "Turning off Exhaust Fan"
        }
    }
}