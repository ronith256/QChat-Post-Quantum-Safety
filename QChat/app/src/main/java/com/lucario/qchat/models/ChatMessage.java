package com.lucario.qchat.models;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class ChatMessage {
    private String senderId;
    private String receiverId;
    private String message;
    private String dateTime;
    private Date dateObject;
    private String conversationId, conversationName, conversationImage;
    private String encryptedMessage;

    public void setEncryptedMessage(String encryptedMessage){
        this.encryptedMessage = encryptedMessage;
    }

    public String getEncryptedMessage(){
        return this.encryptedMessage;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getConversationName() {
        return conversationName;
    }

    public void setConversationName(String conversationName) {
        this.conversationName = conversationName;
    }

    public String getConversationImage() {
        return conversationImage;
    }

    public void setConversationImage(String conversationImage) {
        this.conversationImage = conversationImage;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public Date getDateObject() {
        return dateObject;
    }

    public void setDateObject(Date dateObject) {
        this.dateObject = dateObject;
    }
}
