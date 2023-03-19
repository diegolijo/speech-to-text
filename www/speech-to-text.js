var exec = require('cordova/exec');

var PLUGIN_NAME = 'SpeechToText';

var SpeechToText = {
  enable: function (cb, error, locale) {
    exec(cb, error, PLUGIN_NAME, 'enable', [locale]);
  },
  start: function (cb, error) {
    exec(cb, error, PLUGIN_NAME, 'start', []);
  },
  stop: function (cb, error) {
    exec(cb, error, PLUGIN_NAME, 'stop', []);
  },
  isEnable: function (cb, error) {
    exec(cb, error, PLUGIN_NAME, 'isEnable', []);
  },
  isPlaying: function (cb, error) {
    exec(cb, error, PLUGIN_NAME, 'isPlaying', []);
  },
  download: function (cb, error, locale) {
    exec(cb, error, PLUGIN_NAME, 'download', [locale]);
  },
  getDownloadedLanguages: function (cb, error) {
    exec(cb, error, PLUGIN_NAME, 'getDownloadedLanguages', []);
  },
  getAvailableLanguages: function (cb, error) {
    exec(cb, error, PLUGIN_NAME, 'getAvailableLanguages', []);
  },
  speechText: function (cb, error, value, flush) {
    exec(cb, error, PLUGIN_NAME, 'speechText', [value, flush]);
  },
  getSpeechVoices: function (cb, error) {
    exec(cb, error, PLUGIN_NAME, 'getSpeechVoices', []);
  },
  setSpeechVoice: function (cb, error, value) {
    exec(cb, error, PLUGIN_NAME, 'setSpeechVoice', [value]);
  },
  setSpeechVolume: function (cb, error, value) {
    exec(cb, error, PLUGIN_NAME, 'setSpeechVolume', [value]);
  },
  setSpeechPitch: function (cb, error, value) {
    exec(cb, error, PLUGIN_NAME, 'setSpeechPitch', [value]);
  },
  playSound: function (cb, error, path, volume) {
    exec(cb, error, PLUGIN_NAME, 'playSound', [path, volume]);
  }
};
module.exports = SpeechToText;