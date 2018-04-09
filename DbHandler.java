package com.kap_vlad.android.javachat;

import org.sqlite.JDBC;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DbHandler {
    // Константа, в которой хранится адрес подключения
    private static final String CON_STR = "jdbc:sqlite:D:\\cs50Final\\db\\chat.db";

    // Используем шаблон одиночка, чтобы не плодить множество
    // экземпляров класса DbHandler
    private static DbHandler instance = null;

    public static synchronized DbHandler getInstance() throws SQLException {
        if (instance == null)
            instance = new DbHandler();
        return instance;
    }

    // Объект, в котором будет храниться соединение с БД
    private Connection connection;

    private DbHandler() throws SQLException {
        // Регистрируем драйвер, с которым будем работать
        // в нашем случае Sqlite
        DriverManager.registerDriver(new JDBC());
        // Выполняем подключение к базе данных
        this.connection = DriverManager.getConnection(CON_STR);
    }


    // Добавление пользователя в БД
    public boolean addUser(String username, String password) {
        // Создадим подготовленное выражение, чтобы избежать SQL-инъекций
        try (PreparedStatement statement = this.connection.prepareStatement(
                "INSERT INTO users(`userName`, `userHash`) " +
                        "VALUES(?, ?)")) {
            statement.setObject(1, username);
            statement.setObject(2, password);
            // Выполняем запрос
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    // Удаление пользователя по id
    public boolean deleteUser(int id) {
        try (PreparedStatement statement = this.connection.prepareStatement(
                "DELETE FROM users WHERE id = ?")) {
            statement.setObject(1, id);
            // Выполняем запрос
            statement.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    //есть ли пользователь с данным именем пользователя и паролем
    public boolean isUserValid (String userName, String password){

        // Statement используется для того, чтобы выполнить sql-запрос
        try (PreparedStatement statement = this.connection.prepareStatement("SELECT id, userHash  FROM users WHERE userName = ?")) {
            statement.setObject(1, userName);
            // В resultSet будет храниться результат нашего запроса,
            // который выполняется командой statement.executeQuery()
            ResultSet resultSet = statement.executeQuery();

            // Проходимся по нашему resultSet (в нашем случае длина set будет 1 или 0, т.к. имя пользователя уникально)
            while (resultSet.next()) {
                if (resultSet.getString("userHash").equals(password)){
                    //если такое имя пользователя есть в базе и хэши совпадают возвращаем true
                    return true;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            // Если произошла ошибка - возвращаем false
            return false;
        }

        //если не нашли имя пользователя с таким именем или хеши не совпадают
        return false;
    }


    //получаем имя пользователя по id
    public String getUserNameForId  (long id){

        // Statement используется для того, чтобы выполнить sql-запрос
        try (PreparedStatement statement = this.connection.prepareStatement("SELECT  userName  FROM users WHERE id = ?")) {
            statement.setObject(1, id);
            // В resultSet будет храниться результат нашего запроса,
            // который выполняется командой statement.executeQuery()
            ResultSet resultSet = statement.executeQuery();

            // Проходимся по нашему resultSet (в нашем случае длина set будет 1 или 0, т.к. id пользователя уникально)
            while (resultSet.next()) {
                    //если такое id пользователя есть в базе возвращаем сооветствующее имя пользоваеля
                    return resultSet.getString("userName");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            // Если произошла ошибка - возвращаем false
        }

        //если не нашли пользователя с таким id
        return null;
    }



    //получаем никнейм пользователя по id
    public long getIdForUserName  (String userName){

        // Statement используется для того, чтобы выполнить sql-запрос
        try (PreparedStatement statement = this.connection.prepareStatement("SELECT  id  FROM users WHERE userName = ?")) {
            statement.setObject(1, userName);
            // В resultSet будет храниться результат нашего запроса,
            // который выполняется командой statement.executeQuery()
            ResultSet resultSet = statement.executeQuery();

            // Проходимся по нашему resultSet (в нашем случае длина set будет 1 или 0, т.к. id пользователя уникально)
            while (resultSet.next()) {
                //если такое id пользователя есть в базе возвращаем сооветствующее имя пользоваеля
                return resultSet.getLong("id");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        //если не нашли пользователя с таким именем пользователя
        return 0;
    }

    // Смена имени пользователя по id
    public boolean changeUserName(int id, String userName) {
        try (PreparedStatement statement = this.connection.prepareStatement(
                "UPDATE users SET userName = ? WHERE id = ?")) {
            statement.setObject(1, userName);
            statement.setObject(2, id);
            // Выполняем запрос
            statement.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }



    // Смена имени пароля по id
    public boolean changePassword(int id, String password) {
        try (PreparedStatement statement = this.connection.prepareStatement(
                "UPDATE users SET userHash = ? WHERE id = ?")) {
            statement.setObject(1, Security.get_SHA_512_SecurePassword(password));
            statement.setObject(2, id);
            // Выполняем запрос
            statement.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    // Добавление сообщения в историю
    public long addMessage(Message message, long idUser) {
        // Создадим подготовленное выражение, чтобы избежать SQL-инъекций
        try (PreparedStatement statement = this.connection.prepareStatement(
                "INSERT INTO messages(`idUser`, `receiver`,  `sender`, `message`) " +
                        "VALUES(?, ?, ?, ?)")) {
            statement.setObject(1, idUser);
            statement.setObject(2, message.getEmailReciever());
            statement.setObject(3, message.getEmailSender());
            statement.setObject(4, message.getText());
            // Выполняем запрос
            statement.execute();

        } catch (SQLException e) {
            e.printStackTrace();
        }


        try (PreparedStatement statement = this.connection.prepareStatement("SELECT  id  FROM messages WHERE rowid=last_insert_rowid()")) {

            // В resultSet будет храниться результат нашего запроса,
            // который выполняется командой statement.executeQuery()
            ResultSet resultSet = statement.executeQuery();

            // Проходимся по нашему resultSet (в нашем случае длина set будет 1 или 0, т.к. id пользователя уникально)
            while (resultSet.next()) {
                //если такое id пользователя есть в базе возвращаем сооветствующее имя пользоваеля
                return resultSet.getLong("id");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        //если не нашли сообщение
        return 0;



    }

    //отмечаем, что сообщение доставлено адресату
    public void messageSended(long idMessage) {
        try (PreparedStatement statement = this.connection.prepareStatement(
                "UPDATE messages SET isSended = ? WHERE id = ?")) {
            statement.setObject(1, true);
            statement.setObject(2, idMessage);
            // Выполняем запрос
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //находим все не доставленные сообщения для id
    public List<Long> getNonSended (long id) {
        // Statement используется для того, чтобы выполнить sql-запрос
        try (PreparedStatement statement = this.connection.prepareStatement("SELECT  id  FROM messages WHERE idUser = ? AND isSended = 0")) {
            statement.setObject(1, id);
            // В resultSet будет храниться результат нашего запроса,
            // который выполняется командой statement.executeQuery()
            ResultSet resultSet = statement.executeQuery();
            List<Long> list = new ArrayList<>();

            // Проходимся по нашему resultSet
            while (resultSet.next()) {
                //заполняем список
                list.add(resultSet.getLong("id"));
            }
            return list;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        //если ничего не нашли
        return null;
    }


    //получаем сообщение из базы данных по id
    public Message getMessageFromId  (long id){

        // Statement используется для того, чтобы выполнить sql-запрос
        try (PreparedStatement statement = this.connection.prepareStatement("SELECT  *  FROM messages WHERE id = ?")) {
            statement.setObject(1, id);
            // В resultSet будет храниться результат нашего запроса,
            // который выполняется командой statement.executeQuery()
            ResultSet resultSet = statement.executeQuery();

            // Проходимся по нашему resultSet (в нашем случае длина set будет 1 или 0, т.к. id  уникально)
            while (resultSet.next()) {
                String sender = resultSet.getString("sender");
                String receiver = resultSet.getString("receiver");
                String text = resultSet.getString("message");
                String date = resultSet.getString("date");

                return new Message(MessageType.TEXT_SERVER, sender, receiver, text, date, id);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        //если не нашли пользователя с таким именем пользователя
        return null;
    }

}
