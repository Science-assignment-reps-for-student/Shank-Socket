package kr.hs.dsm_scarfs.shank.socket.payload;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ErrorResponse {
    private Integer status;
    private String message;
}
