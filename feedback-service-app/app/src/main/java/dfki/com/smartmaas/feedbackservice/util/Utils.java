package dfki.com.smartmaas.feedbackservice.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import dfki.com.smartmaas.feedbackservice.R;
import dfki.com.smartmaas.feedbackservice.activity.MainActivity;


public class Utils {
    public static final String tag = "Utils";
    private static LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            1.0f
    );
    private static LinearLayout.LayoutParams buttonLayoutPrms = new LinearLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT);

    public static void setBlinking(View view) {
        Animation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(350);
        animation.setStartOffset(20);
        animation.setRepeatMode(Animation.REVERSE);
        animation.setRepeatCount(Animation.INFINITE);
        view.startAnimation(animation);
    }

    public static LinearLayout.LayoutParams getConsLayParams() {
        return layoutParams;
    }

    public static String convertLatLongToAddress(double lat, double lng, MainActivity mainActivity) throws IOException {
        Geocoder geocoder = new Geocoder(mainActivity, Locale.getDefault());
        List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
        return addresses.get(0).getAddressLine(0);
    }

    public static HashMap<String, Double> convertAddressToLatLng(String addressName, MainActivity mainActivity) throws IOException {
        Geocoder geocoder = new Geocoder(mainActivity, Locale.getDefault());
        List<Address> addresses = geocoder.getFromLocationName(addressName, 1);
        HashMap<String, Double> location = new HashMap<>();
        location.put("latitude", addresses.get(0).getLatitude());
        location.put("longitude", addresses.get(0).getLongitude());

        return location;
    }

    public static String convertToJson(Object object) throws JsonProcessingException {
        if (object == null)
            return "";
        ObjectMapper objectMapper = new ObjectMapper();

        return objectMapper.writeValueAsString(object);
    }

    public static String serializeToXml(Object object) throws Exception {
        if (object == null)
            return "";
        Serializer serializer = new Persister();
        StringWriter stringWriter = new StringWriter();
        serializer.write(object, stringWriter);

        return stringWriter.toString();
    }

    public static Object deserializeToObject(String xml, Object object) throws Exception {
        Serializer serializer = new Persister();
        object = serializer.read(object.getClass(), xml);
        return object;
    }

    public static void sendRequestToFeedbackWebService(Context context, String data, String webServiceUrl, String contentType,
                                                       HashMap<String, String> moreHeaders,
                                                       String successMsg, String errorMsg) {
        HttpsURLConnection httpsURLConnection;

        try {
            URL request_url = new URL(webServiceUrl);

            httpsURLConnection = (HttpsURLConnection) request_url.openConnection();
            httpsURLConnection.setRequestMethod("POST");
            httpsURLConnection.setDoInput(true);


            httpsURLConnection.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    if (context.getResources().getString(R.string.feedback_web_service_url).contains(hostname)) {
                        return true;
                    }
                    return false;
                }
            });


            httpsURLConnection.setSSLSocketFactory((SSLSocketFactory) SSLSocketFactory.getDefault());
            httpsURLConnection.connect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void postToFeedbWS(Context context, String data, String webServiceUrl, String contentType,
                                     HashMap<String, String> moreHeaders,
                                     String successMsg, String errorMsg) {


        StringRequest stringRequest = new StringRequest(Request.Method.POST, webServiceUrl,
                new Response.Listener<String>() {

                    @Override
                    public void onResponse(String response) {
                        makeToast(context, successMsg);
                        Log.i(tag, context.getResources()
                                .getString(R.string.fbs_response_message) + response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                makeToast(context, errorMsg);
                Log.e(tag, context.getResources().getString(R.string.fbs_error_message)
                        + error.getMessage());
            }
        }) {

            @Override
            public byte[] getBody() throws AuthFailureError {
                return data.getBytes(Charset.defaultCharset());
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put(context.getResources()
                        .getString(R.string.headers_content_type_message), contentType);
                for (String key : moreHeaders.keySet()) {
                    String value = moreHeaders.get(key);
                    headers.put(key, value);
                }
                return headers;
            }
        };


        Volley.newRequestQueue(context).add(stringRequest);

//        Volley.newRequestQueue(context, new HurlStack(null, getSocketFactory(context))).add(stringRequest);
    }

    public static SSLSocketFactory getSocketFactory(Context appContext) {

        CertificateFactory cf = null;
        try {

            cf = CertificateFactory.getInstance("X.509");
            InputStream caInput = appContext.getResources().openRawResource(R.raw.feedservcertificate);
            Certificate ca;
            try {

                ca = cf.generateCertificate(caInput);
                Log.e("CERT", "ca=" + ((X509Certificate) ca).getSubjectDN());
            } finally {
                caInput.close();
            }


            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);


            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);


            HostnameVerifier hostnameVerifier = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {

                    Log.e("CipherUsed", session.getCipherSuite());
                    String serverURL = appContext.getResources().getString(R.string.feedback_web_service_url);
                    if (serverURL.contains(hostname)) {
                        return true;
                    }
                    return false;
//                    return hostname.compareTo()==0; //The Hostname of your server.

                }
            };


            HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);
            SSLContext context = null;
            context = SSLContext.getInstance("TLS");

            context.init(null, tmf.getTrustManagers(), null);
            HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());

            SSLSocketFactory sf = context.getSocketFactory();


            return sf;

        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String removeAllSpaces(String trimMe) {
        StringBuilder trimmed = new StringBuilder();
        for (String s : trimMe.split(" ")) {
            trimmed.append(s);
        }
        return trimmed.toString();

    }

    public static void hideKeyboard(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public static void showKeyboard(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(view, 0);
        }
    }

    public static void requestFocusAndShowKeyboard(Context context, View view) {
        view.requestFocus();
        showKeyboard(context, view);
    }

    public static void hideNavigationBottomView(ConstraintLayout constraintLayout) {
        LinearLayout.LayoutParams params = Utils.getConsLayParams();
        params.weight = 0;
        constraintLayout.setLayoutParams(params);
    }

    public static void showNavigationBottomView(ConstraintLayout constraintLayout) {
        LinearLayout.LayoutParams params = Utils.getConsLayParams();
        params.weight = 1;
        constraintLayout.setLayoutParams(params);
    }

    public static void saveStringToPreferences(Context context, String key, String value) {
        SharedPreferences.Editor sharedPreferences = context.getSharedPreferences(
                context.getResources().getString(R.string.shared_preferences_name), Context.MODE_PRIVATE).edit();
        sharedPreferences.putString(key, value);
        sharedPreferences.apply();
    }

    public static String fetchStringFromPreferences(Context context, String key) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(
                context.getResources().getString(R.string.shared_preferences_name), Context.MODE_PRIVATE);
        return sharedPreferences.getString(key, context.getResources().
                getString(R.string.no_data_found_shrd_prfs));
    }

    public static void makeToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
