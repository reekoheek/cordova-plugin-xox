
/**
 * XMLHttpRequest
 */

var exec = require('cordova/exec');

var NativeXHR = window.NativeXHR = XMLHttpRequest;

var XOX = window.XOX = {
    listeners: {},

    postMessage: function(message, options) {
        if (this.listeners[message]) {
            for(var i in this.listeners[message]) {
                var fn = this.listeners[message][i];
                if (typeof fn === 'function') {
                    var result = fn(options);
                    if (result) {
                        break;
                    }
                }
            }
        }
    },

    addEventListener: function(message, callback) {
        this.listeners[message] = this.listeners[message] || [];
        this.listeners[message].push(callback);
    }
};

if (location.href === 'file:///android_asset/www/tmp.html') {
    document.addEventListener('deviceready', function() {
        exec(null, null, 'XOX', 'ready', []);
    }, false);

    XOX.addEventListener('send', function(options) {
        var i;

        console.log('send', options);

        var xhr = new XMLHttpRequest();
        xhr.open(options.type, options.url, options.async, options.username, options.password);

        for (i in options.headers) {
            xhr.setRequestHeader(i, options.headers[i]);
        }

        xhr.onload = function() {
            console.log('onload', options.id);
            exec(null, null, 'XOX', 'load', [{
                id: options.id,
                xhr: xhr
            }]);
        };

        xhr.onerror = function(progressEvt) {
            console.error('onerror', options.id, progressEvt);
            exec(null, null, 'XOX', 'error', [{
                id: options.id,
                xhr: xhr,
                event: progressEvt
            }]);
            // exec(null, null, 'XOX', 'onerror', arguments);
        };

        try {
            xhr.send(options.data || null);
        } catch (e) {
            throw e;
        }
    });

    module.exports = XMLHttpRequest;
} else {

    var nativeXHR = new NativeXHR();

    var XHR = function() {
        this.headers = {};

        for (var i in nativeXHR) {
            if (typeof nativeXHR[i]) {
                continue;
            }
            this[i] = nativeXHR[i];
        }
    };

    XHR.prototype = {

        setRequestHeader: function(key, value) {
            this.headers[key] = value;
        },

        getAllResponseHeaders: function() {
            return this.headers;
        },

        getResponseHeader: function(key) {
            return this.headers[key] || null;
        },

        open: function(type, url, async, username, password) {
            this.type = type;
            this.url = url;
            this.async = async;
            this.username = username;
            this.password = password;

            if (typeof async == 'undefined') {
                async = true;
            }

            if (!async) {
                throw new Error('Tampered XHR do not support synchronously open.');
            }

        },

        send: function(data) {
            console.log('xhr.send', data);

            var that = this;

            this.data = data;

            var successCallback = function(remoteXhr) {
                console.log('Success callback', arguments);
                for (var i in remoteXhr) {
                    if (i.substr(0, 2) == 'on') {
                        continue;
                    }
                    that[i] = remoteXhr[i];
                }

                setTimeout(function() {
                    that.onload();
                });
            };

            var errorCallback = function() {
                console.error('Error callback', arguments);
            };

            exec(successCallback, errorCallback, "XOX", "send", [this]);
        }
    };

    module.exports = XHR;
}

