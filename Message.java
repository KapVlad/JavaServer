package com.kap_vlad.android.javachat;

import java.io.Serializable;

public class Message implements Serializable {
    private MessageType type;    //тип сообщения
    private String emailSender;  //отправитель сообщения
    private String hashPassword; //hash пароля
    private String emailReceiver;//получатель сообщеня
    private String text;         //текст сообщения
    private String date;         //дата создания сообщения
    private long id;             //id сообщения в базе данных сервера

    public Message(MessageType type) { //NAME_REQUEST, NAME_ACCEPTED, INVALID_NAME_OR_PASSWORD
        this.type = type;
    }

    public Message(MessageType type, String emailSender, String hashPassword) { //USER_NAME, REGISTER
        this.type = type;
        this.emailSender = emailSender;
        this.hashPassword = hashPassword;
    }

    public Message(MessageType type, String emailSender, String emailReceiver, String text) { //TEXT_CLIENT
        this.type = type;
        this.emailSender = emailSender;
        this.emailReceiver = emailReceiver;
        this.text = text;
    }

    public Message(MessageType type, String emailSender, String emailReceiver, String text, String date, long id) { //TEXT_SERVER
        this.type = type;
        this.emailSender = emailSender;
        this.emailReceiver = emailReceiver;
        this.text = text;
        this.date = date;
        this.id = id;
    }

    public Message(MessageType type, long id) { //TEXT_SENDED
        this.type = type;
        this.id = id;
    }

    public MessageType getType() {
        return type;
    }

    public String getEmailSender() {
        return emailSender;
    }

    public String getHashPassword() {
        return hashPassword;
    }

    public String getEmailReciever() {
        return emailReceiver;
    }

    public String getText() {
        return text;
    }

    public String getDate() {
        return date;
    }

    public long getId() {
        return id;
    }
}
