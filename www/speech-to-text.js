var exec = require('cordova/exec');

var PLUGIN_NAME = 'SpeechToText';

var SpeechToText = { 
  enable: function ( cb, error) {
    exec(cb, error, PLUGIN_NAME, 'enable', [idioma]);
  },
  start: function ( cb, error) {
    exec(cb, error, PLUGIN_NAME, 'start', []);
  },
  stop: function (cb, error) {
    exec(cb, error, PLUGIN_NAME, 'stop', []);
  }
};
module.exports = SpeechToText;