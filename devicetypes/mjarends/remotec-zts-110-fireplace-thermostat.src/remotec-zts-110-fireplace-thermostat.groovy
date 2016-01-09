/**
 *  Z-Wave Fireplace Thermostat 2
 *
 *  Copyright 2015 Mitch Arends
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
metadata {
	definition (name: "Remotec ZTS-110 Fireplace Thermostat", namespace: "mjarends", author: "Mitch Arends") {
		capability "Actuator"
		capability "Battery" // need to implement
		capability "Switch"
		capability "Temperature Measurement"
		capability "Thermostat Heating Setpoint"
		capability "Thermostat Mode"
		capability "Thermostat Operating State"
		capability "Thermostat Setpoint"
		capability "Configuration"
		capability "Polling"
		capability "Sensor"

		//battery
		//switch
		//temperature
		//heatingSetpoint
		//thermostatSetpoint
		//thermostatMode
		//thermostatOperatingState
		
		command "switchMode"
        command "quickSetHeat"

		fingerprint deviceId: "0x0806", inClusters: "0x20,0x31,0x40,0x42,0x43,0x44,0x45,0x47,0x70,0x72,0x80,0x81,0x85,0x86", manufacturer: "Remotec", model: "ZTS-110"
	}

	simulator {
		status "off"			: "command: 4003, payload: 00"
		status "heat"			: "command: 4003, payload: 01"
		status "cool"			: "command: 4003, payload: 02"
		status "auto"			: "command: 4003, payload: 03"
		status "emergencyHeat"	: "command: 4003, payload: 04"

		status "fanAuto"		: "command: 4403, payload: 00"
		status "fanOn"			: "command: 4403, payload: 01"
		status "fanCirculate"	: "command: 4403, payload: 06"

		status "heat 60"        : "command: 4303, payload: 01 09 3C"
		status "heat 68"        : "command: 4303, payload: 01 09 44"
		status "heat 72"        : "command: 4303, payload: 01 09 48"

		status "cool 72"        : "command: 4303, payload: 02 09 48"
		status "cool 76"        : "command: 4303, payload: 02 09 4C"
		status "cool 80"        : "command: 4303, payload: 02 09 50"

		status "temp 58"        : "command: 3105, payload: 01 2A 02 44"
		status "temp 62"        : "command: 3105, payload: 01 2A 02 6C"
		status "temp 70"        : "command: 3105, payload: 01 2A 02 BC"
		status "temp 74"        : "command: 3105, payload: 01 2A 02 E4"
		status "temp 78"        : "command: 3105, payload: 01 2A 03 0C"
		status "temp 82"        : "command: 3105, payload: 01 2A 03 34"

		status "idle"			: "command: 4203, payload: 00"
		status "heating"		: "command: 4203, payload: 01"
		status "cooling"		: "command: 4203, payload: 02"
		status "fan only"		: "command: 4203, payload: 03"
		status "pending heat"	: "command: 4203, payload: 04"
		status "pending cool"	: "command: 4203, payload: 05"
		status "vent economizer": "command: 4203, payload: 06"

		// reply messages
		reply "2502": "command: 2503, payload: FF"
	}

	tiles(scale: 2) {
		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
                attributeState "off", label: 'Off', action: "switch.on", icon: "st.Outdoor.outdoor19", backgroundColor: "#ffffff"
				attributeState "on", label: 'Heat', action: "switch.off", icon: "st.Weather.weather14", backgroundColor: "#79b821"
			}
             tileAttribute ("device.thermostatOperatingState", key: "SECONDARY_CONTROL") {
           		attributeState "statusText", label:'${currentValue}'
            }
		}
		controlTile("heatSliderControl", "device.heatingSetpoint", "slider", height: 2, width: 4, inactiveLabel: false, range:"(50..85)") {
			state "setHeatingSetpoint", action:"quickSetHeat", backgroundColor:"#d04e00"
		}
		valueTile("heatingSetpoint", "device.heatingSetpoint", height: 2, width: 2, inactiveLabel: false, decoration: "flat") {
			state "heat", label:'${currentValue}° heat', backgroundColor:"#ffffff"
		}
		valueTile("battery", "device.battery", height: 2, width: 2, inactiveLabel: false, decoration: "flat") {
        	state "battery", label:'${currentValue}% battery', unit:""
        }
		standardTile("refresh", "device.thermostatMode", height: 2, width: 2, inactiveLabel: false, decoration: "flat") {
			state "default", action:"polling.poll", icon:"st.secondary.refresh"
		}
		standardTile("configure", "device.configure", height: 2, width: 2, inactiveLabel: false, decoration: "flat") {
			state "configure", label:'', action:"configuration.configure", icon:"st.secondary.configure"
		}
		main "switch"
		details(["switch", "heatSliderControl", "heatingSetpoint", "battery", "refresh", "configure"])
	}
}

// parse events into attributes
def parse(String description) {
    log.debug "description: ${description}"
	def cmd = zwave.parse(description, [0x42:1, 0x43:2, 0x31: 3])
    // Process the parsed command
    log.debug "command: ${cmd}"
    def result = []
    if (cmd) {
		result << zwaveEvent(cmd)
        log.debug "parsed ${cmd} to ${result}"
      	// if the result is null then skip processing
		if (!result) {
	   	 	log.debug "no commands were parsed - skipping parse method"
			return null
		}
	}
   
    // Create the result to return
    def result2 = []
	result.each{
    	def map = it
        // evaluate the returned results
        if (map.isStateChange && map.name in ["heatingSetpoint","thermostatMode"]) {
            def map2 = [
                name: "thermostatSetpoint",
                unit: getTemperatureScale()
            ]
            if (map.name == "thermostatMode") {
                state.lastTriedMode = map.value
                if (map.value == "heat") {
                    map2.value = device.latestValue("heatingSetpoint")
                    log.info "THERMOSTAT, latest heating setpoint = ${map2.value}"
                }
            } else {
                def mode = device.latestValue("thermostatMode")
                log.info "THERMOSTAT, latest mode = ${mode}"
                if (map.name == "heatingSetpoint" && mode == "heat") {
                    map2.value = map.value
                    map2.unit = map.unit
                }
            }
            if (map2.value != null) {
                log.debug "THERMOSTAT, adding setpoint event: $map2"
                result2 << createEvent(map2)
            }
        }
        // add switch event
        if (map.name in ["thermostatMode"]) {
            def map2 = [
                name: "switch"
            ]
            // Determine the switch state
            if (map.value == "off") {
                map2.value = "off"
            } else if (map.value == "heat") {
                map2.value = "on"
            }
            if (map2.value != null) {
                log.debug "THERMOSTAT, adding switch event: $map2"
                result2 << createEvent(map2)
            }
        }
    }
    // add any new z-wave events
   	result.addAll(result2)

	log.debug "Parse returned $result for $cmd"
	result
}

// Event Generation
def zwaveEvent(physicalgraph.zwave.commands.thermostatsetpointv2.ThermostatSetpointReport cmd) {
    log.debug "ThermostatSetpointReport: ${cmd}"
	def cmdScale = cmd.scale == 1 ? "F" : "C"
	def map = [:]
	map.value = convertTemperatureIfNeeded(cmd.scaledValue, cmdScale, cmd.precision)
	map.unit = getTemperatureScale()
	map.displayed = false
	switch (cmd.setpointType) {
		case 1:
			map.name = "heatingSetpoint"
			break;
		default:
			return [:]
	}
	// So we can respond with same format
	state.size = cmd.size
	state.scale = cmd.scale
	state.precision = cmd.precision
	createEvent(map)
}

def zwaveEvent(physicalgraph.zwave.commands.sensormultilevelv3.SensorMultilevelReport cmd){
    log.debug "SensorMultilevelReport: ${cmd}"
	def map = [:]
	if (cmd.sensorType == 1) {
		map.value = convertTemperatureIfNeeded(cmd.scaledSensorValue, cmd.scale == 1 ? "F" : "C", cmd.precision)
		map.unit = getTemperatureScale()
		map.name = "temperature"
	} else if (cmd.sensorType == 5) {
		map.value = cmd.scaledSensorValue
		map.unit = "%"
		map.name = "humidity"
	}
	createEvent(map)
}

def zwaveEvent(physicalgraph.zwave.commands.thermostatoperatingstatev1.ThermostatOperatingStateReport cmd){
    log.debug "ThermostatOperatingStateReport: ${cmd}"
	def map = [:]
	switch (cmd.operatingState) {
		case physicalgraph.zwave.commands.thermostatoperatingstatev1.ThermostatOperatingStateReport.OPERATING_STATE_IDLE:
			map.value = "idle"
			break
		case physicalgraph.zwave.commands.thermostatoperatingstatev1.ThermostatOperatingStateReport.OPERATING_STATE_HEATING:
			map.value = "heating"
			break
		case physicalgraph.zwave.commands.thermostatoperatingstatev1.ThermostatOperatingStateReport.OPERATING_STATE_COOLING:
			map.value = "cooling"
			break
		case physicalgraph.zwave.commands.thermostatoperatingstatev1.ThermostatOperatingStateReport.OPERATING_STATE_FAN_ONLY:
			map.value = "fan only"
			break
		case physicalgraph.zwave.commands.thermostatoperatingstatev1.ThermostatOperatingStateReport.OPERATING_STATE_PENDING_HEAT:
			map.value = "pending heat"
			break
		case physicalgraph.zwave.commands.thermostatoperatingstatev1.ThermostatOperatingStateReport.OPERATING_STATE_PENDING_COOL:
			map.value = "pending cool"
			break
		case physicalgraph.zwave.commands.thermostatoperatingstatev1.ThermostatOperatingStateReport.OPERATING_STATE_VENT_ECONOMIZER:
			map.value = "vent economizer"
			break
	}
	map.name = "thermostatOperatingState"
	createEvent(map)
}

def zwaveEvent(physicalgraph.zwave.commands.thermostatmodev2.ThermostatModeReport cmd) {
    log.debug "ThermostatModeReport: ${cmd}"
	def map = [:]
	switch (cmd.mode) {
		case physicalgraph.zwave.commands.thermostatmodev2.ThermostatModeReport.MODE_OFF:
			map.value = "off"
			break
		case physicalgraph.zwave.commands.thermostatmodev2.ThermostatModeReport.MODE_HEAT:
			map.value = "heat"
			break
		case physicalgraph.zwave.commands.thermostatmodev2.ThermostatModeReport.MODE_AUXILIARY_HEAT:
			map.value = "emergency heat"
			break
		case physicalgraph.zwave.commands.thermostatmodev2.ThermostatModeReport.MODE_COOL:
			map.value = "cool"
			break
		case physicalgraph.zwave.commands.thermostatmodev2.ThermostatModeReport.MODE_AUTO:
			map.value = "auto"
			break
	}
	map.name = "thermostatMode"
	createEvent(map)
}

def zwaveEvent(physicalgraph.zwave.commands.thermostatmodev2.ThermostatModeSupportedReport cmd) {
    log.debug "ThermostatModeSupportedReport: ${cmd}"
	def supportedModes = ""
	if(cmd.off) {
		supportedModes += "off "
	} else if(cmd.heat) {
		supportedModes += "heat "
	} else {
		log.trace "ignoring ${cmd} mode"
	}
	log.debug "supported modes ${supportedModes}"
	
	state.supportedModes = supportedModes
}

def zwaveEvent(physicalgraph.zwave.commands.batteryv1.BatteryReport cmd) {
        def map = [ name: "battery", unit: "%" ]
        if (cmd.batteryLevel == 0xFF) {  // Special value for low battery alert
                map.value = 1
                map.descriptionText = "${device.displayName} has a low battery"
                map.isStateChange = true
        } else {
                map.value = cmd.batteryLevel
        }
        // Store time of last battery update so we don't ask every wakeup, see WakeUpNotification handler
        state.lastbatt = new Date().time
        createEvent(map)
}

// Battery powered devices can be configured to periodically wake up and
// check in. They send this command and stay awake long enough to receive
// commands, or until they get a WakeUpNoMoreInformation command that
// instructs them that there are no more commands to receive and they can
// stop listening.
def zwaveEvent(physicalgraph.zwave.commands.wakeupv2.WakeUpNotification cmd)
{
        def result = [createEvent(descriptionText: "${device.displayName} woke up", isStateChange: false)]

        // Only ask for battery if we haven't had a BatteryReport in a while
        if (!state.lastbatt || (new Date().time) - state.lastbatt > 24*60*60*1000) {
                result << response(zwave.batteryV1.batteryGet())
                result << response("delay 1200")  // leave time for device to respond to batteryGet
        }
        result << response(zwave.wakeUpV1.wakeUpNoMoreInformation())
        result
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
	log.debug "Zwave event received: $cmd"
    [:]
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
	log.warn "Unexpected zwave command $cmd"
    [:]
}


// Command Implementations
def poll() {
	log.debug "polling thermostat"
	delayBetween([
		zwave.sensorMultilevelV3.sensorMultilevelGet().format(), // current temperature
		zwave.thermostatSetpointV1.thermostatSetpointGet(setpointType: 1).format(),
		zwave.thermostatModeV2.thermostatModeGet().format(),
		zwave.thermostatOperatingStateV1.thermostatOperatingStateGet().format(),
        zwave.batteryV1.batteryGet().format()
	], 2300)
}

def on() {
	log.debug "turning the thermostat on"
	heat()
}

def off() {
	log.debug "turning the thermostat off"
	delayBetween([
		zwave.thermostatModeV2.thermostatModeSet(mode: 0).format(),
		zwave.thermostatModeV2.thermostatModeGet().format()
	], standardDelay)
}

def quickSetHeat(degrees) {
	log.debug "setting thermostat set point to $degrees"
	setHeatingSetpoint(degrees, 1000)
}

def setHeatingSetpoint(degrees, delay = 30000) {
	log.debug "setting thermostat set point to $degrees with delay $delay"
	setHeatingSetpoint(degrees.toDouble(), delay)
}

def setHeatingSetpoint(Double degrees, Integer delay = 30000) {
	log.debug "setting thermostat set point to $degrees with delay $delay"
	def deviceScale = state.scale ?: 1
	def deviceScaleString = deviceScale == 2 ? "C" : "F"
    def locationScale = getTemperatureScale()
	def p = (state.precision == null) ? 1 : state.precision

    def convertedDegrees
    if (locationScale == "C" && deviceScaleString == "F") {
    	convertedDegrees = celsiusToFahrenheit(degrees)
    } else if (locationScale == "F" && deviceScaleString == "C") {
    	convertedDegrees = fahrenheitToCelsius(degrees)
    } else {
    	convertedDegrees = degrees
    }
	log.debug "converted degrees: ${convertedDegrees}"

	delayBetween([
		zwave.thermostatSetpointV1.thermostatSetpointSet(setpointType: 1, scale: deviceScale, precision: p, scaledValue: convertedDegrees).format(),
		zwave.thermostatSetpointV1.thermostatSetpointGet(setpointType: 1).format()
	], delay)
}

def heat() {
	log.debug "turning the thermostat on"
	delayBetween([
		zwave.thermostatModeV2.thermostatModeSet(mode: 1).format(),
		zwave.thermostatModeV2.thermostatModeGet().format()
	], standardDelay)
}

def emergencyHeat() {
	log.debug "turning the thermostat on"
	heat()
}

def cool() {
	log.debug "turning the thermostat on"
	heat()
}

def auto() {
	log.debug "turning the thermostat on"
	heat()
}

def setThermostatMode(String value) {
	log.debug "set thermostat mode to $value"
	delayBetween([
		zwave.thermostatModeV2.thermostatModeSet(mode: modeMap[value]).format(),
		zwave.thermostatModeV2.thermostatModeGet().format()
	], standardDelay)
}

def configure() {
	log.debug "configuring device"
	delayBetween([
		//zwave.thermostatModeV2.thermostatModeSupportedGet().format(),
		//zwave.associationV1.associationSet(groupingIdentifier:1, nodeId:[zwaveHubNodeId]).format(),
        
        // Note that configurationSet.size is 1, 2, or 4 and generally
        // must match the size the device uses in its configurationReport
        //zwave.configurationV1.configurationSet(parameterNumber:1, size:2, scaledConfigurationValue:100).format(),

		// Can use the zwaveHubNodeId variable to add the hub to the
        // device's associations:
        //zwave.associationV1.associationSet(groupingIdentifier:2, nodeId:zwaveHubNodeId).format(),

       	// Make sure sleepy battery-powered sensors send their
        // WakeUpNotifications to the hub every 4 hours:
        zwave.wakeUpV1.wakeUpIntervalSet(seconds:4 * 3600, nodeid:zwaveHubNodeId).format()

	], 2300)
}

def modes() {
	//["off", "heat", "cool", "auto", "emergency heat"]
	["off", "heat"]
}

def switchMode() {
	log.debug "switching thermostate mode"
	def currentMode = device.currentState("thermostatMode")?.value
	def lastTriedMode = state.lastTriedMode ?: currentMode ?: "off"
	def supportedModes = getDataByName("supportedModes")
	def modeOrder = modes()
	def next = { modeOrder[modeOrder.indexOf(it) + 1] ?: modeOrder[0] }
	def nextMode = next(lastTriedMode)
	if (supportedModes?.contains(currentMode)) {
		while (!supportedModes.contains(nextMode) && nextMode != "off") {
			nextMode = next(nextMode)
		}
	}
	state.lastTriedMode = nextMode
	delayBetween([
		zwave.thermostatModeV2.thermostatModeSet(mode: modeMap[nextMode]).format(),
		zwave.thermostatModeV2.thermostatModeGet().format()
	], 1000)
}

def switchToMode(nextMode) {
	log.debug "switching to mode ${nextMode}"
	def supportedModes = getDataByName("supportedModes")
	if(supportedModes && !supportedModes.contains(nextMode)) log.warn "thermostat mode '$nextMode' is not supported"
	if (nextMode in modes()) {
		state.lastTriedMode = nextMode
		"$nextMode"()
	} else {
		log.debug("no mode method '$nextMode'")
	}
}

def getDataByName(String name) {
	log.debug "getDataByName ${name}"
	state[name] ?: device.getDataValue(name)
}

def getModeMap() { [
	"off": 0,
	"heat": 1
]}

private getStandardDelay() {
	3000
}
