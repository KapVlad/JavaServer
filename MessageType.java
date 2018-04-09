package com.kap_vlad.android.javachat;

public enum MessageType {
    NAME_REQUEST, //запрос имени и пароля
    USER_NAME,    //имя пользователя
    NAME_ACCEPTED,//имя принято
    TEXT_CLIENT,  //текстовое сообщение от клиента к серверу
    TEXT_SERVER,  //текстовое сообщение от сервера клинту
    TEXT_SENDED,  //сообщает серверу, что сообщение доставлено


    INVALID_NAME_OR_PASSWORD, //неверное имя или пароль
    REGISTER                 //регистрация нового пользователя

}
