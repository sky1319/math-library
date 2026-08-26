package com.example.library.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserAdminRequest {

    @NotBlank(groups = Create.class, message = "用户ID不能为空")
    @Size(max = 64, message = "用户ID不能超过64个字符")
    private String userId;

    @NotBlank(groups = Create.class, message = "姓名不能为空")
    @Size(max = 100, message = "姓名不能超过100个字符")
    private String name;

    @NotBlank(groups = Create.class, message = "初始密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度应为6到64个字符")
    private String password;

    @Pattern(regexp = "USER|LIBRARIAN|ADMIN", message = "角色必须为USER、LIBRARIAN或ADMIN")
    private String role;

    @Email(message = "邮箱格式不正确")
    @Size(max = 255, message = "邮箱不能超过255个字符")
    private String email;

    @Size(max = 32, message = "手机号不能超过32个字符")
    private String phone;

    private Boolean enabled;

    public interface Create { }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
