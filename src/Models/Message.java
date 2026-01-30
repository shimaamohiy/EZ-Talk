package Models;

import Utils.Generator;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

public class Message implements Serializable {
    private final String MessageID;
    private final String senderNumber;
    private final String senderName;
    private final LocalDateTime sendTime;
    private String Text;
    private final Map<String, LocalDateTime> seenBy = new HashMap<>();
    private boolean isReply;
    private boolean isStarred;
    private Message replyTo;

    public Message(String senderNumber, String text, String senderName) {
        MessageID = Generator.generateId("MG");
        this.senderNumber = senderNumber;
        Text = text;
        sendTime= LocalDateTime.now();
        this.senderName = senderName;
        isReply = false;
    }

    public void markSeen(String phoneNumber) {
        seenBy.put(phoneNumber, LocalDateTime.now());
    }

    // === Getters ===
    public String getMessageID() {
        return MessageID;
    }

    public String getMobileNumber() {
        return senderNumber;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getText() {
        return Text;
    }

    public Message getReplyTo() {
        return replyTo;
    }

    public boolean isReply() {
        return isReply;
    }

    public Map<String, LocalDateTime> getSeenBy() {
        return seenBy;
    }

    public boolean isStarred() {
        return isStarred;
    }

    // === Setters ===
    public void setReply(boolean reply) {
        isReply = reply;
    }

    public void setReplyTo(Message replyTo) {
        this.replyTo = replyTo;
    }

    public LocalDateTime getTimestamp() {
        return sendTime;
    }

    public void setStarred(boolean bool){
        isStarred = bool;
    }
}
