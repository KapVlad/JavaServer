package com.kap_vlad.android.javachat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

    private static Map<Long, Connect> connectionMap = new ConcurrentHashMap<>();

    public static void main (String[] args) throws Exception{

        try (ServerSocket serverSocket = new ServerSocket(1100)) { //запускаем на порту 1100
            System.out.println("Сервер запущен");
            while (true) {
                Socket socket = serverSocket.accept();
                Handler handler = new Handler(socket);
                handler.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendBroadcastMessage(Message message) { // отправить сообщение всем участникам чата
        try {
            for (Connect c: connectionMap.values()){
                c.send(message);
            }
        }
        catch (IOException e){
            System.out.println("Сообщение не удалось отправить!");
        }
    }

    private static class Handler extends Thread {
        private Socket socket;
        private DbHandler dbHandler;



        public Handler(Socket socket) {

            this.socket = socket;
            try {
                dbHandler = DbHandler.getInstance();
                System.out.println("Базаданных подключена");
            }
            catch (SQLException e){
                System.out.println("Ошибка базы данных");
            }
        }

        @Override
        public void run () {
            ConsoleHelper.writeMessage("Установлено новое соединение с удаленным адресом " + socket.getRemoteSocketAddress());
            long userId = 0; // создаем пустую переменную для id пользователя

            try (Connect connect = new Connect(socket) ){ // Создаем новое соединение из поля socket

                userId = serverHandshake(connect);  // Вызывае метод, реализующий рукопожатие с клиентом, сохраняя id нового клиента
                serverMainLoop(connect, userId); // Запускаем главный цикл обработки сообщений сервером
            }
            catch (IOException e){
                ConsoleHelper.writeMessage("Произошла ошибка при обмене данными с удаленным адресом.");
            }
            catch (ClassNotFoundException e){
                ConsoleHelper.writeMessage("Произошла ошибка при обмене данными с удаленным адресом.");
            }

            if (userId != 0) connectionMap.remove(userId); // если рукопожатие прошло нормально и мы сохранили имя, удаляем его из connectionMap
            ConsoleHelper.writeMessage("Соединение с абонентом " + dbHandler.getUserNameForId(userId) + " закрыто");
        }

        private long serverHandshake(Connect connect) throws IOException, ClassNotFoundException { //добавление участника в чат

            while (true){ //Если какая-то проверка не прошла, заново запросить имя клиента
                connect.send(new Message(MessageType.NAME_REQUEST)); // отправляем запрос имени
                System.out.println("отправлено сообщение типа: " + MessageType.NAME_REQUEST + " для " + socket.getRemoteSocketAddress());
                Message message = connect.receive(); // получаем ответ клиента и сохраняем в переменную
                System.out.println("клиент ответил: \nтип - " + message.getType() + "\nname: " + message.getEmailSender() + "\npassword: " + message.getHashPassword());
                if (message.getType() == MessageType.USER_NAME &&
                        !message.getEmailSender().isEmpty() && !message.getHashPassword().isEmpty()){ //если получили сообщение с именем пользователя и паролем для входа и оно не пустое

                    if (dbHandler.isUserValid(message.getEmailSender(), message.getHashPassword())){ //проверяем корректность имени пользователя и пароля
                        connectionMap.put(dbHandler.getIdForUserName(message.getEmailSender()), connect); // Добавить нового пользователя и соединение с ним в connectionMap
                        connect.send(new Message(MessageType.NAME_ACCEPTED)); //Отправить клиенту команду информирующую, что его имя принято
                        return dbHandler.getIdForUserName(message.getEmailSender()); //Вернуть id в качестве возвращаемого значения
                    }
                    else {
                        System.out.println("INVALID NAME OR PASSWORD");
                        connect.send(new Message(MessageType.INVALID_NAME_OR_PASSWORD));
                    }
                }
                else if (message.getType() == MessageType.REGISTER &&
                            !message.getEmailSender().isEmpty() && !message.getHashPassword().isEmpty()){ //если пришел запрос на регистрацию нового пользователя
                    if (dbHandler.addUser(message.getEmailSender(), message.getHashPassword())){
                        connectionMap.put(dbHandler.getIdForUserName(message.getEmailSender()), connect); // Добавить нового пользователя и соединение с ним в connectionMap
                        connect.send(new Message(MessageType.NAME_ACCEPTED)); //Отправить клиенту команду информирующую, что его имя принято
                        return dbHandler.getIdForUserName(message.getEmailSender()); //Вернуть id в качестве возвращаемого значения
                    }
                    else {
                        System.out.println("INVALID REGISTRATION");
                        connect.send(new Message(MessageType.INVALID_NAME_OR_PASSWORD));
                    }
                }
            }
        }

        /*private void sendListOfUsers(Connect connect, int userId) throws IOException { //отправка клиенту (новому участнику) информации об остальных клиентах (участниках) чата
            for (int id: connectionMap.keySet()){ //проходимся по списку пользователей
                if (id != userId) connect.send(new Message(MessageType.USER_ADDED, dbHandler.getNickNameForId(id))); // если id не совпадает с id переданного пользователя, то отправляем ему сообщение о nickName других участников
            }
        }*/

        private void serverMainLoop(Connect connect, long userId) throws IOException, ClassNotFoundException { //главный цикл обработки сообщений сервером
            //проверяем есть ли у данного пользователя не досталенные сообщения и если да то отправляем ему их
            List<Long> list = dbHandler.getNonSended(userId);
            if (list != null) {
                for (Long l: list){
                    Message message = dbHandler.getMessageFromId(l);//получаем сообщение из бзы данных
                    connect.send(message);                          //отправляем сообщения
                    Message answer = connect.receive();             //ждем подтверждения
                    if (answer.getType() == MessageType.TEXT_SENDED){
                        dbHandler.messageSended(answer.getId()); //если получили подтверждение меняем значение в б.д. на полученное
                    }
                }
            }


            while (true){
                Message message = connect.receive(); // получаем сообщение от клиента
                if (message.getType() == MessageType.TEXT_CLIENT){ // если тип сообщения ТЕКСТ
                   // String s = dbHandler.getNickNameForId(userId).concat(": ").concat(message.getData()); //формируем строку сообщения из имения пользователя, отправившего сообщение и самого текста
                   // sendBroadcastMessage(new Message(MessageType.TEXT, s));     //отправляем сообщение всем участникам чата с новым текстом
                   // dbHandler.addMassege(userId, 0, message.getData());

                    //получаем id получателя сообщения
                    long id = dbHandler.getIdForUserName(message.getEmailReciever());

                    //добавляем сообщение в базу данных
                    long idMessage = dbHandler.addMessage(message, id);

                    //если пользователь подключен в данный момент, отправляем ему сообщение
                    if (connectionMap.containsKey(id)){
                        Connect con = connectionMap.get(id); //достаем из connectionMap соединение с нужным абонентом
                        //пересобираем сообщение и отправляем получателю
                        Message m = dbHandler.getMessageFromId(idMessage);
                        con.send(m);
                    }

                }
                else if (message.getType() == MessageType.TEXT_SENDED){
                    dbHandler.messageSended(message.getId()); //если получили подтверждение меняем значение в б.д. на полученное
                }
                else {
                    ConsoleHelper.writeMessage("Error!");
                }
            }
        }
    }
}
