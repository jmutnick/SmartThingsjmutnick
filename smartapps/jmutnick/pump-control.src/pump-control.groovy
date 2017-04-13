/**
 *  Dry the Wetspot
 *
 *  Copyright 2014 Scottin Pollock
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "Pump Control",
    namespace: "jmutnick",
    author: "Scottin Pollock, edited by JMutnick",
    description: "Turns switch on and off based on moisture sensor input.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/water_moisture.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/water_moisture@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Meta/water_moisture@2x.png"
)


preferences {
	section("When water is sensed...") {
		input "sensor", "capability.waterSensor", title: "Where?", required: true
	}
	section("Turn on a pump...") {
		input "pump", "capability.switch", title: "Which?", required: true
	}
    section("Send Notifications?") {
        input("recipients", "contact", title: "Send notifications to", multiple:true)
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	subscribe(sensor, "water.dry", waterHandler)
	subscribe(sensor, "water.wet", waterHandler)
}

def updated() {
	log.debug "Installed with settings: ${settings}"
	unsubscribe()
	subscribe(sensor, "water.dry", waterHandler)
	subscribe(sensor, "water.wet", waterHandler)
}

def waterHandler(evt) {
	log.debug "Pump says ${evt.value}"
	if (evt.value == "wet") {
        try {
			pump.on([delay: 15000]) //wait 15 seconds
        	sendNotificationToContacts("Flood detected by ${sensor.displayName}. ${pump.displayName} has been turned on.", recipients)
			}
        catch(ex) {
        	log.debug("Failed to work", ex)
        }
	} 
    else if (evt.value == "dry") {
    	try {
			pump.off([delay: 60000]) //wait 60 seconds
        	sendNotificationToContacts("Dry conditions detected by ${sensor.displayName}. ${pump.displayName} has been turned off.", recipients)
		}
        catch(ex) {
        	log.debug("Failed to work", ex)
        }
    }
}