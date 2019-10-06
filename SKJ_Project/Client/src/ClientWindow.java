
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ClientWindow  extends JFrame implements TCPConnectionListener { //сделаем интерфейс клиента


    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ClientWindow(); //создаем нового клиента таким способом
            }
        });
    }

    private JTextArea log = new JTextArea(); //поле с сообщениями
    private JTextField fieldInput = new JTextField(); //поле,куда мы будем писать

    private TCPConnection connection;



    private ClientWindow() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(600, 600);
        setLocationRelativeTo(null); //сделаем так, чтобы окно было всегда в центре
        setAlwaysOnTop(true); //окно всегда будет сверху и не будет упираться в другое окно
        setVisible(true);

        log.setEditable(false);
        log.setLineWrap(true); //сделаем так, чтобы текст переносился на новую строку


        fieldInput.addActionListener(new ActionListener() { //он нам нужен, чтобы читать то, что мы пишем в fieldInput
            @Override
            public void actionPerformed(ActionEvent e) {
                String msg = fieldInput.getText();
                if(msg.equals(""))
                    return;
                fieldInput.setText(null);
                connection.sendString(msg);
            }
        });

        JScrollPane jScrollPane = new JScrollPane(log);

        add(jScrollPane, BorderLayout.CENTER);
        add(fieldInput, BorderLayout.SOUTH);

        try {
            connection = new TCPConnection(this, "localhost", 8190); //подключаемся к серверу
        } catch (Exception e) {
            printMsg("Connection exception: " + e);
        }

    }


    @Override
    public void onConnectionReady(TCPConnection tcpConnection) {
        printMsg("Connection ready...");
    }

    @Override
    public void onReceiveCommand(TCPConnection tcpConnection, String msg) {
        printMsg(msg);
    }

    @Override
    public void onDisconnect(TCPConnection tcpConnection) {
        printMsg("Connection close");
    }

    @Override
    public void onException(TCPConnection tcpConnection, Exception e) {
        printMsg("Connection exception: " + e);
    }


    //напишем метод, который быдет писать в наше текстовое поле log
    //мы с ним будем работать из раздгых потоков, поэтому синхронизируем его

    public synchronized void printMsg(final String msg){
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                log.append(msg + "\n"); //добавляем строчку и символ новой строки
                log.setCaretPosition(log.getDocument().getLength()); //устанавливаем курсок в конец документа(autoscroll может не работать)
            }
        });
    }


}
