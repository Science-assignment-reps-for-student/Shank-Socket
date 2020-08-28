package kr.hs.dsm_scarfs.shank.socket.service.socket;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import kr.hs.dsm_scarfs.shank.socket.entities.message.Message;
import kr.hs.dsm_scarfs.shank.socket.entities.message.repository.MessageRepository;
import kr.hs.dsm_scarfs.shank.socket.entities.user.User;
import kr.hs.dsm_scarfs.shank.socket.entities.user.UserFactory;
import kr.hs.dsm_scarfs.shank.socket.payload.MessageRequest;
import kr.hs.dsm_scarfs.shank.socket.payload.MessageResponse;
import kr.hs.dsm_scarfs.shank.socket.security.AuthorityType;
import kr.hs.dsm_scarfs.shank.socket.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SocketServiceImpl implements SocketService {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserFactory userFactory;

    private final MessageRepository messageRepository;

    private final SocketIOServer server;

    @Override
    public void connect(SocketIOClient client) {
        int studentId;
        int adminId;
        try {
            studentId = Integer.parseInt(client.getHandshakeData().getSingleUrlParam("studentId"));
            adminId = Integer.parseInt(client.getHandshakeData().getSingleUrlParam("adminId"));
        } catch (NumberFormatException e) {
            clientDisconnect(client, "Can not resolve users ids");
            return;
        }

        String token = client.getHandshakeData().getSingleUrlParam("token");
        if (!jwtTokenProvider.validateToken(token)) {
            clientDisconnect(client, "Can not resolve token");
            return;
        }
        User user;
        try {
            user = userFactory.getUser(jwtTokenProvider.getUserEmail(token));
        } catch (Exception e) {
            clientDisconnect(client, "Could not get user");
            return;
        }

        client.set("user", user);
        if (user.getType().equals(AuthorityType.STUDENT) && user.getId().equals(studentId)) {
            client.joinRoom(studentId + ":" + adminId);
        } else if (user.getType().equals(AuthorityType.ADMIN) && user.getId().equals(adminId)) {
            client.joinRoom(studentId + ":" + adminId);
        } else {
            clientDisconnect(client, "Can not join room (permission denied)");
            return;
        }

        printLog(
                client,
                String.format("Socket Connected, Session Id: %s%n", client.getSessionId())
        );
    }

    @Override
    public void disConnect(SocketIOClient client) {
        printLog(
                client,
                String.format("Socket Disconnected, Session Id: %s%n", client.getSessionId())
        );
    }

    @Override
    public void chat(SocketIOClient client, MessageRequest messageRequest) {
        int studentId = 0;
        int adminId = 0;
        for (String room : client.getAllRooms()) {
            if (room.length() == 0) continue;
            String[] splitRoom = room.split(":");
            studentId = Integer.parseInt(splitRoom[0]);
            adminId = Integer.parseInt(splitRoom[1]);
        }
        if (studentId == 0 || adminId == 0) {
            client.disconnect();
            return;
        }
        User user = client.get("user");
        if (!user.getId().equals(studentId) && !user.getId().equals(adminId)) {
            client.disconnect();
            return;
        }

        Message message = messageRepository.save(
                Message.builder()
                        .studentId(studentId)
                        .adminId(adminId)
                        .message(messageRequest.getMessage())
                        .type(user.getType())
                        .time(LocalDateTime.now())
                        .isDeleted(false)
                        .isShow(false)
                        .build()
        );

        server.getRoomOperations(studentId + ":" + adminId).sendEvent("receive", MessageResponse.builder()
                .id(message.getId())
                .message(message.getMessage())
                .time(message.getTime())
                .type(message.getType())
                .build());

        printLog(
                client,
                String.format("Send Message [Student: %d, Admin: %d] %s -> %n", studentId, adminId, user.getType())
        );
    }

    private void printLog(SocketIOClient client, String content) {
        Date date = new Date();
        SimpleDateFormat DateFor = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
        String stringDate= DateFor.format(date);

        System.out.printf(
                "%s  %s - [%s] - " + content,
                stringDate,
                "SOCKET",
                client.getRemoteAddress().toString().substring(1)
        );
    }

    private void clientDisconnect(SocketIOClient client, String reason) {
        client.disconnect();
        printLog(
                client,
                String.format("Socket Disconnected, Session Id: %s - %s%n", client.getSessionId(), reason)
        );
    }

}
