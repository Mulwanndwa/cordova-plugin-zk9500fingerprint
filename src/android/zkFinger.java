package cordova.plugin.zkteco.scan;

import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import java.nio.ByteBuffer;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;

import com.zkteco.android.biometric.core.device.ParameterHelper;
import com.zkteco.android.biometric.core.device.TransportType;
import com.zkteco.android.biometric.core.utils.LogHelper;
import com.zkteco.android.biometric.core.utils.ToolUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.widget.LinearLayout;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import com.zkteco.android.biometric.module.fingerprintreader.FingerprintCaptureListener;
import com.zkteco.android.biometric.module.fingerprintreader.FingerprintSensor;
import com.zkteco.android.biometric.module.fingerprintreader.FingprintFactory;
import com.zkteco.android.biometric.module.fingerprintreader.ZKFingerService;
import com.zkteco.android.biometric.module.fingerprintreader.exception.FingerprintException;

import android.util.Log;
import android.util.Base64;

import android.content.BroadcastReceiver;
import java.io.ByteArrayOutputStream;

public class zkFinger extends CordovaPlugin
{

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

    //String  newUid1 = (String) textViewUid.getText();
    private int newUid = 25;
    private String strBase64 = "";
    public String baseUrl = "https://cmsdemo.placeorder.co.za/";
    public String pos_token = "";
    public String fingerTemp = "";

    public String stringNewTem = "";

    public Boolean isRegistered = false;
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

    int imageCount = 0;

    private CallbackContext command;
//    private ImageView imageView = null;
//    private TextView textView = null;

    LinearLayout layout;

    AlertDialog dialog;

    public final int MY_OP = 11;

    /*
    @Override
    protected void onDestroy() 
    {
        super.onDestroy();
        // Destroy fingerprint sensor when it's not used
        FingerVeinFactory.destroy(fingerVeinSensor);
    }
    */

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
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException
    {

        command = callbackContext;

        //fingerprintSensor = MainActivity.getFingerprintSensor();

//        AlertDialog.Builder builder = new AlertDialog.Builder( cordova.getActivity());
//        LayoutInflater inflater = cordova.getActivity().getLayoutInflater();
//        builder.setView(inflater.inflate(R.layout.dialog_signin, null));
//        dialog = builder.create();
//
//        dialog.show();
//
//        textView = dialog.findViewById(R.id.textView);
//        imageView = dialog.findViewById(R.id.imageView);
        InitDevice();
        try {
            startFingerprintSensor(command);
        } catch (FingerprintException e) {
            throw new RuntimeException(e);
        }
        try
        {

            //@ establish a connection to the device
            //startFingerVeinSensor(callbackContext);

            if (action.equals("scan")) 
            {


                this.OnBnBegin(callbackContext);

                cordova.setActivityResultCallback (this);
                Intent intent = new Intent();
                intent.putExtra("base64", strBase64);
                that = this;
                cordova.startActivityForResult(that, intent, MY_OP);

                return true;
            }
            else if(action.equals("write"))
            {

                return true;

            }
            else if(action.equals("saveTemplate"))
            {


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

    private void startFingerprintSensor(CallbackContext callbackContext) throws FingerprintException {
        // Define output log level
        LogHelper.setLevel(Log.VERBOSE);
        // Start fingerprint sensor
        Map fingerprintParams = new HashMap();
        //set vid
        fingerprintParams.put(ParameterHelper.PARAM_KEY_VID, VID);
        //set pid
        fingerprintParams.put(ParameterHelper.PARAM_KEY_PID, PID);
        fingerprintSensor = FingprintFactory.createFingerprintSensor(cordova.getActivity().getApplicationContext(), TransportType.USB, fingerprintParams);
        ////textView.setText("Begin"+fingerprintSensor);

    }

    private void InitDevice()
    {

        UsbManager musbManager = null;
        //if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB_MR1) {
            musbManager = (UsbManager)cordova.getActivity().getApplicationContext().getSystemService(Context.USB_SERVICE);
        //}
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
        Context context = cordova.getActivity().getApplicationContext().getApplicationContext();
        context.registerReceiver(mUsbReceiver, filter);

        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
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
        //}


    }

    public void OnBnBegin(CallbackContext cbContext) throws FingerprintException{
        try {
            final CallbackContext callbackContext = cbContext;
            if (bstart) return;
            fingerprintSensor.open(0);
            final FingerprintCaptureListener listener = new FingerprintCaptureListener() {

                @Override
                public void captureOK( final  byte[] fpImage) {


                    final int width = fingerprintSensor.getImageWidth();
                    final int height = fingerprintSensor.getImageHeight();

                   cordova.getActivity().runOnUiThread(new Runnable() {
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

                                //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
                                    strBase64 = Base64.encodeToString(byteArray, Base64.NO_WRAP);

                               //}

                                //StoreManager storeManager = new StoreManager(getApplicationContext());

                                //stringNewTem = new String(byteArray);

                                LogHelper.i("strBase64 - "+strBase64);

                                //Intent intent = new Intent();
                                intent.putExtra("base64", strBase64);

                                cordova.startActivityForResult(that, intent, MY_OP);

                                //callbackContext.success(strBase64);


                            }

                            //command.success("FakeStatus:" + fingerprintSensor.getFakeStatus()+"\n"+stringNewTem+"\n"+stringNewTem);
                        }
                    });


                }
                @Override
                public void captureError(FingerprintException e) {
                    final FingerprintException exp = e;
                   cordova.getActivity().runOnUiThread(new Runnable() {
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
                   cordova.getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            callbackContext.error("extract fail, errorcode:" + err);
                        }
                    });
                }

                @Override
                public void extractOK(byte[] fpTemplate){


                    final byte[] tmpBuffer = fpTemplate;

                    //command.success("Capturing...");

                   cordova.getActivity().runOnUiThread(new Runnable() {

                        @SuppressLint("SetTextI18n")
                        @Override

                        public void run() {



                            if(isRegistered){
                                //textView.setText("User ID: "+newUid+". The finger already enrolled.");
                                return;
                            }

                            if (isRegister) {
                                byte[] bufids = new byte[256];
                                int ret = ZKFingerService.identify(tmpBuffer, bufids, 55, newUid);

                                if (ret > 0) {
                                    isRegister = false;
                                    enrollidx = 0;
                                    //textView.setBackgroundColor(Color.parseColor("#7aaa36"));
                                    //textView.setText("The finger already enroll by userId: " + newUid);
                                    return;
                                }


                                if (enrollidx > 0 && ZKFingerService.verify(regtemparray[enrollidx-1], tmpBuffer) <= 0)
                                {
                                    ////textView.setText("Please press the same finger 3 times for the enrollment");
                                    //textView.setBackgroundColor(Color.parseColor("#7aaa36"));
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
                                        //textView.setText("Enroll successful, User ID: " + newUid);
                                        //textView.setBackgroundColor(Color.parseColor("#7aaa36"));
                                    } else {
                                        //textView.setText("Enroll failed");
                                        //textView.setBackgroundColor(Color.parseColor("#db493b"));
                                    }
                                    isRegister = false;
                                    isRegistered = true;

                                } else {
                                    //textView.setText("You need to press the " + (3 - enrollidx) + " time fingerprint");

                                    //textView.setBackgroundColor(Color.parseColor("#7aaa36"));
                                }

                            } else {
                                byte[] bufids = new byte[256];
                                int ret = ZKFingerService.identify(tmpBuffer, bufids, 55, newUid);

                                if (ret > 0) {

                                    String strRes[] = new String(bufids).split("\t");

                                    //textView.setText("Identify success, userid: " + newUid + ", score:" + strRes[1]);
                                    //textView.setBackgroundColor(Color.parseColor("#7aaa36"));

                                } else {
                                    callbackContext.error("Identification failed");
                                    //textView.setBackgroundColor(Color.parseColor("#7aaa36"));
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
                //callbackContext.success("Start capture");

            }else{
                isRegister = true;
                enrollidx = 0;
                isRegistered = false;
                //textView.setText("You need to press the 3 time fingerprint");
            }

        }catch (FingerprintException e)
        {
            //textView.setBackgroundColor(Color.parseColor("#db493b"));
            command.error("Begin capture fail.errorcode:"+ e.getErrorCode() + "err message:" + e.getMessage() + "inner code:" + e.getInternalErrorCode());
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
            if( resultCode == Activity.RESULT_OK && data.hasExtra("base64") )
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
