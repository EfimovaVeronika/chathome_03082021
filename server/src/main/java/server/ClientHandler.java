package server;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class ClientHandler implements Runnable {
    Socket socket;
    Server server;
    DataInputStream in;
    DataOutputStream out;

    private static final Logger logger = LogManager.getLogger(ClientHandler.class);

    private boolean authenticated;
    private String nickname;
    private String login;

    public ClientHandler(Socket socket, Server server) {
        try {
            this.socket = socket;
            this.server = server;

            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("Ошибка сокета");
        }
    }

    @Override
    public void run() {
        try {
            socket.setSoTimeout(120000);

            // цикл аутентификации
            while (true) {

                String str = in.readUTF();

                if (str.equals("/end")) {
                    sendMsg("/end");
                    logger.info("Client disconnected");
                    break;
                }
                if (str.startsWith("/auth ")) {
                    String[] token = str.split("\\s+");
                    nickname = server.getAuthService()
                            .getNicknameByLoginAndPassword(token[1], token[2]);
                    login = token[1];
                    if (nickname != null) {
                        if (!server.isLoginAuthenticated(login)) {
                            sendMsg("/authok " + nickname + " " + login);
                            server.subscribe(this);
                            authenticated = true;
                            socket.setSoTimeout(0);
                            break;
                        } else {
                            logger.info("С этим логином уже вошли");
                            sendMsg("С этим логином уже вошли");
                        }
                    } else {
                        logger.info("Неверный логин / пароль");
                        sendMsg("Неверный логин / пароль");
                    }
                }




                if (str.startsWith("/reg ")) {
                    String[] token = str.split("\\s+");
                    if (token.length < 4) {
                        continue;
                    }

                    boolean regOk = server.getAuthService().
                            registration(token[1], token[2], token[3]);
                    if (regOk) {
                        sendMsg("/regok");
                    } else {
                        sendMsg("/regno");
                    }
                }
            }
            // цикл работы
            while (authenticated) {
                String str = in.readUTF();

                if (str.startsWith("/")) {

                    if (str.startsWith("/rename ")) {
                        String[] token = str.split("\\s+");
                        if (token.length < 2) {
                            continue;
                        }

                        boolean renameOk = server.getAuthService().
                                renameNickName(nickname, token[1]);
                        if (renameOk) {
                            sendMsg("/renameok");
                            logger.info("Пользователь {} cменил(а) никнейм на {}", nickname, token[1]);
                            sendMsg("Пользователь" + nickname + "cменил(а) никнейм на " + token[1]);
                            nickname = token[1];
                            server.broadcastClientList();


                        } else {
                            sendMsg("/renameno");
                        }
                    }

                    if (str.equals("/end")) {
                        sendMsg("/end");
                        logger.info("Client disconnected");
                        break;
                    }

                    if (str.startsWith("/w")) {
                        String[] token = str.split("\\s+", 3);
                        if (token.length < 3) {
                            continue;
                        }
                        server.privateMsg(this, token[1], token[2]);
                    }
                } else {
                    server.broadcastMsg(this, str);
                }
            }
        } catch (SocketTimeoutException e) {
            sendMsg("/end");
            logger.info("Client disconnected");
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("Ошибка сокета");
        } finally {
            server.unsubscribe(this);
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
                logger.error("Ошибка сокета");
            }
        }
    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("Ошибка сокета");
        }
    }

    public String getNickname() {
        return nickname;
    }

    public String getLogin() {
        return login;
    }
}
