package pl.wietwioorki.to22019.dao;

import pl.wietwioorki.to22019.model.User;

import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    private static List<User> users = new ArrayList<>();

    public static void addUser(User user){
        users.add(user);
    }

    public static User findById(long id){
        for(User user : users){
            if(user.getId() == id){
                return user;
            }
        }
        return null;
    }

    public static User findByLogin(String login){
        for(User user: users){
            if(user.getLogin().equals(login)){
                return user;
            }
        }
        return null;
    }
}
