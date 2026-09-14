import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class AdminTest {
    public static void main(String[] args) {
        // 直接使用 BCrypt 编码器，不需要注入
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String encoded = encoder.encode("123456");
        System.out.println(encoded);
    }
}