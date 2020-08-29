package kr.hs.dsm_scarfs.shank.socket.service.socket;

import com.corundumstudio.socketio.SocketIOClient;
import kr.hs.dsm_scarfs.shank.socket.payload.MessageRequest;

public interface SocketService {
    void connect(SocketIOClient client);
    void disConnect(SocketIOClient client);
    void joinRoom(SocketIOClient client, String room);
    void chat(SocketIOClient client, MessageRequest messageRequest);
}
