device-type.weatherstation
==========================

http://build.smartthings.com/projects/weatherstation/

This is an internet-based weather station SmartDevice for your SmartThings Hub.  This device will
automatically use your Hub's (or pre-defined) location for the current conditions.

Credit for this device type goes to:
#SmartThings
#yracine

## Installation

### Install Using GitHub Integration
Follow these steps (all within the SmartThings IDE):
- Click on the `My Device Types` tab
- Click `Settings`
- Click `Add new repository` and use the following parameters:
  - Owner: `mjarends`
  - Name: `SmartThingsPersonal`
  - Branch: `master`
- Click `Save`
- Click `Update from Repo` and select the repository we just added above
- Find and Select `smartweather-station-tile.groovy`
- Select `Publish`(bottom right of screen near the `Cancel` button)
- Click `Execute Update`
- Note the response at the top. It should be something like "`Updated 0 devices and created 1 new devices, 1 published`"
- Verify that the two devices show up in the list and are marked with Status `Published`

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
