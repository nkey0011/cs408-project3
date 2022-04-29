package edu.jsu.mcis.cs408.webservicedemo;

/*
handles the GET POST DELETE requests
 */

import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.net.ssl.HttpsURLConnection;

public class WebServiceDemoViewModel extends ViewModel {

    private static final String TAG = "WebServiceDemoViewModel";

    private static final String URL = "http://ec2-3-143-211-101.us-east-2.compute.amazonaws.com/CS408_SimpleChat/Chat";

    private static final String USERNAME = "Frodo";

    private String message; // need to grab userinput from MainActivity

    private MutableLiveData<JSONObject> jsonData;

    private final ExecutorService requestThreadExecutor;
    private final Runnable httpGetRequestThread, httpPostRequestThread, httpDeleteRequestThread;
    private Future<?> pending;

    public WebServiceDemoViewModel() {

        requestThreadExecutor = Executors.newSingleThreadExecutor();

        httpGetRequestThread = new Runnable() {

            @Override
            public void run() {

                /* If a previous request is still pending, cancel it */

                if (pending != null) { pending.cancel(true); }

                /* Begin new request now, but don't wait for it */

                try {
                    pending = requestThreadExecutor.submit(new HTTPRequestTask("GET", URL));
                }
                catch (Exception e) { Log.e(TAG, " Exception: ", e); }

            }

        };

        httpPostRequestThread = new Runnable() {

            @Override
            public void run() {

                /* If a previous request is still pending, cancel it */

                if (pending != null) { pending.cancel(true); }

                /* Begin new request now, but don't wait for it */

                try {
                    pending = requestThreadExecutor.submit(new HTTPRequestTask("POST", URL));
                }
                catch (Exception e) { Log.e(TAG, " Exception: ", e); }

            }

        };

        httpDeleteRequestThread = new Runnable() {
            @Override
            public void run() {
                /* If a previous request is still pending, cancel it */

                if (pending != null) { pending.cancel(true); }

                /* Begin new request now, but don't wait for it */

                try {
                    pending = requestThreadExecutor.submit(new HTTPRequestTask("DELETE", URL));
                }
                catch (Exception e) { Log.e(TAG, " Exception: ", e); }
            }
        };

    }

    // Start GET Request (called from Activity)

    public void sendGetRequest() {
        httpGetRequestThread.run();
    }

    // Start POST Request (called from Activity)

    public void sendPostRequest(String m) {
        setMessage(m);
        httpPostRequestThread.run();
    }

    // Start DELETE request (called from Activity)
    public void sendDeleteRequest(){ httpDeleteRequestThread.run();}

    // Setter / Getter Methods for JSON LiveData

    private void setJsonData(JSONObject json) {
        this.getJsonData().postValue(json);
    }

    public void setMessage(String m) { this.message = m; }

    public MutableLiveData<JSONObject> getJsonData() {
        if (jsonData == null) {
            jsonData = new MutableLiveData<>();
        }
        return jsonData;
    }

    // Private Class for HTTP Request Threads

    private class HTTPRequestTask implements Runnable {

        private static final String TAG = "HTTPRequestTask";
        private final String method, urlString;

        HTTPRequestTask(String method, String urlString) {
            this.method = method;
            this.urlString = urlString;
        }

        @Override
        public void run() {
            JSONObject results = doRequest(urlString);
            setJsonData(results);
        }

        /* Create and Send Request */

        private JSONObject doRequest(String urlString) {

            StringBuilder r = new StringBuilder();
            String line;

            HttpURLConnection conn = null;
            JSONObject results = null;

            /* Log Request Data */

            try {

                /* Check if task has been interrupted */

                if (Thread.interrupted())
                    throw new InterruptedException();

                /* Create Request */

                URL url = new URL(urlString);

                conn = (HttpURLConnection)url.openConnection();

                conn.setReadTimeout(10000);    /* ten seconds */
                conn.setConnectTimeout(15000); /* fifteen seconds */
                conn.setRequestMethod(method);
                conn.setDoInput(true);

                /* Add Request Parameters (if any) */

                if (method.equals("POST") ) {

                    conn.setDoOutput(true);

                    // Create example parameters (these will be echoed back by the API)

                    //String p = username + ": " + message;

                    String p = "{\"name\":\"" + USERNAME + "\", \"message\":\"" + message + "\"}";

                    // Write parameters to request body

                    OutputStream out = conn.getOutputStream();
                    out.write(p.getBytes());
                    out.flush();
                    out.close();

                }

                /* Send Request */

                conn.connect();

                /* Check if task has been interrupted */

                if (Thread.interrupted())
                    throw new InterruptedException();

                /* Get Reader for Results */

                int code = conn.getResponseCode();

                if (code == HttpsURLConnection.HTTP_OK || code == HttpsURLConnection.HTTP_CREATED) {

                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                    /* Read Response Into StringBuilder */

                    do {
                        line = reader.readLine();
                        if (line != null)
                            r.append(line);
                    }
                    while (line != null);

                }

                /* Check if task has been interrupted */

                if (Thread.interrupted())
                    throw new InterruptedException();

                /* Parse Response as JSON */

                results = new JSONObject(r.toString());

            }
            catch (Exception e) {
                Log.e(TAG, " Exception: ", e);
            }
            finally {
                if (conn != null) { conn.disconnect(); }
            }

            /* Finished; Log and Return Results */

            Log.d(TAG, " JSON: " + r.toString());

            return results;

        }

    }



}