package id.co.xinix.cordova.xox;

import android.util.Log;
import org.apache.cordova.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.LinkedList;

public class XOX extends CordovaPlugin {
    protected static JobManager manager;

    public JobManager getManager() {
        if (XOX.manager == null) {
            XOX.manager = new JobManager(this.cordova);
        }
        return XOX.manager;
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("send")) {
            JSONObject options = args.getJSONObject(0);
            this.getManager().send(options, this.webView, callbackContext);
            return true;
        } else if (action.equals("ready")) {
            LOG.d("XOX", "XOX is ready");
            this.getManager().start();
            return true;
        } else if (action.equals("load")) {
            LOG.d("XOX", "XOX callback onLoad");
            JSONObject options = args.getJSONObject(0);
            this.getManager().load(options);
            return true;
        } else if (action.equals("error")) {
            LOG.d("XOX", "XOX callback onError");
            JSONObject options = args.getJSONObject(0);
            this.getManager().error(options);
            return true;
        }
        return false;
    }

    static class JobManager {

        protected CordovaInterface cordova;

        protected int id = 0;

        protected boolean ready = false;

        protected LinkedList<JSONObject> queue = new LinkedList<JSONObject>();

        protected LinkedHashMap<Integer, JSONObject> processMap = new LinkedHashMap<Integer, JSONObject>();

        protected CordovaWebView webView;

        public JobManager(CordovaInterface cordova) {
            this.cordova = cordova;

            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JobManager.this.getWebView();
                }
            });
        }

        protected CordovaWebView getWebView() {
            if (this.webView == null) {
                this.makeWebView();
            }

            return this.webView;
        }

        protected CordovaWebView makeWebView() {
            this.webView = new CordovaWebView(this.cordova.getActivity());

            CordovaWebViewClient webViewClient = this.makeWebViewClient(this.webView);
            CordovaChromeClient webChromeClient = this.makeChromeClient(this.webView);

            this.webView.setWebViewClient(webViewClient);
            this.webView.setWebChromeClient(webChromeClient);
            webViewClient.setWebView(this.webView);
            webChromeClient.setWebView(this.webView);

            this.webView.loadUrl("file:///android_asset/www/tmp.html");

            return this.webView;
        }

        protected CordovaWebViewClient makeWebViewClient(CordovaWebView webView) {
            if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
                return new CordovaWebViewClient(this.cordova, webView);
            } else {
                return new IceCreamCordovaWebViewClient(this.cordova, webView);
            }
        }

        protected CordovaChromeClient makeChromeClient(CordovaWebView webView) {
            return new CordovaChromeClient(this.cordova, webView);
        }

        public void send(JSONObject options, CordovaWebView originWebView, CallbackContext callbackContext) {
            try {
                options.put("id", this.id++);
                options.put("origin", originWebView);
                options.put("callbackId", callbackContext.getCallbackId());

                LOG.d("XOX", "send:" + callbackContext.getCallbackId());

                this.queue.add(options);

                this.sendNext();
            } catch (JSONException e) {
                callbackContext.error(e.getMessage());
            }
        }

        synchronized protected void sendNext() {

            final JobManager manager = this;

            this.cordova.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    try {

                        if (!manager.ready || manager.queue.isEmpty()) {
                            return;
                        }
                        LOG.d("XOX", "sendNext:" + manager.queue.size() + " from:" + this);

                        final JSONObject options = manager.queue.poll();

                        if (options == null) {
                            return;
                        }

                        manager.processMap.put(options.getInt("id"), options);

                        manager.getWebView().sendJavascript("setTimeout(function() { XOX.postMessage('send'," + options.toString() + "); })");

                    } catch (JSONException e) {
                        LOG.e("XOX", e.getMessage(), e);
                    }

                    manager.sendNext();
                }
            });

        }

        public void start() {
            this.ready = true;
            this.sendNext();
        }

        public void load(JSONObject options) {
            try {
                LOG.d("XOX", "load id:" + options.getInt("id"));
                JSONObject o = this.processMap.get(options.getInt("id"));
                LOG.d("XOX", "request:" + o);
                CallbackContext callbackContext = new CallbackContext(o.getString("callbackId"), (CordovaWebView) o.get("origin"));
                callbackContext.success((JSONObject) options.get("xhr"));
            } catch (JSONException e) {
                LOG.e("XOX", e.getMessage(), e);
            }
        }

        public void error(JSONObject options) {
            try {
                LOG.d("XOX", "error id:" + options.getInt("id"));
                JSONObject o = this.processMap.get(options.getInt("id"));
                LOG.d("XOX", "request:" + o);
                CallbackContext callbackContext = new CallbackContext(o.getString("callbackId"), (CordovaWebView) o.get("origin"));
                callbackContext.error((JSONObject) options.get("xhr"));
            } catch (JSONException e) {
                LOG.e("XOX", e.getMessage(), e);
            }
        }
    }
}