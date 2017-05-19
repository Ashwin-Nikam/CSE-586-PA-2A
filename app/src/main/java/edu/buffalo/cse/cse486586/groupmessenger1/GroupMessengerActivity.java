package edu.buffalo.cse.cse486586.groupmessenger1;

import android.app.Activity;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.telephony.TelephonyManager;
import android.content.Context;
import java.net.ServerSocket;
import android.os.AsyncTask;
import android.util.Log;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.net.Socket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */

public class GroupMessengerActivity extends Activity implements View.OnClickListener {
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static final LinkedList<String> REMOTE_PORTS = new LinkedList<String>();
    static final int SERVER_PORT = 10000;
    static Context appContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        REMOTE_PORTS.add(REMOTE_PORT0);
        REMOTE_PORTS.add(REMOTE_PORT1);
        REMOTE_PORTS.add(REMOTE_PORT2);
        REMOTE_PORTS.add(REMOTE_PORT3);
        REMOTE_PORTS.add(REMOTE_PORT4);


        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */

        try {

            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            Log.e(TAG,"Server socket created");
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);

        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;

        }

        final Button sendButton = (Button) findViewById(R.id.button4);
            sendButton.setOnClickListener(
                    new View.OnClickListener(){
                        @Override
                        public void onClick(View v) {
                            EditText inputText = (EditText) findViewById(R.id.editText1);
                            String input = String.valueOf(inputText.getText());  //input contains the message received from editText1
                            inputText.setText("");
                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, input, myPort);
                        }
                    }

            );



    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }


    @Override
    public void onClick(View v) {

    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        private Uri buildUri(String scheme, String authority) {
            Uri.Builder uriBuilder = new Uri.Builder();
            uriBuilder.authority(authority);
            uriBuilder.scheme(scheme);
            return uriBuilder.build();
        }

        Uri providerUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger1.provider");


        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            Log.e(TAG,"Server Task");
            /*
             * TODO: Fill in your server code that receives messages and passes them
             * to onProgressUpdate().
             */

            //--------------------------------------------------------------------------------

            int key=0;
            for(int i=0;i<9999;i++){       //This loop ensures that messages can be sent 9999 times on both sides
                try {
                    Socket s1 = serverSocket.accept();
                    Log.e(TAG,"Listening...");
                    DataOutputStream dOS = new DataOutputStream(s1.getOutputStream());
                    DataInputStream dIS = new DataInputStream(s1.getInputStream());

                    String message = dIS.readUTF();
                    ContentValues keyValueToInsert = new ContentValues();
                    keyValueToInsert.put("key",Integer.toString(key));
                    keyValueToInsert.put("value",message);
                    Uri newUri = getContentResolver().insert(providerUri,keyValueToInsert);

                    publishProgress(message);
                    Log.e(TAG,"Message Received");
                    dOS.writeUTF("Ack");

                    dOS.flush();
                    dIS.close();
                    dOS.close();
                    s1.close();
                    key++;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            //--------------------------------------------------------------------------------

            return null;

        }

        protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
            String strReceived = strings[0].trim();
            TextView tv = (TextView) findViewById(R.id.textView1);
            tv.append(strReceived + "\t\n");
            Log.e(TAG,"Display 1");

            /*
             * The following code creates a file in the AVD's internal storage and stores a file.
             *
             * For more information on file I/O on Android, please take a look at
             * http://developer.android.com/training/basics/data-storage/files.html
             */

            String filename = "GroupMessengerOutput";
            String string = strReceived + "\n";
            FileOutputStream outputStream;

            try {
                outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                outputStream.write(string.getBytes());
                outputStream.close();
            } catch (Exception e) {
                Log.e(TAG, "File write failed");
            }

            return;
        }
    }

    /***
     * ClientTask is an AsyncTask that should send a string over the network.
     * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
     * an enter key press event.
     *
     * @author stevko
     *
     */
    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {

            for(String remotePort: REMOTE_PORTS){
                try {
                    Log.e(TAG,"Client Task");
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remotePort));  //client socket connects to server socket by connecting to ip:port

                    String msgToSend = msgs[0];
                /*
                 * TODO: Fill in your client code that sends out a message.
                 */

                    //---------------------------------------------------------------------

                    DataInputStream dIS = new DataInputStream(socket.getInputStream());
                    DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());

                    dOut.writeUTF(msgToSend);
                    Log.e(TAG,"Message Sent");
                    dOut.flush();
                    String ack;
                    ack = dIS.readUTF();

                    dIS.close();
                    dOut.close();
                    if(ack.equals("Ack")){
                        socket.close();
                        Log.i(TAG,ack + "Received");
                        Log.i(TAG,"Socket closed");
                    }
                    //---------------------------------------------------------------------

                } catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask UnknownHostException");
                } catch (IOException e) {
                    Log.e(TAG, "ClientTask socket IOException");
                }


            }

            return null;
        }

    }

}
