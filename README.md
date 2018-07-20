# FoxBoycott

A quick Android app for automatically notifying you if you're at a location that helps support Fox News via advertisements.

## Setup

1. Get a Places SDK API key from https://developers.google.com/places/android-sdk/signup
2. Add the API key to the AndroidManifest.xml API_KEY metadata.
3. Create a Firebase account and enable the Remote Config feature.
4. Create a new parameter in Remote Config called 'advertisers' and set the data to a comma separated list of known companies that advertise on Fox News (https://www.foxnewsadvertisers.com)
3. Connect Android Studio to your Firebase account (Tools > Firebase > Remote Config > Connect).
