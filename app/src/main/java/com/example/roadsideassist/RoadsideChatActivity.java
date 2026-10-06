package com.example.roadsideassist;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class RoadsideChatActivity extends AppCompatActivity {

    private LinearLayout chatContainer;
    private EditText edtMessage;
    private ScrollView chatScrollView;
    private OkHttpClient client;
    private JSONArray chatHistory;
    
    // Using OpenRouter's auto-routing free model so it always finds a working free model
    private static final String MODEL_NAME = "openrouter/free";
    private static final String API_KEY = BuildConfig.OPENROUTER_API_KEY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatContainer = findViewById(R.id.chatContainer);
        edtMessage = findViewById(R.id.edtMessage);
        Button btnSend = findViewById(R.id.btnSend);
        chatScrollView = findViewById(R.id.chatScrollView);
        
        Button btnFlatTire = findViewById(R.id.btnFlatTire);
        Button btnOverheat = findViewById(R.id.btnOverheat);
        Button btnBattery = findViewById(R.id.btnBattery);
        Button btnNoise = findViewById(R.id.btnNoise);
        
        client = new OkHttpClient();
        chatHistory = new JSONArray();

        String carBrand = getIntent().getStringExtra("CAR_BRAND");
        String carModel = getIntent().getStringExtra("CAR_MODEL");
        String carYear = getIntent().getStringExtra("CAR_YEAR");
        
        String carInfo = (carYear != null ? carYear : "") + " " + 
                         (carBrand != null ? carBrand : "") + " " + 
                         (carModel != null ? carModel : "");
        carInfo = carInfo.trim();
        if (carInfo.isEmpty()) {
            carInfo = "vehicle";
        }

        try {
            JSONObject systemMsg = new JSONObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", "You are an expert roadside assistant. The user is driving a " + carInfo + ". Be concise, helpful, and provide step-by-step assistance for their vehicle issues. Do not use markdown formatting in your responses, use plain text.");
            chatHistory.put(systemMsg);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Initial Greeting
        addMessageBubble("Hi! I see you are driving a " + carInfo + ". I'm your Roadside Assistant. What seems to be the problem today?", false);

        btnSend.setOnClickListener(v -> {
            String userText = edtMessage.getText().toString().trim();
            handleUserMessage(userText);
        });
        
        btnFlatTire.setOnClickListener(v -> handleUserMessage("I have a flat tire."));
        btnOverheat.setOnClickListener(v -> handleUserMessage("My engine is overheating."));
        btnBattery.setOnClickListener(v -> handleUserMessage("I think my battery is dead."));
        btnNoise.setOnClickListener(v -> handleUserMessage("My car is making a strange noise."));
    }
    
    private void handleUserMessage(String userText) {
        if (!userText.isEmpty()) {
            addMessageBubble(userText, true);
            edtMessage.setText("");
            
            // Add a small delay for UI smoothness before calling the API
            chatContainer.postDelayed(() -> callChatbotAPI(userText), 300);
        }
    }

    private void addMessageBubble(String text, boolean isUser) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(16f);
        tv.setPadding(32, 24, 32, 24);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 8, 0, 8);

        if (isUser) {
            params.gravity = Gravity.END;
            tv.setBackgroundColor(Color.parseColor("#DDDDDD"));
            tv.setTextColor(Color.BLACK);
        } else {
            params.gravity = Gravity.START;
            tv.setBackgroundColor(Color.parseColor("#FFF3E0")); // Light orange
            tv.setTextColor(Color.BLACK);
        }
        
        tv.setLayoutParams(params);
        chatContainer.addView(tv);

        // Auto-scroll to bottom
        chatScrollView.post(() -> chatScrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }

    private void callChatbotAPI(String userMessage) {
        try {
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            chatHistory.put(userMsg);

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("model", MODEL_NAME); 
            jsonBody.put("messages", chatHistory);

            RequestBody body = RequestBody.create(
                jsonBody.toString(),
                MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url("https://openrouter.ai/api/v1/chat/completions")
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Content-Type", "application/json")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> addMessageBubble("Network error: Unable to reach the assistant.", false));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            JSONObject responseJson = new JSONObject(response.body().string());
                            String botReply = responseJson.getJSONArray("choices")
                                    .getJSONObject(0)
                                    .getJSONObject("message")
                                    .getString("content");
                            
                            JSONObject botMsg = new JSONObject();
                            botMsg.put("role", "assistant");
                            botMsg.put("content", botReply);
                            chatHistory.put(botMsg);
                            
                            runOnUiThread(() -> addMessageBubble(botReply, false));
                        } catch (JSONException e) {
                            runOnUiThread(() -> addMessageBubble("Error parsing the assistant's response.", false));
                        }
                    } else {
                        runOnUiThread(() -> addMessageBubble("API Error: " + response.code(), false));
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> addMessageBubble("An unexpected error occurred.", false));
        }
    }
}