

public interface TCPConnectionListener { //интерфейс для определения событий, которые могут возникать в TCPConnection

    // Что может случиться:

    void onConnectionReady(TCPConnection tcpConnection); //событие может быть готовым (когда мы запустили соединение)

    void onReceiveCommand(TCPConnection tcpConnection, String command); //соединение приняло строчку

    void onDisconnect(TCPConnection tcpConnection); //соединение прервалось

    void onException (TCPConnection tcpConnection, Exception e); //случилось исключение

}
