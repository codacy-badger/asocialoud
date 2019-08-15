package org.yardimci.asocialoud.members.db.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "tbl_members")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true)
    private String loginName;


    @Column(name = "realname", nullable = false)
    private String realName;

    @Column(name = "email", nullable = false)
    private String email;

    //@JsonProperty(access = Access.WRITE_ONLY)
    @Column(name = "password", nullable = false)
    private String password;

    @JsonIgnore
    @OneToMany(/*mappedBy = "memberToFollow",*/ orphanRemoval = true, cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<FollowData> followDataList;

    @Transient
    private String token;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @JsonIgnore
    public String getPassword() {
        return password;
    }

    @JsonProperty
    public void setPassword(String password) {
        this.password = password;
    }

    public List<FollowData> getFollowDataList() {
        return followDataList;
    }

    public void setFollowDataList(List<FollowData> followDataList) {
        this.followDataList = followDataList;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        if (obj instanceof Member) {
            if (((Member) obj).getLoginName().equals(this.getLoginName())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.getLoginName());
    }

    @Override
    public String toString() {
        return String.format("Member [id=%s, name=%s, loginName=%s]", id, realName, loginName);
    }

}
