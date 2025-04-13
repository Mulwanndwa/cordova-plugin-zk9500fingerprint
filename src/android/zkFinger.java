package cordova.plugin.zkteco.scan;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.app.PendingIntent;

import java.io.ByteArrayOutputStream;

import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.zkteco.android.biometric.core.device.ParameterHelper;
import com.zkteco.android.biometric.core.device.TransportType;
import com.zkteco.android.biometric.core.utils.LogHelper;
import com.zkteco.android.biometric.core.utils.ToolUtils;
import com.zkteco.android.biometric.module.fingerprintreader.FingerprintCaptureListener;
import com.zkteco.android.biometric.module.fingerprintreader.FingerprintSensor;
import com.zkteco.android.biometric.module.fingerprintreader.FingprintFactory;
import com.zkteco.android.biometric.module.fingerprintreader.ZKFingerService;
import com.zkteco.android.biometric.module.fingerprintreader.exception.FingerprintException;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import android.annotation.SuppressLint;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONObject;

import ordev.pos.placeorder.R;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import android.app.ProgressDialog;
import android.widget.AutoCompleteTextView;
public class zkFinger extends CordovaPlugin
{
    public AutoCompleteTextView autoTextView = null;
    private static final int VID = 6997;    //zkteco device VID always 6997
    private static final int PID = 292;    //fvs100 PID always 512

    private boolean bstart = false;
    private boolean bIsRegister = false;
    private int enrollCount = 3;
    private int enrollIndex = 0;
    private byte[][] regFPTemparray = new byte[3][2048];
    private String[] regFVTemplates = new String[3];
    private int regID = 0;

    private boolean isRegister = false;
    private int uid = 123;
    private byte[][] regtemparray = new byte[3][2048];  //register template buffer array
    private int enrollidx = 0;
    private byte[] lastRegTemp = new byte[2048];

    private FingerprintSensor fingerprintSensor = null;

    private final String ACTION_USB_PERMISSION = "ordev.pos.placeorder.USB_PERMISSION";

    private int newUid = 0;
    private String siteID = "2";
    private String strBase64 = "";
    public String baseUrl = "";
    public String pos_token = "";
    public String fingerTemp = "";

    public String stringNewTem = "";

    public Boolean isRegistered = false;
    String SupervisorUserid = "5756";
    public  byte[] regTemp2;

    byte[] Enroll_Template;
    byte[] Verify_Template;

    public   byte[] finalFpImage;

    public  String savedStrBase64;

    private static final int BMP_WIDTH_OF_TIMES = 4;
    private static final int BYTE_PER_PIXEL = 3;

    byte[] temArr1;
    byte[] temArr2;
    byte[] temArr3;

    public  int enrollidx2 = 0;

    byte[] imgTemp;
    byte[] regTemp1 = new byte[0];

    int imageCount = 0;

    private CallbackContext command;

    LinearLayout layout;

    AlertDialog dialog;

    public final int MY_OP = 11;

    private ImageView imageView = null;
    private TextView textView = null;
    private View dialogView = null;
    private Activity activity = null;
    private JSONArray temp_users = null;

    public int[] json2int (JSONArray arr)
    {

        // Create an int array to accomodate the numbers.
        int[] respArr = new int[arr.length()];

        // Extract numbers from JSON array.
        for (int i = 0; i < arr.length(); ++i) {
            respArr[i] = arr.optInt(i);
        }

        return  respArr;
    }

    zkFinger that;
    Intent intent;
    private BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action))
            {
                synchronized (this)
                {
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
                    {
                        LogHelper.i("have permission!");
                    }
                    else
                    {
                        LogHelper.e("not permission!");
                    }
                }
            }
        }
    };

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        activity = this.cordova.getActivity();


        command = callbackContext;


        try {
            if (action.equals("scan")) {
                isRegistered = true;
                //JSONArray user_data = args;
                //Log.i("temp_users args",args.toString());
                //user_data.put(new JSONObject(args.toString()));

                if(args.length() > 0){
                    for (int u = 0; u < args.length(); u++) {
                        JSONObject jsonObject0 = args.getJSONObject(u);
                        pos_token = jsonObject0.getString("token");
                        siteID = jsonObject0.getString("site_id");
                        baseUrl = jsonObject0.getString("base_url");
                        temp_users = new JSONArray(jsonObject0.getString("users"));
                    }
                }

                Log.i("temp_users",temp_users.toString());
                Log.i("temp_users site_id",siteID);
                Log.i("temp_users user_token",pos_token);
                //baseUrl = args.getJSONArray(1).toString();
                final int[] initCount = {0};
                activity.runOnUiThread(() -> {
                    activity.setContentView(R.layout.activity_main);
                    textView = activity.findViewById(R.id.textView);
                    imageView = activity.findViewById(R.id.imageView);
                    autoTextView = activity.findViewById(R.id.autoCompleteTextView);

                    autoTextView.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                            Log.d("editable",charSequence.toString());
                        }
                        @Override
                        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                            Log.d("initCount", i + " - " + i1 + " - " + i2 + "- " + charSequence.length());
                            if (i2 > i1 && i1 > 0) {
                                String charSequenceStr = charSequence.toString();
                                if (charSequenceStr.contains("-")) {
                                    String[] editableStr = charSequenceStr.split("-");
                                    Log.d("editableStr", editableStr[1]);
                                    getUser(editableStr[1], false);

                                }

                            }
                            initCount[0] = i2;
                        }


                        @Override
                        public void afterTextChanged(Editable editable) {
                            Log.d("editable", "" + editable.length() + " = " + initCount[0]);
                        }

                    });
                });


                InitDevice(command);
                callbackContext.success("Native view shown");
                cordova.setActivityResultCallback (this);
                // Intent intent = new Intent();
                // intent.putExtra("base64", strBase64);
                // that = this;
                // cordova.startActivityForResult(that, intent, MY_OP);
                return true;
            }
            else if(action.equals("write")){
                return true;
            }
            else if(action.equals("saveTemplate")){
                return true;
            }

            return false;



        }
        catch (Exception e){
            callbackContext.error(e.getMessage());
            return false;
        }

    }



    private void writeTemplateToFile(String file, String content, CallbackContext callbackContext) {
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(file, true)));
            out.write(content);
            out.write("\r\n");
            callbackContext.success(content);
        } catch (Exception e) {
            callbackContext.error(e.getMessage());
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void writeFile(String fileName,byte[] content, int length, CallbackContext callbackContext){
        try{
            File file = new File(fileName);
            FileOutputStream stream = new FileOutputStream(file);
            stream.write(content, 0, length);
            stream.close();
            callbackContext.success(content.toString());
        }
        catch(Exception e){
            callbackContext.error(e.getMessage());
        }
    }

    private void startFingerprintSensor() throws FingerprintException {

        UsbManager manager = (UsbManager)cordova.getActivity().getApplicationContext().getSystemService(Context.USB_SERVICE);
        PendingIntent permissionIntent = PendingIntent.getBroadcast(cordova.getActivity().getApplicationContext(), 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        for (UsbDevice usbDevice : deviceList.values())
        {
            if (usbDevice.getVendorId() == VID && usbDevice.getProductId() == PID)
            {
                manager.requestPermission(usbDevice, permissionIntent);
            }
        }
        LogHelper.setLevel(Log.VERBOSE);
        // Start fingerprint sensor
        Map fingerprintParams = new HashMap();
        //set vid
        fingerprintParams.put(ParameterHelper.PARAM_KEY_VID, VID);
        //set pid
        fingerprintParams.put(ParameterHelper.PARAM_KEY_PID, PID);
        fingerprintSensor = FingprintFactory.createFingerprintSensor(cordova.getActivity().getApplicationContext(), TransportType.USB, fingerprintParams);


    }

    private void InitDevice(CallbackContext callbackContext) throws FingerprintException {

        // UsbManager musbManager = null;
        // //if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB_MR1) {
        // musbManager = (UsbManager)cordova.getActivity().getApplicationContext().getSystemService(Context.USB_SERVICE);
        // //}
        // IntentFilter filter = new IntentFilter();
        // filter.addAction(ACTION_USB_PERMISSION);
        // filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
        // Context context = cordova.getActivity().getApplicationContext().getApplicationContext();
        // context.registerReceiver(mUsbReceiver, filter);

        // //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
        // for (UsbDevice device : musbManager.getDeviceList().values())
        // {
        //     if (device.getVendorId() == VID && device.getProductId() == PID)
        //     {
        //         if (!musbManager.hasPermission(device))
        //         {
        //             Intent intent = new Intent(ACTION_USB_PERMISSION);
        //             PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        //             musbManager.requestPermission(device, pendingIntent);
        //         }
        //     }
        // }


        UsbManager manager = (UsbManager)cordova.getActivity().getApplicationContext().getSystemService(Context.USB_SERVICE);
        PendingIntent permissionIntent = PendingIntent.getBroadcast(cordova.getActivity().getApplicationContext(), 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        for (UsbDevice usbDevice : deviceList.values())
        {
            if (usbDevice.getVendorId() == VID && usbDevice.getProductId() == PID)
            {
                manager.requestPermission(usbDevice, permissionIntent);
            }
        }
        LogHelper.setLevel(Log.VERBOSE);
        // Start fingerprint sensor
        Map fingerprintParams = new HashMap();
        //set vid
        fingerprintParams.put(ParameterHelper.PARAM_KEY_VID, VID);
        //set pid
        fingerprintParams.put(ParameterHelper.PARAM_KEY_PID, PID);
        fingerprintSensor = FingprintFactory.createFingerprintSensor(cordova.getActivity().getApplicationContext(), TransportType.USB, fingerprintParams);


        System.out.println("imageView - "+imageView);

        OnBnBegin(callbackContext);


    }

    @SuppressLint("LongLogTag")
    public void OnBnBegin(CallbackContext callbackContext) throws FingerprintException
    {


        try {
            if (bstart) return;

            fingerprintSensor.open(0);

            final FingerprintCaptureListener listener = new FingerprintCaptureListener() {

                @Override
                public void captureOK( final  byte[] fpImage) {


                    final int width = fingerprintSensor.getImageWidth();
                    final int height = fingerprintSensor.getImageHeight();

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            System.out.println("imageView - "+fpImage);

                            if(null != fpImage)
                            {
                                imageCount++;
                                ToolUtils.outputHexString(fpImage);
                                LogHelper.i("width=" + width + "\nHeight=" + height);
                                Bitmap bitmapFp = ToolUtils.renderCroppedGreyScaleBitmap(fpImage, width, height);
                                imageView.setImageBitmap(bitmapFp);

                                ByteArrayOutputStream stream2 = new ByteArrayOutputStream();
                                bitmapFp.compress(Bitmap.CompressFormat.JPEG, 100, stream2);
                                byte[] byteArray = stream2.toByteArray();

                            }

                            //textView.setText("FakeStatus:" + fingerprintSensor.getFakeStatus()+"\n"+stringNewTem+"\n"+stringNewTem);
                        }
                    });


                }
                @Override
                public void captureError(FingerprintException e) {
                    final FingerprintException exp = e;
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            LogHelper.i("captureError  errno=" + exp.getErrorCode() +
                                    ",Internal error code: " + exp.getInternalErrorCode() + ",message=" + exp.getMessage());


                        }
                    });
                }
                @Override
                public void extractError(final int err)
                {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //dialog.dismiss();
                            textView.setText("extract fail, errorcode:" + err);

                        }
                    });
                }

                @SuppressLint("LongLogTag")
                @Override
                public void extractOK(byte[] fpTemplate){

                    final byte[] tmpBuffer = fpTemplate;

                    isRegistered = true;

                    activity.runOnUiThread(new Runnable() {

                        @SuppressLint("SetTextI18n")
                        @Override

                        public void run() {

                            if (isRegister) {
                                byte[] bufids = new byte[256];
                                int ret = ZKFingerService.identify(tmpBuffer, bufids, 55, 1);

                                if (ret > 0) {
                                    String strRes[] = new String(bufids).split("\t");

                                    String new_user_id = strRes[0].toString().substring(4);

                                    Log.d("new_user_id",new_user_id);

                                    isRegister = false;
                                    enrollidx = 0;

                                    textView.setText("The finger already enroll by user ID: " + new_user_id);

                                    return;
                                }


                                if (enrollidx > 0 && ZKFingerService.verify(regtemparray[enrollidx-1], tmpBuffer) <= 0)
                                {
                                    textView.setText("Please press the same finger 3 times for the enrollment");
                                    //textView.setBackgroundColor(Color.parseColor("#7aaa36"));
                                    return;
                                }


                                System.arraycopy(tmpBuffer, 0, regtemparray[enrollidx], 0, 2048);

                                //strBase64 = Base64.encodeToString(regtemparray[enrollidx], 0, ret,Base64.DEFAULT);

                                enrollidx++;

                                if (enrollidx == 3) {
                                    byte[] regTemp = new byte[2048];
                                    if (0 < (ret = ZKFingerService.merge(regtemparray[0], regtemparray[1], regtemparray[2], regTemp))) {

                                        System.arraycopy(regTemp, 0, regTemp, 0, ret);
                                        if(!Objects.equals(SupervisorUserid, "5756") &&
                                                !Objects.equals(SupervisorUserid, "") &&
                                                !Objects.equals(SupervisorUserid, null)) {
                                            storeFingerprint();
                                        }
                                        //textView.setText("Enroll successful, User ID: " + newUid);

                                    } else {
                                        textView.setText("Enroll failed");
                                    }
                                    isRegister = false;
                                    isRegistered = true;

                                } else {
                                    textView.setText("You need to press the " + (3 - enrollidx) + " time fingerprint");

                                }

                            } else {
                                byte[] bufids = new byte[256];
                                int ret = ZKFingerService.identify(tmpBuffer, bufids, 55, 1);

                                if (ret > 0) {

                                    String strRes[] = new String(bufids).split("\t");
                                    String new_user_id = strRes[0].toString().substring(4);

                                    textView.setText("Identify success, user ID: " + new_user_id + ", score:" + strRes[1]);
                                    //command.success(new_user_id);
                                } else {
                                    isRegister = false;
                                    enrollidx = 0;

                                    newUid = 0;

                                    textView.setText("Access denied. This user is not enrolled.");
                                    OnBnRescan();
                                    // command.success("Access denied. This user is not enrolled.");
                                }
                                //Base64 Template
                                //String strBase64 = Base64.encodeToString(tmpBuffer, 0, fingerprintSensor.getLastTempLen(), Base64.NO_WRAP);
                            }
                        }
                    });
                }


            };
            fingerprintSensor.setFingerprintCaptureListener(0, listener);
            fingerprintSensor.startCapture(0);
            bstart = true;
            if (newUid == 0) {
                textView.setText("Please begin capture first");
            }else{
                isRegister = true;
                enrollidx = 0;
                isRegistered = false;
                textView.setText("You need to press the 3 time fingerprint");
            }
            SyncTemps();

        }catch (FingerprintException e)
        {
            textView.setBackgroundColor(Color.parseColor("#db493b"));
            textView.setText("Begin capture fail.errorcode:"+ e.getErrorCode() + "err message:" + e.getMessage() + "inner code:" + e.getInternalErrorCode());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
    private void getUser(String user_id, boolean scanned) {

        String postUrl = baseUrl+"api/fingerprints/get_user_fingerprint";
        RequestQueue requestQueue = Volley.newRequestQueue(activity);
        JSONObject postData = new JSONObject();
        ProgressDialog pd = new ProgressDialog(activity);
        pd.setMessage("Fetching user details...");
        pd.show();

        textView.setText("Please begin capture first");

        try {
            postData.put("user_id", user_id);
            postData.put("site_id", siteID);
            postData.put("token",pos_token);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.d("postData",postData.toString());

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, postUrl, postData, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                pd.dismiss();
                JSONObject jsonObject = null;
                JSONArray  jsonArray = null;


                try {

                    jsonObject = response.getJSONObject("data");
                    Log.d("new jsonArray",jsonObject.getString("name"));
                    String[] fullname = jsonObject.getString("name").split(" ");
                    newUid = Integer.parseInt(jsonObject.getString("id"));
                    Log.d("is_override_user", jsonObject.getString("is_override_user"));

                    if(jsonObject.getString("fingerprint_template") != "" &&
                            jsonObject.getString("fingerprint_template") != "null" &&
                            jsonObject.getString("fingerprint_template") != null){

                        if(scanned){
                            //OnDone();
                        }

                    }else {

                        if(scanned) {

                            textView.setBackgroundColor(Color.parseColor("#db493b"));
                            textView.setText("Access denied. This user is not enrolled.");

                            return;
                        }

                        newUid = Integer.parseInt(jsonObject.getString("id"));
                        isRegistered = false;
                        isRegister = true;
                        enrollidx = 0;

                        textView.setBackgroundColor(Color.parseColor("#7aaa36"));

                    }

                    if(jsonObject.getString("name").equals("Cash Sale") || jsonObject.getString("id").equals("5756")){
                        OnBnRescan();
                    }
                } catch (JSONException e) {
                    //Log.d("error json",e.toString());
                    //throw new RuntimeException(e);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //Log.d("Temp: ",storeManager.getBioTemp().toString());
                pd.dismiss();

                NetworkResponse networkResponse = error.networkResponse;
                String errorText = "";
                Log.d("Get user error",error.networkResponse.toString());
                if (networkResponse != null && networkResponse.data != null) {
                    String messageR = new String(networkResponse.data);
                    try {
                        JSONObject jsonObject = new JSONObject(messageR);
                        errorText = jsonObject.getString("message");
                    } catch (JSONException e) {
                        errorText = "Oops, something went wrong, please try again later.";
                    }
                }else{
                    errorText = "Oops, something went wrong, please try again later.";
                }
                if(scanned) {
                    textView.setBackgroundColor(Color.parseColor("#db493b"));
                    textView.setText("Access denied. This user is not enrolled.");

                    return;
                }
                textView.setText(errorText);
                textView.setBackgroundColor(Color.parseColor("#db493b"));
                isRegister = true;
                enrollidx = 0;
                // clear();

                error.printStackTrace();
            }
        });

        requestQueue.add(jsonObjectRequest);

    }
    public void OnBnRescan() {
        if (bstart) {
            isRegister = false;
            enrollidx = 0;
            textView.setText("Please begin capture first");
        }else {
            textView.setText("Please begin capture first");
        }
    }
    @SuppressLint("LongLogTag")
    public void  CustomerSearch() throws JSONException {

        String allUsersStr = "";
        for (int i = 0; i < temp_users.length(); i++) {
            JSONObject explrObject = temp_users.getJSONObject(i);
            allUsersStr += explrObject.getString("name") +" - "+
                    explrObject.getString("id")+
                    " - "+explrObject.getString("cellphone")+",";
        }
        String delimiter = ",";
        String[] split_string = allUsersStr.split(delimiter);
        System.out.println("countryNameList"+ Arrays.toString(split_string));
        ArrayAdapter adapter = new ArrayAdapter(activity.getApplicationContext(), android.R.layout.simple_list_item_1,  split_string);

        autoTextView.setAdapter(adapter);
        autoTextView.setThreshold(1);//start searching from 1 character
        autoTextView.setAdapter(adapter);   //set the adapter for displaying country name list
    }
    public void  SyncTemps() throws JSONException {
        CustomerSearch();

        try {
            JSONArray jsonArr = temp_users;
            for (int f = 0; f < jsonArr.length(); f++) {
                JSONObject explrObject1 = jsonArr.getJSONObject(f);

                if(explrObject1.getString("fingerprint_template") != "null" &&
                        explrObject1.getString("fingerprint_template") != "" &&
                        explrObject1.getString("fingerprint_template") != null){
                    String myUserID = explrObject1.getString("id");
                    Log.d("my User ID",myUserID+" "+explrObject1.getString("fingerprint_template").getBytes());
                    ZKFingerService.save(explrObject1.getString("fingerprint_template").getBytes(), "test" + myUserID);
                    //storeManager.setUserCode(explrObject1.getString("code"),myUserID);
                }
                Log.d("temp id",explrObject1.getString("id"));
            }
            Log.d("jsonArr",jsonArr.toString());
//            TextView textViewUsers = (TextView)findViewById(R.id.textViewUsers);
//            textViewUsers.setText("Number of enrolled users: "+jsonArr.length());
            //textView.setText("Please begin capture first");

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

    }
    public double compareByteArrays(byte[] a, byte[] b) {
        int n = Math.min(a.length, b.length), nLarge = Math.max(a.length, b.length);
        int unequalCount = nLarge - n;
        for (int i=0; i<n; i++)
            if (a[i] != b[i]) unequalCount++;
        return unequalCount * 100.0 / nLarge;
    }


    private static byte[] writeInt(int value) throws IOException {
        byte[] b = new byte[4];

        b[0] = (byte)(value & 0x000000FF);
        b[1] = (byte)((value & 0x0000FF00) >> 8);
        b[2] = (byte)((value & 0x00FF0000) >> 16);
        b[3] = (byte)((value & 0xFF000000) >> 24);

        return b;
    }

    /**
     * Write short to little-endian byte array
     * @param value
     * @return
     * @throws IOException
     */
    private static byte[] writeShort(short value) throws IOException {
        byte[] b = new byte[2];

        b[0] = (byte)(value & 0x00FF);
        b[1] = (byte)((value & 0xFF00) >> 8);

        return b;
    }

    private void storeFingerprint() {

        String postUrl = baseUrl+"api/fingerprints/enroll_user_fingerprint";
        RequestQueue requestQueue = Volley.newRequestQueue(activity);
        JSONObject postData = new JSONObject();

        stringNewTem = Base64.encodeToString(regTemp1, Base64.DEFAULT);

        try {
            postData.put("user_id", newUid);
            postData.put("supervisor_user_id", SupervisorUserid);
            postData.put("fingerprint_template", stringNewTem);
            postData.put("fingerprint_image", strBase64);
            postData.put("token",pos_token);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ProgressDialog pd = new ProgressDialog(activity);
        pd.setMessage("Enrolling fingerprint...");
        pd.show();

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, postUrl, postData, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                pd.dismiss();

                textView.setText("Enroll successful, User ID: " + newUid);
                textView.setBackgroundColor(Color.parseColor("#7aaa36"));

                ZKFingerService.save(regTemp1, "test" + newUid);

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //Log.d("Temp: ",storeManager.getBioTemp().toString());
                pd.dismiss();
                NetworkResponse networkResponse = error.networkResponse;
                String errorText = "";
                if (networkResponse != null && networkResponse.data != null) {
                    String messageR = new String(networkResponse.data);
                    try {
                        JSONObject jsonObject = new JSONObject(messageR);
                        errorText = jsonObject.getString("message");
                    } catch (JSONException e) {
                        errorText = "Enroll failed, You need to press the 3 time fingerprint.";
                    }
                }else{
                    errorText = "Enroll failed, You need to press the 3 time fingerprint.";
                }

                textView.setText(errorText);
                textView.setBackgroundColor(Color.parseColor("#db493b"));
                isRegister = true;
                enrollidx = 0;
               
                error.printStackTrace();
            }
        });

        requestQueue.add(jsonObjectRequest);
    }


    private void verifyFingerPrint() {

    }
    public void OnBnStop() throws FingerprintException
    {
        try {
            if (bstart)
            {
                //stop capture
                fingerprintSensor.stopCapture(0);
                bstart = false;
                fingerprintSensor.close(0);
                command.success("stop capture succ");
            }
            else
            {
                command.success("already stop");
            }
        } catch (FingerprintException e) {
            command.success("stop fail, errno=" + e.getErrorCode() + "\nmessage=" + e.getMessage());

        }

    }

    public void OnBnEnroll() {
        if (bstart) {
            isRegistered = false;
            isRegister = true;
            enrollidx = 0;
            command.success("You need to press the 3 time fingerprint");
        }
        else
        {
            command.success("please begin capture first");
        }
    }

    public void OnBnVerify() {
        if (bstart) {
            isRegister = false;
            enrollidx = 0;
        }else {
            command.success("Please begin capture first");
        }
    }
    public void clear() {

        ZKFingerService.clear();

    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        LogHelper.i("requestCode - "+requestCode);
        if( requestCode == MY_OP )
        {
            if( resultCode == cordova.getActivity().RESULT_OK && data.hasExtra("base64") )
            {
                PluginResult result = new PluginResult(PluginResult.Status.OK, data.getStringExtra("base64"));
                result.setKeepCallback(true);
                command.sendPluginResult(result);
            }
            else
            {
                PluginResult result = new PluginResult(PluginResult.Status.ERROR, "no params returned successfully" );
                result.setKeepCallback(true);
                command.sendPluginResult(result);
            }
        }
    }


}
