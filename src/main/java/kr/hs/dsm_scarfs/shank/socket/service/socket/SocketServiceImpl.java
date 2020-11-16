package kr.hs.dsm_scarfs.shank.socket.service.socket;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import kr.hs.dsm_scarfs.shank.socket.entities.message.Message;
import kr.hs.dsm_scarfs.shank.socket.entities.message.repository.MessageRepository;
import kr.hs.dsm_scarfs.shank.socket.entities.user.User;
import kr.hs.dsm_scarfs.shank.socket.entities.user.UserFactory;
import kr.hs.dsm_scarfs.shank.socket.payload.ErrorResponse;
import kr.hs.dsm_scarfs.shank.socket.payload.MessageRequest;
import kr.hs.dsm_scarfs.shank.socket.payload.MessageResponse;
import kr.hs.dsm_scarfs.shank.socket.security.AuthorityType;
import kr.hs.dsm_scarfs.shank.socket.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class SocketServiceImpl implements SocketService {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserFactory userFactory;

    private final MessageRepository messageRepository;

    private final SocketIOServer server;

    @Override
    public void connect(SocketIOClient client) {
        String token = client.getHandshakeData().getSingleUrlParam("token");
        if (!jwtTokenProvider.validateToken(token)) {
            clientDisconnect(client, 403, "Can not resolve token");
            return;
        }

        User user;
        try {
            user = userFactory.getUser(jwtTokenProvider.getUserEmail(token));
            client.set("user", user);
        } catch (Exception e) {
            clientDisconnect(client, 404, "Could not get user");
            return;
        }


        printLog(
                client,
                String.format("Socket Connected [%s %s], Session Id: %s%n",
                        user.getType(),
                        user.getEmail(),
                        client.getSessionId())
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
    public void joinRoom(SocketIOClient client, String room) {
        int studentId;
        int adminId;
        try {
            studentId = Integer.parseInt(room.split(":")[0]);
            adminId = Integer.parseInt(room.split(":")[1]);
        } catch (NumberFormatException e) {
            clientDisconnect(client, 403, "Can not resolve users ids");
            return;
        }

        User user = client.get("user");
        if (user == null) {
            clientDisconnect(client, 403, "Invalid Connection");
            return;
        } else if (user.getType().equals(AuthorityType.STUDENT) && user.getId().equals(studentId)) {
            client.joinRoom(studentId + ":" + adminId);
        } else if (user.getType().equals(AuthorityType.ADMIN) && user.getId().equals(adminId)) {
            client.joinRoom(studentId + ":" + adminId);
        } else {
            clientDisconnect(client, 403, "Can not join room (permission denied)");
            return;
        }

        String arrow;
        if (user.getType().equals(AuthorityType.STUDENT))
            arrow = "->";
        else
            arrow = "<-";
        printLog(
                client,
                String.format("Join Room [Student(%d) %s Admin(%d)] Session Id: %s%n",
                        studentId, arrow, adminId, client.getSessionId())
        );
    }

    @Override
    public void chat(SocketIOClient client, MessageRequest messageRequest) {
        String[] splitRoom = messageRequest.getRoom().split(":");
        Integer studentId = Integer.parseInt(splitRoom[0]);
        Integer adminId = Integer.parseInt(splitRoom[1]);
        String room = studentId + ":" + adminId;

        if (!client.getAllRooms().contains(room)) {
            clientDisconnect(client, 401, "Permission Denied");
            return;
        }

        User user = client.get("user");
        if (user == null) {
            clientDisconnect(client, 403, "Invalid Connection");
            return;
        } else if (!user.getId().equals(studentId) && !user.getId().equals(adminId)) {
            client.disconnect();
            return;
        }

        Message message = messageRepository.save(
                Message.builder()
                        .studentId(studentId)
                        .adminId(adminId)
                        .message(messageRequest.getMessage())
                        .type(user.getType())
                        .time(LocalDateTime.now(ZoneId.of("Asia/Seoul")))
                        .isDeleted(false)
                        .isShow(false)
                        .build()
        );

        User target;
        try {
            if (message.getType().equals(AuthorityType.STUDENT))
                target = userFactory.getAdmin(message.getAdminId());
            else
                target = userFactory.getStudent(message.getStudentId());
        } catch (Exception e) {
            clientDisconnect(client, 404, "Could not get user");
            return;
        }

        server.getRoomOperations(room).sendEvent("receive", MessageResponse.builder()
                .id(message.getId())
                .name(target.getName())
                .message(message.getMessage())
                .time(message.getTime())
                .type(message.getType())
                .isDeleted(message.isDeleted())
                .target(target.getId())
                .build());

        String arrow;
        if (user.getType().equals(AuthorityType.STUDENT))
            arrow = "->";
        else
            arrow = "<-";

        printLog(
                client,
                String.format("Send Message [Student(%d) %s Admin(%d)] : %s %n",
                        studentId, arrow, adminId, message.getMessage())
        );
    }

    @SneakyThrows
    private void printLog(SocketIOClient client, String content) {
        SimpleDateFormat DateFor = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
        String stringDate= DateFor.parse(LocalDateTime.now(ZoneId.of("Asia/Seoul")).toString().replace("T", " ")).toString();

        System.out.printf(
                "%s  %s - [%s] - %s",
                stringDate,
                "SOCKET",
                client.getRemoteAddress().toString().substring(1),
                content
        );
    }

    private void clientDisconnect(SocketIOClient client, Integer status, String reason) {
        client.sendEvent("error", new ErrorResponse(status, reason));
        client.disconnect();
        printLog(
                client,
                String.format("Socket Disconnected, Session Id: %s - %s%n", client.getSessionId(), reason)
        );
    }

}
