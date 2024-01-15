package com.lucario.qchat.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.lucario.qchat.adapters.ConversationAdapter;
import com.lucario.qchat.databinding.ActivityConversationBinding;
import com.lucario.qchat.models.ChatMessage;
import com.lucario.qchat.models.User;
import com.lucario.qchat.networks.ApiClient;
import com.lucario.qchat.networks.ApiService;
import com.lucario.qchat.utilities.BaseActivity;
import com.lucario.qchat.utilities.Constants;
import com.lucario.qchat.utilities.GGH;
import com.lucario.qchat.utilities.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConversationActivity extends BaseActivity {

    private ActivityConversationBinding binding;
    private ConversationAdapter conversationAdapter;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;
    private List<ChatMessage> chatMessageList;
    private User receiverUser;
    private String conversationId;
    private boolean isReceiverAvailable = false;
    private String receiverPhoneNumber = null;

    private byte [] aesKey = new byte[16];

    private HashMap<String, String> conversationAesMap;

//    public static void main (String[] args){
//        String text = "hello world";
//        byte [] key = "secretAESkey1234".getBytes(StandardCharsets.UTF_8);
//        byte [] iv = "secretAESkey1234".getBytes(StandardCharsets.UTF_8);
//        String encryptedText = encryptMessage(text, key, iv);
//        System.out.println(encryptedText);
//        String decryptedText = decryptMessage(encryptedText, key, iv);
//        System.out.println(decryptedText);
//
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityConversationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        loadConversationAESMap();
        loadReceiverDetails();
        setListeners();
        init();
        listMessages();
    }

    private void setListeners() {
        binding.imageBack.setOnClickListener(view -> onBackPressed());
        binding.layoutSend.setOnClickListener(view -> sendMessage());
        binding.imagePhoneCall.setOnClickListener(view -> startPhoneCall());
    }

    private void init() {
        preferenceManager = new PreferenceManager(getApplicationContext());
        chatMessageList = new ArrayList<>();

        //Set adapter to  recyclerview
        conversationAdapter = new ConversationAdapter(
                chatMessageList,
                getBitmapFromEncodedUrl(receiverUser.getImage()),
                preferenceManager.getString(Constants.KEY_USER_ID)
        );
        binding.conversationRecyclerView.setAdapter(conversationAdapter);

        database = FirebaseFirestore.getInstance();
    }

    public static String encryptMessage(String message, byte [] key, byte[] IV) {
        try {
            byte [] keyBytes = Arrays.copyOf(key, 16);
            IV = Arrays.copyOf(IV, 16);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            // Convert String key to SecretKey format
            byte [] plaintext = message.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(IV);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] cipherText = cipher.doFinal(plaintext);
            return java.util.Base64.getEncoder().encodeToString(cipherText);
        } catch (Exception e){
            System.out.println("EXCEPTION MOFO" + e.getMessage());
            return null;
        }
    }


    private void loadConversationAESMap(){
        // Loads the aes key map from local file (serialized file)
        conversationAesMap = new HashMap<>();
        try (FileInputStream fileInputStream = new FileInputStream("keys.map");
             ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)) {

            // Read the HashMap from the file
            conversationAesMap = (HashMap<String, String>) objectInputStream.readObject();
            System.out.println("HashMap loaded successfully!");

        } catch (FileNotFoundException e) {
            System.err.println("File not found: ");
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void saveConversationAESMap(){
        try (FileOutputStream fileOutputStream = new FileOutputStream("keys.map");
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)) {

            // Write the HashMap to the file
            objectOutputStream.writeObject(conversationAesMap);
            System.out.println("HashMap dumped successfully!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String decryptMessage(String message, byte [] key, byte[] IV) {
        try {
            byte [] cipherText = java.util.Base64.getDecoder().decode(message);
            byte [] keyBytes = Arrays.copyOf(key, 16);
            IV = Arrays.copyOf(IV, 16);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(IV);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] decryptedText = cipher.doFinal(cipherText);
            return new String(decryptedText);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    private void sendMessage() {
        if (binding.inputMessage.getText().toString().trim().isEmpty()) {
            return;
        }

        byte [] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        getAESKey();

        String encryptedMessage = encryptMessage(binding.inputMessage.getText().toString(),aesKey, iv);
        System.out.println(encryptedMessage);
        String encryptedIV = GGH.encrypt(iv,receiverUser.getPublicKey());

        HashMap<String, Object> chatMessage = new HashMap<>();
        chatMessage.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        chatMessage.put(Constants.KEY_RECEIVER_ID, receiverUser.getId());
        chatMessage.put(Constants.KEY_MESSAGE, encryptedMessage);
        chatMessage.put(Constants.KEY_TIMESTAMP, new Date());
        chatMessage.put(Constants.KEY_IV, encryptedIV);

        database.collection(Constants.KEY_COLLECTION_CHAT)
                .add(chatMessage);

        if (conversationId != null) {
            updateConversationConversion(binding.inputMessage.getText().toString());
        } else {
            HashMap<String, Object> conversation = new HashMap<>();
            byte [] key = new byte[16];
            new SecureRandom().nextBytes(key);
            conversation.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
            conversation.put(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME));
            conversation.put(Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
            conversation.put(Constants.KEY_RECEIVER_ID, receiverUser.getId());
            conversation.put(Constants.KEY_RECEIVER_NAME, receiverUser.getName());
            conversation.put(Constants.KEY_RECEIVER_IMAGE, receiverUser.getImage());
            conversation.put(Constants.KEY_MESSAGE, binding.inputMessage.getText().toString());
            conversation.put(Constants.KEY_LAST_MESSAGE, binding.inputMessage.getText().toString());
            conversation.put(Constants.KEY_TIMESTAMP, new Date());
            conversation.put(Constants.AES_KEY, GGH.encrypt(key, receiverUser.getPublicKey()));
            addConversion(conversation);
        }

        if (!isReceiverAvailable) {
            try {
                JSONArray tokens = new JSONArray();
                tokens.put(receiverUser.getToken());

                JSONObject data = new JSONObject();
                data.put(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                data.put(Constants.KEY_NAME, preferenceManager.getString(Constants.KEY_NAME));
                data.put(Constants.KEY_FCM_TOKEN, preferenceManager.getString(Constants.KEY_FCM_TOKEN));
                data.put(Constants.KEY_MESSAGE, binding.inputMessage.getText().toString());

                JSONObject body = new JSONObject();
                body.put(Constants.REMOTE_MESSAGE_DATA, data);
                body.put(Constants.REMOTE_MESSAGE_REGISTRATION_IDS, tokens);

                sendNotification(body.toString());

            } catch (Exception e) {
                showToast("sent failed" + e.getMessage());
            }
        }

        if(!conversationAesMap.containsKey(conversationId)){
            conversationAesMap.put(conversationId, new String(aesKey, StandardCharsets.UTF_8));
            saveConversationAESMap();
        }

        binding.inputMessage.setText(null);
    }

    private void listMessages() {
        // Iam sender You are receiver
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverUser.getId())
                .addSnapshotListener(eventListener);

        // You are sender Iam receiver
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, receiverUser.getId())
                .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }

    @SuppressLint("NotifyDataSetChanged")
    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null) {
            return;
        }
        if (value != null) {
            int count = chatMessageList.size();
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.setEncryptedMessage(documentChange.getDocument().getString(Constants.KEY_MESSAGE));
                    chatMessage.setMessage(decryptMessage(documentChange.getDocument().getString(Constants.KEY_MESSAGE), "secretAESkey1234".getBytes(), "secretAESkey1234".getBytes()));
                    chatMessage.setSenderId(documentChange.getDocument().getString(Constants.KEY_SENDER_ID));
                    chatMessage.setReceiverId(documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID));
                    chatMessage.setDateTime(getReadableDateTime(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP)));
                    chatMessage.setDateObject(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                    chatMessageList.add(chatMessage);
                }
            }

            chatMessageList.sort(Comparator.comparing(ChatMessage::getDateObject));
            if (count == 0) {
                conversationAdapter.notifyDataSetChanged();
            } else {
                conversationAdapter.notifyItemRangeInserted(chatMessageList.size(), chatMessageList.size());
                binding.conversationRecyclerView.smoothScrollToPosition(chatMessageList.size() - 1);
            }
            binding.conversationRecyclerView.setVisibility(View.VISIBLE);
        }
        binding.progressBar.setVisibility(View.GONE);

        if (conversationId == null) {
            checkForConversationConversion();
        }
    };

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void sendNotification(String messageBody) {
        ApiClient.getApiClient().create(ApiService.class)
                .sendMessage(Constants.getRemoteMessageHeaders(),
                        messageBody).enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                        if (response.isSuccessful()) {
                            try {
                                if (response.body() != null) {
                                    JSONObject responseJson = new JSONObject(response.body());
                                    JSONArray results = responseJson.getJSONArray("results");
                                    if (responseJson.getInt("failure") == 1) {
                                        JSONObject error = (JSONObject) results.get(0);
                                        showToast(error.getString("error"));
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            // showToast("Notification sent successfully");
                        } else {
                            showToast("Error: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                        showToast(t.getMessage());
                    }
                });
    }

    private void listenAvailabilityOfReceiver() {
        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(receiverUser.getId())
                .addSnapshotListener(ConversationActivity.this, (value, error) -> {
                    if (error != null) {
                        return;
                    }
                    if (value != null) {
                        if (value.getLong(Constants.KEY_USER_AVAILABILITY) != null) {
                            int availability = Objects.requireNonNull(value.getLong(Constants.KEY_USER_AVAILABILITY))
                                    .intValue();
                            isReceiverAvailable = availability == 1;
                        }
                        receiverUser.setToken(value.getString(Constants.KEY_FCM_TOKEN));
                        if (receiverUser.getImage() == null) {
                            receiverUser.setImage(value.getString(Constants.KEY_IMAGE));
                            conversationAdapter.setReceiverProfileImage(getBitmapFromEncodedUrl(receiverUser.getImage()));
                            conversationAdapter.notifyItemRangeChanged(0, chatMessageList.size());
                        }
                    }
                    if (isReceiverAvailable) {
                        // Receiver is available (online)
                        binding.textUserAvailability.setVisibility(View.VISIBLE);
                    } else {
                        //Receiver is not available (offline)
                        binding.textUserAvailability.setVisibility(View.GONE);
                    }

                });
    }

    private void startPhoneCall() {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS).document(receiverUser.getId()).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot snapshot = task.getResult();
                        if (snapshot.exists()) {
                            receiverPhoneNumber = snapshot.getString(Constants.KEY_PHONE);
                            Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + receiverPhoneNumber));
                            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, 1);
                            } else {
                                startActivity(intent);
                            }

                        }
                    }
                });
    }

    void loadReceiverDetails() {
        receiverUser = (User) getIntent().getSerializableExtra(Constants.KEY_USER);
        binding.textUserName.setText(receiverUser.getName());
    }

    private Bitmap getBitmapFromEncodedUrl(String image) {
        if (image != null) {
            byte[] bytes = Base64.decode(image, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }
        return null;
    }

    private String getReadableDateTime(Date date) {
        return new SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault()).format(date);
    }

    private void addConversion(HashMap<String, Object> conversion) {
        database.collection(Constants.KEY_COLLECTION_RECENT_CONVERSATION)
                .add(conversion)
                .addOnSuccessListener(documentReference -> conversationId = documentReference.getId());
    }

    private void checkForConversationConversion() {
        if (chatMessageList.size() != 0) {
            checkForConversionRemotely(preferenceManager.getString(Constants.KEY_USER_ID),
                    receiverUser.getId());
            checkForConversionRemotely(receiverUser.getId(),
                    preferenceManager.getString(Constants.KEY_USER_ID));
        }
    }

    private void updateConversationConversion(String message) {
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_RECENT_CONVERSATION).document(conversationId);
        HashMap<String, Object> update = new HashMap<>();
        update.put(Constants.KEY_LAST_MESSAGE, message);
        update.put(Constants.KEY_TIMESTAMP, new Date());
        documentReference.update(update);
    }

    private void getAESKey(){
        if(conversationAesMap.containsKey(conversationId)){
            aesKey = Objects.requireNonNull(conversationAesMap.get(conversationId)).getBytes(StandardCharsets.UTF_8);
            return;
        }

        try{
            DocumentReference documentReference =
                    database.collection(Constants.KEY_COLLECTION_RECENT_CONVERSATION).document(conversationId);
            documentReference.get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    aesKey = GGH.decrypt(GGH.decodeFromBase64(task.getResult().getString(Constants.AES_KEY),10,10), preferenceManager.getString("ggh-private-key"), preferenceManager.getString("ggh-bad-vector"));
                    conversationAesMap.put(conversationId, new String(aesKey, StandardCharsets.UTF_8));
                }
            });
        } catch (Exception ignored){
            new SecureRandom().nextBytes(aesKey);
        }
    }

    private void checkForConversionRemotely(String senderId, String receiverId) {
        database.collection(Constants.KEY_COLLECTION_RECENT_CONVERSATION)
                .whereEqualTo(Constants.KEY_SENDER_ID, senderId)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverId)
                .get()
                .addOnCompleteListener(conversationOnCompleteListener);
    }

    private final OnCompleteListener<QuerySnapshot> conversationOnCompleteListener = new OnCompleteListener<QuerySnapshot>() {
        @Override
        public void onComplete(@NonNull Task<QuerySnapshot> task) {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0) {
                DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                conversationId = documentSnapshot.getId();
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        listenAvailabilityOfReceiver();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

}