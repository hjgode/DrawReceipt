package com.github.danielfelgar.drawreceipt;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.danielfelgar.drawreceiptlib.ReceiptBuilder;
import com.honeywell.mobility.print.LinePrinter;
import com.honeywell.mobility.print.LinePrinterException;
import com.honeywell.mobility.print.PrintProgressEvent;
import com.honeywell.mobility.print.PrintProgressListener;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase;


public class MainActivity extends AppCompatActivity {

    @BindView(R.id.btDraw)
    Button btDraw;
    @BindView(R.id.ivReceipt)
    ImageView ivReceipt;
    @BindView(R.id.msgText)
    TextView textMsg;
    @BindView(R.id.buttonPrint)
    Button buttonPrint;

    ImageViewTouch imageViewTouch;

    final String filename="receipt.png";
    final String TAG="ReceiptPrinting";

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        imageViewTouch=findViewById(R.id.imageViewTouch);

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},123);
        }

        copyAssetFiles();

        buttonPrint.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view)
            {
                // Create a PrintTask to do printing on a separate thread.
                PrintTask task = new PrintTask();

                // Executes PrintTask with the specified parameter which is passed
                // to the PrintTask.doInBackground method.
                task.execute("PB51", "00:06:66:03:84:C9");
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,  String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 123: {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.length > 0) && grantResults[0]==PackageManager.PERMISSION_GRANTED) {

                } else {
                    // denied
                }
                break;
            }
        }
    }

    @OnClick(R.id.btDraw)
    public void drawReceipt(View view) {
        Bitmap barcode = BitmapFactory.decodeResource(this.getResources(), R.drawable.barcode);

        ReceiptBuilder receipt = new ReceiptBuilder(1200);
        receipt.setMargin(30, 20).
                setAlign(Paint.Align.CENTER).
                setColor(Color.BLACK).
                setTextSize(60).
                setTypeface(this, "fonts/RobotoMono-Regular.ttf").
                addText("LakeFront Cafe").
addText("صلاحية أسطوانة الغاز").
                addText("1234 Main St.").
addText("ہیلو ورلڈ").
                addText("Palo Alto, CA 94568").
                addText("999-999-9999").
                addBlankSpace(30).
                setAlign(Paint.Align.LEFT).
                addText("Terminal ID: 123456", false).
                setAlign(Paint.Align.RIGHT).
                addText("1234").
                setAlign(Paint.Align.LEFT).
                addLine().
                addText("08/15/16", false).
                setAlign(Paint.Align.RIGHT).
                addText("SERVER #4").
                setAlign(Paint.Align.LEFT).
                addParagraph().
                addText("CHASE VISA - INSERT").
                addText("AID: A000000000011111").
                addText("ACCT #: *********1111").
                addParagraph().
                setTypeface(this, "fonts/RobotoMono-Bold.ttf").
                addText("CREDIT SALE").
                addText("UID: 12345678", false).
                setAlign(Paint.Align.RIGHT).
                addText("REF #: 1234").
                setTypeface(this, "fonts/RobotoMono-Regular.ttf").
                setAlign(Paint.Align.LEFT).
                addText("BATCH #: 091", false).
                setAlign(Paint.Align.RIGHT).
                addText("AUTH #: 0701C").
                setAlign(Paint.Align.LEFT).
                addParagraph().
                setTypeface(this, "fonts/RobotoMono-Bold.ttf").
                addText("AMOUNT", false).
                setAlign(Paint.Align.RIGHT).
                addText("$ 15.00").
                setAlign(Paint.Align.LEFT).
                addParagraph().
                addText("TIP", false).
                setAlign(Paint.Align.RIGHT).
                addText("$        ").
                addLine(180).
                setAlign(Paint.Align.LEFT).
                addParagraph().
                addText("TOTAL", false).
                setAlign(Paint.Align.RIGHT).
                addText("$        ").
                addLine(180).
                addParagraph().
                setAlign(Paint.Align.CENTER).
                setTypeface(this, "fonts/RobotoMono-Regular.ttf").
                addText("APPROVED").
                addParagraph().
                addImage(barcode);
        Bitmap bmpReceipt=receipt.build();

        int width=bmpReceipt.getWidth();
        int height=bmpReceipt.getHeight();
        Log.d(TAG, "Receipt size="+width+"x"+height);
        //TODO: If bitmap is smaller, center print?
        ivReceipt.setImageBitmap(bmpReceipt);

        imageViewTouch.setDisplayType(ImageViewTouchBase.DisplayType.FIT_WIDTH);
        imageViewTouch.setImageBitmap(bmpReceipt);

        //save bitmap for printing
        File outputFile = new File(getExternalFilesDir(null), filename);
        try (FileOutputStream out = new FileOutputStream(outputFile)) {
            bmpReceipt.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored
            Log.d(TAG, "bitmap saved to: "+outputFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
/*
        Geocoder coder = new Geocoder(this);
        try {
            List<Address> enderecos = coder.getFromLocation(-22.90827, -47.06501, 1);
            enderecos.isEmpty();
        } catch (IOException e) {
            e.printStackTrace();
        }
*/
    }

    private void copyAssetFiles()
    {
        InputStream input = null;
        OutputStream output = null;
        // Copy the asset files we delivered with the application to a location
        // where the LinePrinter can access them.
        try
        {
            AssetManager assetManager = getAssets();
            //String[] files = {"printer_profiles0.JSON", "honeywell_logo.bmp" };
            //String[] files = {"printer_profiles0.JSON", "sameday_stamp.jpg", "sameday_logo.jpg" };
            String[] files = {"printer_profiles.JSON"};//, "sameday_stamp.bmp", "sameday_logo.bmp" };

            for (String filename : files)
            {
                try{
                    input = assetManager.open(filename);
                    File outputFile = new File(getExternalFilesDir(null), filename);

                    output = new FileOutputStream(outputFile);

                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = input.read(buf)) > 0)
                    {
                        output.write(buf, 0, len);
                    }
                    input.close();
                    input = null;

                    output.flush();
                    output.close();
                    output = null;
                }catch(Exception ex){
                    Log.d("Receipt","Error copying asset file: " + filename);
//                    textMsg.append("Error copying asset file: " + filename);
                }
            }
        }
        catch (Exception ex)
        {
            Log.d("Receipt","Error copying asset files");
            //textMsg.append("Error copying asset files.");
        }
        finally
        {
            try
            {
                if (input != null)
                {
                    input.close();
                    input = null;
                }

                if (output != null)
                {
                    output.close();
                    output = null;
                }
            }
            catch (IOException e){}
        }
    }

    /**
     * This class demonstrates printing in a background thread and updates
     * the UI in the UI thread.
     */
    public class PrintTask extends AsyncTask<String, Integer, String> {
        private static final String PROGRESS_CANCEL_MSG = "Printing cancelled\n";
        private static final String PROGRESS_COMPLETE_MSG = "Printing completed\n";
        private static final String PROGRESS_ENDDOC_MSG = "End of document\n";
        private static final String PROGRESS_FINISHED_MSG = "Printer connection closed\n";
        private static final String PROGRESS_NONE_MSG = "Unknown progress message\n";
        private static final String PROGRESS_STARTDOC_MSG = "Start printing document\n";


        /**
         * Runs on the UI thread before doInBackground(Params...).
         */
        @Override
        protected void onPreExecute()
        {
            // Clears the Progress and Status text box.
            textMsg.setText("");

            // Disables the Print button.
            buttonPrint.setEnabled(false);
            // Disables the Sign button.
            //buttonSign.setEnabled(false);

            // Shows a progress icon on the title bar to indicate
            // it is working on something.
            setProgressBarIndeterminateVisibility(true);
        }

        /**
         * This method runs on a background thread. The specified parameters
         * are the parameters passed to the execute method by the caller of
         * this task. This method can call publishProgress to publish updates
         * on the UI thread.
         */
        @Override
        protected String doInBackground(String... args)
        {
            LinePrinter lp = null;
            String sResult = null;
            String sPrinterID = args[0];
            String sMacAddr = args[1];
            String sDocNumber = "1234567890";

            if (sMacAddr.contains(":") == false && sMacAddr.length() == 12)
            {
                // If the MAC address only contains hex digits without the
                // ":" delimiter, then add ":" to the MAC address string.
                char[] cAddr = new char[17];

                for (int i=0, j=0; i < 12; i += 2)
                {
                    sMacAddr.getChars(i, i+2, cAddr, j);
                    j += 2;
                    if (j < 17)
                    {
                        cAddr[j++] = ':';
                    }
                }

                sMacAddr = new String(cAddr);
            }

            String sPrinterURI = "bt://" + sMacAddr;
            //HGO String sUserText = editUserText.getText().toString();

            LinePrinter.ExtraSettings exSettings = new LinePrinter.ExtraSettings();

            exSettings.setContext(MainActivity.this);

            try
            {
                File profiles = new File (getExternalFilesDir(null), "printer_profiles.JSON");
                if(!profiles.exists()){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textMsg.setText("Missing printer definition JSON file!");
                        }
                    });

                    throw new FileNotFoundException();
                }
                StringBuilder text = new StringBuilder();
                try {
                    BufferedReader br = new BufferedReader(new FileReader(profiles));
                    String line;
                    while ((line = br.readLine()) != null) {
                        text.append(line);
                        text.append('\n');
                    }
                    br.close() ;
                }catch (IOException e) {
                    e.printStackTrace();
                }

                lp = new LinePrinter(
                        profiles.getAbsolutePath(),// text.toString(),// profiles.getAbsolutePath(),
                        sPrinterID,
                        sPrinterURI,
                        exSettings);

                // Registers to listen for the print progress events.
                lp.addPrintProgressListener(new PrintProgressListener() {
                    public void receivedStatus(PrintProgressEvent aEvent)
                    {
                        // Publishes updates on the UI thread.
                        publishProgress(aEvent.getMessageType());
                    }
                });

                //A retry sequence in case the bluetooth socket is temporarily not ready
                int numtries = 0;
                int maxretry = 2;
                while(numtries < maxretry)
                {
                    try{
                        lp.connect();  // Connects to the printer
                        break;
                    }
                    catch(LinePrinterException ex ){
                        numtries++;
                        Thread.sleep(1000);
                    }
                }
                if (numtries == maxretry) lp.connect();//Final retry

                // Prints the Honeywell logo graphic on the receipt if the graphic
                // file exists.
                if(true){
                    //File logoFile = new File (getExternalFilesDir(null), "sameday_logo.jpg");
                    File receiptFile = new File(getExternalFilesDir(null), filename);
                    //get Bitmap width and height
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    //Returns null, sizes are in the options variable
                    BitmapFactory.decodeFile(receiptFile.getAbsolutePath(), options);
                    int width = options.outWidth;
                    int height = options.outHeight;
                    //ie Receipt size=1200x1795
                    int printWidth=5*203; //for a PB51: 5x203
                    int offsetX=0;
                    if(width<printWidth){
                        //add offset
                        offsetX=printWidth-width/2;
                    }
                    if(receiptFile.exists()){
                    lp.writeGraphic(receiptFile.getAbsolutePath(),
                            LinePrinter.GraphicRotationDegrees.DEGREE_0,
                            offsetX,  // Offset in printhead dots from the left of the page
                            5*200, // Desired graphic width on paper in printhead dots, scaling supported
                            height // Desired graphic height on paper in printhead dots
                            );
                    }else{
                        lp.write("Receipt image file not found");
                    }
                    lp.newLine(0);
                }
                sResult = "Number of bytes sent to printer: " + lp.getBytesWritten();
            }
            catch (LinePrinterException ex)
            {
                sResult = "LinePrinterException: " + ex.getMessage();
            }
            catch (Exception ex)
            {
                if (ex.getMessage() != null)
                    sResult = "Unexpected exception: " + ex.getMessage();
                else
                    sResult = "Unexpected exception.";
            }
            finally
            {
                if (lp != null)
                {
                    try
                    {
                        lp.disconnect();  // Disconnects from the printer
                        lp.close();  // Releases resources
                    }
                    catch (Exception ex) {}
                }
            }

            // The result string will be passed to the onPostExecute method
            // for display in the the Progress and Status text box.
            return sResult;
        }

        /**
         * Runs on the UI thread after publishProgress is invoked. The
         * specified values are the values passed to publishProgress.
         */
        @Override
        protected void onProgressUpdate(Integer... values)
        {
            // Access the values array.
            int progress = values[0];

            switch (progress)
            {
                case PrintProgressEvent.MessageTypes.CANCEL:
                    textMsg.append(PROGRESS_CANCEL_MSG);
                    break;
                case PrintProgressEvent.MessageTypes.COMPLETE:
                    textMsg.append(PROGRESS_COMPLETE_MSG);
                    break;
                case PrintProgressEvent.MessageTypes.ENDDOC:
                    textMsg.append(PROGRESS_ENDDOC_MSG);
                    break;
                case PrintProgressEvent.MessageTypes.FINISHED:
                    textMsg.append(PROGRESS_FINISHED_MSG);
                    break;
                case PrintProgressEvent.MessageTypes.STARTDOC:
                    textMsg.append(PROGRESS_STARTDOC_MSG);
                    break;
                default:
                    textMsg.append(PROGRESS_NONE_MSG);
                    break;
            }
        }

        /**
         * Runs on the UI thread after doInBackground method. The specified
         * result parameter is the value returned by doInBackground.
         */
        @Override
        protected void onPostExecute(String result)
        {
            // Displays the result (number of bytes sent to the printer or
            // exception message) in the Progress and Status text box.
            if (result != null)
            {
                textMsg.append(result);
            }

            // Dismisses the progress icon on the title bar.
            setProgressBarIndeterminateVisibility(false);

            // Enables the Print button.
            buttonPrint.setEnabled(true);
            // Enables the Sign button.
            //buttonSign.setEnabled(true);
        }
    } //endofclass PrintTask

}
