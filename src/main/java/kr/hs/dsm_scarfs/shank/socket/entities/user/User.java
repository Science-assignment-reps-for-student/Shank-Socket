package kr.hs.dsm_scarfs.shank.socket.entities.user;

import kr.hs.dsm_scarfs.shank.socket.security.AuthorityType;

public interface User {
    Integer getId();
    String getEmail();
    String getPassword();
    String getName();
    String getStudentNumber();
    Integer getStudentClassNumber();
    AuthorityType getType();
}
