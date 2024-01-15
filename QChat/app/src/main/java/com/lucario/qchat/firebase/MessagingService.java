package com.lucario.qchat.firebase;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.lucario.qchat.R;
import com.lucario.qchat.activities.ConversationActivity;
import com.lucario.qchat.models.User;
import com.lucario.qchat.utilities.Constants;

import java.nio.charset.StandardCharsets;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class MessagingService extends FirebaseMessagingService {
    private static final String TAG = "FCM";

    private static String decryptMessage(String encryptedMessage, String secretKey) {
        try {
            // Generate a secret key
            SecretKeySpec key = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");

            // Create a cipher instance and initialize it with the secret key in decryption mode
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);

            // Decode the Base64-encoded encrypted message
            byte[] encryptedBytes = Base64.decode(encryptedMessage, Base64.DEFAULT);

            // Decrypt the message
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

            // Convert the decrypted bytes to a string
            return new String(decryptedBytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);

        User user = new User();
        user.setName(message.getData().get(Constants.KEY_NAME));
        user.setId(message.getData().get(Constants.KEY_USER_ID));
        user.setToken(message.getData().get(Constants.KEY_FCM_TOKEN));

        int notificationId = new Random().nextInt();
        String channelId = "chat_conversation_message";

        String encryptedMessage = message.getData().get(Constants.KEY_MESSAGE);
        String decryptedMessage = decryptMessage(encryptedMessage, "hello");

        Intent intent = new Intent(this, ConversationActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(Constants.KEY_USER, user);
        PendingIntent pendingIntent = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getActivity(this, 0, intent,  PendingIntent.FLAG_MUTABLE);
        }else{
            pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId);
        builder.setSmallIcon(R.drawable.ic_notification);
        builder.setContentTitle(user.getName());
        builder.setContentText(message.getData().get(Constants.KEY_MESSAGE));
        builder.setStyle(new NotificationCompat.BigTextStyle().bigText(
                message.getData().get(Constants.KEY_MESSAGE)
        ));
        builder.setPriority(NotificationCompat.PRIORITY_HIGH);
        builder.setContentIntent(pendingIntent);
        builder.setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence channelName = "Chat Message";
            String channelDescription = "This notification channel is used for conversation";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel notificationChannel = new NotificationChannel(channelId, channelName, importance);
            notificationChannel.setDescription(channelDescription);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(notificationChannel);
        }

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.notify(notificationId, builder.build());
    }
}



