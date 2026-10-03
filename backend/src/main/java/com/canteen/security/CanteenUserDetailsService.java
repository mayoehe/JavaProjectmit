package com.canteen.security;

import com.canteen.entity.User;
import com.canteen.repository.UserRepository;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

/** Loads users by studentId for Spring Security. Role becomes ROLE_STUDENT / ROLE_ADMIN. */
@Service
public class CanteenUserDetailsService implements UserDetailsService {

  private final UserRepository users;

  public CanteenUserDetailsService(UserRepository users) {
    this.users = users;
  }

  @Override
  public UserDetails loadUserByUsername(String studentId) throws UsernameNotFoundException {
    User u = users.findByStudentId(studentId)
        .orElseThrow(() -> new UsernameNotFoundException("No user: " + studentId));
    boolean enabled = !"DISABLED".equalsIgnoreCase(u.getStatus());
    return new org.springframework.security.core.userdetails.User(
        u.getStudentId(), u.getPasswordHash(), enabled, true, true, true,
        List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole())));
  }
}
