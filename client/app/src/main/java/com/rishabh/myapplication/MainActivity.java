   package com.rishabh.myapplication;

import android.content.Context;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.rishabh.myapplication.utils.Constants;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, KeyEvent.Callback{

    Context context;
    Button playPauseButton;
    Button nextButton;
    Button previousButton;
    TextView mousePad;

    private boolean isConnected=false;
    private boolean mouseMoved=false;
    private Socket socket;
    private PrintWriter out;
    public RelativeLayout relativeLayout;

    private float initX =0;
    private float initY =0;
    private float disX =0;
    private float disY =0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;
        playPauseButton = (Button)findViewById(R.id.playPauseButton);
        nextButton = (Button)findViewById(R.id.nextButton);
        previousButton = (Button)findViewById(R.id.previousButton);

        relativeLayout=(RelativeLayout)findViewById(R.id.activity_main);

        playPauseButton.setOnClickListener(this);
        nextButton.setOnClickListener(this);
        previousButton.setOnClickListener(this);

        mousePad = (TextView)findViewById(R.id.mousePad);
        mousePad.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(isConnected && out!=null){
                    switch(event.getAction()){
                        case MotionEvent.ACTION_DOWN:
                            //save X and Y positions when user touches the TextView
                            initX =event.getX();
                            initY =event.getY();
                            mouseMoved=false;
                            break;
                        case MotionEvent.ACTION_MOVE:
                            disX = event.getX()- initX;
                            disY = event.getY()- initY;
                            initX = event.getX();
                            initY = event.getY();
                            if(disX !=0|| disY !=0){
                                new SendMessage().execute(disX +","+ disY);
                            }
                            mouseMoved=true;
                            break;
                        case MotionEvent.ACTION_UP:
                            //consider a tap only if usr did not move mouse after ACTION_DOWN
                            if(!mouseMoved){
                                new SendMessage().execute(Constants.MOUSE_LEFT_CLICK);
                            }
                    }
                }
                return true;
            }
        });
    }

    public class SendMessage extends AsyncTask<String,Void,Void>{
        @Override
        protected Void doInBackground(String... params) {
            out.println(params[0]);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Log.d("SendMessage","message sent");
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        String str=event.getDisplayLabel()+"";

        Log.d("key event is ",event.getUnicodeChar()+"");
        if(!event.isShiftPressed())
            Log.d("key pressed is ",str.toLowerCase());
        else
            Log.d("key pressed is ",str.toUpperCase());

        if(event.getUnicodeChar()!=0)
            new SendMessage().execute("key-"+event.getUnicodeChar());
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.playPauseButton:
                InputMethodManager inputMethodManager =
                        (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.toggleSoftInputFromWindow(
                        relativeLayout.getApplicationWindowToken(),
                        InputMethodManager.SHOW_FORCED, 0);
                if (isConnected && out!=null) {
                    new SendMessage().execute(Constants.PLAY);
                }
                break;
            case R.id.nextButton:
                if (isConnected && out!=null) {
                    new SendMessage().execute(Constants.NEXT);
                }
                break;
            case R.id.previousButton:
                if (isConnected && out!=null) {
                    new SendMessage().execute(Constants.PREVIOUS);
                }
                break;
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if(id == R.id.action_connect) {
            ConnectPhoneTask connectPhoneTask = new ConnectPhoneTask();
            connectPhoneTask.execute(Constants.SERVER_IP); //try to connect to server in another thread
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public class ConnectPhoneTask extends AsyncTask<String,Void,Boolean> {

        private final String TAG = this.getClass().getSimpleName();

        @Override
        protected Boolean doInBackground(String... params) {
            boolean result = true;
            try {
                InetAddress serverAddr = InetAddress.getByName(params[0]);
                socket = new Socket(serverAddr, Constants.SERVER_PORT);//Open socket on server IP and port
            } catch (IOException e) {
                Log.e(TAG, "Error while connecting", e);
                result = false;
            }
            return result;
        }

        @Override
        protected void onPostExecute(Boolean result)
        {
            isConnected = result;
            Toast.makeText(context,isConnected?"Connected to server!":"Error while connecting", Toast.LENGTH_LONG).show();
            try {
                if(isConnected) {
                    out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket
                            .getOutputStream())), true); //create output stream to send data to server
                }
            }catch (IOException e){
                Log.e(TAG, "Error while creating OutWriter", e);
                Toast.makeText(context,"Error while connecting",Toast.LENGTH_LONG).show();
            }
        }
    }
}
