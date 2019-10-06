
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Server implements TCPConnectionListener{ //делаем сервер слушателем событий

    public static void main(String [] args){
        new Server(); //создаем сервер (и он сразу же запускается, потому что вызвался конструктор)
    }

    //так как соединений будет несколько, нам понадобится список этих соединений
    private final ArrayList<TCPConnection> connections = new ArrayList<>();
    private List<String> connectionsNames = new ArrayList<>();
    private Timer time = new Timer();
    private boolean isGame;


    public Server() { //создаем конструктор нашего сервера
        System.out.println("Server is running...");
        isGame = false;


        try(ServerSocket serverSocket = new ServerSocket(8190)){ //этот класс умеет слушать какой-то порт и принимать входящее соединение

            while (true){ //в этом безконечном (потому что соединений может быть несколько) цикле серевер принимает водящие соединения

                try {

                    new TCPConnection(this, serverSocket.accept()); //для каждого соединения создаем свой TCPConnection
                    //тепер, как слушателя, мы можем передать себя и сокет, котрый можно вытянуть из ServerSocket при помощи метода accept()


                }catch (IOException e){
                    System.out.println("TCPConnection exception: "+e);
                }
            }

        }catch (IOException e){
            throw new RuntimeException(e);
        }

    }


    //теперь нужно правильно написать реакцию на события
    //так как клиентов и потоков будет очень много, нужно синхронизировать все эти методы,
    //чтобы одновременно нельзя было из разных потоков в них попасть,
    //так как это может нарушить их работу


    @Override
    public synchronized void onConnectionReady(TCPConnection tcpConnection) {
        tcpConnection.setKEY(generateKEY()); //при подключении сгенерируем ключ для клиента
        System.out.println(tcpConnection.getPORT()+"<<<<<<<<<<<<<<<<<<");
        tcpConnection.sendString("Server is running...");
    }

    @Override
    public synchronized void onReceiveCommand(TCPConnection tcpConnection, String msg) {

        if(msg != null) {

            if (tcpConnection.isTimeToGiveANum) {
                try {
                    if(!tcpConnection.isNumberSet) { //если мы уже выбрали число, мы не сможем его поменять
                        int num = Integer.parseInt(msg) + tcpConnection.getKEY(); //к числу, которое ввел игрок прибавляем его ключ в целях шифрования
                        tcpConnection.setNUM(num);
                        tcpConnection.isNumberSet = true;
                        tcpConnection.isTimeToGiveANum = false;
                        System.out.println(tcpConnection.getNAME() + " -- " + num);
                    }
                    else
                        tcpConnection.sendString('\n' + "You have already set your number");

                } catch (NumberFormatException e) {
                    tcpConnection.sendString('\n' + "This is not an int");
                }
            } else {

                String[] command = msg.split("\\s"); //запишем каждое отдельное слово в массив
                boolean isCommand = false;


                if (command.length == 1) {
                    if (command[0].equals("HELP")) { //команда ACK возварщает информацию о всех клиентах, которые ждут игры либо уже играют
                        isCommand = true;
                        String info = "Commands: "
                                      +'\n' + "ACK: returns information about all the players"
                                      +'\n' + "SET <name>: sets your nickname as <name>"
                                      +'\n' + "JOIN <name>: join the game using the nickname <name> of one of the players"
                                      +'\n' + "QUIT: leave the game";

                        tcpConnection.sendString(info);
                    }

                    if (command[0].equals("ACK")) { //команда ACK возварщает информацию о всех клиентах, которые ждут игры либо уже играют
                        isCommand = true;
                        tcpConnection.sendString(connections.toString());
                        System.out.println(connections.toString());

                    }

                    if (command[0].equals("QUIT")) { //комонда QUIT дает возможность покинуть игру
                        isCommand = true;
                        if (exists(String.valueOf(tcpConnection.getNAME()))) { //если клиент еще не подключен к ингре, он не сможет выйти

                            if(!isGame) { //пока идет игра, мы не сможем выйти

                                connectionsNames.remove(tcpConnection.getNAME());
                                connections.remove(tcpConnection);
                                tcpConnection.isConnected = false; //опускаем флаг isConnected
                                tcpConnection.sendString('\n' + "You disconnected");
                                sendToAllConnections('\n' + "Client disconnected: " + tcpConnection);

                            }else tcpConnection.sendString('\n' + "You cannot leave the game until you play with all the players");

                        } else tcpConnection.sendString('\n' + "You are not connected");
                    }


                }
                if (command.length == 2) {
                    if (command[0].equals("JOIN") && command[1].trim().length() != 0) {
                        isCommand = true;

                        if (!tcpConnection.hasName) { //подключится можно будет только тогда, когда имя установлено
                            tcpConnection.sendString('\n' + "Set your name before joining the game");

                        } else {

                            if (tcpConnection.isConnected) { //если клиен уже подключен, он не сможет подключится еще раз
                                tcpConnection.sendString('\n' + "You have connected already");
                            } else {
                                if (!isGame) { //можно подключиться к игре только тогда, когда она еще не началась
                                    if (command[1].equals(String.valueOf(tcpConnection.getNAME()))) {
                                        if (connections.size() == 0) {
                                            tcpConnection.setIPORT(tcpConnection.getPORT());
                                            connections.add(tcpConnection);
                                            connectionsNames.add(tcpConnection.getNAME());
                                            tcpConnection.isConnected = true; //поднимаем флаг isConnected
                                            sendToAllConnections('\n' + "Client connected: " + tcpConnection); //вышлем всем сообщение, что такой-то клиент подключился

                                            Timer();

                                        } else tcpConnection.sendString('\n' + "connections size is not 0..."); //в случае если клиент хочет подключится к игре используя собственный порт, а к игре уже кто-то подключен
                                    } else {
                                        if (exists(command[1])) {

                                            boolean exists = exists(tcpConnection.getNAME());

                                            if (!exists) {
                                                tcpConnection.setIPORT(getAgentPort(command[1]));
                                                connections.add(tcpConnection);
                                                connectionsNames.add(tcpConnection.getNAME());
                                                tcpConnection.isConnected = true;
                                                sendToAllConnections('\n' + "Client connected: " + tcpConnection); //вышлем всем сообщение, что такой-то клиент подключился
                                            } else {
                                                tcpConnection.sendString('\n' + "The name already exists");
                                                tcpConnection.hasName = false;
                                            }

                                        } else tcpConnection.sendString('\n' + "There is no such client name on the server");
                                    }
                                } else tcpConnection.sendString('\n' + "The Game has already started");
                            }
                        }
                    }
                }

                if (command.length == 2) {
                    if (!tcpConnection.isConnected && command[0].equals("SET")) { //клиент может установить/изменить свое имя, пока он не подключен к игре
                        isCommand = true;
                        if (command[1].trim().length() != 0) {
                            tcpConnection.setNAME(command[1]);
                            tcpConnection.hasName = true;
                            tcpConnection.sendString('\n' + "Your name has been set");
                        } else
                            tcpConnection.sendString('\n' + "The name cannot be null");
                    }
                }

                if(!isCommand){
                    tcpConnection.sendString('\n' + "This is not a command");
                    String info = '\n' + "Commands: "
                            +'\n' + "ACK: returns information about all the players"
                            +'\n' + "SET <name>: sets your nickname as <name>"
                            +'\n' + "JOIN <name>: join the game using the nickname <name> of one of the players"
                            +'\n' + "QUIT: leave the game";

                    tcpConnection.sendString(info);
                }
            }
        }
    }

    @Override
    public synchronized void onDisconnect(TCPConnection tcpConnection) {
        connections.remove(tcpConnection); //соответственно, если кто-то отключился, мы его из этого списка убираем
        sendToAllConnections('\n' + "Client disconnected: "+tcpConnection); //аналогично
    }

    @Override
    public synchronized void onException(TCPConnection tcpConnection, Exception e) {
        connectionsNames.remove(tcpConnection.getNAME()); //если случилось исключение и клиент отключился, убираем его имя с коллекции имен
        System.out.println("TCPConnection exception: "+e); //выведем в консоль сообщение об ошибке
    }


    public void onGame() {
        sendToAllConnections('\n' + "The Game begins!");
        sendToAllConnections('\n' + "Number of players in the game: " + connections.size());
        //int size = connections.size();
        List<List<TCPConnection>> pairs = new ArrayList<>();
        List<String> results = new ArrayList<>(); //сюда мы будем добавлять рузультаты всех игр
        List tmp = new ArrayList();
        tmp.addAll(connections);
        pairs = cartesian(tmp);

        for (int i = 0; i < pairs.size(); i++) {

            TCPConnection player1 =  pairs.get(i).get(0);
            TCPConnection player2 =  pairs.get(i).get(1);

            TCPConnection start = null; //игрок, от которого начинается очтет
            TCPConnection other = null; //второй игрок
            TCPConnection winner = null; //победитель

            player1.sendString('\n' + "Please, write an integer");
            player2.sendString('\n' + "Please, write an integer");

            while (!player1.isNumberSet || !player2.isNumberSet) {
                setNum(player1);
                setNum(player2);
            }

                int random = (int) (Math.random() * 2); //перменная random может принимать два значения - 0 или 1, и в зависимости от этого определяем, от кого начнем отчет

                System.out.println("RANDOM: "+ random);

                if (random == 0) {
                    start = player1;
                    other = player2;
                }
                else {
                    start = player2;
                    other = player1;
                }

                int SUM = player1.getNUM() + player2.getNUM() - (player1.getKEY()+player2.getKEY()); //чтобы правильно посчитать сумму, отнимаем от нее сумму ключей игроков

                if(SUM%2 == 0)
                    winner = other;
                else
                    winner = start;

                String result = "-------------------------" +"\n"+ "PLAYER1: "+player1+"\n"+"PLAYER2: "+player2+"\n"+
                        "results: Num1 = "+(player1.getNUM() - player1.getKEY())+", Num2 = "+(player2.getNUM() - player2.getKEY() )+"; start from: "+start+"; winner: "+winner+
                        "\n"+"-------------------------" + "\n";

                player1.sendString('\n' + result);
                player2.sendString('\n' + result);
                results.add(result);


                player1.isNumberSet = false; //опускаем флаг isNumberSet (чтобы можно было вписать другое число в следующей игре)
                player2.isNumberSet = false;

                player1.isTimeToGiveANum = false;
                player2.isTimeToGiveANum = false;

        }

        sendToAllConnections('\n' + "RESULTS:");
        sendToAllConnections('\n' + String.valueOf(results));
        sendToAllConnections('\n' + "Game over");

        isGame = false;

        for (int i = 0; i < connections.size(); i++) {
            connections.get(i).inGame = false;
            connections.get(i).isTimeToGiveANum = false;
            connections.get(i).isNumberSet = false;
        }

        Timer(); //теперь игра запускается рекурсивно
    }


    //сделаем метод, который будет рассылать всем клиентам сообщения
    private void sendToAllConnections(String msg){
        System.out.println(msg);
        for (int i = 0; i < connections.size(); i++) { //пройдемся по циклу
            connections.get(i).sendString(msg); //и вышлем каждому клиенту сообщение
        }
    }

    //сделаем метод, который будет возвращать индивидуальный ключ для каждого клиента
    private int generateKEY(){
        int KEY = (int)(Math.random()*1000);
        return KEY;
    }

    //метод, который проверяет совпадение имени в параметре метода с портами клиентов, которые уже подключились к игре
    public boolean exists(String name){
        boolean exists = false;
        for (int i = 0; i < connections.size(); i++) {
            if (String.valueOf(connections.get(i).getNAME()).equals(name)) {
                exists = true;
                break;
            }
        }
        return exists;
    }

    //метод, который возвращает порт игрока, если имя в параметре совпадает с его именем
    public int getAgentPort(String name){
        for (int i = 0; i < connections.size(); i++) {
            if(String.valueOf(connections.get(i).getNAME()).equals(name)){
                return connections.get(i).getPORT();
            }
        }
        return 0;
    }


    //метод, который создаст Декартово произведение листа connections
    public static List<List<TCPConnection>> cartesian (List <TCPConnection> list){
        List<List<TCPConnection>> result = new ArrayList<List<TCPConnection>>();
        for (int i = 0; i < list.size(); i++) {
            for (int j = 1; j < list.size(); j++) {
                List pair = new ArrayList();
                pair.add(list.get(i));
                pair.add(list.get(j));
                result.add(pair); //он вернет нам коллекцию пар всех игроков, которые будут играть между собой
            }
            list.remove(i);
            i--;
        }
        return result;
    }

    public synchronized void setNum(TCPConnection tcpConnection){
        tcpConnection.isTimeToGiveANum = true;
    }


    //напишем метод, который будет запускать игру
    public void Timer(){
        //sendToAllConnections('\n'+"The Game starts in 10 seconds");
        time  = new Timer();
        time.schedule(new TimerTask() { //и теперь, когда первый игрок подключился, запускаем таймер

            @Override
            public void run() { //ПЕРЕЗАГРУЖАЕМ МЕТОД RUN В КОТОРОМ ДЕЛАЕТЕ ТО ЧТО ВАМ НАДО
                if (connections.size() > 1) { //начнем игру только когда количество игроков будет 2 и больше
                    System.out.println("The Game begins!");
                    System.out.println("Number of players in the game: " + connections.size());
                    for (int i = 0; i < connections.size(); i++) {
                        connections.get(i).inGame = true;
                    }
                    time.cancel();
                    isGame = true;
                    onGame();

                    return;
                }

                System.out.println("Number of players in the game: " + connections.size());
                sendToAllConnections('\n' + "The Game starts in 10 seconds");

                if(connections.size() == 0)
                    time.cancel();
            }
        }, 10000, 10000); //(10000 - ПОДОЖДАТЬ ПЕРЕД НАЧАЛОМ В МИЛИСЕК, ПОВТОРЯТСЯ 10 СЕКУНД (1 СЕК = 1000 МИЛИСЕК))


    }

}