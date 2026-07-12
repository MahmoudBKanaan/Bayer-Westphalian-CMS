import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
public class BcryptHashGenerator {
  public static void main(String[] args) {
    BCryptPasswordEncoder enc = new BCryptPasswordEncoder();
    String raw = args.length > 0 ? args[0] : "password";
    if (args.length > 1) {
      System.out.println(enc.matches(raw, args[1]));
    } else {
      System.out.println(enc.encode(raw));
    }
  }
}
