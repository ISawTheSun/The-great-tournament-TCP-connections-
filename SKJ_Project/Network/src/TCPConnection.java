import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.Charset;

public class TCPConnection { //он будет работать со строками

    private  final Socket socket; //Socket который связан с соединением
    private final Thread rxThread; //поток слушает входящие сообщения
    //Он постоянно читает поток ввода, и если строчка прилетела, он генерирует событие
    private final BufferedReader in; //поток ввода
    private final BufferedWriter out; //поток вывода (для работы со строками)

    private int KEY; //ключ шифрования, который значение которого будет выдано сервером
    private String NAME = "Nickname"; //имя клиента
    private int PORT;
    private InetAddress IP;
    private int IPORT; //порт клиента, которому вышлем JOIN
    private int NUM = 0;


    public boolean isConnected = false;
    public boolean hasName = false;
    public boolean inGame = false;
    public boolean isTimeToGiveANum = false;
    public boolean isNumberSet = false;

    private final TCPConnectionListener eventListener; //и  теперь добавляем поле слушателя событий


    //создадим два конструктора:


    //первый конструктор ориентирован на то, что кто-то снаружи создаст сокет
    public TCPConnection(final TCPConnectionListener eventListener, Socket socket) throws IOException {

        this.eventListener = eventListener; //предаем событие в конструктор

        this.socket = socket; //нужно получить у этого сокета входящий и исходящий поток,
        //чтобы принимать и писать туда какие-то байты
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), Charset.forName("UTF-8")));
        //можно указать кодировку
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), Charset.forName("UTF-8")));

        PORT = socket.getPort();
        IP = socket.getInetAddress();

        rxThread = new Thread(new Runnable() { //создаем поток, переопределяем метод run()
            @Override
            public void run() {
                try {
                    eventListener.onConnectionReady(TCPConnection.this); //передаем сюда экземпляр нашего класса (соединение установлено)
                    while (!rxThread.isInterrupted()) { //читаем строки в бесконечном цикле (пока мы его не прервем)

                        String msg = in.readLine(); //прочитали строку
                        eventListener.onReceiveCommand(TCPConnection.this, msg); //соединение принимает строку

                    }
                } catch (IOException e) {
                    eventListener.onException(TCPConnection.this, e); //передаем исключение интерфейсу eventListener

                } finally {
                    eventListener.onDisconnect(TCPConnection.this); //исключение случилось по причине обрыва соединения
                }
            }
        });
        rxThread.start(); //и запускаем наш поток
    }

    //и, соответственно, второй ориентирован но то, что сокет создастся внутри
    public TCPConnection(TCPConnectionListener eventListener, String ip, int port) throws Exception{ //а для создания сокета нам требуеться занть IP адрес и номер порта
        this(eventListener, new Socket(ip, port)); //и вызываем в этом конструкторе первый конструктор
    }


    // теперь нам нужны два метода: написать сообщение и оборвать соединение
    // чтобы к этим методам можно было безопасно обращаться с разных потоков, они должны быть синхронизированы


    public synchronized void sendString(String str){
        try {
            out.write(str + "\r\n"); //передаем строку ( "\r\n" - символ новой строки (стандарт Windows))
            out.flush(); //данная команда сбасывает все с буфера и отправляет строку

        } catch (IOException e) {
            eventListener.onException(TCPConnection.this, e);
            disconnect();//если не удалось передать строчку, останавливаем поток (и, соответсвенно, закрываем сокет)
        }
    }

    public synchronized void disconnect(){ //метод нужен, чтобы снаружи мы могли в любой момент оборвать соединение
        rxThread.interrupt(); // прерываем поток
        try {
            socket.close(); //закрываем сокет, что может привести к исключению, котое мы передаем в событие
        } catch (IOException e) {
            eventListener.onException(TCPConnection.this, e);
        }
    }


    public void setKEY(int KEY){
        this.KEY = KEY;
        System.out.println("Key = "+this.KEY);
    }

    public void setIPORT(int IPORT) {
        this.IPORT = IPORT;
    }

    public int getKEY() {
        return KEY;
    }

    public int getPORT() {
        return PORT;
    }


    public void setNAME(String NAME){
        this.NAME = NAME;

        System.out.println(this.NAME);
    }

    public String getNAME() {
        return NAME;
    }

    public void setNUM(int NUM) {
        this.NUM = NUM;
        isNumberSet = true;
    }

    public int getNUM() {
        return NUM;
    }


    //и переопределим toString(), чтобы видеть, кто подключился/отключился

    @Override
    public String toString() {
        return '\n' + "TCPConnection: "+ NAME + " " + IP + " "+ PORT + " -- "+IPORT;
    }
}