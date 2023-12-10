# cordova-offline-speech

cordova-offile-speech is a Cordova npm package for text to speech functionality.

**Note : This library is only compatible with the native Android platform.**

## install
- cordova plugins add vosk-speech-to-text

## text-to-speech Methods

enable(cb, error, locale)
This method enables the text to speech functionality.

start(cb, error)
This method starts the text to speech engine.

stop(cb, error)
This method stops the text to speech engine.

isEnable(cb, error)
This method checks if the text to speech functionality is enabled.

isPlaying(cb, error)
This method checks if the text to speech engine is playing.

download(cb, error, locale)
This method downloads the necessary text to speech language model for specified locale.

getDownloadedLanguages(cb, error)
This method gets the list of downloaded languages for text to speech.

getAvailableLanguages(cb, error)
This method gets the list of available languages for text to speech that can be downloaded.

## speech-to-text Methods

speechText(cb, error, value, flush)
This method converts the value text to speech.

getSpeechVoices(cb, error)
This method gets the list of available text to speech voices.

setSpeechVoice(cb, error, value)
This method sets the voice for text to speech.

setSpeechVolume(cb, error, value)
This method sets the volume for text to speech.

setSpeechPitch(cb, error, value)
This method sets the pitch for text to speech.

## audio Methods

playSound(cb, error, path, volume)
This method plays a sound file at the specified assets path at the specified volume.

## usage
 ```typescript 
    declare const cordova: any;
    cordova.plugins.SpeechToText.enable((value: any) => {
      console.log(value);
    }, (err: any) => {
      console.log(err);
    }, locale);
```
