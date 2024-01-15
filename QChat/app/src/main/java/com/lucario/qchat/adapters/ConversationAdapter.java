package com.lucario.qchat.adapters;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lucario.qchat.databinding.ItemContainerReceivedMessageBinding;
import com.lucario.qchat.databinding.ItemContainerSentMessageBinding;
import com.lucario.qchat.models.ChatMessage;

import java.util.List;

public class ConversationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<ChatMessage> chatMessageList;
    private Bitmap receiverProfileImage;
    private final String senderId;

    public static final int VIEW_TYPE_SENT = 1;
    public static final int VIEW_TYPE_RECEIVED = 2;

    public void setReceiverProfileImage(Bitmap bitmap){
        receiverProfileImage = bitmap;
    }

    @Override
    public int getItemViewType(int position) {
        if (chatMessageList.get(position).getSenderId().equals(senderId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    public ConversationAdapter(List<ChatMessage> chatMessageList, Bitmap receiverProfileImage, String senderId) {
        this.chatMessageList = chatMessageList;
        this.receiverProfileImage = receiverProfileImage;
        this.senderId = senderId;
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (VIEW_TYPE_SENT == viewType) {
            return new SentMessageViewHolder(ItemContainerSentMessageBinding
                    .inflate(LayoutInflater.from(parent.getContext()),
                            parent,
                            false));
        } else {
            return new ReceivedMessageViewHolder(ItemContainerReceivedMessageBinding.inflate(
                    LayoutInflater.from(parent.getContext()),
                    parent,
                    false
            ));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == VIEW_TYPE_SENT) {
            ((SentMessageViewHolder) holder).setData(chatMessageList.get(position));
        } else {
            ((ReceivedMessageViewHolder) holder).setData(chatMessageList.get(position), receiverProfileImage);
        }
    }

    @Override
    public int getItemCount() {
        return chatMessageList.size();
    }

    static class SentMessageViewHolder extends RecyclerView.ViewHolder {

        private final ItemContainerSentMessageBinding binding;

        SentMessageViewHolder(ItemContainerSentMessageBinding itemContainerSentMessageBinding) {
            super(itemContainerSentMessageBinding.getRoot());
            binding = itemContainerSentMessageBinding;
        }

        void setData(ChatMessage chatMessage) {
            binding.textSentMessage.setText(chatMessage.getMessage());
            binding.textDateAndTime.setText(chatMessage.getDateTime());
            binding.textSentMessage.setOnLongClickListener(e->{
                if(binding.textSentMessage.getText().toString().equals(chatMessage.getEncryptedMessage())){
                    binding.textSentMessage.setText(chatMessage.getMessage());
                    return true;
                }
                binding.textSentMessage.setText(chatMessage.getEncryptedMessage());
                return true;
            });
        }
    }

    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {

        private final ItemContainerReceivedMessageBinding binding;

        ReceivedMessageViewHolder(ItemContainerReceivedMessageBinding itemContainerReceivedMessageBinding) {
            super(itemContainerReceivedMessageBinding.getRoot());
            binding = itemContainerReceivedMessageBinding;
        }

        void setData(ChatMessage chatMessage, Bitmap receiverProfile) {
            binding.textReceivedMessage.setText(chatMessage.getMessage());
            binding.textReceivedMessage.setOnLongClickListener(e->{
                if(binding.textReceivedMessage.getText().toString().equals(chatMessage.getEncryptedMessage())){
                    binding.textReceivedMessage.setText(chatMessage.getMessage());
                    return true;
                }
                binding.textReceivedMessage.setText(chatMessage.getEncryptedMessage());
                return true;
            });
            binding.textDateAndTime.setText(chatMessage.getDateTime());
            if (receiverProfile != null) {
                binding.imageSenderProfile.setImageBitmap(receiverProfile);
            }
        }
    }

}
