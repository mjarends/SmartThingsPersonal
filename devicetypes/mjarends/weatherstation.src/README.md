device-type.weatherstation
==========================

http://build.smartthings.com/projects/weatherstation/

This is an internet-based weather station SmartDevice for your SmartThings Hub.  This device will
automatically use your Hub's (or pre-defined) location for the current conditions.

## Installation

1. Create a new device type (https://graph.api.smartthings.com/ide/devices)

a) Hit the "+New SmartDevice" at the top right corner, choose a device name

b) Hit the "From Code" tab on the left corner

c) Copy and paste the code from device-type.weatherstation.groovy under https://github.com/yracine/device-type.weatherstation

d) Hit the create button at the bottom

e) Hit the "publish/for me" button at the top right corner (in the code window)


2. Create a new device (https://graph.api.smartthings.com/device/list)
    * Name: Your choice
    * Device Network Id: WEATHERSTATION-01 (increase for each weather station you add)
    * Type: Smart WeatherStation Tile (should be near the last option)
    * Location: Choose the correct location
    * Hub/Group: Choose a hub (this will autopopulate the zipcode)



## Use

The device, by itself, is relatively unimpressive. It just displays the following data
for each of your locations:

 * Outside Temperature
 * Humidity
 * Feels Like (Temperature and Relative Humidity)
 * Current Forecast
 * Wind Speed
 * Wind Direction
 * UV Index
 * Precipitation over last hour

The real magic is writing SmartApps that do something based on this data.

For example, you can alert yourself when:

 * the Wind Speed is high, so you can secure your garbage cans.
 * the UV Index is high, so you can remind yourself to take sunscreen.
 * the forecast is rainy, so you can take your umbrella.
 * the precipitation is zero so you can go outside (very useful in Seattle)
 * ...and more!
