package kr.hs.dsm_scarfs.shank.socket.controller;

import com.corundumstudio.socketio.SocketIOServer;
import kr.hs.dsm_scarfs.shank.socket.payload.MessageRequest;
import kr.hs.dsm_scarfs.shank.socket.service.socket.SocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@RequiredArgsConstructor
public class SocketController {

    private final SocketIOServer server;

    private final SocketService socketService;

    @PostConstruct
    public void setSocketMapping() {
        server.addConnectListener(socketService::connect);
        server.addDisconnectListener(socketService::disConnect);

        server.addEventListener("send", MessageRequest.class,
                (client, data, ackSender) -> socketService.chat(client, data));

    }

}
