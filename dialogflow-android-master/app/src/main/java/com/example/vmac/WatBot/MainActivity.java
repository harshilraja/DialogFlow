package com.example.vmac.WatBot;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;




import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ai.api.AIDataService;
import ai.api.AIListener;
import ai.api.AIServiceContext;
import ai.api.AIServiceContextBuilder;
import ai.api.AIServiceException;
import ai.api.RequestExtras;
import ai.api.android.AIConfiguration;
import ai.api.android.AIService;
import ai.api.android.GsonFactory;
import ai.api.model.AIContext;
import ai.api.model.AIError;
import ai.api.model.AIEvent;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Metadata;
import ai.api.model.Result;
import ai.api.model.Status;
import ai.api.ui.AIDialog;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import static com.ibm.watson.developer_cloud.android.library.audio.MicrophoneHelper.REQUEST_PERMISSION;


public class MainActivity extends AppCompatActivity implements AIListener {


    private RecyclerView recyclerView;
    private ChatAdapter mAdapter;
    private ArrayList messageArrayList;
    private EditText inputMessage;
    private ImageButton btnSend;
    private ImageButton btnRecord;
    //private Map<String,Object> context = new HashMap<>();
    private com.ibm.watson.developer_cloud.conversation.v1.model.Context context = null;
    private boolean initialRequest;
    private boolean permissionToRecordAccepted = false;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static String TAG = "MainActivity";
    private static final int RECORD_REQUEST_CODE = 101;
    private boolean listening = false;
    private Context mContext;
    private String workspace_id;
    private String conversation_username;
    private String conversation_password;
    private String STT_username;
    private String STT_password;
    private String TTS_username;
    private String TTS_password;
    private String analytics_APIKEY;
    private SpeakerLabelsDiarization.RecoTokens recoTokens;
    private static TextToSpeech textToSpeech;
    private AIService aiService;
    private Gson gson = GsonFactory.getGson();
    private AIDataService aiDataService ;
    private String sessionId;
    private AIRequest aiRequest ;
    private AIConfiguration config ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = getApplicationContext();
        conversation_username = mContext.getString(R.string.conversation_username);
        conversation_password = mContext.getString(R.string.conversation_password);
        workspace_id = mContext.getString(R.string.workspace_id);
        STT_username = mContext.getString(R.string.STT_username);
        STT_password = mContext.getString(R.string.STT_password);
        TTS_username = mContext.getString(R.string.TTS_username);
        TTS_password = mContext.getString(R.string.TTS_password);
        analytics_APIKEY = mContext.getString(R.string.mobileanalytics_apikey);

        inputMessage = (EditText) findViewById(R.id.message);
        btnSend = (ImageButton) findViewById(R.id.btn_send);
        btnRecord= (ImageButton) findViewById(R.id.btn_record);


        String customFont = "Montserrat-Regular.ttf";
        Typeface typeface = Typeface.createFromAsset(getAssets(), customFont);
        inputMessage.setTypeface(typeface);
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        messageArrayList = new ArrayList<>();
        mAdapter = new ChatAdapter(messageArrayList);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);
        this.inputMessage.setText("");
        this.initialRequest = true;

        int permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission to record denied");
            makeRequest();
        }
         config = new AIConfiguration("1f007ad3e5bc4c66948233b7e17ade65",
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);
        aiService = AIService.getService(this, config);
        aiService.setListener(this);


        aiRequest = new AIRequest();

        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), recyclerView, new ClickListener() {
            @Override
            public void onClick(View view, final int position) {
                Thread thread = new Thread(new Runnable() {
                    public void run() {
                        Message audioMessage;
                        try {

                            audioMessage =(Message) messageArrayList.get(position);
                            /*streamPlayer = new StreamPlayer();
                            if(audioMessage != null && !audioMessage.getMessage().isEmpty())
                                //Change the Voice format and choose from the available choices
                                streamPlayer.playStream(textToSpeech.synthesize(audioMessage.getMessage(), Voice.EN_LISA).execute());
                            else
                                streamPlayer.playStream(textToSpeech.synthesize("No Text Specified", Voice.EN_LISA).execute());*/

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                thread.start();
            }

            @Override
            public void onLongClick(View view, int position) {
//                recordMessage();

            }
        }));


        btnRecord.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(checkInternetConnection()) {
                    String message = inputMessage.getText().toString().trim();


                    Log.i(TAG, "message: " + message);
                    if (!message.equals("")) {
                        Log.i(TAG, "message 1: " + message);
                        aiRequest.setQuery(message);
                        new AsyncTask<AIRequest,Void,AIResponse>(){

                            @Override
                            protected AIResponse doInBackground(AIRequest... aiRequests) {
                                final AIRequest request = aiRequests[0];
                                try {
                                    final AIResponse response = aiDataService.request(aiRequest);
                                    return response;
                                } catch (AIServiceException e) {
                                }
                                return null;
                            }
                            @Override
                            protected void onPostExecute(AIResponse response) {
                                if (response != null) {

                                    Result result = response.getResult();
                                    String reply = result.getFulfillment().getSpeech();
                                    textToSpeech.speak(reply, TextToSpeech.QUEUE_FLUSH, null);
                                }
                            }
                        }.execute(aiRequest);

                     /* textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null);
                  final List<AIContext> contexts = Collections.singletonList(new AIContext(message));
                    final RequestExtras requestExtras = new RequestExtras(contexts, null);
                    aiService.startListening(requestExtras);
                    aiService.startListening();*/
                    }
                    else {
                        aiService.startListening();
                    }

                    inputMessage.setText("");
                }
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(checkInternetConnection()) {
                    Log.e("TTS", "Button Send Clicked");
                    sendMessage();
                }
            }
        });

        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = textToSpeech.setLanguage(Locale.US);
                    if (result == TextToSpeech.LANG_MISSING_DATA
                            || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "This Language is not supported");
                    } else {
                        btnSend.setEnabled(true);
                        btnRecord.setEnabled(true);
                    }
                }else {
                    Log.e("TTS", "Initilization Failed!");
                }
            }
        });
    };

    // Speech-to-Text Record Audio permission
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
            case RECORD_REQUEST_CODE: {

                if (grantResults.length == 0
                        || grantResults[0] !=
                        PackageManager.PERMISSION_GRANTED) {

                    Log.i(TAG, "Permission has been denied by user");
                } else {
                    Log.i(TAG, "Permission has been granted by user");
                }
                return;
            }

        }
    }

    private void sendMessage() {

        final String inputmessage = this.inputMessage.getText().toString().trim();
        AIServiceContext customContext = AIServiceContextBuilder.buildFromSessionId(sessionId);
        aiDataService = new AIDataService(config,customContext);
        Log.e("TTS", "inputmessage"+inputmessage);
//        if (!this.initialRequest) {
            Log.e("TTS", "inputmessage 1");
            Message inputMessage = new Message();
            inputMessage.setMessage(inputmessage);
            inputMessage.setId("1");
            messageArrayList.add(inputMessage);
            Log.i(TAG, "Sending a message to Watson Conversation Service");

            /*final List<AIContext> contexts = Collections.singletonList(new AIContext(inputmessage));
            final RequestExtras requestExtras = new RequestExtras(contexts, null);
            aiService.startListening(requestExtras);*/
            aiRequest.setQuery(inputmessage);
            new AsyncTask<AIRequest,Void,AIResponse>(){
                private AIError aiError;
                @Override
                protected AIResponse doInBackground(AIRequest... aiRequests) {
                    final AIRequest request = aiRequests[0];
                    try {
                        final AIResponse response = aiDataService.request(aiRequest);
                        return response;
                    } catch (AIServiceException e) {
                    }
                    return null;
                }
                @Override
                protected void onPostExecute(AIResponse response) {
                    if (response != null) {
                        Log.i(TAG, "Received success response"+gson.toJson(response));
                        Result result = response.getResult();
                        String reply = result.getFulfillment().getSpeech();
                        final Message outMessage=new Message();
                        Log.i(TAG, "Speech Text View"+reply);
                        outMessage.setMessage(reply);
                        outMessage.setId("2");
                        messageArrayList.add(outMessage);
                        textToSpeech.speak(reply, TextToSpeech.QUEUE_FLUSH, null);
                    }
                    runOnUiThread(new Runnable() {
                        public void run() {
                            mAdapter.notifyDataSetChanged();
                            if (mAdapter.getItemCount() > 1) {
                                recyclerView.getLayoutManager().smoothScrollToPosition(recyclerView, null, mAdapter.getItemCount()-1);

                            }

                        }
                    });
                }
            }.execute(aiRequest);
        this.inputMessage.setText("");
        mAdapter.notifyDataSetChanged();
    }

    protected void makeRequest() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                RECORD_REQUEST_CODE);
    }

    /**
     * Check Internet Connection
     * @return
     */
    private boolean checkInternetConnection() {
        // get Connectivity Manager object to check connection
        ConnectivityManager cm =
                (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        // Check for network connections
        if (isConnected){
            return true;
        }
        else {
            textToSpeech.speak("No Internet Connection available", TextToSpeech.QUEUE_FLUSH, null);
            return false;
        }

    }

    private void showMicText(final String text) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                inputMessage.setText(text);
            }
        });
    }

    private void enableMicButton() {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                btnRecord.setEnabled(true);
            }
        });
    }

    private void showError(final Exception e) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        });
    }


    @Override
    public void onResult(final AIResponse response) {
        runOnUiThread(new Runnable() {
        @Override
        public void run() {
        Log.d(TAG, "onResult");
        Log.i(TAG, "Received success response"+gson.toJson(response));

        // this is example how to get different parts of result object
        final Status status = response.getStatus();
        Log.i(TAG, "Status code: " + status.getCode());
        Log.i(TAG, "Status type: " + status.getErrorType());

        /* final Result result = response.getResult();
        Log.i(TAG, "Resolved query: " + result.getResolvedQuery());

       Log.i(TAG, "Action: " + result.getAction());
        final String speech = result.getFulfillment().getSpeech();
        Log.i(TAG, "Speech: " + speech);
        inputMessage.setText(speech);
//                TTS.speak(speech);
        textToSpeech.speak(speech, TextToSpeech.QUEUE_FLUSH, null);
        final Metadata metadata = result.getMetadata();
        if (metadata != null) {
            Log.i(TAG, "Intent id: " + metadata.getIntentId());
            Log.i(TAG, "Intent name: " + metadata.getIntentName());
        }

        final HashMap<String, JsonElement> params = result.getParameters();
        if (params != null && !params.isEmpty()) {
            Log.i(TAG, "Parameters: ");
            for (final Map.Entry<String, JsonElement> entry : params.entrySet()) {
                Log.i(TAG, String.format("%s: %s", entry.getKey(), entry.getValue().toString()));
            }
        }*/
            final Message outMessage=new Message();
            final Message inMessage=new Message();
            if(response!=null)
            {
                if(response.getStatus()!=null)
                {
                    final Result result = response.getResult();
                    Log.i(TAG, "Resolved query: " + result.getResolvedQuery());
                    final String inputSpeech = result.getResolvedQuery();
                    final String speech = result.getFulfillment().getSpeech();
                    Log.i(TAG, "Speach Text " + speech);
                    if(null !=speech){
                        inMessage.setMessage(inputSpeech);
                        inMessage.setId("1");
                        outMessage.setMessage(speech);
                        outMessage.setId("2");
                    }
                    messageArrayList.add(inMessage);
                    messageArrayList.add(outMessage);
                    sessionId = response.getSessionId();
                    textToSpeech.speak(speech, TextToSpeech.QUEUE_FLUSH, null);
                }
                runOnUiThread(new Runnable() {
                    public void run() {
                        mAdapter.notifyDataSetChanged();
                        if (mAdapter.getItemCount() > 1) {
                            recyclerView.getLayoutManager().smoothScrollToPosition(recyclerView, null, mAdapter.getItemCount()-1);

                        }

                    }
                });
            }
            }

        });

    }

    @Override
    public void onError(AIError error) {

    }

    @Override
    public void onAudioLevel(float level) {

    }

    @Override
    public void onListeningStarted() {

    }

    @Override
    public void onListeningCanceled() {

    }

    @Override
    public void onListeningFinished() {

    }
}
