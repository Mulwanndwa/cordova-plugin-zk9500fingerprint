package cordova.plugin.zkteco.scan;
import static android.os.Environment.getExternalStoragePublicDirectory;

import static org.junit.Assert.assertArrayEquals;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;
import java.nio.file.Files;
import java.io.File;
import android.widget.Toast;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.IOException;
import java.nio.ByteBuffer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;


public class zkFinger extends CordovaPlugin 
{

    private static final int VID = 6997;
    private static final int PID = 292;

    private CallbackContext command;

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
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException 
    {

        command = CallbackContext;

        InitDevice();
        try {
            startFingerprintSensor();
        } catch (FingerprintException e) {
            throw new RuntimeException(e);
        }


        try{

            if (action.equals("scan")) 
            {           
                this.captureBio(callbackContext);
                return true;
            }
            else if(action.equals("write"))
            {

                String fileName = args.getString(0);
                int[]  content  =  json2int( args.getJSONArray(1) );
                int    length   = args.getInt(2);


                ByteBuffer byteBuffer = ByteBuffer.allocate(content.length * 4);
                IntBuffer intBuffer = byteBuffer.asIntBuffer();
                intBuffer.put(content);

                byte[] array = byteBuffer.array();

                this.writeFile(fileName,array,length,callbackContext);
                return true;

            }
            else if(action.equals("saveTemplate"))
            {

                String file     = args.getString(0);
                String content  = args.getString(1);

                this.writeTemplateToFile(file, content, callbackContext);
                return true;
            }
            
            return false;


        
        }
        catch (Exception e)
        {
                        
            callbackContext.error(e.getMessage());
            return false;
        }

    }



    private void startFingerprintSensor() throws FingerprintException {
        // Define output log level
        LogHelper.setLevel(Log.VERBOSE);
        // Start fingerprint sensor
        Map fingerprintParams = new HashMap();
        //set vid
        fingerprintParams.put(ParameterHelper.PARAM_KEY_VID, VID);
        //set pid
        fingerprintParams.put(ParameterHelper.PARAM_KEY_PID, PID);
        fingerprintSensor = FingprintFactory.createFingerprintSensor(this, TransportType.USB, fingerprintParams);

        OnBnBegin();

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }

    public void saveBitmap(Bitmap bm,int id) {
        final String dir = getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/";
        File f = new File(dir, "fingerprint-"+id+".bmp");

        if (f.exists()) {
            f.delete();
        }
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(f);
            bm.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.flush();
            out.close();


            //command.success("Image save id:"+id);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            String errMsg = e.getMessage();
            command.success(errMsg);

        }catch (IOException e) {
            e.printStackTrace();
            String errMsg = e.getMessage();
            command.success(errMsg);

        }

    }

    private void InitDevice()
    {

        UsbManager musbManager = (UsbManager)this.getSystemService(Context.USB_SERVICE);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
        Context context = this.getApplicationContext();
        context.registerReceiver(mUsbReceiver, filter);

        for (UsbDevice device : musbManager.getDeviceList().values())
        {
            if (device.getVendorId() == VID && device.getProductId() == PID)
            {
                if (!musbManager.hasPermission(device))
                {
                    Intent intent = new Intent(ACTION_USB_PERMISSION);
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
                    musbManager.requestPermission(device, pendingIntent);
                }
            }
        }



    }

    public void OnBnBegin() throws FingerprintException
    {
        //storeFingerprint();


        try {
            if (bstart) return;
            fingerprintSensor.open(0);
            //ZKFingerService.clear();
            final FingerprintCaptureListener listener = new FingerprintCaptureListener() {

                @Override
                public void captureOK( final  byte[] fpImage) {


                    final int width = fingerprintSensor.getImageWidth();
                    final int height = fingerprintSensor.getImageHeight();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            if(null != fpImage)
                            {
                                imageCount++;
                                ToolUtils.outputHexString(fpImage);
                                LogHelper.i("width=" + width + "\nHeight=" + height);
                                Bitmap bitmapFp = ToolUtils.renderCroppedGreyScaleBitmap(fpImage, width, height);
                                //imageView.setImageBitmap(bitmapFp);

                                ByteArrayOutputStream stream2 = new ByteArrayOutputStream();
                                bitmapFp.compress(Bitmap.CompressFormat.JPEG, 100, stream2);
                                byte[] byteArray = stream2.toByteArray();

                                strBase64 = Base64.encodeToString(byteArray, Base64.NO_WRAP);

                                StoreManager storeManager = new StoreManager(getApplicationContext());

                                stringNewTem = new String(byteArray);

                                if (isRegistered) {
                                    verifyFingerPrint();
                                    return;
                                }

                                if (enrollidx < 3) {
                                    if(!isRegistered) {
                                        storeManager.setImage2(strBase64);
                                        storeFingerprint();
                                    }
                                }

                            }

                            //command.success("FakeStatus:" + fingerprintSensor.getFakeStatus()+"\n"+stringNewTem+"\n"+stringNewTem);
                        }
                    });


                }
                @Override
                public void captureError(FingerprintException e) {
                    final FingerprintException exp = e;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            LogHelper.d("captureError  errno=" + exp.getErrorCode() +
                                    ",Internal error code: " + exp.getInternalErrorCode() + ",message=" + exp.getMessage());
                        }
                    });
                }
                @Override
                public void extractError(final int err)
                {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            command.success("extract fail, errorcode:" + err);
                        }
                    });
                }

                @Override
                public void extractOK(byte[] fpTemplate){


                    final byte[] tmpBuffer = fpTemplate;

                    runOnUiThread(new Runnable() {

                        @SuppressLint("SetTextI18n")
                        @Override

                        public void run() {

                            if(isRegistered){
                                //command.success("User ID: "+newUid+". The finger already enrolled.");
                                return;
                            }

                            if (isRegister) {
                                byte[] bufids = new byte[256];
                                int ret = ZKFingerService.identify(tmpBuffer, bufids, 55, newUid);

                                if (ret > 0) {
                                    isRegister = false;
                                    enrollidx = 0;
                                    textView.setBackgroundColor(Color.parseColor("#7aaa36"));
                                    command.success("The finger already enroll by userId: " + newUid);
                                    return;
                                }


                                if (enrollidx > 0 && ZKFingerService.verify(regtemparray[enrollidx-1], tmpBuffer) <= 0)
                                {
                                    command.success("Please press the same finger 3 times for the enrollment");
                                    textView.setBackgroundColor(Color.parseColor("#7aaa36"));
                                    return;
                                }


                                System.arraycopy(tmpBuffer, 0, regtemparray[enrollidx], 0, 2048);

                                //strBase64 = Base64.encodeToString(regtemparray[enrollidx], 0, ret,Base64.DEFAULT);

                                enrollidx++;

                                //stringNewTem = new String(regtemparray[enrollidx]);

                                if (enrollidx == 3) {
                                    byte[] regTemp = new byte[2048];
                                    if (0 < (ret = ZKFingerService.merge(regtemparray[0], regtemparray[1], regtemparray[2], regTemp))) {
                                        ZKFingerService.save(regTemp, "test" + newUid);
                                        System.arraycopy(regTemp, 0, regTemp, 0, ret);
                                        command.success("Enroll successful, User ID: " + newUid);
                                        textView.setBackgroundColor(Color.parseColor("#7aaa36"));
                                    } else {
                                        command.success("Enroll failed");
                                        textView.setBackgroundColor(Color.parseColor("#db493b"));
                                    }
                                    isRegister = false;
                                    isRegistered = true;

                                } else {
                                    command.success("You need to press the " + (3 - enrollidx) + " time fingerprint");
                                    textView.setBackgroundColor(Color.parseColor("#7aaa36"));
                                }

                            } else {
                                byte[] bufids = new byte[256];
                                int ret = ZKFingerService.identify(tmpBuffer, bufids, 55, newUid);

                                if (ret > 0) {

                                    String strRes[] = new String(bufids).split("\t");

                                    command.success("Identify success, userid: " + newUid + ", score:" + strRes[1]);
                                    textView.setBackgroundColor(Color.parseColor("#7aaa36"));
                                    AlertDialog.Builder builder = new AlertDialog.Builder( MainActivity.this);
                                    LayoutInflater inflater = MainActivity.this.getLayoutInflater();
                                    builder.setView(inflater.inflate(R.layout.dialog_signin, null));
                                    AlertDialog dialog = builder.create();
                                    dialog.show();
                                    new Thread() {
                                        @Override
                                        public void run() {
                                            try {
                                                this.sleep(3000);
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                            Intent data = new Intent();

                                            setResult(RESULT_OK, data);
                                            finish();

                                        }
                                    }.start();
                                } else {
                                    command.success("Identification failed");
                                    textView.setBackgroundColor(Color.parseColor("#7aaa36"));
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
            if (newUid != 0 && fingerTemp != ""  && fingerTemp !="none" && fingerTemp !="null" && fingerTemp != null && fingerTemp != "false") {
                command.success("Start capture");

            }else{
                isRegister = true;
                enrollidx = 0;
                isRegistered = false;
                command.success("You need to press the 3 time fingerprint");
            }

            textView.setBackgroundColor(Color.parseColor("#7aaa36"));

        }catch (FingerprintException e)
        {
            textView.setBackgroundColor(Color.parseColor("#db493b"));
            command.success("Begin capture fail.errorcode:"+ e.getErrorCode() + "err message:" + e.getMessage() + "inner code:" + e.getInternalErrorCode());
        }
    }

    public double compareByteArrays(byte[] a, byte[] b) {
        int n = Math.min(a.length, b.length), nLarge = Math.max(a.length, b.length);
        int unequalCount = nLarge - n;
        for (int i=0; i<n; i++)
            if (a[i] != b[i]) unequalCount++;
        return unequalCount * 100.0 / nLarge;
    }



    public static byte[] convertToBmp24bit(byte[] imageData) {
        Bitmap orgBitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);

        if(orgBitmap == null){
            return null;
        }

        //image size
        int width = orgBitmap.getWidth();
        int height = orgBitmap.getHeight();

        //image dummy data size
        //reason : the amount of bytes per image row must be a multiple of 4 (requirements of bmp format)
        byte[] dummyBytesPerRow = null;
        boolean hasDummy = false;
        int rowWidthInBytes = BYTE_PER_PIXEL * width; //source image width * number of bytes to encode one pixel.
        if(rowWidthInBytes%BMP_WIDTH_OF_TIMES>0){
            hasDummy=true;
            //the number of dummy bytes we need to add on each row
            dummyBytesPerRow = new byte[(BMP_WIDTH_OF_TIMES-(rowWidthInBytes%BMP_WIDTH_OF_TIMES))];
            //just fill an array with the dummy bytes we need to append at the end of each row
            for(int i = 0; i < dummyBytesPerRow.length; i++){
                dummyBytesPerRow[i] = (byte)0xFF;
            }
        }

        //an array to receive the pixels from the source image
        int[] pixels = new int[width * height];

        //the number of bytes used in the file to store raw image data (excluding file headers)
        int imageSize = (rowWidthInBytes+(hasDummy?dummyBytesPerRow.length:0)) * height;
        //file headers size
        int imageDataOffset = 0x36;

        //final size of the file
        int fileSize = imageSize + imageDataOffset;

        //Android Bitmap Image Data
        orgBitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        //ByteArrayOutputStream baos = new ByteArrayOutputStream(fileSize);
        ByteBuffer buffer = ByteBuffer.allocate(fileSize);

        try {
            /*
            BITMAP FILE HEADER Write Start
            */
            buffer.put((byte)0x42);
            buffer.put((byte)0x4D);

            //size
            buffer.put(writeInt(fileSize));

            //reserved
            buffer.put(writeShort((short)0));
            buffer.put(writeShort((short)0));

            //image data start offset
            buffer.put(writeInt(imageDataOffset));

            /* BITMAP FILE HEADER Write End */

            //*******************************************

            /* BITMAP INFO HEADER Write Start */
            //size
            buffer.put(writeInt(0x28));

            //width, height
            //if we add 3 dummy bytes per row : it means we add a pixel (and the image width is modified.
            buffer.put(writeInt(width+(hasDummy?(dummyBytesPerRow.length==3?1:0):0)));
            buffer.put(writeInt(height));

            //planes
            buffer.put(writeShort((short)1));

            //bit count
            buffer.put(writeShort((short)24));

            //bit compression
            buffer.put(writeInt(0));

            //image data size
            buffer.put(writeInt(imageSize));

            //horizontal resolution in pixels per meter
            buffer.put(writeInt(0));

            //vertical resolution in pixels per meter (unreliable)
            buffer.put(writeInt(0));

            buffer.put(writeInt(0));

            buffer.put(writeInt(0));

            /* BITMAP INFO HEADER Write End */
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        int row = height;
        int col = width;
        int startPosition = (row - 1) * col;
        int endPosition = row * col;
        while( row > 0 ){
            for(int i = startPosition; i < endPosition; i++ ){
                buffer.put((byte)(pixels[i] & 0x000000FF));
                buffer.put((byte)((pixels[i] & 0x0000FF00) >> 8));
                buffer.put((byte)((pixels[i] & 0x00FF0000) >> 16));
            }
            if(hasDummy){
                buffer.put(dummyBytesPerRow);
            }
            row--;
            endPosition = startPosition;
            startPosition = startPosition - col;
        }

        return buffer.array();
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
        String postUrl = baseUrl+"api/pos/store_fingerprint";
        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
        JSONObject postData = new JSONObject();

        try {
            postData.put("user_id", newUid);
            //postData.put("fingerprint_template", stringNewTem);
            postData.put("fingerprint_image", strBase64);
            postData.put("token",pos_token);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        //Toast.makeText(getApplicationContext(), "Response: "+ postData, Toast.LENGTH_LONG).show();
        ProgressDialog pd = new ProgressDialog(MainActivity.this);
        pd.setMessage("Enrolling fingerprint...");
        pd.show();

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, postUrl, postData, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                pd.dismiss();

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

                command.success(errorText);
                textView.setBackgroundColor(Color.parseColor("#db493b"));
                isRegister = true;
                enrollidx = 0;
                // clear();

                error.printStackTrace();
            }
        });

        requestQueue.add(jsonObjectRequest);

    }


    private void verifyFingerPrint() {
        String postUrl = baseUrl+"api/pos/verify_fingerprint";
        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
        JSONObject postData = new JSONObject();

        try {
            postData.put("user_id", newUid);
            postData.put("fingerprint_image", strBase64);
            postData.put("token",pos_token);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        //Toast.makeText(getApplicationContext(), "Response: "+ postData, Toast.LENGTH_LONG).show();
        ProgressDialog pd = new ProgressDialog(MainActivity.this);
        pd.setMessage("Verifying fingerprint...");
        pd.show();

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, postUrl, postData, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                pd.dismiss();

                AlertDialog.Builder builder = new AlertDialog.Builder( MainActivity.this);
                LayoutInflater inflater = MainActivity.this.getLayoutInflater();
                builder.setView(inflater.inflate(R.layout.dialog_signin, null));

                AlertDialog dialog = builder.create();
                dialog.show();

                new Thread() {
                    @Override
                    public void run() {
                        try {
                            this.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Intent data = new Intent();
                        Bundle b = new Bundle();

                        data.putExtras(b);
                        setResult(RESULT_OK, data);
                        finish();

                    }
                }.start();

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
                        errorText = "Verification failed, please try again.";
                    }
                }else{
                    errorText = "Verification failed, please try again.";
                }

                command.success(errorText);
                textView.setBackgroundColor(Color.parseColor("#db493b"));
                isRegister = true;
                enrollidx = 0;
                // clear();

                error.printStackTrace();
            }
        });

        requestQueue.add(jsonObjectRequest);

    }
    public void OnBnStop(View view) throws FingerprintException
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

    public void OnBnEnroll(View view) {
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
        Log.d("Zkteco FingerPrint", "clear");

        ZKFingerService.clear();

    }
}
