# cordova-plugin-offline-speech

cordova-pludin-offile-speech is a Cordova plugin for speech functionality.

This library is built using the VOSK Offline Speech Recognition API. For a deeper understanding of how the underlying speech recognition functionality operates, we recommend referring to the official documentation provided by VOSK.

- VOSK Offline Speech Recognition API Documentation: [https://alphacephei.com/vosk/](https://alphacephei.com/vosk/)

Please visit the above link to explore the VOSK documentation and gain insights into the features and capabilities it offers.

For any issues or inquiries related specifically to the VOSK Offline Speech Recognition API, we recommend reaching out to the VOSK community and support channels as outlined in their documentation.

**Note : This library is only compatible with the native Android platform.**

## install
```bash
cordova plugins add vosk-speech-to-text
```

## speech-to-text Methods

enable(cb:Function, error:Function, locale:string)
This method enables the text to speech functionality.

start(cb:Function, error:Function)
This method starts the text to speech engine.

stop(cb:Function, error:Function)
This method stops the text to speech engine.

isEnable(cb:Function, error:Function)
This method checks if the text to speech functionality is enabled.

isPlaying(cb:Function, error:Function)
This method checks if the text to speech engine is playing.

download(cb:Function, erro:Function, locale:string)
This method downloads the necessary text to speech language model for specified locale.

getDownloadedLanguages(cb:Function, error:Function)
This method gets the list of downloaded languages for text to speech.

getAvailableLanguages(cb:Function, error:Function)
This method gets the list of available languages for text to speech that can be downloaded.

## text-to-speech Methods

speechText(cb:Function, error:Function, value:string, flush)
This method converts the value text to speech.

getSpeechVoices(cb:Function, error:Function)
This method gets the list of available text to speech voices.

setSpeechVoice(cb:Function, error:Function, value:string)
This method sets the voice for text to speech.

setSpeechVolume(cb:Function, error:Function, value:number)
This method sets the volume for text to speech (a number between 0 and 1).

setSpeechPitch(cb:Function, error:Function, value:number)
This method sets the pitch for text to speech. 1.0 is the normal pitch, lower values lower the tone of the synthesized voice, greater values increase it.

## audio Methods

playSound(cb:Function, error:Function, path:string, volume:number)
This method plays a sound file at the specified assets path at the specified volume (a number between 0 and 1).

## example
 ```typescript 
declare const cordova: any;
...
cordova.plugins.SpeechToText.enable(function(response){
  console.log(response);
}, function(error){
  console.log(error);
}, 'en-US');
...
// Checking if enabled
cordova.plugins.SpeechToText.isEnable(function(response){
  console.log(response);
}, function(error){
  console.log(error);
});
...
// Starting text-to-speech
cordova.plugins.SpeechToText.start(function(response){
  console.log(response);
}, function(error){
  console.log(error);
});
...
// Speaking text
cordova.plugins.SpeechToText.speechText(function(response){
  console.log(response);
}, function(error){
  console.log(error);
}, 'Hello, world!', true);
...
```
