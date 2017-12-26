definition(
    name: "Pool Notifications",
    namespace: "jmutnick",
    author: "Jonathan Mutnick; jmutnick@umich.edu",
    description: "Various Pool Notifications",
    category: "My Apps",
    iconUrl: "https://s3.us-east-2.amazonaws.com/jmutnick-icons/notification.png",
    iconX2Url: "https://s3.us-east-2.amazonaws.com/jmutnick-icons/notification%402x.png",
    iconX3Url: "https://s3.us-east-2.amazonaws.com/jmutnick-icons/notification%402x.png")


preferences {
    section("Choose switch:") {
        input "pool", "capability.switch", title: "Pool Switch"
    }
    section("Send Notifications?") {
        input("recipients", "contact", title: "Send notifications to", multiple:true)
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
    subscribe(pool, "switch2.on", PoolOnEvent)
    subscribe(pool, "switch2.off", PoolOffEvent)
}

def PoolOnEvent(evt){    
    def params = [
        uri: "https://hooks.slack.com/services/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",
        body: '{"text": ":smartthings: Pool Control Turned On by SmartThings"}',
        contentType: "application/x-www-form-urlencoded",
    ]
    
    try {
        httpPost(params) { resp -> 
            log.debug "Pool Control Turned On"
            if (location.contactBookEnabled && recipients) {
            	sendNotificationToContacts("Pool Control Turned On", recipients)
        	}
        }
    } catch (e) {
        log.error "something went wrong: $e"
    }
}

def PoolOffEvent(evt){
     def params = [
        uri: "https://hooks.slack.com/services/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",
        body: '{"text": ":smartthings: Pool Control Turned Off by SmartThings"}',
        contentType: "application/x-www-form-urlencoded",
    ]
    
    try {
        httpPost(params) { resp -> 
            log.debug "Pool Control Turned Off"
            if (location.contactBookEnabled && recipients) {
            	sendNotificationToContacts("Pool Control Turned Off", recipients)
        	}
        }
    } catch (e) {
        log.error "something went wrong: $e"
    }
}