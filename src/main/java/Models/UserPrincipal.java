package Models;

/* import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails; */

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class UserPrincipal /* implements UserDetails */ {
    private User user;

    public UserPrincipal(User user){
        this.user = user;
    }

    /* @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>(); */

        // Extract list of permissions (name)
        /* this.user.getPermissionList().forEach(p -> {
            GrantedAuthority authority = new SimpleGrantedAuthority(p);
            authorities.add(authority);
        }); */
        /* GrantedAuthority authority1 = new SimpleGrantedAuthority("READ");
        authorities.add(authority1);
        GrantedAuthority authority2 = new SimpleGrantedAuthority("WRITE");
        authorities.add(authority2); */

        // Extract list of roles (ROLE_name)
        /* this.user.getRoleList().forEach(r -> {
            GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + r);
            authorities.add(authority);
        }); */
        /* GrantedAuthority authority3 = new SimpleGrantedAuthority("ROLE_" + "USER");
        authorities.add(authority3);
        GrantedAuthority authority4 = new SimpleGrantedAuthority("ROLE_" + "ADMIN");
        authorities.add(authority4);

        return authorities;
    }

    @Override
    public String getPassword() {
        return this.user.getPassword();
    }

    @Override
    public String getUsername() {
        return this.user.getUserName();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        // return this.user.getActive() == 1;
        return true;
    } */
}
