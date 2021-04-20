/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xoservergame;

/**
 *
 * @author ahmed
 */
public class User {

    private final int score;
    private final String userName;
    private final String password;
    private final String state;
    User(String userName,String password,String state,int score){
      this.score=score;
      this.state=state;
      this.userName=userName;
      this.password=password;
    }
    public String getPassword() {
        return password;
    }

    public int getScore() {
        return score;
    }

    public String getState() {
        return state;
    }

    public String getUserName() {
        return userName;
    }
    
}
