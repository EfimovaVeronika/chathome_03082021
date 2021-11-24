package server;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SimpleAuthService implements AuthService {
    private Connection connection;

    private class UserData {
        String login;
        String password;
        String nickname;

        public UserData(String login, String password, String nickname) {
            this.login = login;
            this.password = password;
            this.nickname = nickname;
        }
    }

    private List<UserData> users;

    public SimpleAuthService() {
        try {
            connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/homeChatDb", "postgres", "postgres");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    @Override
    public String getNicknameByLoginAndPassword(String login, String password) {

        try {
            String st = "select nickname from users where (login ILIKE'" + login.toLowerCase() +
                    "') and password = '" + password + "'";
            PreparedStatement preparedStatement = connection.prepareStatement(st);
           ResultSet resultSet =  preparedStatement.executeQuery();
           resultSet.next();
               return resultSet.getString("nickname");

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return null;

    }


    @Override
    public boolean registration(String login, String password, String nickname) {

        try {
           String st = "insert into users (login, password, nickname) values ('" + login.toLowerCase() + "', '" + password + "', '" + nickname + "')";
           PreparedStatement preparedStatement = connection.prepareStatement(st);
           preparedStatement.executeUpdate();
               return true;

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return false;
    }
}






